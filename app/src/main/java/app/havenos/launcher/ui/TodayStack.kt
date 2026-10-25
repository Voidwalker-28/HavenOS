package app.havenos.launcher.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.data.AppEntry
import app.havenos.launcher.data.AppRepository
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.LocalPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * M7 Wave 1 (P1) — Today section / column + Smart Stack.
 *
 * NEW FILE ONLY. The coordinator wires this into the one continuous scroll
 * home (via [TodaySection]) and, on Expanded, beside the flow (via
 * [TodayColumn]). Nothing here is a separate screen and nothing here
 * mentions a separate screen: there is one scroll home, and Today is a
 * section inside it on Compact/Medium and a column beside it on Expanded.
 *
 * Design (Minimal only): the SAME built-in cards live in ONE frame with a
 * 28dp corner radius, drawn with [GlassSurface] so the material follows
 * the user's material setting. Cards are plain content inside that frame —
 * never nested frames and never outlined stacked boxes. Content is never
 * given a fixed height that could clip it: the frame grows with its card
 * (a minimum height only), and each card bounds its own row count.
 *
 * Cards: Clock + weather, Calendar next events, Suggested apps (local
 * launch history, passed in by the coordinator), Battery. Media is NOT
 * built here — it is P2 work and is deliberately absent.
 *
 * Settings keys referenced BY NAME only (the coordinator owns the store
 * entries; this file reads none of them directly):
 * - `todayOn` (Boolean, default true): Today on/off, honoured by both
 *   wrappers — when false they render nothing.
 * - `smartStackAutoRotate` (Boolean, default false): time-based
 *   auto-rotation, off by default.
 *
 * Offline: no network call is made here. Weather is whatever the
 * coordinator passes in (the existing weather feed's last known value);
 * when it carries a fetch time it is shown as "Updated HH:MM".
 */

// ------------------------------------------------------------------ model

/** One card of the Smart Stack, in swipe order. */
enum class TodayCard { CLOCK_WEATHER, CALENDAR, SUGGESTED, BATTERY }

/** All cards, in swipe order. */
val TODAY_CARDS: List<TodayCard> = TodayCard.entries.toList()

/** Last-known weather for the Clock + weather card. */
data class TodayWeather(
    val tempText: String,
    val condition: String,
    val city: String,
    /** Epoch ms of the last successful fetch; 0 = unknown (no "Updated" line). */
    val fetchedAtMs: Long = 0L,
)

/** One next event for the Calendar card. */
data class TodayEvent(val title: String, val timeLabel: String)

// ------------------------------------------------------- pure stack logic
// (Unit-tested in TodayCategoriesCalmTest; no Compose runtime needed.)

/** Pause window after any touch on the stack, in milliseconds. */
const val SMART_STACK_TOUCH_PAUSE_MS: Long = 10_000L

/** True while auto-rotation must stay paused after a touch at [lastTouchMs]. */
fun isSmartStackPaused(lastTouchMs: Long, nowMs: Long): Boolean =
    lastTouchMs > 0L && nowMs - lastTouchMs < SMART_STACK_TOUCH_PAUSE_MS

/**
 * Time-of-day mapping for auto-rotation. Morning shows weather (inside the
 * Clock + weather card) and calendar; midday leans on calendar; evening
 * leans on suggested apps; night returns to clock + weather.
 */
fun autoRotatePreferredCard(hourOfDay: Int): TodayCard = when (hourOfDay) {
    in 5..11 -> TodayCard.CLOCK_WEATHER
    in 12..17 -> TodayCard.CALENDAR
    in 18..23 -> TodayCard.SUGGESTED
    else -> TodayCard.CLOCK_WEATHER
}

/** Next card index, wrapping around. */
fun smartStackNext(index: Int, count: Int): Int =
    if (count <= 0) 0 else (index + 1) % count

/** Previous card index, wrapping around. */
fun smartStackPrev(index: Int, count: Int): Int =
    if (count <= 0) 0 else (index - 1 + count) % count

/**
 * Expanded-column width in dp: ~34% of the screen width, clamped to
 * 280–420dp (the doc's column sizing, applied to a column — not a
 * separate screen). Pure float form so unit tests need no Compose types.
 */
fun todayColumnWidthDp(screenWidthDp: Float): Float =
    (screenWidthDp * 0.34f).coerceIn(280f, 420f)

// ------------------------------------------------------------- battery

data class BatteryState(val percent: Int, val charging: Boolean)

fun readBattery(ctx: Context): BatteryState? {
    return try {
        val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level < 0 || scale <= 0) return null
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        BatteryState(
            percent = (level * 100 / scale).coerceIn(0, 100),
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
        )
    } catch (t: Throwable) {
        null
    }
}

