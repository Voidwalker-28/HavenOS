package app.havenos.launcher.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.AppEntry
import app.havenos.launcher.data.HomeItem
import app.havenos.launcher.data.ItemKind
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.LocalPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import org.json.JSONArray

// ---------------------------------------------------------------------------
// M9 Worker C — Widgets Real. Pure logic lives here so it is unit-testable
// without Compose; the composables below are the one shared frame and the
// state surfaces every widget renders instead of ever going blank.
// ---------------------------------------------------------------------------

/** The one built-in frame radius (Doc 2 §6 / task item 1). */
const val WIDGET_FRAME_RADIUS_DP = 28

/** Frame padding for every built-in widget. */
const val WIDGET_FRAME_PADDING_DP = 16

/** Weather cache is stale after 30 minutes (Doc 3 §2.5). */
const val WEATHER_STALE_MS = 30 * 60_000L

/** Calendar content-observer throttle (Doc 3 §2.5). */
const val CALENDAR_OBSERVER_THROTTLE_MS = 30_000L

/** Stack auto-rotation stays paused this long after a touch (Doc 3 §2.7). */
const val WIDGET_STACK_TOUCH_PAUSE_MS = 10_000L

// ------------------------------------------------------------ pure helpers

/** Locality only: shared helper from TodayPage.kt (same package, same
 * semantics — "Delhi, National Capital Territory, India" -> "Delhi").
 * This file deliberately does NOT redefine it; the duplicate definition
 * broke the build with conflicting overloads, so call sites below use
 * the TodayPage definition. */

/** Temperature in the Settings unit. Weather data already arrives in that
 * unit (WeatherStore requests °F when units == 1), so this only formats —
 * it never converts, which is what kept 28° and 75° from disagreeing. */
fun formatTempDisplay(temp: Double, units: Int): String {
    val rounded = Math.round(temp)
    return if (units == 1) "$rounded°F" else "$rounded°C"
}

/** True when a weather fetch at [fetchedAtMs] is stale at [nowMs]. */
fun isWeatherStale(fetchedAtMs: Long, nowMs: Long): Boolean =
    fetchedAtMs <= 0L || nowMs - fetchedAtMs > WEATHER_STALE_MS

/** "Updated HH:MM" footer text; "" when no fetch time is known. */
fun formatUpdatedAt(fetchedAtMs: Long): String {
    if (fetchedAtMs <= 0L) return ""
    return "Updated " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(fetchedAtMs))
}

/** Every render state a widget may be in (Doc 3 §2.4). Never blank. */
enum class WidgetState {
    LOADING, READY, EMPTY, NEEDS_PERMISSION, ERROR, OFFLINE, PROVIDER_REMOVED,
}

/**
 * Pure state resolution, in precedence order: a removed provider beats
 * everything; permission beats loading; an error beats an empty cache only
 * when there is nothing to show (a stale cache renders OFFLINE instead).
 */
fun resolveWidgetState(
    providerRemoved: Boolean = false,
    permissionGranted: Boolean = true,
    loading: Boolean = false,
    hasData: Boolean = false,
    isEmpty: Boolean = false,
    error: Boolean = false,
    staleCache: Boolean = false,
): WidgetState = when {
    providerRemoved -> WidgetState.PROVIDER_REMOVED
    !permissionGranted -> WidgetState.NEEDS_PERMISSION
    error && !hasData -> WidgetState.ERROR
    error && staleCache -> WidgetState.OFFLINE
    loading && !hasData -> WidgetState.LOADING
    staleCache && hasData -> WidgetState.OFFLINE
    isEmpty || !hasData -> WidgetState.EMPTY
    else -> WidgetState.READY
}

/** Whole-cell sizes (cells, not half-cell units) a built-in kind allows,
 * largest first. The sheet and resize chips only offer these. */
