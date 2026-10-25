package app.havenos.launcher.ui.settings.hubs

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.IconPack
import app.havenos.launcher.data.IconPacks
import app.havenos.launcher.data.WP_GRADIENTS
import app.havenos.launcher.data.WP_SOLID_DEFAULT
import app.havenos.launcher.theme.GlassState
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.GlassTier
import app.havenos.launcher.theme.LocalGlass
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.theme.LocalTokens
import app.havenos.launcher.theme.crossWindowBlur
import app.havenos.launcher.theme.resolveTokens
import app.havenos.launcher.ui.rememberAppIcon
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import app.havenos.launcher.ui.settings.shell.LocalDraftLayer
import app.havenos.launcher.ui.settings.shell.SearchEntry

// Search index entries: SearchEntry is defined once in the shell
// (ui/settings/shell/SettingsSearch.kt); this hub only lists its rows.
// ---------------------------------------------------------------------------

val LookSearchEntries: List<SearchEntry> = listOf(
    SearchEntry("Accent", listOf("colour", "color", "accent", "tint", "highlight"), "Look", "look"),
    SearchEntry("Mode", listOf("dark", "light", "night", "theme", "auto", "system"), "Look", "look"),
    SearchEntry("Material", listOf("blur", "glass", "frosted", "translucent", "solid", "surface"), "Look", "look"),
    SearchEntry("Corner radius", listOf("corners", "rounded", "radius", "shape"), "Look", "look"),
    SearchEntry("Font", listOf("typeface", "serif", "monospace", "text"), "Look", "look"),
    SearchEntry("Label mode", listOf("labels", "pill", "text", "legibility", "wallpaper"), "Look", "look"),
    SearchEntry("Icon shape", listOf("icons", "shape", "squircle", "circle", "rounded", "square"), "Look", "look"),
    SearchEntry("Icon packs", listOf("icons", "pack", "theme", "iconpack"), "Look", "look"),
    SearchEntry("Themed icons", listOf("monochrome", "themed", "icons", "black", "white"), "Look", "look"),
    SearchEntry("Wallpaper", listOf("wallpaper", "background", "image", "photo", "gradient"), "Look", "look"),
    SearchEntry("Wallpaper dim", listOf("wallpaper", "dim", "darken", "brightness"), "Look", "look"),
    SearchEntry("Wallpaper tint", listOf("wallpaper", "tint", "overlay", "colour"), "Look", "look"),
)

// ---------------------------------------------------------------------------
// Shared hub components (used by both hub files in this package).
// Rules: titles are at most 3 words, values live on the right, help lives
// behind a ⓘ popover of at most 90 characters. No tier tags, no
// no dead-control wording, no paragraph helper text.
// ---------------------------------------------------------------------------

@Composable
fun HubTopBar(onBack: () -> Unit, title: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "‹ Back",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onBack() }.padding(vertical = 8.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun HubSection(label: String) {
    Text(
        label.uppercase(),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
fun HubInfoButton(help: String) {
    var open by remember { mutableStateOf(false) }
    Text(
        "ⓘ",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        fontSize = 16.sp,
        modifier = Modifier.clickable { open = true }.padding(4.dp),
    )
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            text = { Text(help, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { open = false }) { Text("OK") }
            },
        )
    }
}

/** One lean row: title (<= 3 words) + live value, optional control below. */
@Composable
fun HubRow(
    title: String,
    value: String,
    info: String? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) scheme.onSurface else scheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f),
            )
            if (info != null) HubInfoButton(info)
            if (value.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    value,
                    fontSize = 15.sp,
                    color = scheme.onSurface.copy(alpha = if (enabled) 0.65f else 0.4f),
                )
            }
        }
        content()
    }
}

@Composable
fun HubSwitchRow(
    title: String,
    checked: Boolean,
    info: String? = null,
    enabled: Boolean = true,
    onSpotlight: (() -> Unit)? = null,
    onChange: (Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) scheme.onSurface else scheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.weight(1f),
        )
        if (info != null) HubInfoButton(info)
        Text(
            if (checked) "On" else "Off",
            fontSize = 15.sp,
            color = scheme.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.padding(end = 8.dp),
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { onSpotlight?.invoke(); onChange(it) },
        )
    }
}

