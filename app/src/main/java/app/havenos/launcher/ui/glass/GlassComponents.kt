package app.havenos.launcher.ui.glass

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.havenos.launcher.theme.LocalAccentRoles
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.theme.LocalState
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// HavenOS 3.1 Phase B5 — Haven Glass components. Every component carries
// pressed (ripple), focused (accent ring), and disabled-with-reason
// (dimmed + reason caption) states. Labels wrap to two lines before
// ellipsizing; long-press on an ellipsized label shows the full text.
// Colours come only from the palette and accent roles — no hex literals.
// ---------------------------------------------------------------------------

@Composable
fun HavenIconImage(
    icon: HavenIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 24.dp,
) {
    Icon(
        imageVector = icon.vector(),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

/** Icon tile: 36dp, 12dp radius, accent-soft background (strength-dependent). */
@Composable
fun HavenIconTile(
    icon: HavenIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 20.dp,
) {
    val roles = LocalAccentRoles.current
    Box(
        modifier
            .size(size)
            .clip(HavenRadii.iconTileShape())
            .background(Color(roles.accentContainer), HavenRadii.iconTileShape()),
        contentAlignment = Alignment.Center,
    ) {
        HavenIconImage(icon, contentDescription, tint = Color(roles.accent), size = iconSize)
    }
}

/** Glass1 card. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    level: GlassLevel = GlassLevel.GLASS1,
    shape: Shape = HavenRadii.cardShape(),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.glass(level, shape), content = content)
}

/**
 * Section label + rows in ONE Glass1 container with hairline dividers
 * (not a card per row). Rows are lambdas so dividers land between them.
 */
@Composable
fun GroupedSection(
    label: String,
    modifier: Modifier = Modifier,
    rows: List<@Composable () -> Unit>,
) {
    val palette = LocalPalette.current
    val roles = LocalAccentRoles.current
    Column(modifier) {
        Text(
            label,
            style = HavenType.sectionLabel,
            color = Color(roles.accent),
            modifier = Modifier.padding(bottom = HavenSpacing.s8),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .glass(GlassLevel.GLASS1, HavenRadii.groupShape())
                .padding(vertical = HavenSpacing.s4),
        ) {
            rows.forEachIndexed { i, row ->
                if (i > 0) {
                    HorizontalDivider(
                        color = (if (palette.bg.luminance() < 0.5f) Color.White else Color.Black)
                            .copy(alpha = glassHairlineAlpha(GlassLevel.GLASS1)),
                        modifier = Modifier.padding(horizontal = HavenSpacing.s16),
                    )
                }
                row()
            }
        }
    }
}

/** Label that wraps to two lines before ellipsizing; long-press shows the full text. */
@Composable
fun HavenLongPressText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
) {
    var showFull by remember { mutableStateOf(false) }
    Box(modifier) {
        Text(
            text,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { showFull = true },
            ),
        )
        if (showFull) {
            Popup(
                onDismissRequest = { showFull = false },
                properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
            ) {
                Box(
                    Modifier
                        .widthIn(max = 320.dp)
                        .glass(GlassLevel.GLASS3, HavenRadii.controlShape())
                        .padding(HavenSpacing.s16)
                        .clickable { showFull = false },
                ) {
                    Text(text, style = style, color = color)
                }
            }
        }
    }
}