fun builtinWidgetSizes(kind: Int): List<Pair<Int, Int>> = when (kind) {
    ItemKind.WEATHER -> listOf(4 to 4, 4 to 3, 4 to 2, 2 to 2)
    ItemKind.AGENDA -> listOf(4 to 4, 4 to 2, 2 to 2)
    ItemKind.CLOCK -> listOf(4 to 2, 2 to 2)
    ItemKind.SUGGESTED -> listOf(4 to 2, 2 to 2)
    ItemKind.BATTERY -> listOf(4 to 2, 2 to 2)
    ItemKind.QUICK_SEARCH -> listOf(4 to 1, 2 to 1)
    else -> listOf(4 to 2, 2 to 2)
}

/**
 * Provider-allowed cell sizes from a minimum size and a resize-mode mask
 * (bit 1 = horizontal, bit 2 = vertical, as in AppWidgetProviderInfo).
 * Candidates are the Doc 3 size chips; the minimum itself is always kept.
 */
fun allowedCellSizes(minCellsW: Int, minCellsH: Int, resizeMode: Int): List<Pair<Int, Int>> {
    val canW = resizeMode and 1 != 0
    val canH = resizeMode and 2 != 0
    val candidates = listOf(2 to 2, 4 to 2, 2 to 4, 4 to 4)
    val kept = candidates.filter { (w, h) ->
        w >= minCellsW && h >= minCellsH &&
            (w == minCellsW || canW) && (h == minCellsH || canH)
    }
    return if (kept.isEmpty()) listOf(minCellsW to minCellsH) else kept
}

/** Clamp a span (cells) into [min]..[max]; an impossible resize clamps,
 * it never overlaps or errors. */
fun clampSpan(span: Int, min: Int, max: Int): Int = span.coerceIn(min, max)

/**
 * First free spot for any of [sizesUnits] (half-cell units, tried in order)
 * on [page], row-major. Null when nothing fits anywhere at any size.
 */
fun findSpotForSizes(
    items: List<HomeItem>,
    page: Int,
    sizesUnits: List<Pair<Int, Int>>,
    colsU: Int,
    rowsU: Int,
): Triple<Int, Int, Pair<Int, Int>>? {
    for ((w, h) in sizesUnits) {
        val spot = findSpot(items, page, w, h, colsU, rowsU)
        if (spot != null) return Triple(spot.first, spot.second, w to h)
    }
    return null
}

/**
 * Tap-to-add placement (Doc 3 §2.2): current page first, then other pages
 * in order. Returns (page, col, row, sizeUnits), or null when every page
 * is full — the caller then offers a new page.
 */
fun findSpotAcrossPagesSized(
    items: List<HomeItem>,
    startPage: Int,
    pageCount: Int,
    sizesUnits: List<Pair<Int, Int>>,
    colsU: Int,
    rowsU: Int,
): Quad<Int, Int, Int, Pair<Int, Int>>? {
    val order = (listOf(startPage) + (0 until pageCount).filter { it != startPage })
    for (page in order) {
        val found = findSpotForSizes(items, page, sizesUnits, colsU, rowsU)
        if (found != null) return Quad(page, found.first, found.second, found.third)
    }
    return null
}

/** Tiny 4-tuple (Kotlin stdlib stops at Triple). */
data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/** Stack payload: JSON array of ItemKind ints. Never throws. */
fun parseStackMembers(payload: String): List<Int> = try {
    val arr = JSONArray(payload)
    List(arr.length()) { i -> arr.optInt(i, -1) }
        .filter { it in 2..12 || it in 16..18 }
} catch (t: Throwable) {
    emptyList()
}

fun stackMembersJson(members: List<Int>): String =
    JSONArray().apply { for (m in members) put(m) }.toString()

/** Next stack index, wrapping. */
fun stackNextIndex(index: Int, count: Int, delta: Int): Int {
    if (count <= 0) return 0
    return ((index + delta) % count + count) % count
}

