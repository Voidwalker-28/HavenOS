package app.havenos.launcher.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.AppEntry
import app.havenos.launcher.data.CalEvent
import app.havenos.launcher.data.CalendarRepo
import app.havenos.launcher.data.HomeItem
import app.havenos.launcher.data.ItemKind
import app.havenos.launcher.data.WeatherNet
import app.havenos.launcher.data.parseAllowedCalendars
import app.havenos.launcher.data.weatherInfo
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.theme.LocalState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------
// M9 Worker C (continued): real Weather / Agenda / Suggested hosts.
// In edit mode every interactive handler is disabled so a tap selects the
// widget (Doc 1 §3: taps in edit mode never launch) — content still renders
// live under the selection ring.
// ---------------------------------------------------------------------------

private fun hasPermission(ctx: Context, perm: String): Boolean =
    ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED

/** Open the system calendar app at today. */
fun openCalendarAppIntent(ctx: Context) {
    try {
        val uri = CalendarContract.CONTENT_URI.buildUpon()
            .appendPath("time").appendPath(System.currentTimeMillis().toString()).build()
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (t: Throwable) {
        // no calendar app; stay on home
    }
}

/** Open one event in the calendar app. */
fun openCalendarEventIntent(ctx: Context, event: CalEvent) {
    try {
        val uri = if (event.id > 0L) {
            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id)
        } else {
            CalendarContract.Events.CONTENT_URI
        }
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (t: Throwable) {
        // no calendar app; stay on home
    }
}

/** Insert-event intent for the Empty-state action. */
fun addCalendarEventIntent(ctx: Context) {
    try {
        ctx.startActivity(
            Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (t: Throwable) {
        // no calendar app; stay on home
    }
}

/** Unstack placements: first member stays in place; the rest take the
 * first free spots on the same page. Members that fit nowhere are left
 * out of the result — the editor disables Unstack in that case, so no
 * member is ever silently dropped. */
fun unstackPlacements(
    item: HomeItem,
    members: List<Int>,
    pageItems: List<HomeItem>,
    colsU: Int,
    rowsU: Int,
): List<HomeItem> {
    if (members.size < 2) return emptyList()
    val gridCols = if (colsU > 0) colsU else maxOf(
        pageItems.maxOfOrNull { it.col + it.colSpan } ?: 0,
        item.col + item.colSpan, 8,
    )
    val gridRows = if (rowsU > 0) rowsU else maxOf(
        pageItems.maxOfOrNull { it.row + it.rowSpan } ?: 0,
        item.row + item.rowSpan * members.size, 8,
    )
    val placed = mutableListOf<HomeItem>()
    // The staying member keeps the stack's own rect, so it occupies that
    // rect for the rest of the search — otherwise an extra member could
    // be placed right on top of it.
    val staying = item.copy(kind = members.first(), payload = "")
    val occupied = (pageItems.filter { it.id != item.id } + staying).toMutableList()
    for (kind in members.drop(1)) {
        val sizes = builtinWidgetSizes(kind).map { (w, h) -> w * 2 to h * 2 }
        val found = findSpotForSizes(occupied, item.page, sizes, gridCols, gridRows) ?: return emptyList()
        val placedItem = HomeItem(
            id = "w:$kind:${java.util.UUID.randomUUID()}",
            kind = kind,
            page = item.page,
            col = found.first,
            row = found.second,
            colSpan = found.third.first,
            rowSpan = found.third.second,
            payload = "",
        )
        placed += placedItem
        occupied += placedItem
    }
    return placed
}

// ------------------------------------------------------------ city picker

@Composable
private fun CityPickerDialog(
    onDismiss: () -> Unit,
    onPicked: (label: String, lat: Double, lon: Double) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Triple<String, Double, Double>>>(emptyList()) }
    var error by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weather city") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("City name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (searching) {
                    Spacer(Modifier.height(8.dp))
                    Text("Searching…", color = LocalPalette.current.muted, fontSize = 12.sp)
                }
                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = LocalPalette.current.muted, fontSize = 12.sp)
                }
                LazyColumn(Modifier.fillMaxWidth().height(220.dp)) {
                    items(results, key = { it.first + it.second.toString() }) { r ->
                        TextButton(
                            onClick = { onPicked(r.first, r.second, r.third) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                r.first, fontSize = 13.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = query.isNotBlank() && !searching,
                onClick = {
                    searching = true
                    error = ""
                    scope.launch {
                        val found = withContext(Dispatchers.IO) { WeatherNet.geocode(query.trim(), 5) }
                        searching = false
                        results = found
                        if (found.isEmpty()) error = "City not found — check the name and connection."
                    }
                },
            ) { Text("Search") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ------------------------------------------------------------ weather (real)

@Composable
fun WeatherWidgetReal(app: HavenApp, item: HomeItem, editMode: Boolean, modifier: Modifier) {
    val palette = LocalPalette.current
    val state = LocalState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val data by app.weather.data.collectAsState()
    val updating by app.weather.updating.collectAsState()
    val failed by app.weather.error.collectAsState()
    var showCityDialog by remember { mutableStateOf(false) }
    var locationNote by remember { mutableStateOf("") }

    LaunchedEffect(state.weatherCity, state.weatherLat, state.weatherUnits, state.weatherNetwork) {
        app.weather.refresh(state)
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) return@rememberLauncherForActivityResult
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val loc = try {
            lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (t: Throwable) {
            null
        }
        if (loc == null) {
            locationNote = "No recent location found — choose a city instead."
        } else {
            locationNote = ""
            scope.launch {
                app.settings.setWeather("Current location", loc.latitude.toString(), loc.longitude.toString())
                app.weather.refresh(app.settings.state.value, force = true)
            }
        }
    }

    if (showCityDialog) {
        CityPickerDialog(
            onDismiss = { showCityDialog = false },
            onPicked = { label, lat, lon ->
                showCityDialog = false
                scope.launch {
                    app.settings.setWeather(label, lat.toString(), lon.toString())
                    app.weather.refresh(app.settings.state.value, force = true)
                }
            },
        )
    }

    val cellsW = item.colSpan / 2
    val cellsH = item.rowSpan / 2
    val frameClick = if (editMode) Modifier else Modifier.clickable {
        app.weather.refresh(state, force = true)
    }
    GlassSurface(
        shape = RoundedCornerShape(WIDGET_FRAME_RADIUS_DP.dp),
        modifier = modifier.then(frameClick),
    ) {
        Column(Modifier.fillMaxSize().padding(WIDGET_FRAME_PADDING_DP.dp), verticalArrangement = Arrangement.Center) {
            when {
                !state.weatherNetwork -> {
                    Text("Weather is off in Settings", color = palette.onSurface, fontSize = 14.sp)
                }
                state.weatherLat.isBlank() || state.weatherLon.isBlank() -> {
                    Text("Choose a city", color = palette.onSurface, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Location is only used to pick your current city for local weather.",
                        color = palette.muted, fontSize = 12.sp,
                    )
                    if (locationNote.isNotEmpty()) {
                        Text(locationNote, color = palette.muted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showCityDialog = true },
                            enabled = !editMode,
                        ) { Text("Set city") }
                        TextButton(
                            onClick = { locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                            enabled = !editMode,
                        ) { Text("Use my location") }
                    }
                }
                data == null && updating -> {
                    // Skeleton in the frame — never a bare name.
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.fillMaxWidth(0.5f).height(24.dp).clip(RoundedCornerShape(6.dp)).background(palette.glassBorder))
                        Box(Modifier.fillMaxWidth(0.8f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(palette.glassBorder))
                    }
                }
                data == null && failed -> {
                    Text("Couldn't load", color = palette.onSurface, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { app.weather.refresh(state, force = true) },
                        enabled = !editMode,
                    ) { Text("Retry") }
                }
                data == null -> {
                    Text("Loading weather…", color = palette.muted, fontSize = 13.sp)
                }
                else -> {
                    val w = data!!
                    val (label, symbol) = weatherInfo(w.code)
                    val locality = localityOnly(w.cityName)
                    val stale = isWeatherStale(w.fetchedAtMs, System.currentTimeMillis())
                    if (cellsW <= 2) {
                        // 2×2: icon, temperature, condition.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(symbol, fontSize = 34.sp, color = palette.onSurface)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    formatTempDisplay(w.temp, state.weatherUnits),
                                    color = palette.onSurface, fontSize = 26.sp,
                                    fontWeight = FontWeight.Light, maxLines = 1,
                                )
                                Text(
                                    label, color = palette.muted, fontSize = 11.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(symbol, fontSize = 38.sp, color = palette.onSurface)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    formatTempDisplay(w.temp, state.weatherUnits),
                                    color = palette.onSurface, fontSize = 32.sp,
                                    fontWeight = FontWeight.Light, maxLines = 1,
                                )
                                Text(
                                    locality, color = palette.onSurface, fontSize = 13.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    label, color = palette.muted, fontSize = 12.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (!editMode) {
                                TextButton(onClick = { showCityDialog = true }) { Text("City", fontSize = 11.sp) }
                            }
                        }
                        val today = w.daily.firstOrNull()
                        if (today != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "H: ${formatTempDisplay(today.max, state.weatherUnits)} · " +
                                    "L: ${formatTempDisplay(today.min, state.weatherUnits)}",
                                color = palette.muted, fontSize = 12.sp, maxLines = 1,
                            )
                        }
                        if (w.hourly.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                for (h in w.hourly.take(4)) {
                                    val (_, hs) = weatherInfo(h.code)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(h.label, color = palette.muted, fontSize = 10.sp, maxLines = 1)
                                        Text(hs, fontSize = 15.sp, color = palette.onSurface)
                                        Text(
                                            formatTempDisplay(h.temp, state.weatherUnits),
                                            color = palette.onSurface, fontSize = 11.sp, maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                        if (cellsH >= 3 && w.daily.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                for (d in w.daily.take(5)) {
                                    val (_, ds) = weatherInfo(d.code)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(d.label, color = palette.muted, fontSize = 10.sp, maxLines = 1)
                                        Text(ds, fontSize = 14.sp, color = palette.onSurface)
                                        Text(
                                            "${formatTempDisplay(d.max, state.weatherUnits)} / " +
                                                formatTempDisplay(d.min, state.weatherUnits),
                                            color = palette.onSurface, fontSize = 10.sp, maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (failed) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Couldn't refresh · ", color = palette.muted, fontSize = 11.sp)
                            TextButton(
                                onClick = { app.weather.refresh(state, force = true) },
                                enabled = !editMode,
                            ) { Text("Retry", fontSize = 11.sp) }
                        }
                    }
                    if (stale) WidgetOfflineLine(w.fetchedAtMs)
                    if (updating) Text("Updating…", color = palette.muted, fontSize = 11.sp)
                }
            }
        }
    }
}

// ------------------------------------------------------------ agenda (real)

@Composable
fun AgendaWidgetReal(app: HavenApp, item: HomeItem, editMode: Boolean, modifier: Modifier) {
    val palette = LocalPalette.current
    val state = LocalState.current
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasPermission(context, Manifest.permission.READ_CALENDAR)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }
    val tomorrow = item.payload == "tomorrow"
    val now = useNow(60_000L)
    val dayKey = remember(now) {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))
    }
    var events by remember { mutableStateOf<List<CalEvent>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var lastLoadMs by remember { mutableStateOf(0L) }
    var observerTick by remember { mutableStateOf(0) }

    // Calendar content observer, throttled to once per 30 s (Doc 3 §2.5).
    DisposableEffect(granted) {
        if (!granted) return@DisposableEffect onDispose {}
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                if (System.currentTimeMillis() - lastLoadMs > CALENDAR_OBSERVER_THROTTLE_MS) {
                    observerTick++
                }
            }
        }
        try {
            context.contentResolver.registerContentObserver(
                CalendarContract.Instances.CONTENT_URI, true, observer,
            )
        } catch (t: Throwable) {
            // provider may refuse; the minute ticker still refreshes
        }
        onDispose {
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (t: Throwable) {
                // already gone
            }
        }
    }

    LaunchedEffect(granted, tomorrow, state.calendarsJson, dayKey, observerTick) {
        if (!granted) {
            loaded = false
            return@LaunchedEffect
        }
        val allowed = parseAllowedCalendars(state.calendarsJson)
        val isDayView = item.rowSpan >= 8
        val dayOffset = when {
            tomorrow -> 1
            isDayView -> 0
            else -> null
        }
        events = withContext(Dispatchers.IO) {
            CalendarRepo.upcoming(context, if (isDayView) 24 else 12, allowed, dayOffset)
        }
        lastLoadMs = System.currentTimeMillis()
        loaded = true
    }

    val cellsW = item.colSpan / 2
    val headerClick = if (editMode) Modifier else Modifier.clickable { openCalendarAppIntent(context) }
    GlassSurface(
        shape = RoundedCornerShape(WIDGET_FRAME_RADIUS_DP.dp),
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxSize().padding(WIDGET_FRAME_PADDING_DP.dp)) {
            Row(Modifier.fillMaxWidth().then(headerClick), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (tomorrow) "Tomorrow" else "Agenda",
                    color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, modifier = Modifier.weight(1f),
                )
                if (!tomorrow && item.rowSpan >= 8) {
                    Text(
                        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(now)),
                        color = palette.muted, fontSize = 12.sp, maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            when {
                !granted -> {
                    Text(
                        "Calendar access is needed to show your events.",
                        color = palette.onSurface, fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                        enabled = !editMode,
                    ) { Text("Allow calendar") }
                }
                !loaded -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.fillMaxWidth(0.8f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(palette.glassBorder))
                        Box(Modifier.fillMaxWidth(0.6f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(palette.glassBorder))
                    }
                }
                events.isEmpty() -> {
                    Text("No events today", color = palette.onSurface, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { addCalendarEventIntent(context) },
                        enabled = !editMode,
                    ) { Text("Add event") }
                }
                else -> {
                    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                    val shown = when {
                        cellsW <= 2 -> events.take(1)
                        item.rowSpan >= 8 -> events
                        else -> events.take(3)
                    }
                    for (ev in shown) {
                        val rowClick = if (editMode) Modifier else Modifier.clickable {
                            openCalendarEventIntent(context, ev)
                        }
                        Row(
                            Modifier.fillMaxWidth().then(rowClick).padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(8.dp).clip(CircleShape)
                                    .background(androidx.compose.ui.graphics.Color(ev.color)),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (ev.allDay) "All day" else timeFmt.format(Date(ev.beginMs)),
                                color = palette.muted, fontSize = 12.sp, maxLines = 1,
                                modifier = Modifier.width(if (item.rowSpan >= 8) 72.dp else 88.dp),
                            )
                            Text(
                                ev.title, color = palette.onSurface, fontSize = 13.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------ suggested host

/** Suggested widget fed by the launcher's own launch history (local only). */
@Composable
fun SuggestedWidgetHost(
    app: HavenApp,
    appsByKey: Map<String, AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    onAppMenu: ((AppEntry) -> Unit)? = null,
    editMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val stats by app.appRepo.stats.collectAsState()
    val now = useNow(60_000L)
    val hour = remember(now) {
        Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
    }
    val ranked = remember(stats, appsByKey, hour, now) {
        suggestApps(
            stats = stats.entries.map { LaunchStat(it.key, it.value.first, it.value.second) },
            appsByKey = appsByKey,
            hourOfDay = hour,
            nowMs = now,
            limit = 8,
        )
    }
    SuggestedWidget(
        apps = ranked,
        appRepo = app.appRepo,
        onLaunch = onLaunch,
        onAppMenu = onAppMenu,
        editMode = editMode,
        modifier = modifier,
    )
}