// M9 Worker B: renamed from formatUpdatedAt — WidgetReal.kt
// (Worker C) declares a public helper with the same signature in
// this package, which made every call site ambiguous. This helper
// is private to this file; only its in-file call site changes.
private fun formatUpdatedAtText(fetchedAtMs: Long): String {
    if (fetchedAtMs <= 0L) return ""
    return "Updated " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(fetchedAtMs))
}

// -------------------------------------------------------------- the stack

/**
 * The Smart Stack itself: ONE 28dp glass frame showing one card at a time.
 *
 * @param appRepo icon source for suggested apps (AppIconImage).
 * @param onLaunch launch handler for a suggested app.
 * @param weather last-known weather, or null when there is none to show.
 * @param weatherOff true when the user turned weather off in Settings.
 * @param events next events; only used when [calendarGranted] is true.
 * @param calendarGranted true only when READ_CALENDAR is granted. When
 *   false the Calendar card shows a prompt instead of an empty list, and
 *   never queries the calendar.
 * @param onRequestCalendarPermission invoked from the Calendar card prompt.
 * @param suggested locally-ranked apps (coordinator: launch-history order).
 * @param autoRotate time-based auto-rotation; default OFF. Pass the value
 *   of the `smartStackAutoRotate` key.
 * @param modifier caller sizing. The frame itself is full-width of whatever
 *   the caller gives it; it never sets its own width.
 */
