package app.havenos.launcher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.data.AppEntry
import app.havenos.launcher.data.AppRepository
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.LocalPalette

/**
 * M9 — Today as a DEDICATED PAGE with a card flow (Doc 2 §4.3, task
 * model). This replaces M8's Today-column-beside-the-grid on page 1:
 * the pager now holds [Today page] + grid pages on every size class,
 * and the Today page is a flow of cards, not a cell grid and not a
 * single rotating stack.
 *
 * Ownership split (Doc 3 §2.9): Worker C owns the card CONTENT; this
 * file owns the page shell + layout only. The card bodies are reused
 * verbatim from TodayStack.kt (made internal there, not copied here),
 * so content changes land in both places at once. Card order here is
 * the Doc 2 §4.3 order: Clock + weather, Agenda, Suggested, Battery.
 * Card drag-reorder (Doc 3 §2.9) is NOT wired here: reorder is
 * edit-mode drag logic, owned by the edit worker; the shell renders a
 * fixed order until that wiring exists.
 *
 * Layout: Expanded = 2-column flow, left column ~38% of the page width
 * clamped to 320–460dp ([todayLeftColumnWidthDp]), right column fills,
 * 24dp gutter. Medium/Compact = a single scrollable column of the same
 * cards. The whole page scrolls vertically so cards never clip on short
 * screens; horizontal swipes still change pages (verticalScroll only
 * consumes vertical movement). "Today" header title: 20sp semibold
 * (homeTypeFor todayTitle). The dock is host-owned and stays visible
 * below the pager on this page like every other page.
 *
 * Today cannot be deleted (it never appears in the page overview,
 * which lists grid pages only) but can be disabled in Settings
 * (`todayOn` off removes the page without deleting its card list —
 * the pager model in Grid.kt drops the page; the cards are stateless
 * views over weather/events/history, so nothing is lost).
 *
 * Weather locality (Doc 2 §4.3): only the locality is shown
 * ("Delhi · 24°C · Clear"), never the full administrative name, and a
 * long name ellipsizes at the end of the locality, never mid-word
 * ([localityOnly] + the card's maxLines = 1 + Ellipsis).
 */

// ------------------------------------------------------------------ pure

/**
 * Locality only: everything before the first comma, trimmed. "Delhi,
 * National Capital Territory of Delhi" -> "Delhi"; a name with no comma
 * is unchanged; a blank result falls back to the input.
 */
fun localityOnly(city: String): String {
    val local = city.substringBefore(",").trim()
    return local.ifBlank { city }
}

/**
 * Expanded Today left-column width in dp: ~38% of the page width,
 * clamped to 320–460dp (Doc 2 §4.3 / task). Pure float form so unit
 * tests need no Compose types.
 */
fun todayLeftColumnWidthDp(pageWidthDp: Float): Float =
    (pageWidthDp * 0.38f).coerceIn(320f, 460f)

/**
 * Glance-line legibility from the whole-wallpaper cell stats: mean
 * luminance + max variance over all cells, resolved through the Auto
 * rule. A full-width glance line has no single cell, so the whole
 * wallpaper is the honest sample (per-cell stats stay with icon
 * labels). Null/empty stats (system wallpaper unreadable) keep the
 * legacy look: white text, no pill, always with the soft shadow.
 */
fun glanceStyleFor(cells: List<CellStats>?): LabelStyle {
    // M10 (BUG-012): no stats means the wallpaper is unknown, so the
    // glance line forces its pill (dark behind white text) instead of
    // guessing bare white text against an unseen wallpaper.
    if (cells.isNullOrEmpty()) return LabelStyle(textColor = LEGIBILITY_WHITE, needsPill = true)
    var lumSum = 0.0
    var maxVar = 0f
    for (c in cells) {
        lumSum += c.luminance.toDouble()
        if (c.variance > maxVar) maxVar = c.variance
    }
    val meanLum = (lumSum / cells.size).toFloat()
    return labelStyleFor(LabelMode.Auto, meanLum, maxVar)
}

// ------------------------------------------------------------- indicator

/**
 * The page indicator (Doc 2 §5.4): 8dp dots, current dot elongated to
 * 24dp in the accent colour, others tonal; the Today page (pager page
 * 0 when Today is on) shows a sun glyph instead of a dot so users know
 * where they are. The row is 32dp tall with the dots centred, so the
 * dots sit 12dp above whatever follows (the dock, in the host layout)
 * and 12dp below whatever precedes them. Tapping a dot jumps to that
 * page; long-press opens the page overview; in edit mode a "+" chip
 * (48dp touch target around a 24dp accent circle) adds a page.
 * Dots are indicators, not primary controls — the row itself is the
 * long-press target.
 */
