package app.havenos.launcher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.data.AppEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * M7 Wave 1 (P1) — Calm mode.
 *
 * NEW FILE ONLY. This is an OVERLAY, not a replacement home: the
 * coordinator renders it on top of the unchanged home (which keeps its
 * own state, dock and widgets underneath) and removes it on exit, so the
 * home restores exactly as it was. This file mutates NO home state: it
 * performs no database, settings or layout writes at all — it only calls
 * the callbacks the coordinator passes in.
 *
 * Entry points are all provided by the coordinator (dock long-press on
 * Library, a gesture, Glance long-press, the edit toolbar, or the
 * schedule below). None of them is wired here, by design.
 *
 * Content: a dim scrim over the home, a thin large clock top-left, the
 * favorites as a large text list anchored in the thumb zone (bottom-left
 * on Compact, a centred column on Expanded), and an "All apps" row that
 * opens the List view through [onOpenAllApps]. There is deliberately NO
 * dock and NO widgets in this overlay.
 *
 * Settings keys referenced BY NAME only (the coordinator owns the store
 * entries; this file reads none of them directly):
 * - `calmFavoritesJson` (String, default ""): JSON array of app keys.
 *   Decode with [parseCalmFavorites]; cap with [calmFavoritesCapped].
 * - `calmScheduleJson` (String, default ""): JSON for [CalmSchedule];
 *   decode with [parseCalmSchedule], encode with [encodeCalmSchedule].
 *
 * Dim + blur note, stated plainly: the overlay itself draws a dim scrim.
 * A true backdrop blur of the home behind it is not available from inside
 * this overlay composable; if the coordinator wants a blurred backdrop it
 * renders the blurred wallpaper behind this overlay and this scrim dims
 * it. Nothing here pretends otherwise.
 */

// ------------------------------------------------------------ favorites

/** Maximum favorites shown in Calm mode. */
const val CALM_FAVORITES_MAX: Int = 8

/** Cap a favorites key list to [CALM_FAVORITES_MAX], preserving order. */
fun calmFavoritesCapped(keys: List<String>): List<String> = keys.take(CALM_FAVORITES_MAX)

/** Parse `calmFavoritesJson`: a JSON array of app keys. Never throws. */
fun parseCalmFavorites(json: String): List<String> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        val out = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val v = arr.optString(i, "")
            if (v.isNotBlank()) out += v
        }
        out
    } catch (t: Throwable) {
        emptyList()
    }
}

/** Encode favorites keys back to the `calmFavoritesJson` string form. */
fun encodeCalmFavorites(keys: List<String>): String {
    val arr = JSONArray()
    for (k in keys) arr.put(k)
    return arr.toString()
}

// ------------------------------------------------------------- schedule

/**
 * A user-defined Calm schedule. [days] uses [Calendar.DAY_OF_WEEK]
 * values (SUNDAY = 1 … SATURDAY = 7); [startMinutes]/[endMinutes] are
 * minutes since midnight. An overnight window (end <= start) wraps
 * past midnight. An empty [days] set never activates.
 */
data class CalmSchedule(
    val enabled: Boolean = false,
    val days: Set<Int> = emptySet(),
    val startMinutes: Int = 0,
    val endMinutes: Int = 0,
)

/** True when the schedule is active at the given day + minute. Pure. */
fun CalmSchedule.isActiveAt(dayOfWeek: Int, minuteOfDay: Int): Boolean {
    if (!enabled || days.isEmpty()) return false
    if (dayOfWeek !in days) return false
    if (startMinutes == endMinutes) return false
    return if (startMinutes < endMinutes) {
        minuteOfDay in startMinutes until endMinutes
    } else {
        minuteOfDay >= startMinutes || minuteOfDay < endMinutes
    }
}

/** Parse `calmScheduleJson`. Malformed input yields a disabled schedule. */
fun parseCalmSchedule(json: String): CalmSchedule {
    if (json.isBlank()) return CalmSchedule()
    return try {
        val root = JSONObject(json)
        val days = mutableSetOf<Int>()
        val arr = root.optJSONArray("days")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val d = arr.optInt(i, -1)
                if (d in 1..7) days += d
            }
        }
        CalmSchedule(
            enabled = root.optBoolean("enabled", false),
            days = days,
            startMinutes = root.optInt("start", 0).coerceIn(0, 1439),
            endMinutes = root.optInt("end", 0).coerceIn(0, 1439),
        )
    } catch (t: Throwable) {
        CalmSchedule()
    }
}