/** Shared row shell: 56dp minimum, label + value + trailing, focus ring, disabled dimming + reason. */
@Composable
private fun HavenRowContainer(
    label: String,
    value: String?,
    info: String?,
    icon: HavenIcon?,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    disabledReason: String?,
    trailing: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
    // 3.1.1 (item 5): hub list rows use a one-line title and a
    // two-line summary; other rows keep the previous 2/2 lines.
    titleMaxLines: Int = 2,
    subMaxLines: Int = 2,
) {
    val palette = LocalPalette.current
    val roles = LocalAccentRoles.current
    var focused by remember { mutableStateOf(false) }
    val click = if (onClick != null && enabled) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = HAVEN_ROW_MIN_HEIGHT)
            .then(click)
            .onFocusChanged { focused = it.isFocused }
            .then(
                if (focused) {
                    Modifier.border(2.dp, Color(roles.accent), RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            )
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = HavenSpacing.s16, vertical = HavenSpacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            HavenIconTile(icon, null, Modifier.padding(end = HavenSpacing.s12))
        }
        Column(Modifier.weight(1f)) {
            HavenLongPressText(label, HavenType.rowLabel, palette.onSurface, maxLines = titleMaxLines)
            val sub = disabledReason ?: info
            if (sub != null) {
                HavenLongPressText(sub, HavenType.caption, palette.muted, maxLines = subMaxLines)
            } else if (value != null) {
                HavenLongPressText(value, HavenType.rowValue, palette.muted, maxLines = subMaxLines)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.padding(start = HavenSpacing.s12))
            trailing()
        }
    }
}

/** Navigation row: label + value + chevron. Hub list rows use a
 *  one-line title and a two-line summary (3.1.1 item 5). */
@Composable
fun HavenNavRow(
    label: String,
    value: String? = null,
    info: String? = null,
    icon: HavenIcon? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    disabledReason: String? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    HavenRowContainer(label, value, info, icon, onClick, enabled, disabledReason, {
        HavenIconImage(HavenIcon.ChevronRight, null, tint = palette.muted, size = 20.dp)
    }, modifier, titleMaxLines = 1, subMaxLines = 2)
}

/** Switch row. */
@Composable
fun HavenSwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    info: String? = null,
    icon: HavenIcon? = null,
    enabled: Boolean = true,
    disabledReason: String? = null,
    modifier: Modifier = Modifier,
) {
    HavenRowContainer(label, null, info, icon, null, enabled, disabledReason, {
        HavenSwitch(checked, onChange, enabled)
    }, modifier)
}

/** Segmented row. */
@Composable
fun HavenSegmentedRow(
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    info: String? = null,
    icon: HavenIcon? = null,
    enabled: Boolean = true,
    disabledReason: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        HavenRowContainer(label, null, info, icon, null, enabled, disabledReason, null,
            Modifier.fillMaxWidth())
        HavenSegmented(
            options, selected, onSelect, enabled,
            Modifier.fillMaxWidth().padding(horizontal = HavenSpacing.s16, vertical = HavenSpacing.s4),
        )
    }
}

/** Slider row: no −/+ buttons, value bubble while dragging, haptic tick per step, Auto chip, debounced writes (onCommit on release). */
@Composable
fun HavenSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: (Float) -> String,
    onCommit: (Float) -> Unit,
    info: String? = null,
    icon: HavenIcon? = null,
    steps: Int = 0,
    onValueChange: ((Float) -> Unit)? = null,
    autoActive: Boolean = false,
    onAutoClick: (() -> Unit)? = null,
    autoLabel: String = "Auto",
    enabled: Boolean = true,
    disabledReason: String? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val roles = LocalAccentRoles.current
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = HavenSpacing.s16, vertical = HavenSpacing.s4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HavenLongPressText(label, HavenType.rowLabel, palette.onSurface, Modifier.weight(1f))
            Text(valueText(value), style = HavenType.rowValue, color = palette.muted)
            if (onAutoClick != null) {
                Spacer(Modifier.padding(start = HavenSpacing.s8))
                Box(
                    Modifier
                        .clip(HavenRadii.chip)
                        .background(if (autoActive) Color(roles.accentContainer) else palette.onSurface.copy(alpha = 0.08f))
                        .clickable(enabled = enabled) { onAutoClick() }
                        .padding(horizontal = HavenSpacing.s12, vertical = 6.dp),
                ) {
                    Text(autoLabel, fontSize = 12.sp, color = if (autoActive) Color(roles.accent) else palette.onSurface)
                }
            }
        }
        if (info != null || disabledReason != null) {
            Text(
                disabledReason ?: info!!,
                style = HavenType.caption,
                color = palette.muted,
                modifier = Modifier.padding(horizontal = HavenSpacing.s16),
            )
        }
        HavenSlider(value, valueRange, steps, onValueChange, onCommit, enabled,
            Modifier.fillMaxWidth().padding(horizontal = HavenSpacing.s16), valueText)
    }
}