/** True while auto-rotation must stay paused after a touch. */
fun isStackRotationPaused(lastTouchMs: Long, nowMs: Long): Boolean =
    lastTouchMs > 0L && nowMs - lastTouchMs < WIDGET_STACK_TOUCH_PAUSE_MS

/**
 * Time-based stack rotation (Doc 3 §2.7, media card excluded — it is
 * LATER): morning cycles weather then agenda; evening shows suggested
 * only. Returns the member kind to show, or null when no member matches.
 */
fun stackAutoMember(members: List<Int>, hourOfDay: Int): Int? = when (hourOfDay) {
    in 5..11 -> members.firstOrNull { it == ItemKind.WEATHER }
        ?: members.firstOrNull { it == ItemKind.AGENDA }
    in 18..23 -> members.firstOrNull { it == ItemKind.SUGGESTED }
    else -> null
}

// ------------------------------------------------------------ suggestions

/** One launch-history row for [suggestApps]: key, launch count, last ms. */
data class LaunchStat(val key: String, val count: Int, val lastMs: Long)

/**
 * Suggested apps, local only, never network: count first, recency second,
 * with a same-hour-of-day boost (an app launched around this hour before
 * ranks above one launched equally often at other hours).
 */
fun suggestApps(
    stats: List<LaunchStat>,
    appsByKey: Map<String, AppEntry>,
    hourOfDay: Int,
    nowMs: Long,
    limit: Int = 8,
): List<AppEntry> {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMs }
    val hour = if (hourOfDay in 0..23) hourOfDay else cal.get(java.util.Calendar.HOUR_OF_DAY)
    return stats
        .mapNotNull { s ->
            val entry = appsByKey[s.key] ?: return@mapNotNull null
            val launchedCal = java.util.Calendar.getInstance().apply { timeInMillis = s.lastMs }
            val sameHour = launchedCal.get(java.util.Calendar.HOUR_OF_DAY) == hour
            val score = s.count * 1000L + (if (sameHour) 500L else 0L) +
                (s.lastMs / 86_400_000L) // recency in whole days, tie-break only
            entry to score
        }
        .sortedByDescending { it.second }
        .map { it.first }
        .take(limit.coerceIn(4, 8))
}

// ------------------------------------------------------------ today seam

/**
 * SEAM FOR WORKER B (Today page shell): the shell calls [TodayFlowCard]
 * for each card and lays the cards out as a flow — never as cell
 * coordinates. Expanded = 2 columns, Medium/Compact = 1 column.
 */
fun todayFlowColumns(isExpanded: Boolean): Int = if (isExpanded) 2 else 1

/** Allowed card sizes (cells) per built-in kind, for Today resize chips. */
fun todayCardSizes(kind: Int): List<Pair<Int, Int>> = builtinWidgetSizes(kind)

/** Reorder Today cards: move the card at [from] to [to], order preserved. */
fun reorderTodayCards(cards: List<HomeItem>, from: Int, to: Int): List<HomeItem> {
    if (from !in cards.indices || to !in cards.indices || from == to) return cards
    val mutable = cards.toMutableList()
    val moved = mutable.removeAt(from)
    mutable.add(to, moved)
    return mutable
}

/**
 * SEAM FOR WORKER B: one Today card. Today and Home share the same widget
 * objects and the same frame — this is [WidgetContent] with no grid
 * geometry, so the shell owns layout and this owns content.
 */
