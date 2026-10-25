package app.havenos.launcher.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

/**
 * Launcher catalog entry. key = "$pkg/$cls#$serial" — distinct per profile,
 * so work-profile apps get distinct keys and their own home placements.
 */
data class AppEntry(
    val key: String,
    val label: String,
    val pkg: String,
    val cls: String,
    val user: UserHandle,
    /** Raw platform category (ApplicationInfo.CATEGORY_*), read once at catalog query time. */
    val category: Int = android.content.pm.ApplicationInfo.CATEGORY_UNDEFINED,
    /** First-install time (ms), read once at catalog query time. 0 when unknown. */
    val installTime: Long = 0L,
)

/** Map a raw platform category to a drawer category id. */
fun categoryIdOf(platformCategory: Int): String = when (platformCategory) {
    android.content.pm.ApplicationInfo.CATEGORY_GAME -> "game"
    android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "social"
    android.content.pm.ApplicationInfo.CATEGORY_AUDIO -> "audio"
    android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> "video"
    android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> "image"
    android.content.pm.ApplicationInfo.CATEGORY_NEWS -> "news"
    android.content.pm.ApplicationInfo.CATEGORY_MAPS -> "maps"
    android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "productivity"
    else -> "other"
}

/**
 * Conservative package heuristic, used ONLY when the platform category
 * is undefined (the common case: most apps ship no category, so the
 * platform value alone left nearly everything in "other"). Only
 * unambiguous, well-known packages are mapped; everything else stays
 * "other" rather than guessed. This is a hand-written list of generic
 * app identities, not data taken from any reference launcher.
 */
fun heuristicCategoryForPackage(pkg: String): String = when (pkg) {
    "com.google.android.apps.maps", "com.waze" -> "maps"
    "com.whatsapp", "com.telegram.messenger", "org.thoughtcrime.securesms",
    "com.facebook.orca", "com.instagram.android", "com.facebook.katana" -> "social"
    "com.spotify.music", "com.google.android.apps.youtube.music" -> "audio"
    "com.google.android.youtube", "com.netflix.mediaclient" -> "video"
    "com.google.android.apps.photos" -> "image"
    "com.google.android.gm", "com.microsoft.office.outlook" -> "productivity"
    else -> "other"
}

/**
 * The drawer category for an app: the platform category when set,
 * otherwise the conservative package heuristic, otherwise "other".
 * User renames (catNamesJson) only change display names, never this id.
 */
fun derivedCategoryId(entry: AppEntry): String {
    val platform = categoryIdOf(entry.category)
    return if (platform != "other") platform else heuristicCategoryForPackage(entry.pkg)
}

/** Parse the category-merge map (category id -> target category id). Never throws. */
fun parseCatMerges(json: String): Map<String, String> = try {
    val obj = JSONObject(json)
    val out = LinkedHashMap<String, String>()
    for (k in obj.keys()) {
        val v = obj.optString(k, "")
        if (k.isNotBlank() && v.isNotBlank() && v != k) out[k] = v
    }
    out
} catch (t: Throwable) {
    emptyMap()
}

fun encodeCatMerges(merges: Map<String, String>): String {
    val obj = JSONObject()
    for ((k, v) in merges) {
        if (k.isNotBlank() && v.isNotBlank() && v != k) obj.put(k, v)
    }
    return obj.toString()
}

/**
 * Final drawer category for an app: the derived category, resolved
 * through at most one user merge hop (a merge chain or cycle resolves
 * to the first hop's target and stops — merges never loop).
 */
fun categoryIdFor(entry: AppEntry, mergeJson: String): String {
    val derived = derivedCategoryId(entry)
    return parseCatMerges(mergeJson)[derived] ?: derived
}

class AppRepository(ctx: Context, private val settings: Settings? = null) {
    private val appCtx = ctx.applicationContext
    private val pm = appCtx.packageManager
    private val um: UserManager? = appCtx.getSystemService(UserManager::class.java)
    private val launcherApps: LauncherApps? = appCtx.getSystemService(LauncherApps::class.java)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    private val iconCache = LruCache<String, Drawable>(64)
    private val bitmapCache = LruCache<String, android.graphics.Bitmap>(256)

    /** Launch stats: app key -> (count, lastLaunchMs). Drives recents + ranking. */
    private val _stats = MutableStateFlow<Map<String, Pair<Int, Long>>>(emptyMap())
    val stats: StateFlow<Map<String, Pair<Int, Long>>> = _stats

    /** Active icon-pack package ("" = system icons); set from the UI when the setting changes. */
    @Volatile
    var activePack: String = ""

    /** Serializes stats seeding, updates and persistence (all on [scope]). */
    private val statsMutex = kotlinx.coroutines.sync.Mutex()
    private var statsSeeded = false

    /** Seed stats from durable storage exactly once; called with [statsMutex] held. */
    private suspend fun seedStatsLocked() {
        if (statsSeeded) return
        statsSeeded = true
        val s = settings ?: return
        _stats.value = try {
            parseLaunchStats(s.persistedState().launchStatsJson)
        } catch (t: Throwable) {
            emptyMap()
        }
    }