@Composable
fun HubSegmented(
    title: String,
    options: List<String>,
    selected: Int,
    info: String? = null,
    enabled: Boolean = true,
    onSpotlight: (() -> Unit)? = null,
    onSelect: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    HubRow(title = title, value = "", info = info, enabled = enabled) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEachIndexed { i, label ->
                val sel = i == selected
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(50))
                        .background(
                            if (sel) scheme.primary
                            else scheme.surfaceVariant.copy(alpha = 0.55f),
                        )
                        .then(
                            if (sel || !enabled) Modifier
                            else Modifier.border(
                                1.dp, scheme.onSurface.copy(alpha = 0.18f),
                                RoundedCornerShape(50),
                            ),
                        )
                        .clickable(enabled = enabled) { onSpotlight?.invoke(); onSelect(i) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (sel) Color.White else scheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/**
 * Slider that shows the drag value live and commits on release, so a drag
 * never spams the store. [value] is the committed value.
 */
@Composable
fun HubSliderRow(
    title: String,
    valueText: (Float) -> String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    info: String? = null,
    enabled: Boolean = true,
    onSpotlight: (() -> Unit)? = null,
    onPreview: ((Float) -> Unit)? = null,
    onCommit: (Float) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var drag by remember(value) { mutableFloatStateOf(value) }
    HubRow(
        title = title,
        value = valueText(drag),
        info = info,
        enabled = enabled,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = drag,
                onValueChange = { drag = it; onPreview?.invoke(it) },
                onValueChangeFinished = { onSpotlight?.invoke(); onCommit(drag) },
                valueRange = range,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                valueText(drag),
                color = scheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun HubMore(content: @Composable () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "More options",
                color = scheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(if (open) "⌄" else "›", color = scheme.onSurface.copy(alpha = 0.55f), fontSize = 18.sp)
        }
        if (open) content()
    }
    Spacer(Modifier.height(8.dp))
}

/** Validated #RRGGBB dialog; [onPick] receives the opaque ARGB int. */
@Composable
fun HubHexDialog(
    initialArgb: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    var text by remember {
        mutableStateOf(
            if (initialArgb != 0) "#%06X".format(initialArgb and 0xFFFFFF) else "#",
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom colour", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("#RRGGBB", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val hex = text.trim().removePrefix("#")
                val parsed = hex.toLongOrNull(16)
                if (hex.length != 6 || parsed == null) {
                    error = "Enter 6 hex digits, e.g. #2F6FCE."
                } else {
                    onPick((0xFF000000L or parsed).toInt())
                }
            }) { Text("Use colour", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        },
    )
}

private val ACCENT_SWATCHES: List<Int> = listOf(
    0xFF2F6FCE.toInt(), 0xFF0F766E.toInt(), 0xFF7C3AED.toInt(),
    0xFFDB2777.toInt(), 0xFFD97706.toInt(), 0xFF16A34A.toInt(),
    0xFFB3541E.toInt(), 0xFF44586C.toInt(),
)

private val SOLID_SWATCHES: List<Int> = listOf(
    WP_SOLID_DEFAULT, 0xFF000000.toInt(), 0xFF2F6FCE.toInt(),
    0xFF7A4A6E.toInt(), 0xFF1F4D3A.toInt(), 0xFF4A4A52.toInt(),
)

private val TINT_SWATCHES: List<Int> = listOf(
    0xFF2F6FCE.toInt(), 0xFF7A4A6E.toInt(), 0xFF1F4D3A.toInt(),
    0xFFB3541E.toInt(), 0xFF101013.toInt(), 0xFFF4F6F8.toInt(),
)

@Composable
private fun SwatchRow(
    selectedArgb: Int,
    swatches: List<Int>,
    onPick: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (argb in swatches) {
            val sel = argb == selectedArgb
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .background(Color(argb))
                    .then(
                        if (sel) Modifier.border(2.dp, scheme.primary, CircleShape)
                        else Modifier.border(1.dp, scheme.onSurface.copy(alpha = 0.2f), CircleShape),
                    )
                    .clickable { onPick(argb) },
            )
        }
    }
}

@Composable
private fun HubHexFieldRow(currentArgb: Int, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (currentArgb != 0) "#%06X".format(currentArgb and 0xFFFFFF) else "No colour set",
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { open = true }) {
            Text("Custom…", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }
    }
    if (open) {
        HubHexDialog(
            initialArgb = currentArgb,
            onDismiss = { open = false },
            onPick = { open = false; onPick(it) },
        )
    }
}