@Composable
fun TodayFlowCard(
    app: HavenApp,
    item: HomeItem,
    appsByKey: Map<String, AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    onOpenSearch: () -> Unit = {},
    onAppMenu: ((AppEntry) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    WidgetContent(
        app = app,
        item = item,
        appsByKey = appsByKey,
        onLaunch = onLaunch,
        modifier = modifier,
        editMode = false,
        onOpenSearch = onOpenSearch,
        onAppMenu = onAppMenu,
    )
}

// ------------------------------------------------------------ widget actions

/**
 * SEAM FOR WORKER A (edit toolbar): duplicate a widget onto the first free
 * spot (same page first, then other pages). Returns the new item, or null
 * when every page is full at every allowed size — the bar then offers a
 * new page instead of silently failing. The caller commits via the DB.
 */
fun duplicateWidgetPlacement(
    item: HomeItem,
    items: List<HomeItem>,
    pageCount: Int,
    colsU: Int,
    rowsU: Int,
): HomeItem? {
    val sizesUnits = builtinWidgetSizes(item.kind).map { (w, h) -> w * 2 to h * 2 }
    val found = findSpotAcrossPagesSized(items, item.page, pageCount, sizesUnits, colsU, rowsU)
        ?: return null
    return item.copy(
        id = "w:${item.kind}:${UUID.randomUUID()}",
        page = found.first,
        col = found.second,
        row = found.third,
        colSpan = found.fourth.first,
        rowSpan = found.fourth.second,
    )
}

/** Stack edit operations, pure: reorder / remove / unstack-to-members. */
fun stackReordered(members: List<Int>, from: Int, to: Int): List<Int> {
    if (from !in members.indices || to !in members.indices) return members
    val mutable = members.toMutableList()
    val moved = mutable.removeAt(from)
    mutable.add(to, moved)
    return mutable
}

// ------------------------------------------------------------ frame + states

/**
 * The one frame for all built-in widgets: 28dp radius, material from the
 * Look, 16dp padding, optional title row. Built-ins drawn through
 * WidgetSurface get the same radius/padding (see Widgets.kt).
 */
@Composable
fun WidgetFrame(
    title: String? = null,
    onTitleClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = LocalPalette.current
    GlassSurface(
        shape = RoundedCornerShape(WIDGET_FRAME_RADIUS_DP.dp),
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxSize().padding(WIDGET_FRAME_PADDING_DP.dp)) {
            if (title != null) {
                Row(
                    Modifier.fillMaxWidth().then(
                        if (onTitleClick != null) Modifier.clickable { onTitleClick() }
                        else Modifier,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title, color = palette.accent, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            content()
        }
    }
}

/** Loading: skeleton bars in the frame — never a bare name. */
@Composable
fun WidgetSkeleton(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val transition = rememberInfiniteTransition(label = "widget-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeleton-alpha",
    )
    WidgetFrame(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.fillMaxWidth(0.55f).height(20.dp).alpha(alpha)
                    .clip(RoundedCornerShape(6.dp)).background(palette.glassBorder),
            )
            Box(
                Modifier.fillMaxWidth(0.85f).height(12.dp).alpha(alpha)
                    .clip(RoundedCornerShape(6.dp)).background(palette.glassBorder),
            )
            Box(
                Modifier.fillMaxWidth(0.7f).height(12.dp).alpha(alpha)
                    .clip(RoundedCornerShape(6.dp)).background(palette.glassBorder),
            )
        }
    }
}

/** Empty: short message + exactly one action. */
@Composable
fun WidgetEmpty(message: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    WidgetFrame(modifier = modifier) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text(message, color = palette.onSurface, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Needs-permission: inline chip -> rationale is the message itself ->
 * system prompt via [onAllow] -> caller re-checks on return (live update). */
@Composable
fun WidgetNeedsPermission(
    message: String,
    chipLabel: String,
    onAllow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    WidgetFrame(modifier = modifier) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text(message, color = palette.onSurface, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAllow) { Text(chipLabel) }
        }
    }
}

/** Error: message + Retry. Never a crash, never blank. */
@Composable
fun WidgetErrorRetry(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    WidgetFrame(modifier = modifier) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text("Couldn't load", color = palette.onSurface, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

/** Offline footer line, appended under cached content. */
@Composable
fun WidgetOfflineLine(fetchedAtMs: Long, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val text = formatUpdatedAt(fetchedAtMs)
    if (text.isNotEmpty()) {
        Text(text, color = palette.muted, fontSize = 11.sp, modifier = modifier)
    }
}

/** Provider removed: message + Remove action. */
@Composable
fun WidgetProviderRemoved(onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    WidgetFrame(modifier = modifier) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text("App removed", color = palette.onSurface, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRemove) { Text("Remove widget") }
        }
    }
}

/** Third-party failure: message + tap to reload. Never a crash. */
@Composable
fun WidgetTpError(onReload: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    WidgetFrame(modifier = modifier) {
        Box(
            Modifier.fillMaxSize().clickable { onReload() },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Problem loading widget", color = palette.onSurface,
                    fontSize = 13.sp, maxLines = 2,
                )
                Spacer(Modifier.height(4.dp))
                Text("Tap to reload", color = palette.muted, fontSize = 12.sp)
            }
        }
    }
}