@Composable
fun TodayStack(
    appRepo: AppRepository,
    onLaunch: (AppEntry) -> Unit,
    weather: TodayWeather?,
    weatherOff: Boolean,
    events: List<TodayEvent>,
    calendarGranted: Boolean,
    onRequestCalendarPermission: () -> Unit,
    suggested: List<AppEntry>,
    autoRotate: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val now = useNow(1000L)
    val is24 = remember { DateFormat.is24HourFormat(context) }

    var current by remember { mutableStateOf(0) }
    var lastTouchMs by remember { mutableStateOf(0L) }
    val lastTouchNow by rememberUpdatedState(lastTouchMs)
    val autoRotateNow by rememberUpdatedState(autoRotate)

    // Auto-rotation: re-evaluate every 10 s against the time-of-day rule,
    // skipping while paused after a touch. Default off; a touch pauses
    // for [SMART_STACK_TOUCH_PAUSE_MS].
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10_000L)
            if (!autoRotateNow) continue
            val touchMs = lastTouchNow
            if (touchMs > 0L && System.currentTimeMillis() - touchMs < SMART_STACK_TOUCH_PAUSE_MS) continue
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val preferred = autoRotatePreferredCard(hour)
            val idx = TODAY_CARDS.indexOf(preferred)
            if (idx >= 0) current = idx
        }
    }

    val swipeThresholdPx = with(LocalDensity.current) { 48.dp.toPx() }
    // Running vertical-drag total, shared by the drag callbacks below.
    // A plain (non-state) holder is enough: the callbacks only run during
    // a gesture, never during composition, so no recomposition is needed
    // for the total itself. The pointerInput key is Unit, so the detector
    // is not restarted when card state changes.
    var dragTotal = 0f
    val swipeModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onVerticalDrag = { change, dragAmount ->
                // Consume so the surrounding scroll does not also move
                // while a stack swipe is in progress.
                change.consume()
                dragTotal += dragAmount
            },
            onDragEnd = {
                lastTouchMs = System.currentTimeMillis()
                when {
                    dragTotal <= -swipeThresholdPx -> current = smartStackNext(current, TODAY_CARDS.size)
                    dragTotal >= swipeThresholdPx -> current = smartStackPrev(current, TODAY_CARDS.size)
                }
                dragTotal = 0f
            },
            onDragCancel = { dragTotal = 0f },
        )
    }

    GlassSurface(
        modifier = modifier.then(swipeModifier).heightIn(min = 180.dp),
        shape = RoundedCornerShape(28.dp),
    ) {
        Box(Modifier.fillMaxWidth().padding(20.dp)) {
            when (TODAY_CARDS[current]) {
                TodayCard.CLOCK_WEATHER -> ClockWeatherCard(now, is24, weather, weatherOff, palette)
                TodayCard.CALENDAR -> CalendarCard(events, calendarGranted, onRequestCalendarPermission, palette)
                TodayCard.SUGGESTED -> SuggestedCard(suggested, appRepo, onLaunch, palette)
                TodayCard.BATTERY -> BatteryCard(readBattery(context), palette)
            }
            // Small dots on the right edge: one per card, current accented.
            Column(
                Modifier.align(Alignment.CenterEnd),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                for (i in TODAY_CARDS.indices) {
                    Box(
                        Modifier
                            .size(if (i == current) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (i == current) palette.accent else palette.muted),
                    )
                }
            }
        }
    }
}

