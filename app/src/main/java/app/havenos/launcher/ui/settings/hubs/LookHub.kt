package app.havenos.launcher.ui.settings.hubs

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.IconPack
import app.havenos.launcher.data.IconPacks
import app.havenos.launcher.data.WP_GRADIENTS
import app.havenos.launcher.data.WP_SOLID_DEFAULT
import app.havenos.launcher.theme.LocalAccentRoles
import app.havenos.launcher.theme.LocalFont
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.theme.Look
import app.havenos.launcher.theme.crossWindowBlur
import app.havenos.launcher.theme.tokensFor
import app.havenos.launcher.ui.glass.AccentSelection
import app.havenos.launcher.ui.glass.AccentSource
import app.havenos.launcher.ui.glass.AccentStrength
import app.havenos.launcher.ui.glass.CURATED_ACCENTS
import app.havenos.launcher.ui.glass.GlassLevel
import app.havenos.launcher.ui.glass.HavenHelp
import app.havenos.launcher.ui.glass.HavenIcon
import app.havenos.launcher.ui.glass.HavenIconImage
import app.havenos.launcher.ui.glass.HavenLongPressText
import app.havenos.launcher.ui.glass.HavenRadii
import app.havenos.launcher.ui.glass.HavenSegmented
import app.havenos.launcher.ui.glass.HavenSlider
import app.havenos.launcher.ui.glass.HavenSwitch
import app.havenos.launcher.ui.glass.HavenType
import app.havenos.launcher.ui.glass.accentRolesFor
import app.havenos.launcher.ui.glass.glass
import app.havenos.launcher.ui.glass.accentSelectionOf
import app.havenos.launcher.ui.glass.argbToHslTriple
import app.havenos.launcher.ui.glass.encodeRecents
import app.havenos.launcher.ui.glass.hslTripleToArgb
import app.havenos.launcher.ui.glass.pushRecent
import app.havenos.launcher.ui.glass.resolveAccentSeed
import app.havenos.launcher.ui.glass.systemSeedColor
import app.havenos.launcher.ui.glass.wallpaperSeedColors
import app.havenos.launcher.ui.rememberAppIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import app.havenos.launcher.ui.settings.shell.DeepLinkAnchor
import app.havenos.launcher.ui.settings.shell.LocalDraftLayer
import app.havenos.launcher.ui.settings.shell.LocalPanelDeepLink
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
// 3.1 Phase C: restyled onto Haven Glass — HavenType, accent section
// labels, HavenSwitch/HavenSegmented/HavenSlider, HavenHelp popovers.
// Rules: titles are at most 3 words, values live on the right, help lives
// behind a HavenHelp popover. No tier tags, no dead-control wording,
// no paragraph helper text.
// ---------------------------------------------------------------------------