/** Value row: label + value, no action. */
@Composable
fun HavenValueRow(
    label: String,
    value: String,
    icon: HavenIcon? = null,
    modifier: Modifier = Modifier,
) {
    HavenRowContainer(label, value, null, icon, null, true, null, null, modifier)
}

/** Action row: label + one button (at most one action; destructive tints with the error colour). */
@Composable
fun HavenActionRow(
    label: String,
    buttonLabel: String,
    onAction: () -> Unit,
    info: String? = null,
    icon: HavenIcon? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
    disabledReason: String? = null,
    modifier: Modifier = Modifier,
) {
    val error = MaterialTheme.colorScheme.error
    HavenRowContainer(label, null, info, icon, null, enabled, disabledReason, {
        TextButton(onClick = onAction, enabled = enabled) {
            Text(buttonLabel, color = if (destructive) error else MaterialTheme.colorScheme.primary)
        }
    }, modifier)
}

/** Info row: label + info text, no control. */
@Composable
fun HavenInfoRow(
    label: String,
    info: String,
    icon: HavenIcon? = null,
    modifier: Modifier = Modifier,
) {
    HavenRowContainer(label, null, info, icon, null, true, null, null, modifier)
}

/**
 * Segmented control: recessed track (white 6% dark / black 6% light),
 * sliding accent-soft thumb with accent text, one spring, each label
 * drawn exactly once above the thumb.
 *
 * 3.1.1 (item 5): segmented layout decision, pure and unit-tested.
 * Equal segments only when every segment is at least [minSegmentPx]
 * AND fits its label plus [labelPaddingPx]; otherwise wrapping chips.
 * An empty option list is an equal layout of zero segments (callers
 * never pass one, but the decision never throws).
 */
sealed interface SegmentedMode {
    data class Equal(val segmentWidthPx: Int) : SegmentedMode
    data object Chips : SegmentedMode
}

