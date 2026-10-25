package app.havenos.launcher.ui

import android.Manifest
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.AppEntry
import app.havenos.launcher.data.BadgeStore
import app.havenos.launcher.data.CalEvent
import app.havenos.launcher.data.CalendarRepo
import app.havenos.launcher.data.HavenNotificationListener
import app.havenos.launcher.data.HomeItem
import app.havenos.launcher.data.ItemKind
import app.havenos.launcher.data.WeatherNet
import app.havenos.launcher.data.parseAllowedCalendars
import app.havenos.launcher.data.weatherInfo
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.LocalFont
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.theme.LocalState
import app.havenos.launcher.theme.LocalTokens
import app.havenos.launcher.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/** Shared 1-second ticker for clocks and other live widgets. */
@Composable
fun useNow(intervalMs: Long = 1000L): Long {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(intervalMs) {
        while (true) {
            delay(intervalMs)
            now = System.currentTimeMillis()
        }
    }
    return now
}

private fun hasCalendarPermission(ctx: Context): Boolean =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

private fun fmtTemp(v: Double): String = "${Math.round(v)}°"

/** Own widgets stay translucent glass by default: tint capped, flat fallback lighter. */
@Composable
private fun WidgetSurface(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    GlassSurface(modifier, shape = RoundedCornerShape(28.dp), tintCap = 0.6f, flatAlpha = 0.8f, content = content)
}

/** Widget dispatcher: renders any non-APP/FOLDER home item inside a glass surface. */
@Composable
fun WidgetContent(
    app: HavenApp,
    item: HomeItem,
    appsByKey: Map<String, AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean = false,
    // Seams (M9): onOpenSearch feeds Quick search; onAppMenu feeds
    // Suggested long-press menus; pageItems/grid dims feed Stack unstack.
    onOpenSearch: () -> Unit = {},
    onAppMenu: ((AppEntry) -> Unit)? = null,
    pageItems: List<HomeItem> = emptyList(),
    gridColsU: Int = 0,
    gridRowsU: Int = 0,
) {
    when (item.kind) {
        ItemKind.CLOCK -> ClockWidget(app, item, editMode, modifier)
        ItemKind.WEATHER -> WeatherWidgetReal(app, item, editMode, modifier)
        // At-a-glance is an Agenda payload variant ("glance"), the same
        // pattern as Agenda "tomorrow" and the Clock variants — so no new
        // ItemKind (Db.kt) is needed. Coordinator picker line (HostWidgets,
        // not this file): OwnWidgetDef("At a glance", ItemKind.AGENDA, 8, 4, "glance").
        ItemKind.AGENDA -> if (item.payload == "glance") AtAGlanceWidget(app, modifier)
            else AgendaWidgetReal(app, item, editMode, modifier)
        ItemKind.SUGGESTED -> SuggestedWidgetHost(app, appsByKey, onLaunch, onAppMenu, editMode, modifier)
        ItemKind.BATTERY -> BatteryWidget(modifier)
        ItemKind.QUICK_SEARCH -> QuickSearchWidget(onOpenSearch, editMode, modifier)
        ItemKind.CALENDAR -> CalendarWidget(modifier)
        ItemKind.NOW_PLAYING -> NowPlayingWidget(modifier)
        ItemKind.STORAGE -> StorageWidget(modifier)
        ItemKind.COUNTDOWN -> CountdownWidget(app, item, modifier)
        ItemKind.TASKS -> TasksWidget(app, modifier)
        ItemKind.NOTES -> NotesWidget(app, item, modifier)
        ItemKind.GALLERY -> GalleryWidget(app, item, modifier)
        ItemKind.QUICK_TILES -> QuickTilesWidget(app, modifier)
        ItemKind.CLOCK_PAGE -> ClockPageWidget(app, item, appsByKey, onLaunch, modifier)
        ItemKind.STACK -> StackWidget(app, item, appsByKey, onLaunch, editMode, modifier, pageItems, gridColsU, gridRowsU, onOpenSearch, onAppMenu)
        ItemKind.TP_WIDGET -> TpWidgetView(app, item, modifier)
        else -> WidgetSurface(modifier) { Box(Modifier.fillMaxSize()) }
    }
}

// ---------------------------------------------------------------- CLOCK

private val CLOCK_VARIANTS = listOf("serif", "digital", "analog", "flip", "editorial")

/** Clock payload: variant, optionally "|tz:<ZoneId>" for a second zone. */
private fun clockSecondZoneId(payload: String): String? {
    val part = payload.split("|").firstOrNull { it.startsWith("tz:") } ?: return null
    return part.removePrefix("tz:").takeIf { it.isNotBlank() }
}