/** Encode a schedule back to the `calmScheduleJson` string form. */
fun encodeCalmSchedule(schedule: CalmSchedule): String {
    val root = JSONObject()
    root.put("enabled", schedule.enabled)
    val arr = JSONArray()
    for (d in schedule.days.sorted()) arr.put(d)
    root.put("days", arr)
    root.put("start", schedule.startMinutes)
    root.put("end", schedule.endMinutes)
    return root.toString()
}

// ------------------------------------------------------------ composable

/**
 * The Calm overlay.
 *
 * @param onExit exit Calm mode. Also fired by the "Exit Calm" button at
 *   the top and by a downward swipe (see below).
 * @param favorites already-resolved favorite entries, in display order.
 *   The coordinator resolves them from `calmFavoritesJson` (via
 *   [parseCalmFavorites]) against the catalog, reusing the existing
 *   favorites data where it exists. The overlay itself caps the list at
 *   [CALM_FAVORITES_MAX]; passing more is harmless.
 * @param onLaunch launch handler for a tapped favorite.
 * @param onOpenAllApps opens the List view (the coordinator's existing
 *   list, not a new list built here).
 * @param onChooseFavorites invoked when favorites are empty and the user
 *   taps the prompt's button; the coordinator shows its picker and
 *   persists the choice to `calmFavoritesJson`.
 * @param expanded true on Expanded (favorites as a centred column),
 *   false on Compact/Medium (favorites anchored bottom-left, thumb zone).
 * @param modifier caller sizing; normally fillMaxSize over the home.
 *
 * Swipe-down exit: a mostly-vertical downward drag of at least 96dp
 * anywhere on the overlay (background or a favorite row) exits. Taps are
 * unaffected — a tap lifts without a drag and never triggers the exit.
 */
@Composable
fun CalmMode(
    onExit: () -> Unit,
    favorites: List<AppEntry> = emptyList(),
    onLaunch: (AppEntry) -> Unit = {},
    onOpenAllApps: () -> Unit = {},
    onChooseFavorites: () -> Unit = {},
    expanded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val now = useNow(1000L)
    val is24 = remember { DateFormat.is24HourFormat(context) }
    val shown = remember(favorites) { favorites.take(CALM_FAVORITES_MAX) }

    val swipeThresholdPx = with(LocalDensity.current) { 96.dp.toPx() }
    val swipeDownModifier = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val down = awaitFirstDown()
                var totalY = 0f
                var totalX = 0f
                var done = false
                while (!done) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                    if (!change.pressed) {
                        if (totalY >= swipeThresholdPx && kotlin.math.abs(totalX) <= totalY) {
                            onExit()
                        }
                        done = true
                    } else {
                        totalY += change.position.y - change.previousPosition.y
                        totalX += change.position.x - change.previousPosition.x
                    }
                }
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .then(swipeDownModifier),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
            // Top row: thin large clock top-left, Exit button at the top.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    val timeFmt = remember(is24) {
                        SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault())
                    }
                    val dateFmt = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
                    Text(
                        timeFmt.format(Date(now)),
                        color = Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                    )
                    Text(
                        dateFmt.format(Date(now)),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                }
                TextButton(onClick = onExit) {
                    Text("Exit Calm", color = Color.White, fontSize = 15.sp)
                }
            }

            // Favorites: centred column on Expanded, thumb zone
            // (bottom-left) on Compact/Medium. No dock, no widgets.
            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = if (expanded) Alignment.Center else Alignment.BottomStart,
            ) {
                if (shown.isEmpty()) {
                    Column {
                        Text(
                            "Choose your Calm favorites",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onChooseFavorites) { Text("Choose apps") }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "All apps",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 17.sp,
                            modifier = Modifier.clickable { onOpenAllApps() }.padding(vertical = 12.dp),
                        )
                    }
                } else {
                    Column(
                        modifier = if (expanded) {
                            Modifier.width(420.dp)
                        } else {
                            Modifier.fillMaxWidth()
                        },
                    ) {
                        for (entry in shown) {
                            Text(
                                entry.label,
                                color = Color.White,
                                // Spec range is 28–34sp; 30sp keeps eight
                                // rows inside the thumb zone on phones.
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Light,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { onLaunch(entry) }
                                    .padding(vertical = 10.dp),
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "All apps",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 17.sp,
                            modifier = Modifier.clickable { onOpenAllApps() }.padding(vertical = 12.dp),
                        )
                    }
                }
            }
            // Bottom breathing room keeps the last row clear of the
            // system gesture area on Compact.
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Convenience for tests/callers: the current time as Calendar fields. */
internal fun calmNowFields(nowMs: Long = System.currentTimeMillis()): Pair<Int, Int> {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
    return cal.get(Calendar.DAY_OF_WEEK) to
        (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE))
}