// ------------------------------------------------------------ new built-ins

/** Suggested apps: 4–8 icons from local history (passed in ranked). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuggestedWidget(
    apps: List<AppEntry>,
    appRepo: app.havenos.launcher.data.AppRepository,
    onLaunch: (AppEntry) -> Unit,
    onAppMenu: ((AppEntry) -> Unit)? = null,
    editMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    WidgetFrame(title = "Suggested", modifier = modifier) {
        if (apps.isEmpty()) {
            Text(
                "Launch some apps and suggestions will appear here",
                color = palette.muted, fontSize = 13.sp,
            )
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (entry in apps.take(8)) {
                    Column(
                        Modifier.weight(1f).then(
                            if (editMode) Modifier
                            else Modifier.combinedClickable(
                                onClick = { onLaunch(entry) },
                                onLongClick = { onAppMenu?.invoke(entry) },
                            ),
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AppIconImage(entry, appRepo, 44.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            entry.label, color = palette.onSurface, fontSize = 10.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private data class BatteryRead(val percent: Int, val charging: Boolean)

/** Battery: level + charging state. The system exposes no public
 * time-remaining estimate, so none is invented — level + state only. */
@Composable
fun BatteryWidget(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val read = remember {
        try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (intent == null) {
                null
            } else {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (level < 0 || scale <= 0) {
                    null
                } else {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    BatteryRead(
                        percent = (level * 100 / scale).coerceIn(0, 100),
                        charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL,
                    )
                }
            }
        } catch (t: Throwable) {
            null
        }
    }
    WidgetFrame(title = "Battery", modifier = modifier) {
        val b = read
        if (b == null) {
            Text("Battery info unavailable", color = palette.muted, fontSize = 13.sp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${b.percent}%", color = palette.onSurface, fontSize = 32.sp,
                    fontWeight = FontWeight.Light, maxLines = 1,
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        if (b.charging) "Charging" else "Not charging",
                        color = palette.onSurface, fontSize = 13.sp, maxLines = 1,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.fillMaxWidth().height(8.dp)
                            .clip(RoundedCornerShape(50)).background(palette.glassBorder),
                    ) {
                        Box(
                            Modifier.fillMaxWidth(b.percent / 100f).height(8.dp)
                                .clip(RoundedCornerShape(50)).background(palette.accent),
                        )
                    }
                }
            }
        }
    }
}