fun segmentedLayoutMode(
    availableWidthPx: Int,
    labelWidthsPx: List<Int>,
    minSegmentPx: Int,
    labelPaddingPx: Int,
): SegmentedMode {
    if (labelWidthsPx.isEmpty()) return SegmentedMode.Equal(0)
    val seg = availableWidthPx / labelWidthsPx.size
    val fits = seg >= minSegmentPx &&
        labelWidthsPx.all { it + labelPaddingPx <= seg }
    return if (fits) SegmentedMode.Equal(seg) else SegmentedMode.Chips
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HavenSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val roles = LocalAccentRoles.current
    val state = LocalState.current
    val isDark = palette.bg.luminance() < 0.5f
    val track = (if (isDark) Color.White else Color.Black).copy(alpha = 0.06f)
    // 3.1.1 (item 5): measure labels (Medium = the widest weight) so
    // the layout decision is exact at any font scale. Equal segments
    // of at least 64dp; when labels cannot fit, wrapping chips — the
    // selected pill never wraps its text and labels never clip.
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    BoxWithConstraints(
        modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(track)
            .alpha(if (enabled) 1f else 0.4f),
    ) {
        val density = LocalDensity.current
        val minSegPx = with(density) { 64.dp.toPx() }.toInt()
        val padPx = with(density) { 8.dp.toPx() }.toInt()
        val labelWidths = options.map { measurer.measure(it, labelStyle).size.width }
        val mode = segmentedLayoutMode(
            with(density) { maxWidth.toPx() }.toInt(),
            labelWidths,
            minSegPx,
            padPx,
        )
        when (mode) {
            is SegmentedMode.Equal -> {
                val segW = with(density) { mode.segmentWidthPx.toDp() }
                val thumbOffset by animateDpAsState(
                    targetValue = segW * selected.coerceIn(0, options.size - 1),
                    animationSpec = havenDpSpring(state.animSpeed, state.reduceMotion),
                    label = "segmentedThumb",
                )
                Box(
                    Modifier
                        .offset(x = thumbOffset)
                        .size(segW, 40.dp)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color(roles.accentContainer)),
                )
                Row(Modifier.fillMaxSize()) {
                    options.forEachIndexed { i, label ->
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable(enabled = enabled) { onSelect(i) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                fontSize = 14.sp,
                                color = if (i == selected) Color(roles.accent) else palette.onSurface,
                                fontWeight = if (i == selected) FontWeight.Medium else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            SegmentedMode.Chips -> {
                FlowRow(
                    Modifier.fillMaxWidth().padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEachIndexed { i, label ->
                        val isSel = i == selected
                        Box(
                            Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(
                                    if (isSel) Color(roles.accentContainer) else Color.Transparent,
                                )
                                .clickable(enabled = enabled) { onSelect(i) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                fontSize = 14.sp,
                                color = if (isSel) Color(roles.accent) else palette.onSurface,
                                fontWeight = if (isSel) FontWeight.Medium else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Switch: 52×32dp, check mark on the thumb when on, accent track when on, neutral track when off. */
@Composable
fun HavenSwitch(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val roles = LocalAccentRoles.current
    val state = LocalState.current
    val haptics = LocalHapticFeedback.current
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = havenDpSpring(state.animSpeed, state.reduceMotion),
        label = "switchThumb",
    )
    Box(
        modifier
            .size(52.dp, 32.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (checked) Color(roles.accent) else palette.onSurface.copy(alpha = 0.18f))
            .semantics {
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                contentDescription = if (checked) "On" else "Off"
            }
            .clickable(enabled = enabled) {
                if (state.pressFeedback && state.hapticStrength > 0) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onChange(!checked)
            },
    ) {
        Box(
            Modifier
                .offset(x = 4.dp + thumbOffset)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                HavenIconImage(HavenIcon.Check, null, tint = Color(roles.accent), size = 14.dp)
            }
        }
    }
}

/** Slider: value bubble while dragging, haptic tick per step, debounced writes via onCommit on release. */
@Composable
fun HavenSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: ((Float) -> Unit)?,
    onCommit: (Float) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    valueText: (Float) -> String = { it.toString() },
) {
    val palette = LocalPalette.current
    val roles = LocalAccentRoles.current
    val state = LocalState.current
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val dragged by interaction.collectIsDraggedAsState()
    var lastStep by remember { mutableStateOf<Int?>(null) }
    val stepSize = if (steps > 0) (valueRange.endInclusive - valueRange.start) / (steps + 1) else 0f
    BoxWithConstraints(modifier) {
        val trackW = maxWidth
        Column {
            if (dragged) {
                val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
                    .coerceIn(0f, 1f)
                // Bubble x in dp: fraction of the track width, clamped so it stays on screen.
                val x = (trackW * fraction - 28.dp).coerceIn(0.dp, (trackW - 56.dp).coerceAtLeast(0.dp))
                Box(
                    Modifier
                        .offset(x = x)
                        .widthIn(min = 56.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color(roles.accent))
                        .padding(horizontal = HavenSpacing.s12, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        valueText(value),
                        color = Color(roles.onAccent),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Slider(
                value = value,
                onValueChange = { v ->
                    if (stepSize > 0f) {
                        val step = ((v - valueRange.start) / stepSize).roundToInt()
                        if (step != lastStep) {
                            lastStep = step
                            if (state.pressFeedback && state.hapticStrength > 0) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                    onValueChange?.invoke(v)
                },
                valueRange = valueRange,
                steps = steps,
                enabled = enabled,
                onValueChangeFinished = { onCommit(value) },
                interactionSource = interaction,
                colors = SliderDefaults.colors(
                    thumbColor = Color(roles.accent),
                    activeTrackColor = Color(roles.accent),
                    inactiveTrackColor = palette.onSurface.copy(alpha = 0.18f),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Banner: tonal accent-container background, icon tile, one action, no hardcoded colours. */
@Composable
fun HavenBanner(
    icon: HavenIcon,
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    val palette = LocalPalette.current
    val roles = LocalAccentRoles.current
    val error = MaterialTheme.colorScheme.error
    val bg = if (danger) error.copy(alpha = 0.12f) else Color(roles.accentContainer)
    Row(
        modifier
            .fillMaxWidth()
            .clip(HavenRadii.groupShape())
            .background(bg)
            .padding(HavenSpacing.s16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HavenIconTile(icon, null, Modifier.padding(end = HavenSpacing.s12))
        Text(text, style = HavenType.rowLabel, color = palette.onSurface, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction) {
            Text(actionLabel, color = if (danger) error else Color(roles.accent), fontWeight = FontWeight.Medium)
        }
    }
}

/** Dialog: Glass3, title + body + text button + at most one filled button. */
@Composable
fun HavenDialog(
    onDismiss: () -> Unit,
    title: String,
    body: String,
    dismissLabel: String,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    danger: Boolean = false,
) {
    val palette = LocalPalette.current
    val roles = LocalAccentRoles.current
    val error = MaterialTheme.colorScheme.error
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .glass(GlassLevel.GLASS3, HavenRadii.dialogShape())
                .padding(HavenSpacing.s24),
        ) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = palette.onSurface)
            Spacer(Modifier.padding(top = HavenSpacing.s8))
            Text(body, fontSize = 14.sp, color = palette.muted)
            Row(
                Modifier.fillMaxWidth().padding(top = HavenSpacing.s24),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(dismissLabel, color = palette.muted)
                }
                if (confirmLabel != null && onConfirm != null) {
                    Spacer(Modifier.padding(start = HavenSpacing.s8))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (danger) error else Color(roles.accent),
                            contentColor = if (danger) Color.White else Color(roles.onAccent),
                        ),
                    ) {
                        Text(confirmLabel)
                    }
                }
            }
        }
    }
}

data class HavenMenuItem(
    val icon: HavenIcon?,
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * 3.1 Phase C: Glass3 dialog shell for dialogs whose body is custom
 * content (fields, lists, rows) that [HavenDialog]'s fixed
 * title/body/buttons shape cannot host. Same anatomy: 20sp title +
 * content + a button row (text buttons, at most one filled button).
 * Callers keep every action and copy; only the surface changes.
 */
@Composable
fun HavenGlassDialog(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalPalette.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier
                .fillMaxWidth()
                .glass(GlassLevel.GLASS3, HavenRadii.dialogShape())
                .padding(HavenSpacing.s24),
        ) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = palette.onSurface)
            Spacer(Modifier.height(HavenSpacing.s12))
            content()
            if (buttons != null) {
                Row(
                    Modifier.fillMaxWidth().padding(top = HavenSpacing.s24),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) { buttons() }
            }
        }
    }
}

/**
 * 3.1 Phase C: icon+label row for dialog-form menus (48dp min,
 * destructive entries tinted danger colour + text). Used by the
 * drawer Library menu and the home dock menu.
 */
@Composable
fun HavenMenuRow(
    icon: HavenIcon?,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val error = MaterialTheme.colorScheme.error
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            HavenIconImage(
                icon,
                null,
                size = 20.dp,
                tint = if (destructive) error else palette.muted,
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            label,
            color = if (destructive) error else palette.onSurface,
            fontSize = 15.sp,
            maxLines = 1,
        )
    }
}

/** The one filled button of a [HavenGlassDialog] button row (accent, or danger red). */
@Composable
fun RowScope.HavenDialogFilledButton(
    label: String,
    onClick: () -> Unit,
    danger: Boolean = false,
    enabled: Boolean = true,
) {
    val roles = LocalAccentRoles.current
    val error = MaterialTheme.colorScheme.error
    Button(
        enabled = enabled,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (danger) error else Color(roles.accent),
            contentColor = if (danger) Color.White else Color(roles.onAccent),
        ),
    ) { Text(label) }
}

/** Menu: Glass3, icon + label rows of 48dp, destructive entries tinted with the error colour. */
@Composable
fun HavenMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<HavenMenuItem>,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val error = MaterialTheme.colorScheme.error
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = HavenRadii.menuShape(),
        containerColor = palette.surface,
        shadowElevation = 0.dp,
    ) {
        items.forEach { item ->
            DropdownMenuItem(
                text = {
                    Text(
                        item.label,
                        color = if (item.destructive) error else palette.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = item.icon?.let {
                    { HavenIconImage(it, null, tint = if (item.destructive) error else palette.muted, size = 20.dp) }
                },
                onClick = { onDismiss(); item.onClick() },
                colors = MenuDefaults.itemColors(
                    textColor = if (item.destructive) error else palette.onSurface,
                ),
                modifier = Modifier.heightIn(min = HAVEN_TOUCH_TARGET),
            )
        }
    }
}

/** Snackbar: Glass3 pill, message + optional action. The caller places and times it. */
@Composable
fun HavenSnackbar(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val roles = LocalAccentRoles.current
    Row(
        modifier
            .glass(GlassLevel.GLASS3, RoundedCornerShape(percent = 50))
            .padding(horizontal = HavenSpacing.s16, vertical = 4.dp)
            .sizeIn(minHeight = HAVEN_TOUCH_TARGET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, color = palette.onSurface, fontSize = 13.sp, modifier = Modifier.weight(1f, fill = false))
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, modifier = Modifier.sizeIn(minHeight = HAVEN_TOUCH_TARGET)) {
                Text(actionLabel, color = Color(roles.accent), fontWeight = FontWeight.Bold)
            }
        }
        if (onDismiss != null) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(HAVEN_TOUCH_TARGET)) {
                HavenIconImage(HavenIcon.Close, "Dismiss", tint = palette.onSurface, size = 16.dp)
            }
        }
    }
}

/** Empty state: small mascot pose (or icon), one-line headline, one-line body, one verb button. */
@Composable
fun EmptyState(
    headline: String,
    body: String,
    buttonLabel: String,
    onButton: () -> Unit,
    modifier: Modifier = Modifier,
    icon: HavenIcon? = null,
    pose: (@Composable () -> Unit)? = null,
) {
    val palette = LocalPalette.current
    Column(
        modifier.fillMaxWidth().padding(HavenSpacing.s24),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (pose != null) {
            pose()
        } else if (icon != null) {
            HavenIconTile(icon, null, size = 56.dp)
        }
        Spacer(Modifier.padding(top = HavenSpacing.s12))
        Text(headline, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = palette.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(body, fontSize = 14.sp, color = palette.muted,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.padding(top = HavenSpacing.s16))
        Button(
            onClick = onButton,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(LocalAccentRoles.current.accent),
                contentColor = Color(LocalAccentRoles.current.onAccent),
            ),
        ) {
            Text(buttonLabel)
        }
    }
}

/** Skeleton: shimmer pulse, static under Reduce Motion. The caller supplies the shape via modifier. */
@Composable
fun HavenSkeleton(modifier: Modifier = Modifier) {
    val state = LocalState.current
    val palette = LocalPalette.current
    if (state.reduceMotion) {
        Box(modifier.background(palette.onSurface.copy(alpha = 0.10f)))
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val alpha by transition.animateFloat(
            initialValue = 0.08f,
            targetValue = 0.18f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
            ),
            label = "skeletonAlpha",
        )
        Box(modifier.background(palette.onSurface.copy(alpha = alpha)))
    }
}

/** Help: a 48dp touch target opening a Glass3 popover with the help text. */
@Composable
fun HavenHelp(
    helpText: String,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val palette = LocalPalette.current
    Box(modifier) {
        IconButton(
            onClick = { open = true },
            modifier = Modifier.size(HAVEN_TOUCH_TARGET),
        ) {
            HavenIconImage(HavenIcon.Help, "Help", tint = palette.muted, size = 22.dp)
        }
        if (open) {
            Popup(
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
            ) {
                Box(
                    Modifier
                        .widthIn(max = 320.dp)
                        .glass(GlassLevel.GLASS3, HavenRadii.controlShape())
                        .padding(HavenSpacing.s16)
                        .clickable { open = false },
                ) {
                    Text(helpText, style = HavenType.rowValue, color = palette.onSurface)
                }
            }
        }
    }
}