    init {
        refresh()
        if (settings != null) {
            scope.launch {
                statsMutex.withLock { seedStatsLocked() }
            }
        }
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    refresh()
                }
            }
            if (Build.VERSION.SDK_INT >= 33) {
                appCtx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                appCtx.registerReceiver(receiver, filter)
            }
        } catch (t: Throwable) {
            // Never crash the app over broadcast registration.
        }
    }

    fun refresh() {
        scope.launch {
            _apps.value = query()
        }
    }

    private fun query(): List<AppEntry> {
        val out = mutableListOf<AppEntry>()
        val seen = HashSet<String>()
        val me = Process.myUserHandle()
        val profiles = um?.userProfiles ?: listOf(me)
        val la = launcherApps ?: return emptyList()
        for (user in profiles) {
            val infos = try {
                la.getActivityList(null, user)
            } catch (t: Throwable) {
                continue // profile may have vanished
            }
            val serial = try {
                um?.getSerialNumberForUser(user) ?: 0L
            } catch (t: Throwable) {
                0L
            }
            // M5: per-app label overrides (long-press icon -> Edit label).
            val labelOverrides = try {
                settings?.state?.value?.let { parseLabelOverrides(it.labelOverridesJson) }
                    ?: emptyMap()
            } catch (t: Throwable) {
                emptyMap()
            }
            for (info in infos) {
                val cn = info.componentName
                val pkg = cn.packageName
                val cls = cn.className
                val key = "$pkg/$cls#$serial"
                if (!seen.add(key)) continue
                val platformLabel = try {
                    info.label?.toString().orEmpty()
                } catch (t: Throwable) {
                    ""
                }
                val label = labelOverrides[key] ?: platformLabel
                // Category + install time are PackageManager binder calls;
                // query() runs on Dispatchers.Default, so read them once here
                // instead of per-composition in the drawer.
                val category = try {
                    info.applicationInfo.category
                } catch (t: Throwable) {
                    android.content.pm.ApplicationInfo.CATEGORY_UNDEFINED
                }
                val installTime = try {
                    pm.getPackageInfo(pkg, 0).firstInstallTime
                } catch (t: Throwable) {
                    0L
                }
                out += AppEntry(key, label.ifBlank { pkg }, pkg, cls, user, category, installTime)
            }
        }
        // Personal profile first, then label order.
        return out.sortedWith(compareBy({ it.user != me }, { it.label.lowercase() }))
    }

    fun launch(e: AppEntry) {
        recordLaunch(e)
        try {
            val la = launcherApps ?: throw IllegalStateException("LauncherApps unavailable")
            la.startMainActivity(ComponentName(e.pkg, e.cls), e.user, null, null)
        } catch (t: Throwable) {
            try {
                pm.getLaunchIntentForPackage(e.pkg)?.let {
                    appCtx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            } catch (t2: Throwable) {
                // app gone; nothing to do
            }
        }
    }

    fun recordLaunch(e: AppEntry) {
        // Confined to the repo scope under the stats mutex: seeding can never
        // be skipped by an early launch, and concurrent launches cannot lose
        // counts or persist out-of-order snapshots.
        scope.launch {
            statsMutex.withLock {
                seedStatsLocked()
                val now = System.currentTimeMillis()
                val updated = _stats.value.toMutableMap()
                val prev = updated[e.key]
                updated[e.key] = (prev?.first ?: 0) + 1 to now
                _stats.value = updated
                settings?.setLaunchStatsJson(encodeLaunchStats(updated))
            }
        }
    }

    /** Clear in-memory launch stats (Settings → Search → Reset ranking). */
    fun clearStats() {
        scope.launch {
            statsMutex.withLock {
                seedStatsLocked()
                _stats.value = emptyMap()
            }
        }
    }

    /** Most recently launched entries (dock recents), excluding [exclude] keys. */
    fun recents(limit: Int, exclude: Set<String>): List<AppEntry> {
        val byKey = _apps.value.associateBy { it.key }
        return _stats.value.entries
            .filter { it.key !in exclude }
            .sortedByDescending { it.value.second }
            .mapNotNull { byKey[it.key] }
            .take(limit)
    }

    /** Per-app icon source from SettingsState.perAppIconJson: pack pkg, or "system". */
    fun perAppPack(key: String): String = try {
        JSONObject(settings?.state?.value?.perAppIconJson ?: "{}").optString(key, "")
    } catch (t: Throwable) {
        ""
    }

    fun icon(e: AppEntry): Drawable? {
        // M5: per-app override (long-press icon -> Edit icon) beats the
        // global pack; "system" means this app always uses its own icon.
        val override = perAppPack(e.key)
        if (override == "system") {
            // fall through to the system icon below
        } else if (override.isNotBlank()) {
            IconPacks.iconFor(appCtx, override, e.pkg, e.cls)?.let { return it }
        } else {
            val pack = activePack
            if (pack.isNotBlank()) {
                IconPacks.iconFor(appCtx, pack, e.pkg, e.cls)?.let { return it }
            }
        }
        iconCache.get(e.key)?.let { return it }
        val d = try {
            pm.getActivityIcon(ComponentName(e.pkg, e.cls))
        } catch (t: Throwable) {
            try {
                pm.defaultActivityIcon
            } catch (t2: Throwable) {
                null
            }
        }
        if (d != null) iconCache.put(e.key, d)
        return d
    }

    /**
     * Icon as a rasterized bitmap, cached. SAFE to call off the main thread —
     * and only from there: this is the async icon pipeline (M1 should-fix).
     */
    fun iconBitmap(e: AppEntry, sizePx: Int = 144): android.graphics.Bitmap? {
        val cacheKey = e.key + "|" + activePack + "|" + perAppPack(e.key)
        bitmapCache.get(cacheKey)?.let { return it }
        val drawable = icon(e) ?: return null
        val bmp = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        bitmapCache.put(cacheKey, bmp)
        return bmp
    }

    fun cachedBitmap(key: String): android.graphics.Bitmap? =
        bitmapCache.get(key + "|" + activePack + "|" + perAppPack(key))
}