/** Quick search: a field that opens the Search card. No results inside. */
@Composable
fun QuickSearchWidget(onOpenSearch: () -> Unit, editMode: Boolean = false, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    WidgetFrame(modifier = modifier) {
        Box(
            Modifier.fillMaxSize().clip(RoundedCornerShape(50))
                .background(palette.surface.copy(alpha = 0.55f))
                .then(if (editMode) Modifier else Modifier.clickable { onOpenSearch() })
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⌕", color = palette.muted, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Search apps, settings…", color = palette.muted, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Configure intent for a hosted third-party widget, or null. */
fun tpConfigureIntent(app: HavenApp, item: HomeItem): Intent? {
    val id = item.payload.toIntOrNull() ?: return null
    return try {
        val info = app.widgetManager.getAppWidgetInfo(id) ?: return null
        val configure = info.configure ?: return null
        Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
            .setComponent(configure)
            .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, id)
    } catch (t: Throwable) {
        null
    }
}

// ------------------------------------------------------------ host seams
// (M9 Worker C — added after Home.kt was found under concurrent sibling
// rework: these pure helpers are the host-side placement/dedup contract
// so the host wires them in one place instead of this worker editing a
// moving Home.kt. All parameters are plain values; the host commits the
// returned item and, when needsNewPage is true, bumps its page count.)

/** Result of [buildPlacedWidget]: the item to insert, and whether the
 * host must create a new page for it (page == pageCount). */
data class WidgetPlacement(val item: HomeItem, val needsNewPage: Boolean)

/**
 * Tap-to-add placement for the sheet's onPlace (Doc 3 §2.2): try the
 * requested size on the current page, then other pages, then smaller
 * allowed sizes; when every page is full, place on a fresh page at
 * (0, 0) with the smallest allowed size and needsNewPage = true. Null
 * only when even the smallest size exceeds the grid itself.
 */
fun buildPlacedWidget(
    kind: Int,
    colSpanUnits: Int,
    rowSpanUnits: Int,
    payload: String,
    items: List<HomeItem>,
    startPage: Int,
    pageCount: Int,
    colsU: Int,
    rowsU: Int,
    nextFlowOrder: Int,
): WidgetPlacement? {
    val requestedCells = colSpanUnits / 2 to rowSpanUnits / 2
    val candidates: List<Pair<Int, Int>> = if (kind == ItemKind.TP_WIDGET) {
        listOf(requestedCells)
    } else {
        val builtin = builtinWidgetSizes(kind)
        val ordered = (listOf(requestedCells) + builtin.filter { (w, h) ->
            w <= requestedCells.first && h <= requestedCells.second
        }).distinct()
        if (ordered.isEmpty()) builtin else ordered
    }
    val sizesUnits = candidates.map { (w, h) -> w * 2 to h * 2 }
    val smallest = sizesUnits.last()
    if (smallest.first > colsU || smallest.second > rowsU) return null
    val id = if (kind == ItemKind.TP_WIDGET) "tp:$payload" else "w:$kind:${UUID.randomUUID()}"
    val found = findSpotAcrossPagesSized(items, startPage, pageCount, sizesUnits, colsU, rowsU)
    if (found != null) {
        return WidgetPlacement(
            HomeItem(
                id, kind, found.first, found.second, found.third,
                found.fourth.first, found.fourth.second, payload,
                flowOrder = nextFlowOrder,
            ),
            needsNewPage = false,
        )
    }
    return WidgetPlacement(
        HomeItem(
            id, kind, pageCount, 0, 0,
            smallest.first, smallest.second, payload,
            flowOrder = nextFlowOrder,
        ),
        needsNewPage = true,
    )
}

/**
 * One-time cleanup for the "two identical Agenda" defect: keep the first
 * seeded Agenda (kind AGENDA, default empty payload; lowest flowOrder,
 * then id) and drop any further ones. Agenda variants with a payload
 * ("glance", "tomorrow") are never touched, and a single seeded Agenda
 * returns the input unchanged.
 */
fun deduplicateSeededAgendas(items: List<HomeItem>): List<HomeItem> {
    val seeded = items.filter { it.kind == ItemKind.AGENDA && it.payload.isEmpty() }
    if (seeded.size <= 1) return items
    val keep = seeded.sortedWith(compareBy({ it.flowOrder }, { it.id })).first()
    return items.filterNot { it.kind == ItemKind.AGENDA && it.payload.isEmpty() && it.id != keep.id }
}
