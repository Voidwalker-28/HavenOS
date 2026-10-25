package app.havenos.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.text.format.DateFormat
import app.havenos.launcher.data.AppEntry
import app.havenos.launcher.data.AppRepository
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.theme.LocalState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * M10 — Today rail (Phase 2) + Medium collapsible strip.
 *
 * The redesign doc's four-zone shell replaces the M9 Today pager page
 * on Expanded landscape: the rail is the left zone (30% of the shell
 * row) and the workspace is the right zone (70%). The rail reuses the
 * M9 Today card bodies (clock+weather, agenda, battery, suggested) so
 * no card logic is forked — only the composition is new. Cards never
 * float over the workspace (the rail is its own column), empty cards
 * collapse to their minimum useful height (the card bodies render
 * their own compact empty states), and the rail is NOT reorderable
 * (reorder happens only via Customize mode, per the doc).
 *
 * Medium (tablet portrait / phone landscape) gets the same content
 * behind a collapsible strip: a 48dp header row (date, weather, next
 * event) that expands to the rail cards in a capped, scrollable area
 * above the workspace. Compact keeps the M9 Today pager page.
 */

// ---------------------------------------------------------------------------
// Shared rail content (Expanded rail + Medium expanded strip)
// ---------------------------------------------------------------------------

@Composable
fun TodayRailContent(
    weather: TodayWeather?,
    weatherOff: Boolean,
    events: List<TodayEvent>,
    calendarGranted: Boolean,
    onRequestCalendarPermission: () -> Unit,
    suggested: List<AppEntry>,
    appRepo: AppRepository,
    onLaunch: (AppEntry) -> Unit,
    onSearch: () -> Unit,
    onAddWidget: () -> Unit,
    onEnterCalm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val now = useNow(1000L)
    val is24 = DateFormat.is24HourFormat(context)
    val localWeather = weather?.copy(city = localityOnly(weather.city))
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
    val dateLine = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header: date/time context + greeting/mode label, plain text
        // (no card frame — the header is the rail's own top, not a card).
        Column {
            Text(
                greeting,
                color = palette.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                dateLine,
                color = palette.onSurface.copy(alpha = 0.65f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TodayClockCard(now, is24, localWeather, weatherOff, palette)
        TodayAgendaCard(events, calendarGranted, onRequestCalendarPermission, palette)
        TodayBatteryCardView(context, palette)
        TodaySuggestedCard(suggested, appRepo, onLaunch, palette)
        // Two or three quick actions (doc §Today rail): Search, Add
        // widget, Calm. Tonal buttons, 48dp targets, labelled.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RailActionButton("Search", onSearch, Modifier.weight(1f))
            RailActionButton("Add widget", onAddWidget, Modifier.weight(1f))
            RailActionButton("Calm", onEnterCalm, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RailActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(palette.surface)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label; role = Role.Button }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = palette.onSurface,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------
// Expanded landscape: the rail itself (scrollable, padded)
// ---------------------------------------------------------------------------

@Composable
fun TodayRail(
    weather: TodayWeather?,
    weatherOff: Boolean,
    events: List<TodayEvent>,
    calendarGranted: Boolean,
    onRequestCalendarPermission: () -> Unit,
    suggested: List<AppEntry>,
    appRepo: AppRepository,
    onLaunch: (AppEntry) -> Unit,
    onSearch: () -> Unit,
    onAddWidget: () -> Unit,
    onEnterCalm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        TodayRailContent(
            weather = weather,
            weatherOff = weatherOff,
            events = events,
            calendarGranted = calendarGranted,
            onRequestCalendarPermission = onRequestCalendarPermission,
            suggested = suggested,
            appRepo = appRepo,
            onLaunch = onLaunch,
            onSearch = onSearch,
            onAddWidget = onAddWidget,
            onEnterCalm = onEnterCalm,
        )
    }
}

// ---------------------------------------------------------------------------
// Medium: collapsible strip above the workspace
// ---------------------------------------------------------------------------

@Composable
fun MediumTodayStrip(
    weather: TodayWeather?,
    weatherOff: Boolean,
    events: List<TodayEvent>,
    calendarGranted: Boolean,
    onRequestCalendarPermission: () -> Unit,
    suggested: List<AppEntry>,
    appRepo: AppRepository,
    onLaunch: (AppEntry) -> Unit,
    onSearch: () -> Unit,
    onAddWidget: () -> Unit,
    onEnterCalm: () -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val live = LocalState.current
    val now = useNow(1000L)
    val shortDate = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(now))
    val weatherBit = if (!weatherOff && weather != null) " · ${weather.tempText}" else ""
    val eventBit = events.firstOrNull()?.let { " · ${it.title}" } ?: ""
    // Motion: slide + fade normally; fade only under reduced motion;
    // instant when motion is Off — the same rule as homeFadeSpec.
    val enter = when {
        live.animSpeed == 0 -> fadeIn(snap())
        live.reduceMotion -> fadeIn(tween(REDUCED_MOTION_FADE_MS))
        else -> slideInVertically(
            animationSpec = tween(speedDuration(300, live.animSpeed)),
            initialOffsetY = { -it / 4 },
        ) + fadeIn(tween(speedDuration(200, live.animSpeed)))
    }
    val exit = when {
        live.animSpeed == 0 -> fadeOut(snap())
        live.reduceMotion -> fadeOut(tween(REDUCED_MOTION_FADE_MS))
        else -> slideOutVertically(
            animationSpec = tween(speedDuration(300, live.animSpeed)),
            targetOffsetY = { -it / 4 },
        ) + fadeOut(tween(speedDuration(200, live.animSpeed)))
    }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onToggle)
                .semantics {
                    contentDescription =
                        if (expanded) "Today, hide details" else "Today, show details"
                    role = Role.Button
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Today",
                color = palette.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "$shortDate$weatherBit$eventBit",
                color = palette.onSurface.copy(alpha = 0.7f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (expanded) "⌃" else "⌄",
                color = palette.onSurface.copy(alpha = 0.7f),
                fontSize = 16.sp,
            )
        }
        AnimatedVisibility(visible = expanded, enter = enter, exit = exit) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight * 0.45f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    TodayRailContent(
                        weather = weather,
                        weatherOff = weatherOff,
                        events = events,
                        calendarGranted = calendarGranted,
                        onRequestCalendarPermission = onRequestCalendarPermission,
                        suggested = suggested,
                        appRepo = appRepo,
                        onLaunch = onLaunch,
                        onSearch = onSearch,
                        onAddWidget = onAddWidget,
                        onEnterCalm = onEnterCalm,
                    )
                }
            }
        }
    }
}