private fun openClockAppIntent(ctx: android.content.Context) {
    try {
        ctx.startActivity(
            Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (t: Throwable) {
        // No clock app handles the intent; stay on home.
    }
}

@Composable
private fun ClockWidget(app: HavenApp, item: HomeItem, editMode: Boolean, modifier: Modifier) {
    val palette = LocalPalette.current
    val tokens = LocalTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val now = useNow(1000L)
    val is24 = remember { DateFormat.is24HourFormat(context) }
    // Variant is the part before any "|tz:..." suffix; the suffix survives
    // variant cycling so a second time zone is never lost by the Aa button.
    val variant = item.payload.substringBefore("|").ifBlank { "serif" }
    val secondZone = remember(item.payload) { clockSecondZoneId(item.payload) }
    val secondTime = remember(now, secondZone, is24) {
        if (secondZone == null) {
            null
        } else {
            try {
                val tz = java.util.TimeZone.getTimeZone(secondZone)
                val fmt = SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault())
                fmt.timeZone = tz
                fmt.format(Date(now)) to tz.getDisplayName(false, java.util.TimeZone.SHORT, Locale.getDefault())
            } catch (t: Throwable) {
                null
            }
        }
    }
    val nextAlarm = remember(now) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            am?.nextAlarmClock?.triggerTime?.let { trigger ->
                SimpleDateFormat("EEE h:mm a", Locale.getDefault()).format(Date(trigger))
            }
        } catch (t: Throwable) {
            null
        }
    }

    WidgetSurface(modifier) {
        Box(
            Modifier.fillMaxSize()
                .then(if (editMode) Modifier else Modifier.clickable { openClockAppIntent(context) })
                .padding(16.dp),
        ) {
            when (variant) {
                "analog" -> AnalogClockFace(now, palette, Modifier.fillMaxSize())
                "flip" -> FlipClockFace(now, is24, palette)
                "editorial" -> EditorialClockFace(now, is24, palette, tokens.serifClock)
                "digital" -> DigitalClockFace(now, is24, palette, serif = false)
                else -> DigitalClockFace(now, is24, palette, serif = true)
            }
            // Second time zone + next alarm, on wide widgets only (tap
            // still opens the clock app; the lines are display-only).
            if (item.colSpan >= 8 && (secondTime != null || nextAlarm != null)) {
                Column(Modifier.align(Alignment.BottomStart)) {
                    if (secondTime != null) {
                        Text(
                            "${secondTime.first} · ${secondTime.second}",
                            color = palette.muted, fontSize = 12.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (nextAlarm != null) {
                        Text(
                            "Alarm $nextAlarm",
                            color = palette.muted, fontSize = 12.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (editMode) {
                // Long-press is taken by drag, so the variant switch lives here in edit mode.
                TextButton(
                    onClick = {
                        val idx = CLOCK_VARIANTS.indexOf(variant).coerceAtLeast(0)
                        val next = CLOCK_VARIANTS[(idx + 1) % CLOCK_VARIANTS.size]
                        val suffix = if ("|" in item.payload) {
                            item.payload.substring(item.payload.indexOf("|"))
                        } else {
                            ""
                        }
                        scope.launch { app.db.homeItems().upsertAll(listOf(item.copy(payload = next + suffix))) }
                    },
                    modifier = Modifier.align(Alignment.TopEnd),
                ) { Text("Aa", color = palette.accent, fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun DigitalClockFace(now: Long, is24: Boolean, palette: Palette, serif: Boolean) {
    val timeFmt = remember(is24) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val font = if (serif) FontFamily.Serif else LocalFont.current
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            timeFmt.format(Date(now)),
            color = palette.onSurface,
            fontSize = 46.sp,
            fontWeight = if (serif) FontWeight.Light else FontWeight.Medium,
            fontFamily = font,
            maxLines = 1,
        )
        Text(dateFmt.format(Date(now)), color = palette.muted, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
private fun EditorialClockFace(now: Long, is24: Boolean, palette: Palette, serif: Boolean) {
    val dayFmt = remember { SimpleDateFormat("EEEE", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }
    val timeFmt = remember(is24) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()) }
    val font = if (serif) FontFamily.Serif else LocalFont.current
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(dayFmt.format(Date(now)), color = palette.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(
            SimpleDateFormat("d", Locale.getDefault()).format(Date(now)),
            color = palette.onSurface, fontSize = 52.sp, fontWeight = FontWeight.Light, fontFamily = font, maxLines = 1,
        )
        Text(dateFmt.format(Date(now)), color = palette.muted, fontSize = 13.sp, maxLines = 1)
        Text(timeFmt.format(Date(now)), color = palette.onSurface, fontSize = 20.sp, maxLines = 1)
    }
}

@Composable
private fun FlipClockFace(now: Long, is24: Boolean, palette: Palette) {
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    var hour = cal.get(Calendar.HOUR_OF_DAY)
    val ampm = if (hour < 12) "AM" else "PM"
    if (!is24) {
        hour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
    }
    val hh = "%02d".format(hour)
    val mm = "%02d".format(cal.get(Calendar.MINUTE))
    val dateFmt = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FlipTile(hh)
            Text(":", color = palette.onSurface, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            FlipTile(mm)
            if (!is24) Text(ampm, color = palette.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Text(dateFmt.format(Date(now)), color = palette.muted, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun FlipTile(text: String) {
    Box(
        Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF17181C)).padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1)
    }
}

@Composable
private fun AnalogClockFace(now: Long, palette: Palette, modifier: Modifier) {
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val sec = cal.get(Calendar.SECOND).toFloat()
    val min = cal.get(Calendar.MINUTE) + sec / 60f
    val hour = cal.get(Calendar.HOUR) + min / 60f
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val c = center
        drawCircle(palette.surface.copy(alpha = 0.35f), radius = r, center = c)
        drawCircle(palette.glassBorder, radius = r, center = c, style = Stroke(width = 2f))
        for (i in 0 until 12) {
            val a = Math.toRadians((i * 30 - 90).toDouble())
            val inner = r * 0.84f
            val outer = r * 0.93f
            drawLine(
                palette.onSurface,
                Offset(c.x + (cos(a) * inner).toFloat(), c.y + (sin(a) * inner).toFloat()),
                Offset(c.x + (cos(a) * outer).toFloat(), c.y + (sin(a) * outer).toFloat()),
                strokeWidth = if (i % 3 == 0) 4f else 2f,
            )
        }
        fun hand(frac: Float, len: Float, width: Float, color: Color) {
            val a = Math.toRadians((frac * 360f - 90f).toDouble())
            drawLine(
                color, c,
                Offset(c.x + (cos(a) * len).toFloat(), c.y + (sin(a) * len).toFloat()),
                strokeWidth = width,
            )
        }
        hand(hour / 12f, r * 0.5f, 7f, palette.onSurface)
        hand(min / 60f, r * 0.74f, 5f, palette.onSurface)
        hand(sec / 60f, r * 0.86f, 2f, palette.accent)
        drawCircle(palette.accent, radius = 5f, center = c)
    }
}

@Composable
private fun CalendarWidget(modifier: Modifier) {
    val palette = LocalPalette.current
    val state = LocalState.current
    val context = LocalContext.current
    val granted = remember { hasCalendarPermission(context) }
    val now = useNow(60_000L)
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val today = cal.get(Calendar.DAY_OF_MONTH)
    var eventDays by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(granted, state.calendarsJson, year, month) {
        if (granted) {
            val allowed = parseAllowedCalendars(state.calendarsJson)
            eventDays = withContext(Dispatchers.IO) { CalendarRepo.monthEventDays(context, year, month, allowed) }
        }
    }

    val firstDow = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, 1)
        }.get(Calendar.DAY_OF_WEEK)
    }
    val daysInMonth = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val monthLabel = remember(year, month) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(
            Calendar.getInstance().apply {
                set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, 1)
            }.time,
        )
    }

    WidgetSurface(modifier) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(monthLabel, color = palette.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                for (d in listOf("S", "M", "T", "W", "T", "F", "S")) {
                    Text(d, color = palette.muted, fontSize = 10.sp, modifier = Modifier.weight(1f), maxLines = 1)
                }
            }
            Spacer(Modifier.height(4.dp))
            val cells = (List(firstDow - 1) { 0 } + (1..daysInMonth).toList())
            val rows = cells.chunked(7)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
                for (rowCells in rows) {
                    Row(Modifier.fillMaxWidth()) {
                        for (day in rowCells) {
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (day > 0) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            Modifier
                                                .size(24.dp)
                                                .then(
                                                    if (day == today) {
                                                        Modifier.clip(CircleShape).background(palette.accent)
                                                    } else {
                                                        Modifier
                                                    },
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                day.toString(),
                                                color = if (day == today) Color.White else palette.onSurface,
                                                fontSize = 12.sp,
                                                fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                            )
                                        }
                                        Box(
                                            Modifier.size(4.dp).clip(CircleShape)
                                                .background(if (day in eventDays) palette.accent else Color.Transparent),
                                        )
                                    }
                                }
                            }
                        }
                        repeat(7 - rowCells.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- NOW PLAYING

private data class MediaInfo(
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val playing: Boolean,
    val controller: MediaController,
)

private fun readMediaSession(ctx: Context): MediaInfo? = try {
    val mm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return null
    val sessions = mm.getActiveSessions(ComponentName(ctx, HavenNotificationListener::class.java))
    val controller = sessions.firstOrNull { it.metadata != null } ?: sessions.firstOrNull() ?: return null
    val md = controller.metadata
    MediaInfo(
        title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() } ?: "Unknown title",
        artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "",
        art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART),
        playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING,
        controller = controller,
    )
} catch (t: Throwable) {
    null
}

@Composable
private fun NowPlayingWidget(modifier: Modifier) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val connected by BadgeStore.connected.collectAsState()
    var tick by remember { mutableStateOf(0) }
    var info by remember { mutableStateOf<MediaInfo?>(null) }

    LaunchedEffect(connected) {
        while (connected) {
            delay(2000)
            tick++
        }
    }
    LaunchedEffect(tick, connected) {
        info = if (connected) withContext(Dispatchers.IO) { readMediaSession(context) } else null
    }

    WidgetSurface(modifier) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.CenterStart) {
            when {
                !connected -> Column {
                    Text(
                        "Notification access needed for Now Playing",
                        color = palette.onSurface, fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        } catch (t: Throwable) {
                            // settings screen unavailable on this device
                        }
                    }) { Text("Enable") }
                }
                info == null -> Text("Nothing playing", color = palette.muted, fontSize = 13.sp)
                else -> {
                    val m = info!!
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (m.art != null) {
                            Image(
                                bitmap = m.art.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                            )
                        } else {
                            Box(
                                Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(palette.accent),
                                contentAlignment = Alignment.Center,
                            ) { Text("♪", color = Color.White, fontSize = 24.sp) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.title, color = palette.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (m.artist.isNotBlank()) {
                                Text(m.artist, color = palette.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Row {
                                TextButton(onClick = {
                                    try { m.controller.transportControls.skipToPrevious() } catch (t: Throwable) {}
                                    tick++
                                }) { Text("Prev", fontSize = 12.sp) }
                                TextButton(onClick = {
                                    try {
                                        if (m.playing) m.controller.transportControls.pause()
                                        else m.controller.transportControls.play()
                                    } catch (t: Throwable) {}
                                    tick++
                                }) { Text(if (m.playing) "Pause" else "Play", fontSize = 12.sp) }
                                TextButton(onClick = {
                                    try { m.controller.transportControls.skipToNext() } catch (t: Throwable) {}
                                    tick++
                                }) { Text("Next", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- STORAGE

private data class StorageInfo(val storageUsed: Long, val storageTotal: Long, val memUsed: Long, val memTotal: Long)

private fun readStorageInfo(ctx: Context): StorageInfo? = try {
    val sf = StatFs(Environment.getDataDirectory().path)
    val sTotal = sf.totalBytes
    val sFree = sf.availableBytes
    val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val mi = ActivityManager.MemoryInfo()
    am?.getMemoryInfo(mi)
    StorageInfo(
        storageUsed = (sTotal - sFree).coerceAtLeast(0L),
        storageTotal = sTotal,
        memUsed = (mi.totalMem - mi.availMem).coerceAtLeast(0L),
        memTotal = mi.totalMem,
    )
} catch (t: Throwable) {
    null
}

private fun fmtGb(bytes: Long): String = String.format(Locale.getDefault(), "%.0f GB", bytes / 1_000_000_000.0)

@Composable
private fun StorageWidget(modifier: Modifier) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    var info by remember { mutableStateOf<StorageInfo?>(null) }
    LaunchedEffect(Unit) { info = withContext(Dispatchers.IO) { readStorageInfo(context) } }

    WidgetSurface(modifier) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
            val s = info
            if (s == null) {
                Text("Storage unavailable", color = palette.muted, fontSize = 13.sp)
            } else {
                StorageBar("Storage", s.storageUsed, s.storageTotal, palette)
                Spacer(Modifier.height(10.dp))
                StorageBar("Memory", s.memUsed, s.memTotal, palette)
                Spacer(Modifier.height(10.dp))
                Button(onClick = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    } catch (t: Throwable) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (t2: Throwable) {
                            // no settings screen reachable
                        }
                    }
                }) { Text("Manage storage") }
            }
        }
    }
}

@Composable
private fun StorageBar(label: String, used: Long, total: Long, palette: Palette) {
    val frac = if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = palette.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Text("${fmtGb(used)} of ${fmtGb(total)} used", color = palette.muted, fontSize = 11.sp, maxLines = 1)
    }
    Spacer(Modifier.height(4.dp))
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(palette.glassBorder)) {
        Box(Modifier.fillMaxWidth(frac).height(8.dp).clip(RoundedCornerShape(50)).background(palette.accent))
    }
}

// ---------------------------------------------------------------- COUNTDOWN

private fun parseCountdown(payload: String): Pair<String, Long> = try {
    val o = JSONObject(payload)
    o.optString("label", "Countdown") to o.optLong("target", 0L)
} catch (t: Throwable) {
    "Countdown" to 0L
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountdownWidget(app: HavenApp, item: HomeItem, modifier: Modifier) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    val now = useNow(1000L)
    val (label, target) = remember(item.payload) { parseCountdown(item.payload) }
    var showEdit by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var editLabel by remember { mutableStateOf(label) }
    var editTarget by remember { mutableStateOf(target) }

    WidgetSurface(modifier) {
        Box(
            Modifier.fillMaxSize().clickable {
                editLabel = label; editTarget = target; showEdit = true
            }.padding(16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column {
                Text(label, color = palette.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                when {
                    target <= 0L -> Text("Tap to set a countdown", color = palette.onSurface, fontSize = 15.sp)
                    target - now <= 0L -> Text("$label — reached", color = palette.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    else -> {
                        val remainSec = (target - now) / 1000L
                        val d = remainSec / 86_400L
                        val h = (remainSec % 86_400L) / 3_600L
                        val m = (remainSec % 3_600L) / 60L
                        val s = remainSec % 60L
                        Text(
                            "${d}d ${"%02d".format(h)}h ${"%02d".format(m)}m ${"%02d".format(s)}s",
                            color = palette.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Light, maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Countdown") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editLabel,
                        onValueChange = { editLabel = it },
                        label = { Text("Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (editTarget > 0L) {
                            "Target: ${SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(editTarget))} (00:00)"
                        } else {
                            "No date set"
                        },
                        color = palette.muted, fontSize = 12.sp,
                    )
                    TextButton(onClick = { showDatePicker = true }) { Text("Pick date") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showEdit = false
                    val payload = JSONObject()
                        .put("label", editLabel.ifBlank { "Countdown" })
                        .put("target", editTarget)
                        .toString()
                    scope.launch { app.db.homeItems().upsertAll(listOf(item.copy(payload = payload))) }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Cancel") } },
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (editTarget > 0L) editTarget else System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { editTarget = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// ---------------------------------------------------------------- TASKS

@Composable
private fun TasksWidget(app: HavenApp, modifier: Modifier) {
    val palette = LocalPalette.current
    val tasks by app.tasks.tasks.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var newText by remember { mutableStateOf("") }
    val open = tasks.withIndex().filter { !it.value.done }

    WidgetSurface(modifier) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Tasks", color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { newText = ""; showAdd = true }) { Text("+ Add", fontSize = 12.sp) }
            }
            if (open.isEmpty()) {
                Text("All done", color = palette.muted, fontSize = 13.sp)
            } else {
                for ((index, task) in open.take(5)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { app.tasks.toggle(index) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(16.dp).clip(CircleShape)
                                .border(1.5.dp, palette.muted, CircleShape),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(task.text, color = palette.onSurface, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("New task") },
            text = {
                OutlinedTextField(
                    value = newText,
                    onValueChange = { newText = it },
                    label = { Text("Task") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    app.tasks.add(newText)
                    showAdd = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } },
        )
    }
}

// ---------------------------------------------------------------- NOTES

@Composable
private fun NotesWidget(app: HavenApp, item: HomeItem, modifier: Modifier) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var showEdit by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(item.payload) }

    WidgetSurface(modifier) {
        Box(
            Modifier.fillMaxSize().clickable {
                editText = item.payload; showEdit = true
            }.padding(16.dp),
        ) {
            if (item.payload.isBlank()) {
                Text("Tap to write a note", color = palette.muted, fontSize = 13.sp)
            } else {
                Text(item.payload, color = palette.onSurface, fontSize = 14.sp, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Note") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showEdit = false
                    scope.launch { app.db.homeItems().upsertAll(listOf(item.copy(payload = editText))) }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Cancel") } },
        )
    }
}

// ---------------------------------------------------------------- GALLERY

private fun decodeGalleryImage(ctx: Context, uriStr: String): Bitmap? = try {
    val uri = Uri.parse(uriStr)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0) {
        null
    } else {
        var sample = 1
        while (bounds.outWidth / sample > 900) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }
} catch (t: Throwable) {
    null
}

@Composable
private fun GalleryWidget(app: HavenApp, item: HomeItem, modifier: Modifier) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }

    val bmp by produceState<Bitmap?>(initialValue = null, item.payload) {
        value = if (item.payload.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) { decodeGalleryImage(context, item.payload) }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        showDialog = false
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (t: Throwable) {
                // provider may not grant persistable access; the uri still works this session
            }
            scope.launch { app.db.homeItems().upsertAll(listOf(item.copy(payload = uri.toString()))) }
        }
    }

    WidgetSurface(modifier) {
        Box(Modifier.fillMaxSize().clickable { showDialog = true }) {
            if (bmp != null) {
                Image(
                    bitmap = bmp!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (item.payload.isBlank()) "Tap to choose a photo" else "Photo unavailable — tap to pick another",
                        color = palette.muted, fontSize = 13.sp,
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Gallery photo") },
            text = { Text("Pick an image to show in this widget.", color = palette.muted, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { picker.launch(arrayOf("image/*")) }) { Text("Choose image") }
            },
            dismissButton = {
                Row {
                    if (item.payload.isNotBlank()) {
                        TextButton(onClick = {
                            showDialog = false
                            scope.launch { app.db.homeItems().upsertAll(listOf(item.copy(payload = ""))) }
                        }) { Text("Clear") }
                    }
                    TextButton(onClick = { showDialog = false }) { Text("Cancel") }
                }
            },
        )
    }
}

// ---------------------------------------------------------------- QUICK TILES

private data class Tile(val label: String, val active: Boolean?, val onClick: () -> Unit)

@Composable
private fun QuickTilesWidget(app: HavenApp, modifier: Modifier) {
    val palette = LocalPalette.current
    val state = LocalState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var torchOn by remember { mutableStateOf(false) }

    fun startSettings(intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (t: Throwable) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (t2: Throwable) {
                // no settings screen reachable
            }
        }
    }

    val tiles = listOf(
        Tile("Flash", torchOn) {
            try {
                val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                val id = cm?.cameraIdList?.firstOrNull { cid ->
                    cm.getCameraCharacteristics(cid).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }
                if (cm != null && id != null) {
                    cm.setTorchMode(id, !torchOn)
                    torchOn = !torchOn
                }
            } catch (t: Throwable) {
                // torch busy / unavailable
            }
        },
        Tile("Wi-Fi", null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startSettings(Intent(Settings.Panel.ACTION_WIFI))
            } else {
                startSettings(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
        },
        Tile("Bluetooth", null) { startSettings(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
        Tile("Dark", state.darkMode == 2) {
            scope.launch { app.settings.setDarkMode(if (state.darkMode == 2) 1 else 2) }
        },
        Tile("Settings", null) { startSettings(Intent(Settings.ACTION_SETTINGS)) },
    )

    WidgetSurface(modifier) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (tile in tiles) {
                Column(
                    Modifier.weight(1f).clickable { tile.onClick() }.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape)
                            .background(palette.surface.copy(alpha = 0.55f))
                            .border(1.dp, palette.glassBorder, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(9.dp).clip(CircleShape).background(
                                when (tile.active) {
                                    true -> palette.accent
                                    false -> palette.muted
                                    null -> palette.glassBorder
                                },
                            ),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(tile.label, color = palette.onSurface, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- CLOCK PAGE

@Composable
private fun ClockPageWidget(
    app: HavenApp,
    item: HomeItem,
    appsByKey: Map<String, AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier,
) {
    val palette = LocalPalette.current
    val state = LocalState.current
    val context = LocalContext.current
    val now = useNow(1000L)
    val is24 = remember { DateFormat.is24HourFormat(context) }
    val weather by app.weather.data.collectAsState()

    LaunchedEffect(state.weatherCity, state.weatherLat, state.weatherUnits, state.weatherNetwork) {
        app.weather.refresh(state)
    }

    val shortcuts = remember(item.payload, appsByKey) {
        val keys = try {
            val arr = JSONArray(item.payload)
            List(arr.length()) { i -> arr.optString(i) }.filter { it.isNotBlank() }
        } catch (t: Throwable) {
            emptyList()
        }
        val picked = keys.mapNotNull { appsByKey[it] }
        if (picked.isNotEmpty()) {
            picked.take(4)
        } else {
            appsByKey.values.sortedBy { it.label.lowercase() }.take(4)
        }
    }

    val timeFmt = remember(is24) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    WidgetSurface(modifier) {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                timeFmt.format(Date(now)),
                color = palette.onSurface, fontSize = 68.sp, fontWeight = FontWeight.Light, maxLines = 1,
            )
            Text(dateFmt.format(Date(now)), color = palette.muted, fontSize = 15.sp, maxLines = 1)
            val w = weather
            if (w != null) {
                val (wlabel, wsymbol) = weatherInfo(w.code)
                Spacer(Modifier.height(6.dp))
                Text(
                    "$wsymbol ${formatTempDisplay(w.temp, state.weatherUnits)} $wlabel · ${localityOnly(w.cityName)}",
                    color = palette.onSurface, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (shortcuts.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                val rows = shortcuts.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (rowEntries in rows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (entry in rowEntries) {
                                Box(
                                    Modifier.width(120.dp).clip(RoundedCornerShape(16.dp))
                                        .background(palette.surface.copy(alpha = 0.5f))
                                        .border(1.dp, palette.glassBorder, RoundedCornerShape(16.dp))
                                        .clickable { onLaunch(entry) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        AppIconImage(entry, app.appRepo, 38.dp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            entry.label, color = palette.onSurface, fontSize = 11.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- STACK

private fun defaultPayloadFor(kind: Int): String = when (kind) {
    ItemKind.CLOCK -> "serif"
    else -> ""
}

@Composable
private fun StackWidget(
    app: HavenApp,
    item: HomeItem,
    appsByKey: Map<String, AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    editMode: Boolean,
    modifier: Modifier,
    pageItems: List<HomeItem> = emptyList(),
    gridColsU: Int = 0,
    gridRowsU: Int = 0,
    onOpenSearch: () -> Unit = {},
    onAppMenu: ((AppEntry) -> Unit)? = null,
) {
    val palette = LocalPalette.current
    val state = LocalState.current
    val scope = rememberCoroutineScope()
    val members = remember(item.payload) { parseStackMembers(item.payload) }
    var index by remember(item.id) { mutableStateOf(0) }
    var lastTouchMs by remember(item.id) { mutableStateOf(0L) }
    var showEdit by remember { mutableStateOf(false) }
    var editMembers by remember(item.payload) { mutableStateOf(parseStackMembers(item.payload)) }
    val tick = useNow(10_000L)
    val safeIndex = if (members.isEmpty()) 0 else index.coerceIn(0, members.size - 1)
    // Time rotation (morning -> weather/agenda, evening -> suggested;
    // media is LATER and never a member here), paused 10 s after touch.
    val autoKind = remember(members, state.smartStackAutoRotate, tick) {
        if (!state.smartStackAutoRotate) {
            null
        } else {
            val hour = Calendar.getInstance().apply { timeInMillis = tick }
                .get(Calendar.HOUR_OF_DAY)
            stackAutoMember(members, hour)
        }
    }
    val paused = isStackRotationPaused(lastTouchMs, tick)
    val shownKind = when {
        members.isEmpty() -> null
        !state.stacksOn -> members.first()
        autoKind != null && !paused -> autoKind
        else -> members[safeIndex]
    }
    val shownIndex = members.indexOf(shownKind).coerceAtLeast(0)

    Box(modifier) {
        if (shownKind == null) {
            WidgetSurface(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Empty stack", color = palette.muted, fontSize = 13.sp)
                }
            }
        } else {
            // shortcut: stack members render with default payloads, so per-member
            // payload edits (note text, countdown target) are not kept in this phase.
            val synthetic = HomeItem(
                id = item.id + "#stack",
                kind = shownKind,
                page = -1,
                col = item.col,
                row = item.row,
                colSpan = item.colSpan,
                rowSpan = item.rowSpan,
                payload = defaultPayloadFor(shownKind),
            )
            Box(
                Modifier.fillMaxSize().then(
                    if (state.stacksOn && members.size > 1) {
                        Modifier.pointerInput(item.id, members.size) {
                            var total = 0f
                            detectVerticalDragGestures(
                                onDragStart = {
                                    total = 0f
                                    lastTouchMs = System.currentTimeMillis()
                                },
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    total += amount
                                },
                                onDragEnd = {
                                    val threshold = 36.dp.toPx()
                                    if (total < -threshold) {
                                        index = (safeIndex + 1) % members.size
                                    } else if (total > threshold) {
                                        index = (safeIndex - 1 + members.size) % members.size
                                    }
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
            ) {
                WidgetContent(app, synthetic, appsByKey, onLaunch, Modifier.fillMaxSize(), editMode, onOpenSearch, onAppMenu)
            }
            if (state.stacksOn && members.size > 1) {
                // Dots on the right edge, vertically centred, 48dp touch
                // area via the surrounding stack swipe region.
                Column(
                    Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    for (i in members.indices) {
                        Box(
                            Modifier.size(6.dp).clip(CircleShape)
                                .background(if (i == shownIndex) palette.accent else palette.muted.copy(alpha = 0.5f)),
                        )
                    }
                }
            }
            if (editMode) {
                // Widget-side Stack action (Worker A owns the bar; this
                // button is the stack's own editor entry in edit mode).
                TextButton(
                    onClick = {
                        editMembers = parseStackMembers(item.payload)
                        showEdit = true
                    },
                    modifier = Modifier.align(Alignment.TopEnd),
                ) { Text("Edit stack", color = palette.accent, fontSize = 12.sp) }
            }
        }
    }
    if (showEdit) {
        StackEditorDialog(
            members = editMembers,
            onMembersChange = { editMembers = it },
            onDismiss = { showEdit = false },
            onSave = {
                showEdit = false
                scope.launch {
                    if (editMembers.size == 1) {
                        val first = editMembers.first()
                        app.db.homeItems().upsertAll(
                            listOf(item.copy(kind = first, payload = defaultPayloadFor(first))),
                        )
                    } else {
                        app.db.homeItems().upsertAll(
                            listOf(item.copy(payload = stackMembersJson(editMembers))),
                        )
                    }
                }
            },
            onUnstack = {
                showEdit = false
                scope.launch {
                    val first = editMembers.first()
                    val extras = unstackPlacements(item, editMembers, pageItems, gridColsU, gridRowsU)
                    val staying = item.copy(kind = first, payload = defaultPayloadFor(first))
                    app.db.homeItems().upsertAll(listOf(staying) + extras)
                }
            },
            unstackEnabled = editMembers.size >= 2 &&
                unstackPlacements(item, editMembers, pageItems, gridColsU, gridRowsU).size ==
                editMembers.size - 1,
        )
    }
}

private fun stackKindLabel(kind: Int): String = when (kind) {
    ItemKind.CLOCK -> "Clock"
    ItemKind.WEATHER -> "Weather"
    ItemKind.AGENDA -> "Agenda"
    ItemKind.CALENDAR -> "Calendar"
    ItemKind.NOW_PLAYING -> "Now Playing"
    ItemKind.STORAGE -> "Storage"
    ItemKind.COUNTDOWN -> "Countdown"
    ItemKind.TASKS -> "Tasks"
    ItemKind.NOTES -> "Notes"
    ItemKind.GALLERY -> "Gallery"
    ItemKind.QUICK_TILES -> "Quick Tiles"
    ItemKind.SUGGESTED -> "Suggested"
    ItemKind.BATTERY -> "Battery"
    ItemKind.QUICK_SEARCH -> "Quick search"
    else -> "Widget"
}

/** Edit stack: reorder, remove, unstack. Save writes the new payload. */
@Composable
private fun StackEditorDialog(
    members: List<Int>,
    onMembersChange: (List<Int>) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onUnstack: () -> Unit,
    unstackEnabled: Boolean,
) {
    val palette = LocalPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit stack") },
        text = {
            Column {
                if (members.isEmpty()) {
                    Text("Empty stack", color = palette.muted, fontSize = 13.sp)
                }
                for ((i, kind) in members.withIndex()) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stackKindLabel(kind), color = palette.onSurface, fontSize = 14.sp,
                            modifier = Modifier.weight(1f), maxLines = 1,
                        )
                        TextButton(
                            enabled = i > 0,
                            onClick = { onMembersChange(stackReordered(members, i, i - 1)) },
                        ) { Text("↑", fontSize = 14.sp) }
                        TextButton(
                            enabled = i < members.size - 1,
                            onClick = { onMembersChange(stackReordered(members, i, i + 1)) },
                        ) { Text("↓", fontSize = 14.sp) }
                        TextButton(
                            enabled = members.size > 1,
                            onClick = { onMembersChange(members.filterIndexed { idx, _ -> idx != i }) },
                        ) { Text("Remove", fontSize = 12.sp) }
                    }
                }
                if (!unstackEnabled && members.size >= 2) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Unstack is unavailable until every member fits on this page.",
                        color = palette.muted, fontSize = 12.sp,
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(enabled = unstackEnabled, onClick = onUnstack) { Text("Unstack") }
                TextButton(enabled = members.isNotEmpty(), onClick = onSave) { Text("Save") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---------------------------------------------------------------- AT A GLANCE

/**
 * At-a-glance widget (stacked summary behavior, original code): one
 * quiet line of date, the next calendar event, and the weather summary.
 * Reads the same sources the existing widgets use — CalendarRepo for
 * events, the WeatherStore cache for weather — with no new permissions
 * and no new network use (weather refreshes only through the existing
 * optional Open-Meteo path). Lines that have no data are omitted, never
 * faked: no calendar grant means no event line, no city/weather means
 * no weather line. Styled by WidgetSurface (Minimal glass) like the
 * other own widgets. Reachable via the Agenda "glance" payload variant.
 */
@Composable
private fun AtAGlanceWidget(app: HavenApp, modifier: Modifier) {
    val palette = LocalPalette.current
    val state = LocalState.current
    val context = LocalContext.current
    val now = useNow(60_000L)
    val dateFmt = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val granted = hasCalendarPermission(context)
    var next by remember { mutableStateOf<CalEvent?>(null) }
    LaunchedEffect(granted, state.calendarsJson, now) {
        next = if (granted) {
            withContext(Dispatchers.IO) {
                CalendarRepo.upcoming(context, 1, parseAllowedCalendars(state.calendarsJson), null)
                    .firstOrNull()
            }
        } else {
            null
        }
    }
    val weather by app.weather.data.collectAsState()
    LaunchedEffect(state.weatherCity, state.weatherLat, state.weatherUnits, state.weatherNetwork) {
        app.weather.refresh(state)
    }
    WidgetSurface(modifier) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                dateFmt.format(Date(now)),
                color = palette.onSurface,
                fontFamily = LocalFont.current,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            if (granted) {
                val ev = next
                Text(
                    if (ev == null) "No upcoming events"
                    else "${ev.title} · ${timeFmt.format(Date(ev.beginMs))}",
                    color = palette.muted,
                    fontFamily = LocalFont.current,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
            }
            val w = weather
            if (state.weatherNetwork && w != null) {
                val (label, symbol) = weatherInfo(w.code)
                Text(
                    "$symbol ${formatTempDisplay(w.temp, state.weatherUnits)} · $label",
                    color = palette.onSurface,
                    fontFamily = LocalFont.current,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
