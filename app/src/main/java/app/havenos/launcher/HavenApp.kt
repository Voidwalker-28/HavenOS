package app.havenos.launcher

import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import app.havenos.launcher.data.AppRepository
import app.havenos.launcher.data.HavenDb
import app.havenos.launcher.data.LayoutCoordinator
import app.havenos.launcher.data.Settings
import app.havenos.launcher.data.TasksStore
import app.havenos.launcher.data.WallpaperStore
import app.havenos.launcher.data.WeatherStore
import app.havenos.launcher.data.buildBackupJson
import app.havenos.launcher.data.network.ApiId
import app.havenos.launcher.data.network.ApiRefreshScheduler
import app.havenos.launcher.data.network.ApiToggles
import app.havenos.launcher.data.network.ApiStore
import app.havenos.launcher.data.network.hasValidatedInternet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class HavenApp : Application() {
    lateinit var db: HavenDb
        private set
    lateinit var settings: Settings
        private set
    /** M9 Worker D: the single state-layer writer (profiles, undo, snapshots, transactions). */
    lateinit var layoutStore: LayoutCoordinator
        private set
    lateinit var appRepo: AppRepository
        private set
    lateinit var wallpaper: WallpaperStore
        private set
    lateinit var weather: WeatherStore
        private set
    lateinit var tasks: TasksStore
        private set
    /** v2 M1: the single cache-first API store (Room api_cache table). */
    lateinit var apiStore: ApiStore
        private set

    /**
     * 3.1 A12: bumps on every MainActivity resume. Today card flows
     * are long-lived (home composition survives overlays), so without
     * this they would never re-run cacheFirstFlow's stale check
     * after a worker was deferred (Xiaomi battery management).
     * Cards key their flow collection on it; a fresh cache emits
     * without any network call, so resumes cost one Room read each.
     */
    /**
     * 3.1.1 (item 3): Safe Mode. True when the last two crashes fell
     * within 60 seconds of each other. While true, Home gates the
     * mascot and island off, API workers are cancelled (not
     * scheduled), and MainActivity forces Solid rendering (blur off)
     * — all as runtime overrides. Stored settings are NEVER mutated
     * by Safe Mode, so Re-enable (or 10 minutes of stable running)
     * restores exactly what the user had on, by construction. The
     * pre-Safe-Mode snapshot is still recorded to safe_mode.txt for
     * View details / diagnostics.
     */
    private val _safeMode = MutableStateFlow(false)
    val safeMode: StateFlow<Boolean> = _safeMode

    private val _homeResumedTick = MutableStateFlow(0)
    val homeResumedTick: StateFlow<Int> = _homeResumedTick

    fun noteHomeResumed() {
        _homeResumedTick.value++
    }
    lateinit var widgetHost: AppWidgetHost
        private set
    lateinit var widgetManager: AppWidgetManager
        private set

    override fun onCreate() {
        super.onCreate()
        installCrashLog()
        // 3.1.1 (item 3): Safe Mode is decided before anything else
        // runs, from the crash markers left by previous processes.
        val safeModeMarkers = evaluateSafeMode()
        db = HavenDb.create(this)
        settings = Settings(this)
        // Snapshot + 10-minute timer need settings; they start here,
        // after assignment (evaluateSafeMode only set the flag).
        if (safeModeMarkers >= 0) startSafeModeJobs(safeModeMarkers)
        layoutStore = LayoutCoordinator(this, db, settings)
        appRepo = AppRepository(this, settings)
        wallpaper = WallpaperStore(this)
        weather = WeatherStore(this)
        tasks = TasksStore(this)
        // 3.1 A2: diagnostics DAO + validated-network check, so card
        // states classify Offline only on a truly unvalidated network.
        apiStore = ApiStore(db.apiCache(), db.apiDiagnostics()) { hasValidatedInternet() }
        // v2 API refresh: KEEP policy, so re-launching never duplicates
        // or resets the schedule; each worker re-checks its toggle.
        // 3.1 A12: a disabled API's worker is cancelled here, not
        // left to no-op (toggle edits also reschedule at once, in
        // the APIs hub).
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // 3.1.1 (item 3): in Safe Mode every worker is
                // CANCELLED (scheduleAll cancels disabled APIs), not
                // scheduled — online workers stay off until Re-enable
                // or the 10-minute stable clear.
                ApiRefreshScheduler.scheduleAll(this@HavenApp) { api ->
                    !_safeMode.value && ApiToggles.isEnabled(settings.state.value, api)
                }
            } catch (t: Throwable) {
                // refresh scheduling is best-effort; cards still read cache
            }
            // 3.1 A13: a crash or kill must not leave the user
            // without a nav bar — restore on every launch when the
            // settings say the bar should be shown.
            try {
                restoreNavBarOnLaunch(this@HavenApp, settings)
            } catch (t: Throwable) {
                // best-effort; the manual row in Gestures and Motion
                // remains as the fallback
            }
        }
        widgetManager = AppWidgetManager.getInstance(this)
        widgetHost = AppWidgetHost(this, WIDGET_HOST_ID)
        maybeDailyBackup()
    }

    // shortcut: daily backup runs on first launch of the day, not via WorkManager; upgrade if exact timing matters.
    private fun maybeDailyBackup() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Read the persisted state directly — never the pre-load default.
                val st = settings.persistedState()
                val today = System.currentTimeMillis() / 86_400_000L
                if (today > st.lastBackupDay) {
                    val json = buildBackupJson(this@HavenApp, st, st.backupIncludeWp)
                    File(filesDir, "backup-auto.json").writeText(json)
                    settings.setLastBackupDay(today)
                }
            } catch (t: Throwable) {
                // auto-backup is best-effort
            }
        }
    }

    /** Keep the last crash's stack trace for Settings → About & Help → Crash log. */
    private fun installCrashLog() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val text = buildString {
                    appendLine("HavenOS crash — ${java.util.Date()}")
                    appendLine(error.stackTraceToString())
                }
                File(filesDir, "last_crash.txt").writeText(text)
                // 3.1.1 (item 3): SMALL timestamp marker, then always
                // delegate to the previous handler — crashes are never
                // swallowed. Feature failures (item 2) never write here.
                writeCrashMarker(System.currentTimeMillis())
            } catch (t: Throwable) {
                // never mask the original crash
            }
            previous?.uncaughtException(thread, error)
        }
    }

    // 3.1.1 (item 3): crash markers + Safe Mode. One timestamp per
    // line, newest 20 kept; the file stays tiny by construction.
    private fun readCrashMarkers(): List<Long> {
        return try {
            val file = File(filesDir, "crash_markers.txt")
            if (!file.exists()) emptyList()
            else file.readLines().mapNotNull { it.trim().toLongOrNull() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeCrashMarker(nowMs: Long) {
        try {
            val kept = pruneCrashMarkers(readCrashMarkers() + nowMs)
            File(filesDir, "crash_markers.txt")
                .writeText(kept.joinToString("\n") + "\n")
        } catch (_: Exception) {
            // never mask the original crash
        }
    }

    /**
     * Decide Safe Mode from previous processes' markers. Runs before
     * init; returns the marker count when Safe Mode was entered, else
     * -1 (the snapshot + timer jobs start later, once settings exist).
     */
    private fun evaluateSafeMode(): Int {
        val markers = readCrashMarkers()
        if (!isCrashLoop(markers)) return -1
        _safeMode.value = true
        try {
            File(filesDir, "safe_mode.txt").writeText(
                "enteredAtMs=${System.currentTimeMillis()}\n",
            )
        } catch (_: Exception) {
        }
        return markers.size
    }

    /** Snapshot what was on + the 10-minute stable timer. */
    private fun startSafeModeJobs(startCount: Int) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Persisted read, never the pre-load default.
                // Informational: re-enable is exact because Safe Mode
                // never mutates stored settings — it only overrides
                // at runtime.
                val st = settings.persistedState()
                val apiOn = try {
                    ApiId.entries.any { api -> ApiToggles.isEnabled(st, api) }
                } catch (_: Exception) {
                    false
                }
                File(filesDir, "safe_mode.txt").writeText(
                    "enteredAtMs=${System.currentTimeMillis()}\n" +
                        "mascotOn=${st.mascotOn}\n" +
                        "islandOn=${st.islandOn}\n" +
                        "surfaceMaterial=${st.surfaceMaterial}\n" +
                        "apiOn=$apiOn\n",
                )
            } catch (_: Exception) {
            }
            // 10 minutes of stable running clears Safe Mode (a new
            // crash restarts the timer, because the next process
            // re-evaluates from the markers). Idempotent with a
            // manual Re-enable.
            delay(10L * 60L * 1000L)
            try {
                if (_safeMode.value && readCrashMarkers().size == startCount) {
                    clearSafeMode()
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Leave Safe Mode: banner goes away, runtime overrides lift, and
     * API workers are rescheduled for the user's enabled toggles.
     * Stored settings were never changed, so this restores exactly
     * what was on. Safe to call twice.
     */
    fun clearSafeMode() {
        _safeMode.value = false
        try {
            File(filesDir, "safe_mode.txt").delete()
        } catch (_: Exception) {
        }
        if (!::settings.isInitialized) return
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ApiRefreshScheduler.scheduleAll(this@HavenApp) { api ->
                    ApiToggles.isEnabled(settings.state.value, api)
                }
            } catch (_: Exception) {
            }
        }
    }

    /** Parse filesDir/last_crash.txt for Diagnostics / View details. Local only. */
    fun readLastCrash(): LastCrash? {
        return try {
            val file = File(filesDir, "last_crash.txt")
            if (!file.exists()) return null
            val lines = file.readLines()
            if (lines.size < 2) return null
            val timeText = lines[0].removePrefix("HavenOS crash — ").trim()
            val first = lines[1]
            val clazz = first.substringBefore(":").trim()
            val message = first.substringAfter(":", "").trim()
            val code = try {
                val info = packageManager.getPackageInfo(packageName, 0)
                @Suppress("DEPRECATION")
                if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
            } catch (_: Exception) {
                0L
            }
            if (clazz.isEmpty()) null else LastCrash(clazz, message, timeText, code)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val WIDGET_HOST_ID = 1024

        fun of(ctx: Context): HavenApp = ctx.applicationContext as HavenApp
    }
}