@Composable
fun HomePageIndicator(
    currentPage: Int,
    pageCount: Int,
    todayOn: Boolean,
    editMode: Boolean,
    onPageSelected: (Int) -> Unit,
    onLongPressOverview: () -> Unit,
    onAddPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Box(
        modifier
            .height(32.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPressOverview,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (p in 0 until pageCount) {
                val current = p == currentPage
                val isToday = todayOn && p == 0
                if (isToday) {
                    // Sun glyph: U+2600, no emoji presentation selector,
                    // so it renders as a text glyph in the accent/muted
                    // colour rather than a colour emoji.
                    Box(
                        Modifier
                            .size(24.dp)
                            .combinedClickable(
                                onClick = { onPageSelected(p) },
                                onLongClick = onLongPressOverview,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "☀",
                            color = if (current) palette.accent
                            else palette.onSurface.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .width(if (current) 24.dp else 8.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (current) palette.accent
                                else palette.onSurface.copy(alpha = 0.4f),
                            )
                            .combinedClickable(
                                onClick = { onPageSelected(p) },
                                onLongClick = onLongPressOverview,
                            ),
                    )
                }
            }
            if (editMode) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(48.dp)
                        .combinedClickable(onClick = onAddPage, onLongClick = {}),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(palette.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "+",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------- the page

/**
 * The Today page shell. Card content comes from TodayStack.kt's card
 * bodies (Worker C owns them); this file only lays them out. Weather
 * is shown locality-only (see [localityOnly]).
 */
@Composable
fun TodayPage(
    appRepo: AppRepository,
    onLaunch: (AppEntry) -> Unit,
    weather: TodayWeather?,
    weatherOff: Boolean,
    events: List<TodayEvent>,
    calendarGranted: Boolean,
    onRequestCalendarPermission: () -> Unit,
    suggested: List<AppEntry>,
    sizeClass: HomeSizeClass,
    onEmptyLongPress: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val now = useNow(1000L)
    val is24 = DateFormat.is24HourFormat(context)
    val localWeather = weather?.copy(city = localityOnly(weather.city))
    BoxWithConstraints(modifier) {
        val pageWidth = maxWidth
        val margin = when (sizeClass) {
            HomeSizeClass.Expanded -> pageWidth * 0.05f
            else -> 16.dp
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = margin, vertical = 16.dp),
        ) {
            Text(
                "Today",
                color = palette.onSurface,
                style = homeTypeFor(sizeClass).todayTitle,
                maxLines = 1,
            )
            Spacer(Modifier.height(24.dp))
            when (sizeClass) {
                HomeSizeClass.Expanded -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Column(
                            Modifier.width(todayLeftColumnWidthDp(pageWidth.value).dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            TodayClockCard(now, is24, localWeather, weatherOff, palette)
                            TodayBatteryCardView(context, palette)
                        }
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            TodayAgendaCard(events, calendarGranted, onRequestCalendarPermission, palette)
                            TodaySuggestedCard(suggested, appRepo, onLaunch, palette)
                        }
                    }
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        TodayClockCard(now, is24, localWeather, weatherOff, palette)
                        TodayAgendaCard(events, calendarGranted, onRequestCalendarPermission, palette)
                        TodaySuggestedCard(suggested, appRepo, onLaunch, palette)
                        TodayBatteryCardView(context, palette)
                    }
                }
            }
            // Empty area below the cards: long-press enters edit mode,
            // mirroring the old Today page's empty-area behaviour.
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { onEmptyLongPress() })
                    },
            )
        }
    }
}

/** Card shells: one frame each (the card bodies themselves live in TodayStack.kt). */
@Composable
fun TodayClockCard(
    now: Long,
    is24: Boolean,
    weather: TodayWeather?,
    weatherOff: Boolean,
    palette: app.havenos.launcher.theme.Palette,
) {
    TodayCardFrame {
        ClockWeatherCard(now, is24, weather, weatherOff, palette)
    }
}

@Composable
fun TodayAgendaCard(
    events: List<TodayEvent>,
    calendarGranted: Boolean,
    onRequestCalendarPermission: () -> Unit,
    palette: app.havenos.launcher.theme.Palette,
) {
    TodayCardFrame {
        CalendarCard(events, calendarGranted, onRequestCalendarPermission, palette)
    }
}

@Composable
fun TodaySuggestedCard(
    suggested: List<AppEntry>,
    appRepo: AppRepository,
    onLaunch: (AppEntry) -> Unit,
    palette: app.havenos.launcher.theme.Palette,
) {
    TodayCardFrame {
        SuggestedCard(suggested, appRepo, onLaunch, palette)
    }
}

@Composable
fun TodayBatteryCardView(
    context: android.content.Context,
    palette: app.havenos.launcher.theme.Palette,
) {
    TodayCardFrame {
        BatteryCard(readBattery(context), palette)
    }
}

/** One Today card frame (Doc 2 §6): 28dp radius, material from Look, 16dp padding. */
@Composable
private fun TodayCardFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    GlassSurface(
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth().padding(16.dp)) {
            content()
        }
    }
}