/** Material card rendering the REAL GlassSurface with that material's tokens. */
@Composable
private fun MaterialCard(
    state: app.havenos.launcher.data.SettingsState,
    material: Int,
    title: String,
    note: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val sysDark = isSystemInDarkTheme()
    val dark = when (state.darkMode) {
        1 -> false
        2 -> true
        3 -> {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            hour >= 19 || hour < 7
        }
        else -> sysDark
    }
    val tokens = remember(state.accentMode, state.accentArgb, state.cornerDp, material) {
        resolveTokens(state.copy(surfaceMaterial = material))
    }
    val palette = if (dark) tokens.dark else tokens.light
    val (gradTop, gradBottom) = if (state.wpKind in 0..3) {
        WP_GRADIENTS[state.wpKind]
    } else {
        0xFF1D3557.toInt() to 0xFF0B0F14.toInt()
    }
    Box(
        Modifier.fillMaxWidth().padding(top = 8.dp)
            .clip(RoundedCornerShape(tokens.corner))
            .background(Brush.verticalGradient(listOf(Color(gradTop), Color(gradBottom))))
            .then(
                if (selected) Modifier.border(2.dp, scheme.primary, RoundedCornerShape(tokens.corner))
                else Modifier,
            )
            .clickable { onSelect() },
    ) {
        CompositionLocalProvider(
            LocalTokens provides tokens,
            LocalPalette provides palette,
            LocalGlass provides GlassState(GlassTier.Partial),
        ) {
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = palette.onSurface)
                    Text(note, fontSize = 12.sp, color = palette.muted)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier.size(28.dp, 16.dp).clip(RoundedCornerShape(tokens.corner))
                                .background(palette.accent),
                        )
                        Box(
                            Modifier.size(28.dp, 16.dp).clip(RoundedCornerShape(tokens.corner))
                                .background(palette.onSurface.copy(alpha = 0.25f)),
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Look hub — Minimal + accent ONLY. No preset carousel, no bundles, no
// .havenlook, no Look Editor. Only controls whose writes visibly render:
// accent (palette/custom; wallpaper-derived falls through in Looks.kt and
// is not offered), mode, material, corner radius, font family, icon shape
// (the 4 masks the renderer has), icon packs, themed icons, wallpaper.
// ---------------------------------------------------------------------------

@Composable
fun LookHubScreen(onBack: () -> Unit, onSpotlight: (String) -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HavenApp
    val settings = app.settings
    val state by settings.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val scroll = rememberScrollState()

    val accentColor = if ((state.accentMode == 1 || state.accentMode == 2) && state.accentArgb != 0) {
        Color(state.accentArgb)
    } else {
        scheme.primary
    }
    val modeName = when (state.darkMode) {
        1 -> "Light"
        2 -> "Dark"
        3 -> "Auto"
        else -> "System"
    }
    val materialName = when (state.surfaceMaterial) {
        0 -> "Solid"
        1 -> "Translucent"
        else -> "Frosted"
    }
    val cornerText = if (state.cornerDp < 0) "Auto" else "${state.cornerDp} dp"
    val blurFallsBack = state.surfaceMaterial == 2 &&
        state.wallpaperMode == 0 && !crossWindowBlur(ctx)

    // Photo picker (system picker grants read access to the chosen photo).
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri: Uri? = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (t: Throwable) {
                // Grant is best-effort; the URI is still stored.
            }
            scope.launch {
                settings.setWpUri(uri.toString())
                settings.setWpKind(5)
                settings.setWallpaperMode(1)
            }
            onSpotlight("wallpaper")
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
        HubTopBar(onBack = onBack, title = "Look")
        Spacer(Modifier.height(4.dp))

        // Hero card: Minimal + accent swatch strip.
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 2.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Minimal", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "$modeName · $materialName · $cornerText",
                    fontSize = 14.sp,
                    color = scheme.onSurface.copy(alpha = 0.65f),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(accentColor)
                            .border(1.dp, scheme.onSurface.copy(alpha = 0.2f), CircleShape),
                    )
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(scheme.surface)
                            .border(1.dp, scheme.onSurface.copy(alpha = 0.2f), CircleShape),
                    )
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(scheme.onSurface),
                    )
                }
            }
        }

        HubSection("Look")
        HubSegmented(
            title = "Accent",
            // Mode 3 (wallpaper-derived) renders as the Minimal default
            // (Looks.kt falls through), so it shows as Default, not Custom.
            options = listOf("Default", "Palette", "Custom"),
            selected = if (state.accentMode == 3) 0 else state.accentMode.coerceIn(0, 2),
            info = "Recolours buttons, toggles and highlights.",
            onSpotlight = { onSpotlight("look") },
            onSelect = { scope.launch { settings.setAccent(it, state.accentArgb) } },
        )
        if (state.accentMode == 1) {
            HubRow(title = "Swatches", value = "") {
                SwatchRow(
                    selectedArgb = state.accentArgb,
                    swatches = ACCENT_SWATCHES,
                    onPick = { scope.launch { settings.setAccent(1, it) } },
                )
            }
        }
        if (state.accentMode == 2) {
            HubRow(title = "Custom hex", value = "") {
                HubHexFieldRow(
                    currentArgb = state.accentArgb,
                    onPick = { scope.launch { settings.setAccent(2, it) } },
                )
            }
        }
        HubSegmented(
            title = "Mode",
            options = listOf("System", "Light", "Dark", "Auto"),
            selected = state.darkMode.coerceIn(0, 3),
            onSpotlight = { onSpotlight("look") },
            onSelect = { scope.launch { settings.setDarkMode(it) } },
        )
        HubRow(title = "Material", value = materialName) {
            MaterialCard(
                state = state, material = 0, title = "Solid",
                note = "Flat, opaque surfaces",
                selected = state.surfaceMaterial == 0,
                onSelect = { onSpotlight("material"); scope.launch { settings.setSurfaceMaterial(0) } },
            )
            MaterialCard(
                state = state, material = 1, title = "Translucent",
                note = "Tinted, see-through surfaces",
                selected = state.surfaceMaterial == 1,
                onSelect = { onSpotlight("material"); scope.launch { settings.setSurfaceMaterial(1) } },
            )
            MaterialCard(
                state = state, material = 2, title = "Frosted",
                note = "Blurred glass surfaces",
                selected = state.surfaceMaterial == 2,
                onSelect = { onSpotlight("material"); scope.launch { settings.setSurfaceMaterial(2) } },
            )
            if (blurFallsBack) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Blur falls back to translucent here.",
                    fontSize = 13.sp,
                    color = scheme.onSurface.copy(alpha = 0.65f),
                )
            }
        }
        HubSliderRow(
            title = "Corner radius",
            valueText = { v -> if (v.roundToInt() < 0) "Auto" else "${v.roundToInt()} dp" },
            value = state.cornerDp.toFloat(),
            range = -1f..40f,
            info = "Rounds cards, dock and folders.",
            onSpotlight = { onSpotlight("material") },
            onCommit = { scope.launch { settings.setCornerDp(it.roundToInt().coerceIn(-1, 40)) } },
        )
        HubSegmented(
            title = "Font",
            options = listOf("System", "Serif", "Mono"),
            selected = state.fontChoice.coerceIn(0, 2),
            onSpotlight = { onSpotlight("look") },
            onSelect = { scope.launch { settings.setFontChoice(it) } },
        )
        // M7 Wave 2: label mode (Legibility.kt). Auto picks text colour
        // per cell and adds a frosted pill only on busy/low-contrast
        // cells; Pill always pills; Off hides labels.
        HubSegmented(
            title = "Label mode",
            options = listOf("Auto", "Pill", "Off"),
            selected = when (state.labelMode) {
                "pill" -> 1
                "off" -> 2
                else -> 0
            },
            info = "Auto pills labels only on busy wallpaper.",
            onSpotlight = { onSpotlight("look") },
            onSelect = {
                scope.launch {
                    settings.setLabelMode(
                        when (it) {
                            1 -> "pill"
                            2 -> "off"
                            else -> "auto"
                        },
                    )
                }
            },
        )

        HubSection("Icons")
        IconShapeRow(
            state = state,
            onSpotlight = { onSpotlight("icons") },
            onSelect = { scope.launch { settings.setIconShapeExt(it) } },
        )

        HubSection("Wallpaper")
        HubSegmented(
            title = "Source",
            options = listOf("System", "HavenOS"),
            selected = state.wallpaperMode.coerceIn(0, 1),
            onSpotlight = { onSpotlight("wallpaper") },
            onSelect = { scope.launch { settings.setWallpaperMode(it) } },
        )
        val havenOnly = state.wallpaperMode == 1
        HubRow(
            title = "Style",
            value = when (state.wpKind) {
                4 -> "Solid"
                5 -> "Photo"
                else -> "Gradient ${state.wpKind + 1}"
            },
            info = if (havenOnly) null else "HavenOS wallpaper only.",
            enabled = havenOnly,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (i in 0..3) {
                    val (top, bottom) = WP_GRADIENTS[i]
                    Box(
                        Modifier.weight(1f).height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(Color(top), Color(bottom))))
                            .then(
                                if (havenOnly && state.wpKind == i) {
                                    Modifier.border(2.dp, scheme.primary, RoundedCornerShape(12.dp))
                                } else Modifier
                            )
                            .clickable(enabled = havenOnly) {
                                onSpotlight("wallpaper")
                                scope.launch { settings.setWpKind(i) }
                            },
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    enabled = havenOnly,
                    onClick = {
                        onSpotlight("wallpaper")
                        scope.launch { settings.setWpKind(4) }
                    },
                ) { Text(if (state.wpKind == 4) "Solid ✓" else "Solid") }
                OutlinedButton(
                    enabled = havenOnly,
                    onClick = {
                        try {
                            photoLauncher.launch(
                                Intent(Intent.ACTION_OPEN_DOCUMENT)
                                    .addCategory(Intent.CATEGORY_OPENABLE)
                                    .setType("image/*"),
                            )
                        } catch (t: Throwable) {
                            // No picker on this device; nothing changes.
                        }
                    },
                ) { Text(if (state.wpKind == 5) "Photo ✓" else "Photo…") }
            }
            if (state.wpKind == 4 && havenOnly) {
                SwatchRow(
                    selectedArgb = state.wpSolid,
                    swatches = SOLID_SWATCHES,
                    onPick = { scope.launch { settings.setWpSolid(it) } },
                )
            }
            if (state.wpKind == 5 && state.wpUri.isNotBlank() && havenOnly) {
                Spacer(Modifier.height(6.dp))
                Text("Photo chosen", fontSize = 13.sp, color = scheme.onSurface.copy(alpha = 0.65f))
            }
        }
        if (havenOnly) {
            WallpaperAdjustRow(
                state = state,
                onSpotlight = { onSpotlight("wallpaper") },
                onCommit = { zoom, offX, offY ->
                    scope.launch {
                        settings.setWpZoom(zoom)
                        settings.setWpOffsets(offX, offY)
                    }
                },
            )
        }

        HubMore {
            IconPackListRow(
                state = state,
                onSpotlight = { onSpotlight("icons") },
                onPick = { scope.launch { settings.setIconPack(it) } },
            )
            HubSwitchRow(
                title = "Themed icons",
                checked = state.monoIcons,
                info = "Desaturates all app icons.",
                onSpotlight = { onSpotlight("icons") },
                onChange = { scope.launch { settings.setMonoIcons(it) } },
            )
            HubSliderRow(
                title = "Wallpaper dim",
                valueText = { v -> "${v.roundToInt()}%" },
                value = state.wpDim.toFloat(),
                range = 0f..60f,
                info = "HavenOS wallpaper only.",
                enabled = havenOnly,
                onSpotlight = { onSpotlight("wallpaper") },
                onCommit = { scope.launch { settings.setWpDim(it.roundToInt().coerceIn(0, 60)) } },
            )
            HubRow(
                title = "Wallpaper tint",
                value = if (state.wpTint == 0) "None" else "Set",
                info = "HavenOS wallpaper only.",
                enabled = havenOnly,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.height(36.dp).clip(RoundedCornerShape(50))
                            .background(scheme.surfaceVariant.copy(alpha = 0.7f))
                            .then(
                                if (state.wpTint == 0) {
                                    Modifier.border(2.dp, scheme.primary, RoundedCornerShape(50))
                                } else Modifier
                            )
                            .clickable(enabled = havenOnly) {
                                scope.launch { settings.setWpTint(0) }
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("None", fontSize = 12.sp) }
                    for (c in TINT_SWATCHES) {
                        val argb = (0x59 shl 24) or (c and 0xFFFFFF)
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(Color(c))
                                .then(
                                    if (state.wpTint == argb) {
                                        Modifier.border(2.dp, scheme.primary, CircleShape)
                                    } else Modifier
                                )
                                .clickable(enabled = havenOnly) {
                                    scope.launch { settings.setWpTint(argb) }
                                },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Icon-shape chips drawn with a REAL installed app icon under each mask. */
@Composable
private fun IconShapeRow(
    state: app.havenos.launcher.data.SettingsState,
    onSpotlight: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HavenApp
    val apps by app.appRepo.apps.collectAsState()
    val first = apps.firstOrNull()
    val bmp = if (first != null) rememberAppIcon(app.appRepo, first) else null
    val scheme = MaterialTheme.colorScheme
    val size = 48.dp
    val shapes = listOf(
        "Square" to RoundedCornerShape(2.dp),
        "Rounded" to RoundedCornerShape(size * 0.28f),
        "Squircle" to RoundedCornerShape(size * 0.42f),
        "Circle" to CircleShape,
    )
    HubRow(
        title = "Icon shape",
        // Shapes 4/5 (teardrop/system) have no renderer mask; nothing is
        // selected and no name is claimed for them.
        value = if (state.iconShapeExt in 0..3) shapes[state.iconShapeExt].first else "",
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            shapes.forEachIndexed { i, (label, shape) ->
                val sel = state.iconShapeExt == i
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(size).clip(shape)
                            .then(
                                if (sel) Modifier.border(2.dp, scheme.primary, shape)
                                else Modifier.border(1.dp, scheme.onSurface.copy(alpha = 0.2f), shape)
                            )
                            .clickable { onSpotlight(); onSelect(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(Modifier.fillMaxSize().background(scheme.primary))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(label, fontSize = 11.sp, color = scheme.onSurface.copy(alpha = 0.65f))
                }
            }
        }
    }
}

/** Icon-pack list; scanning runs off the main thread with a skeleton first. */
@Composable
private fun IconPackListRow(
    state: app.havenos.launcher.data.SettingsState,
    onSpotlight: () -> Unit,
    onPick: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var packs by remember { mutableStateOf<List<IconPack>?>(null) }
    LaunchedEffect(Unit) {
        packs = withContext(Dispatchers.IO) { IconPacks.discover(ctx) }
    }
    HubRow(title = "Icon packs", value = if (state.iconPack.isBlank()) "System" else "Custom") {
        if (packs == null) {
            // Skeleton rows while scanning.
            repeat(3) {
                Box(
                    Modifier.fillMaxWidth().height(48.dp).padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.surfaceVariant.copy(alpha = 0.5f)),
                )
            }
        } else {
            PackRow(
                label = "System default",
                active = state.iconPack.isBlank(),
                onClick = { onSpotlight(); onPick("") },
            )
            for (pack in packs!!) {
                PackRow(
                    label = pack.label,
                    active = state.iconPack == pack.pkg,
                    onClick = { onSpotlight(); onPick(pack.pkg) },
                )
            }
            if (packs!!.isEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "No icon packs installed",
                    fontSize = 13.sp,
                    color = scheme.onSurface.copy(alpha = 0.65f),
                )
            }
        }
    }
}

@Composable
private fun PackRow(label: String, active: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(
            if (active) "Active" else "Use",
            fontSize = 13.sp,
            color = if (active) scheme.primary else scheme.onSurface.copy(alpha = 0.6f),
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/**
 * Wallpaper Adjust: direct-manipulation pinch + drag over the live
 * HavenOS wallpaper preview. Draft only until Done; Cancel writes nothing.
 * No zoom/X/Y sliders anywhere.
 */
@Composable
private fun WallpaperAdjustRow(
    state: app.havenos.launcher.data.SettingsState,
    onSpotlight: () -> Unit,
    onCommit: (zoomPct: Int, offX: Float, offY: Float) -> Unit,
) {
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var adjusting by remember { mutableStateOf(false) }
    var draftZoom by remember { mutableFloatStateOf(1f) }
    var draftOffX by remember { mutableFloatStateOf(0f) }
    var draftOffY by remember { mutableFloatStateOf(0f) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val draftLayer = LocalDraftLayer.current
    var photoBmp by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(state.wpKind, state.wpUri) {
        photoBmp = null
        if (state.wpKind == 5 && state.wpUri.isNotBlank()) {
            photoBmp = withContext(Dispatchers.IO) {
                try {
                    val stream = ctx.contentResolver.openInputStream(Uri.parse(state.wpUri))
                        ?: return@withContext null
                    stream.use { input ->
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeStream(input, null, bounds)
                        val sample = maxOf(
                            1,
                            bounds.outWidth / 1080,
                        )
                        val second = ctx.contentResolver.openInputStream(Uri.parse(state.wpUri))
                            ?: return@withContext null
                        second.use { real ->
                            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                            BitmapFactory.decodeStream(real, null, opts)?.asImageBitmap()
                        }
                    }
                } catch (t: Throwable) {
                    null
                }
            }
        }
    }

    // Live preview behind the panel: while adjusting, the home renders
    // the draft zoom/pan (DraftLayer) instead of the committed values;
    // Done commits and discards, Cancel discards without writing.
    LaunchedEffect(draftZoom, draftOffX, draftOffY, adjusting) {
        if (!adjusting) return@LaunchedEffect
        val targetZoom = (draftZoom * 100).roundToInt().coerceIn(100, 200)
        if (targetZoom != state.wpZoom || draftOffX != state.wpOffX || draftOffY != state.wpOffY) {
            draftLayer?.begin(state)
            draftLayer?.preview {
                it.copy(wpZoom = targetZoom, wpOffX = draftOffX, wpOffY = draftOffY)
            }
        }
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        draftZoom = (draftZoom * zoomChange).coerceIn(1f, 2f)
        val w = boxSize.width.toFloat()
        val h = boxSize.height.toFloat()
        if (w > 0f) draftOffX = (draftOffX + panChange.x / (w * 0.25f)).coerceIn(-1f, 1f)
        if (h > 0f) draftOffY = (draftOffY + panChange.y / (h * 0.25f)).coerceIn(-1f, 1f)
    }

    HubRow(title = "Adjust wallpaper", value = if (adjusting) "Editing" else "") {
        if (!adjusting) {
            Button(onClick = {
                draftZoom = state.wpZoom / 100f
                draftOffX = state.wpOffX
                draftOffY = state.wpOffY
                adjusting = true
            }) { Text("Adjust") }
        } else {
            val (gradTop, gradBottom) = if (state.wpKind in 0..3) {
                WP_GRADIENTS[state.wpKind]
            } else {
                0xFF1D3557.toInt() to 0xFF0B0F14.toInt()
            }
            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(20.dp))
                    .onSizeChanged { boxSize = it }
                    .transformable(transformState),
            ) {
                // Inner layer mirrors Home's WallpaperLayer transform exactly.
                Box(
                    Modifier.fillMaxSize()
                        .graphicsLayer {
                            scaleX = draftZoom
                            scaleY = draftZoom
                            translationX = draftOffX * size.width * 0.25f
                            translationY = draftOffY * size.height * 0.25f
                        },
                ) {
                    when {
                        state.wpKind == 5 && photoBmp != null -> Image(
                            bitmap = photoBmp!!,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        state.wpKind == 4 -> Box(
                            Modifier.fillMaxSize().background(
                                Color(if (state.wpSolid != 0) state.wpSolid else WP_SOLID_DEFAULT),
                            ),
                        )
                        else -> Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(listOf(Color(gradTop), Color(gradBottom))),
                            ),
                        )
                    }
                }
                if (state.wpDim > 0) {
                    Box(
                        Modifier.fillMaxSize()
                            .background(Color.Black.copy(alpha = state.wpDim / 100f)),
                    )
                }
                if (state.wpTint != 0) {
                    Box(Modifier.fillMaxSize().background(Color(state.wpTint)))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Pinch to zoom, drag to move.",
                fontSize = 13.sp,
                color = scheme.onSurface.copy(alpha = 0.65f),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = {
                    onSpotlight()
                    onCommit(
                        (draftZoom * 100).roundToInt().coerceIn(100, 200),
                        draftOffX,
                        draftOffY,
                    )
                    draftLayer?.discard()
                    adjusting = false
                }) { Text("Done") }
                OutlinedButton(onClick = { draftLayer?.discard(); adjusting = false }) { Text("Cancel") }
            }
        }
    }
}