@Composable
fun ClockWeatherCard(
    now: Long,
    is24: Boolean,
    weather: TodayWeather?,
    weatherOff: Boolean,
    palette: app.havenos.launcher.theme.Palette,
) {
    val timeFmt = remember(is24) {
        SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault())
    }
    val dateFmt = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    Column(Modifier.fillMaxWidth().padding(end = 20.dp)) {
        Text(
            timeFmt.format(Date(now)),
            color = palette.onSurface,
            fontSize = 40.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1,
        )
        Text(dateFmt.format(Date(now)), color = palette.muted, fontSize = 14.sp, maxLines = 1)
        Spacer(Modifier.height(12.dp))
        when {
            weatherOff -> Text("Weather is off in Settings", color = palette.muted, fontSize = 13.sp)
            weather == null -> Text("No weather yet", color = palette.muted, fontSize = 13.sp)
            else -> {
                Text(
                    "${weather.tempText} · ${weather.condition} · ${weather.city}",
                    color = palette.onSurface,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val updated = formatUpdatedAtText(weather.fetchedAtMs)
                if (updated.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(updated, color = palette.muted, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun CalendarCard(
    events: List<TodayEvent>,
    granted: Boolean,
    onRequestPermission: () -> Unit,
    palette: app.havenos.launcher.theme.Palette,
) {
    Column(Modifier.fillMaxWidth().padding(end = 20.dp)) {
        Text("Next events", color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        when {
            !granted -> {
                Text(
                    "Calendar access is needed to show your events.",
                    color = palette.onSurface,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Enable calendar access",
                    color = palette.accent,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onRequestPermission() }.padding(vertical = 4.dp),
                )
            }
            events.isEmpty() -> Text("Nothing coming up", color = palette.muted, fontSize = 13.sp)
            else -> {
                for (ev in events.take(3)) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(
                            ev.timeLabel,
                            color = palette.muted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            modifier = Modifier.width(88.dp),
                        )
                        Text(
                            ev.title,
                            color = palette.onSurface,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestedCard(
    suggested: List<AppEntry>,
    appRepo: AppRepository,
    onLaunch: (AppEntry) -> Unit,
    palette: app.havenos.launcher.theme.Palette,
) {
    Column(Modifier.fillMaxWidth().padding(end = 20.dp)) {
        Text("Suggested", color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        if (suggested.isEmpty()) {
            Text("Open apps to see suggestions here", color = palette.muted, fontSize = 13.sp)
        } else {
            for (entry in suggested.take(4)) {
                // M10 (BUG-002): 6dp vertical row padding (was 4dp)
                // so suggested rows breathe inside the card frame.
                Row(
                    Modifier.fillMaxWidth().clickable { onLaunch(entry) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIconImage(entry, appRepo, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        entry.label,
                        color = palette.onSurface,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryCard(
    battery: BatteryState?,
    palette: app.havenos.launcher.theme.Palette,
) {
    Column(Modifier.fillMaxWidth().padding(end = 20.dp)) {
        Text("Battery", color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        if (battery == null) {
            Text("Battery unavailable", color = palette.muted, fontSize = 13.sp)
        } else {
            Text(
                "${battery.percent}%",
                color = palette.onSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (battery.charging) "Charging" else "Not charging",
                color = palette.muted,
                fontSize = 13.sp,
                maxLines = 1,
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50))
                    .background(palette.glassBorder),
            ) {
                Box(
                    Modifier.fillMaxWidth(battery.percent / 100f).height(8.dp)
                        .clip(RoundedCornerShape(50)).background(palette.accent),
                )
            }
        }
    }
}

// -------------------------------------------------------------- wrappers

/**
 * Today inside the one continuous scroll home (Compact/Medium).
 * The coordinator places this as an item of the scroll flow. When
 * [todayOn] is false (the `todayOn` key) it renders nothing.
 * All card props are forwarded unchanged to [TodayStack].
 */
@Composable
fun TodaySection(
    todayOn: Boolean,
    appRepo: AppRepository,
    onLaunch: (AppEntry) -> Unit,
    weather: TodayWeather?,
    weatherOff: Boolean,
    events: List<TodayEvent>,
    calendarGranted: Boolean,
    onRequestCalendarPermission: () -> Unit,
    suggested: List<AppEntry>,
    autoRotate: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!todayOn) return
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            "Today",
            color = LocalPalette.current.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Spacer(Modifier.height(8.dp))
        TodayStack(
            appRepo = appRepo,
            onLaunch = onLaunch,
            weather = weather,
            weatherOff = weatherOff,
            events = events,
            calendarGranted = calendarGranted,
            onRequestCalendarPermission = onRequestCalendarPermission,
            suggested = suggested,
            autoRotate = autoRotate,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Today beside the flow on Expanded. The coordinator computes
 * [columnWidth] with [todayColumnWidthDp] from the current screen width
 * (~34%, clamped 280–420dp) and places this in a Row beside the flow.
 * When [todayOn] is false (the `todayOn` key) it renders nothing.
 * All card props are forwarded unchanged to [TodayStack].
 */
@Composable
fun TodayColumn(
    todayOn: Boolean,
    columnWidth: Dp,
    appRepo: AppRepository,
    onLaunch: (AppEntry) -> Unit,
    weather: TodayWeather?,
    weatherOff: Boolean,
    events: List<TodayEvent>,
    calendarGranted: Boolean,
    onRequestCalendarPermission: () -> Unit,
    suggested: List<AppEntry>,
    autoRotate: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!todayOn) return
    Column(modifier.width(columnWidth).padding(16.dp)) {
        Text(
            "Today",
            color = LocalPalette.current.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Spacer(Modifier.height(8.dp))
        TodayStack(
            appRepo = appRepo,
            onLaunch = onLaunch,
            weather = weather,
            weatherOff = weatherOff,
            events = events,
            calendarGranted = calendarGranted,
            onRequestCalendarPermission = onRequestCalendarPermission,
            suggested = suggested,
            autoRotate = autoRotate,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