@Composable
fun HubSection(label: String) {
    val roles = LocalAccentRoles.current
    Text(
        label.uppercase(),
        style = HavenType.sectionLabel.copy(fontFamily = LocalFont.current),
        color = Color(roles.accent),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/** Help button: the shared 48dp HavenHelp popover (no glyph). */
@Composable
fun HubInfoButton(help: String) {
    HavenHelp(helpText = help)
}

/**
 * 3.1.1 (item 5): adaptive row decision, pure and unit-tested.
 * Single row when the label keeps at least [minLabelPx] after the
 * value, control, and gaps are placed; otherwise the row STACKS
 * (label on top, value + control beneath). Never a weighted Row
 * beside unbounded siblings — that was the per-character wrap.
 */
sealed interface AdaptiveRowPlan {
    data class SingleRow(val labelWidthPx: Int) : AdaptiveRowPlan
    data object Stacked : AdaptiveRowPlan
}

fun adaptiveRowArrangement(
    rowWidthPx: Int,
    valuePx: Int,
    controlPx: Int,
    gapPx: Int,
    minLabelPx: Int,
): AdaptiveRowPlan {
    var others = 0
    if (valuePx > 0) others += valuePx + gapPx
    if (controlPx > 0) others += controlPx + gapPx
    val labelAvail = rowWidthPx - others
    return if (labelAvail >= minLabelPx) {
        AdaptiveRowPlan.SingleRow(labelAvail)
    } else {
        AdaptiveRowPlan.Stacked
    }
}

/**
 * 3.1.1 (item 5): adaptive setting row as a custom Layout. The label
 * gets at least max(140dp, 50% of the row); when label, value, and
 * control do not fit, they stack: label (up to 3 lines) + description
 * on top, value + control in a row beneath. Slots are measured in
 * emission order: label, value (if present), control (if present).
 */
@Composable
fun AdaptiveSettingRow(
    label: @Composable () -> Unit,
    value: (@Composable () -> Unit)? = null,
    control: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Layout(
        content = {
            label()
            if (value != null) value()
            if (control != null) control()
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val rowWidth = constraints.maxWidth
        var index = 1
        val valuePlaceable = if (value != null) {
            measurables[index++].measure(Constraints())
        } else {
            null
        }
        val controlPlaceable = if (control != null) {
            measurables[index].measure(Constraints())
        } else {
            null
        }
        val gapPx = with(density) { 8.dp.roundToPx() }
        val minLabelPx = maxOf(
            with(density) { 140.dp.roundToPx() },
            rowWidth / 2,
        )
        val plan = adaptiveRowArrangement(
            rowWidth,
            valuePlaceable?.width ?: 0,
            controlPlaceable?.width ?: 0,
            gapPx,
            minLabelPx,
        )
        when (plan) {
            is AdaptiveRowPlan.SingleRow -> {
                val labelPlaceable = measurables[0].measure(
                    Constraints(maxWidth = plan.labelWidthPx),
                )
                val height = maxOf(
                    labelPlaceable.height,
                    valuePlaceable?.height ?: 0,
                    controlPlaceable?.height ?: 0,
                )
                // Value/control stay right-aligned (as the old
                // weight-1 row did); the label takes the rest.
                layout(rowWidth, height) {
                    labelPlaceable.placeRelative(0, (height - labelPlaceable.height) / 2)
                    var right = rowWidth
                    if (controlPlaceable != null) {
                        right -= controlPlaceable.width
                        controlPlaceable.placeRelative(right, (height - controlPlaceable.height) / 2)
                        right -= gapPx
                    }
                    if (valuePlaceable != null) {
                        right -= valuePlaceable.width
                        valuePlaceable.placeRelative(right, (height - valuePlaceable.height) / 2)
                    }
                }
            }
            AdaptiveRowPlan.Stacked -> {
                val labelPlaceable = measurables[0].measure(
                    Constraints(maxWidth = rowWidth),
                )
                val row2Height = maxOf(
                    valuePlaceable?.height ?: 0,
                    controlPlaceable?.height ?: 0,
                )
                val height = labelPlaceable.height + gapPx + row2Height
                layout(rowWidth, height) {
                    labelPlaceable.placeRelative(0, 0)
                    var x = 0
                    val y = labelPlaceable.height + gapPx
                    if (valuePlaceable != null) {
                        valuePlaceable.placeRelative(x, y + (row2Height - valuePlaceable.height) / 2)
                        x += valuePlaceable.width + gapPx
                    }
                    if (controlPlaceable != null) {
                        controlPlaceable.placeRelative(x, y + (row2Height - controlPlaceable.height) / 2)
                    }
                }
            }
        }
    }
}

/** One lean row: title (up to 3 lines) + live value, optional control below. */
@Composable
fun HubRow(
    title: String,
    value: String,
    info: String? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    val palette = LocalPalette.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        AdaptiveSettingRow(
            label = {
                HavenLongPressText(
                    title,
                    HavenType.rowLabel.copy(fontFamily = LocalFont.current),
                    if (enabled) palette.onSurface else palette.onSurface.copy(alpha = 0.4f),
                    maxLines = 3,
                )
            },
            value = if (value.isNotEmpty()) {
                {
                    HavenLongPressText(
                        value,
                        HavenType.rowValue.copy(fontFamily = LocalFont.current),
                        palette.muted,
                    )
                }
            } else {
                null
            },
            control = if (info != null) {
                { HubInfoButton(info) }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        )
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
    val palette = LocalPalette.current
    AdaptiveSettingRow(
        label = {
            HavenLongPressText(
                title,
                HavenType.rowLabel.copy(fontFamily = LocalFont.current),
                if (enabled) palette.onSurface else palette.onSurface.copy(alpha = 0.4f),
                maxLines = 3,
            )
        },
        value = {
            Text(
                if (checked) "On" else "Off",
                style = HavenType.rowValue.copy(fontFamily = LocalFont.current),
                color = palette.muted,
            )
        },
        control = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (info != null) HubInfoButton(info)
                HavenSwitch(
                    checked = checked,
                    onChange = { onSpotlight?.invoke(); onChange(it) },
                    enabled = enabled,
                )
            }
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun HubSegmented(
    title: String,
    options: List<String>,
    selected: Int,
    info: String? = null,
    value: String = "",
    enabled: Boolean = true,
    onSpotlight: (() -> Unit)? = null,
    onSelect: (Int) -> Unit,
) {
    HubRow(title = title, value = value, info = info, enabled = enabled) {
        HavenSegmented(
            options = options,
            selected = selected,
            onSelect = { onSpotlight?.invoke(); onSelect(it) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
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
    var drag by remember(value) { mutableFloatStateOf(value) }
    HubRow(
        title = title,
        value = valueText(drag),
        info = info,
        enabled = enabled,
    ) {
        HavenSlider(
            value = drag,
            valueRange = range,
            steps = 0,
            onValueChange = { drag = it; onPreview?.invoke(it) },
            onCommit = { onSpotlight?.invoke(); onCommit(drag) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            valueText = valueText,
        )
    }
}

@Composable
fun HubMore(
    title: String = "More options",
    // 3.1 A10: when set, a panel deep link naming this field scrolls
    // here and forces the section open (one-shot: the remember key
    // follows the link, so a later manual close sticks).
    deepLinkField: String? = null,
    content: @Composable () -> Unit,
) {
    val forceOpen = deepLinkField != null &&
        LocalPanelDeepLink.current?.field == deepLinkField
    var open by remember(forceOpen) { mutableStateOf(forceOpen) }
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        val header: @Composable () -> Unit = {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { open = !open }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    color = scheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                HavenIconImage(
                    HavenIcon.ChevronRight,
                    null,
                    tint = scheme.onSurface.copy(alpha = 0.55f),
                    size = 20.dp,
                    modifier = Modifier.graphicsLayer { rotationZ = if (open) 90f else 0f },
                )
            }
        }
        if (deepLinkField != null) {
            DeepLinkAnchor(field = deepLinkField) { header() }
        } else {
            header()
        }
        if (open) content()
    }
    Spacer(Modifier.height(8.dp))
}

/** Validated #RRGGBB dialog (Glass3 surface, title + field + text + one filled button); [onPick] receives the opaque ARGB int. */
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
    val palette = LocalPalette.current
    val roles = LocalAccentRoles.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .glass(GlassLevel.GLASS3, HavenRadii.dialogShape())
                .padding(24.dp),
        ) {
            Text("Custom colour", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = palette.onSurface)
            Spacer(Modifier.height(12.dp))
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
            Row(
                Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = palette.muted)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val hex = text.trim().removePrefix("#")
                        val parsed = hex.toLongOrNull(16)
                        if (hex.length != 6 || parsed == null) {
                            error = "Enter 6 hex digits, e.g. #2F6FCE."
                        } else {
                            onPick((0xFF000000L or parsed).toInt())
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(roles.accent),
                        contentColor = Color(roles.onAccent),
                    ),
                ) { Text("Use colour", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

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

/**
 * 3.1 Phase B7 Custom accent picker: hue strip + saturation/lightness
 * square (HSL), live preview on every move, recents pushed on release
 * or discrete pick. The eyedropper samples the system wallpaper's
 * reported colours (WallpaperManager); no bitmap sampling exists, so
 * HavenOS wallpapers are not sampled here. Thumb rings track the
 * current colour; sizes are measured, never guessed.
 */
@Composable
private fun CustomAccentPicker(
    currentArgb: Int,
    recents: List<Int>,
    wpSeeds: List<Int>,
    clamped: Boolean,
    onPick: (Int) -> Unit,
    onPickEnd: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var hsl by remember(currentArgb) { mutableStateOf(argbToHslTriple(currentArgb)) }
    LaunchedEffect(currentArgb) { hsl = argbToHslTriple(currentArgb) }
    val hueColor = Color(hslTripleToArgb(hsl.first, 1f, 0.5f))
    var stripWidth by remember { mutableStateOf(0) }
    var squareSize by remember { mutableStateOf(IntSize.Zero) }

    // Hue strip.
    Box(
        Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                        Color.Blue, Color.Magenta, Color.Red,
                    ),
                ),
            )
            .onSizeChanged { stripWidth = it.width }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val hue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                    hsl = Triple(hue, hsl.second, hsl.third)
                    onPick(hslTripleToArgb(hue, hsl.second, hsl.third))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onPickEnd(hslTripleToArgb(hsl.first, hsl.second, hsl.third)) },
                ) { change, _ ->
                    val hue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                    hsl = Triple(hue, hsl.second, hsl.third)
                    onPick(hslTripleToArgb(hue, hsl.second, hsl.third))
                }
            },
    ) {
        Box(
            Modifier.offset {
                IntOffset(((hsl.first / 360f) * stripWidth).roundToInt() - 12, 4)
            }.size(24.dp).clip(CircleShape).border(2.dp, Color.White, CircleShape),
        )
    }
    Spacer(Modifier.height(12.dp))
    // Saturation/lightness square: x = saturation, y = lightness (top = light).
    Box(
        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp))
            .onSizeChanged { squareSize = it }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = (offset.x / size.width).coerceIn(0f, 1f)
                    val l = (1f - offset.y / size.height).coerceIn(0f, 1f)
                    hsl = Triple(hsl.first, s, l)
                    onPick(hslTripleToArgb(hsl.first, s, l))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onPickEnd(hslTripleToArgb(hsl.first, hsl.second, hsl.third)) },
                ) { change, _ ->
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val l = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                    hsl = Triple(hsl.first, s, l)
                    onPick(hslTripleToArgb(hsl.first, s, l))
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        }
        Box(
            Modifier.offset {
                IntOffset(
                    (hsl.second * squareSize.width).roundToInt() - 12,
                    ((1f - hsl.third) * squareSize.height).roundToInt() - 12,
                )
            }.size(24.dp).clip(CircleShape).border(2.dp, Color.White, CircleShape),
        )
    }
    if (clamped) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Adjusted for readability",
            fontSize = 12.sp,
            color = scheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    if (recents.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            recents.forEach { argb ->
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Color(argb))
                        .border(1.dp, scheme.onSurface.copy(alpha = 0.2f), CircleShape)
                        .clickable { onPickEnd(argb) },
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Sample wallpaper",
            fontSize = 14.sp,
            color = scheme.onSurface.copy(alpha = 0.6f),
        )
        if (wpSeeds.isEmpty()) {
            Text(
                "No colours found.",
                fontSize = 14.sp,
                color = scheme.onSurface.copy(alpha = 0.6f),
            )
        } else {
            wpSeeds.forEach { argb ->
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Color(argb))
                        .border(1.dp, scheme.onSurface.copy(alpha = 0.2f), CircleShape)
                        .clickable { onPickEnd(argb) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Look hub — Minimal + accent ONLY. No preset carousel, no bundles, no
// .havenlook, no Look Editor. Only controls whose writes visibly render:
// accent (3.1 Phase B7 sources: Wallpaper/System/Curated/Custom +
// strength; the legacy Default shows only for migrated installs),
// mode, material, corner radius, font family, icon shape (the 4 masks
// the renderer has), icon packs, themed icons, wallpaper.
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

    // 3.1 Phase B7: accent is a DRAFT selection, not a live write. The
    // panel's LocalDraftLayer holds the preview; panel close commits
    // it in one setAccentSelection write (see Home.kt panel onClose).
    // Display values read the draft first so the preview is instant,
    // and the Accent row shows "Previewing" while the draft differs.
    val draftLayer = LocalDraftLayer.current
    val draft = draftLayer?.draft
    val liveSelection = accentSelectionOf(
        state.accentSource, state.accentCurated, state.accentCustomArgb,
        state.accentStrength, state.accentRecents,
    )
    val displaySelection = if (draft != null) {
        accentSelectionOf(
            draft.accentSource, draft.accentCurated, draft.accentCustomArgb,
            draft.accentStrength, draft.accentRecents,
        )
    } else {
        liveSelection
    }
    val accentPreviewing = draft != null && displaySelection != liveSelection
    fun previewSelection(selection: AccentSelection) {
        draftLayer?.begin(state)
        draftLayer?.preview {
            it.copy(
                accentSource = selection.source.name,
                accentCurated = selection.curated,
                accentCustomArgb = selection.customArgb,
                accentStrength = selection.strength.ordinal,
                accentRecents = encodeRecents(selection.recents),
            )
        }
    }

    // Hero accent: the resolved seed (clamped per mode), or the Minimal
    // default when the selection has no seed — the same fallback
    // HavenTheme applies, so the hero never shows a stale accent.
    val isDark = scheme.background.luminance() < 0.5f
    val wpSeeds = remember { wallpaperSeedColors(ctx) }
    val sysSeed = remember(displaySelection.source, isDark) {
        if (displaySelection.source == AccentSource.SYSTEM) systemSeedColor(ctx, isDark) else null
    }
    val displaySeed = resolveAccentSeed(displaySelection, wpSeeds.firstOrNull(), sysSeed)
    val accentColor = displaySeed?.let {
        Color(accentRolesFor(it, isDark, displaySelection.strength).accent)
    } ?: run {
        val base = tokensFor(Look.Minimal)
        if (isDark) base.dark.accent else base.light.accent
    }
    // The Custom picker's clamp note: the stored custom colour is raw;
    // roles clamp it at render, so a clamp here means the hero (and the
    // committed render) differs from the picked colour.
    val customClamped = displaySelection.source == AccentSource.CUSTOM &&
        displaySelection.customArgb != 0 &&
        accentRolesFor(displaySelection.customArgb, isDark, displaySelection.strength).clamped
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
                            .then(
                                if (accentPreviewing) {
                                    // Dashed outline marks an uncommitted
                                    // accent preview (clears on commit/discard).
                                    Modifier.drawBehind {
                                        val stroke = 2.dp.toPx()
                                        drawCircle(
                                            color = scheme.onSurface.copy(alpha = 0.6f),
                                            style = Stroke(
                                                width = stroke,
                                                pathEffect = PathEffect.dashPathEffect(
                                                    floatArrayOf(4.dp.toPx(), 3.dp.toPx()), 0f,
                                                ),
                                            ),
                                        )
                                    }
                                } else {
                                    Modifier.border(1.dp, scheme.onSurface.copy(alpha = 0.2f), CircleShape)
                                },
                            ),
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
        // 3.1 Phase B7 accent sources. "Default" appears only while the
        // stored selection is the migrated legacy default — it is not a
        // choice to return to; any other pick leaves it behind. System
        // is hidden below Android 12. Wallpaper is a suggestion chip
        // only: it is never applied without a tap.
        val sourceOptions = buildList {
            if (displaySelection.source == AccentSource.DEFAULT) add("Default")
            add("Wallpaper")
            if (Build.VERSION.SDK_INT >= 31) add("System")
            add("Curated")
            add("Custom")
        }
        val sourceLabel = when (displaySelection.source) {
            AccentSource.DEFAULT -> "Default"
            AccentSource.WALLPAPER -> "Wallpaper"
            AccentSource.SYSTEM -> "System"
            AccentSource.CURATED -> "Curated"
            AccentSource.CUSTOM -> "Custom"
        }
        DeepLinkAnchor(field = "accent") {
            HubRow(
                title = "Accent",
                value = if (accentPreviewing) "Previewing" else "",
                info = "Recolours buttons, toggles and highlights.",
            ) {
            HavenSegmented(
                options = sourceOptions,
                selected = sourceOptions.indexOf(sourceLabel).coerceAtLeast(0),
                onSelect = { idx ->
                    val picked = when (sourceOptions.getOrNull(idx)) {
                        "Wallpaper" -> displaySelection.copy(source = AccentSource.WALLPAPER)
                        "System" -> displaySelection.copy(source = AccentSource.SYSTEM)
                        "Curated" -> displaySelection.copy(source = AccentSource.CURATED)
                        "Custom" -> displaySelection.copy(source = AccentSource.CUSTOM)
                        else -> null
                    }
                    if (picked != null) previewSelection(picked)
                },
            )
            }
        }
        if (displaySelection.source == AccentSource.WALLPAPER) {
            HubRow(title = "Wallpaper colours", value = "") {
                if (wpSeeds.isEmpty()) {
                    Text(
                        "No wallpaper colours found.",
                        fontSize = 14.sp,
                        color = scheme.onSurface.copy(alpha = 0.6f),
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        wpSeeds.forEach { argb ->
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(Color(argb))
                                    .border(1.dp, scheme.onSurface.copy(alpha = 0.2f), CircleShape)
                                    .clickable {
                                        // A wallpaper chip is a shortcut
                                        // to that exact colour as Custom.
                                        val sel = displaySelection.copy(
                                            source = AccentSource.CUSTOM, customArgb = argb,
                                        )
                                        previewSelection(
                                            sel.copy(recents = pushRecent(sel.recents, argb)),
                                        )
                                    },
                            )
                        }
                    }
                }
            }
        }
        if (displaySelection.source == AccentSource.SYSTEM) {
            HubRow(title = "System colour", value = "") {
                Text(
                    "Uses your device's Material You colours.",
                    fontSize = 14.sp,
                    color = scheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        if (displaySelection.source == AccentSource.CURATED) {
            HubRow(title = "Curated", value = "") {
                Column(Modifier.fillMaxWidth()) {
                    CURATED_ACCENTS.chunked(6).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            row.forEach { curated ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        Modifier.size(40.dp).clip(CircleShape)
                                            .background(Color(curated.argb))
                                            .border(
                                                if (displaySelection.curated == curated.name) 2.dp else 1.dp,
                                                if (displaySelection.curated == curated.name) {
                                                    scheme.primary
                                                } else {
                                                    scheme.onSurface.copy(alpha = 0.2f)
                                                },
                                                CircleShape,
                                            )
                                            .clickable {
                                                previewSelection(
                                                    displaySelection.copy(curated = curated.name),
                                                )
                                            },
                                    )
                                    Text(
                                        curated.name,
                                        fontSize = 11.sp,
                                        color = if (displaySelection.curated == curated.name) {
                                            scheme.primary
                                        } else {
                                            scheme.onSurface.copy(alpha = 0.6f)
                                        },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
        if (displaySelection.source == AccentSource.CUSTOM) {
            CustomAccentPicker(
                currentArgb = displaySelection.customArgb,
                recents = displaySelection.recents,
                wpSeeds = wpSeeds,
                clamped = customClamped,
                onPick = { argb ->
                    previewSelection(displaySelection.copy(customArgb = argb))
                },
                onPickEnd = { argb ->
                    val sel = displaySelection.copy(customArgb = argb)
                    previewSelection(sel.copy(recents = pushRecent(sel.recents, argb)))
                },
            )
        }
        HubRow(title = "Strength", value = "") {
            HavenSegmented(
                options = listOf("Subtle", "Balanced", "Vivid"),
                selected = displaySelection.strength.ordinal.coerceIn(0, 2),
                onSelect = {
                    previewSelection(
                        displaySelection.copy(
                            strength = AccentStrength.entries[it.coerceIn(0, 2)],
                        ),
                    )
                },
            )
        }
        HubMore(title = "Advanced") {
            HubRow(title = "Custom hex", value = "") {
                HubHexFieldRow(
                    currentArgb = if (displaySelection.source == AccentSource.CUSTOM) {
                        displaySelection.customArgb
                    } else {
                        0
                    },
                    onPick = { argb ->
                        val sel = displaySelection.copy(
                            source = AccentSource.CUSTOM, customArgb = argb,
                        )
                        previewSelection(sel.copy(recents = pushRecent(sel.recents, argb)))
                    },
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
        HubSegmented(
            title = "Material",
            options = listOf("Solid", "Translucent", "Frosted"),
            selected = state.surfaceMaterial.coerceIn(0, 2),
            info = if (blurFallsBack) "Blur falls back to translucent here." else null,
            onSpotlight = { onSpotlight("material") },
            onSelect = { scope.launch { settings.setSurfaceMaterial(it) } },
        )
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

        // 3.1 A10: the quick-customize Wallpaper item deep-links here.
        HubMore(title = "Wallpaper", deepLinkField = "wallpaper") {
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
                ) { Text("Solid") }
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
                ) { Text(if (state.wpKind == 5) "Photo" else "Photo…") }
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
