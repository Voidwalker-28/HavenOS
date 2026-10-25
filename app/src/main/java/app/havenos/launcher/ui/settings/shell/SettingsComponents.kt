package app.havenos.launcher.ui.settings.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.havenos.launcher.theme.LocalFont

// ---------------------------------------------------------------------------
// M6 shell components (v2 §6). Tonal cards, no outlines, no group boxes,
// no always-visible helper text, no letter tiles, no tier tags.
// House rules enforced here, not in each hub:
//  - Row height 56dp phone / 64dp tablet; minimum touch target 48dp.
//  - Title is short sentence case (callers keep it to ~3 words); the live
//    value sits on the right, muted, ellipsised rather than wrapped.
//  - Help exists ONLY behind the info button: a small popover capped at
//    90 characters. There is no other helper text on a row.
//  - Sliders: smooth continuous track, 4dp, no tick dots (steps = 0),
//    a value bubble only while dragging, and - / + step buttons.
//  - Switches and segmented controls are driven by the live accent.
// Hubs keep a screen to ~7 rows; overflow goes in [ShellMoreOptions]
// and rare options in [ShellAdvancedFold].
// ---------------------------------------------------------------------------

/** Tablet = device width >= 600dp (the row-height breakpoint, v2 §6). */
@Composable
fun isShellTablet(): Boolean = LocalConfiguration.current.screenWidthDp >= 600

@Composable
fun shellRowHeight(): androidx.compose.ui.unit.Dp =
    if (isShellTablet()) 64.dp else 56.dp

/** Tonal card: panel-lifted fill, 20dp radius, 16dp padding, no border. */
@Composable
fun ShellCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val tokens = LocalShellTokens.current
    val click = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ShellRadii.card))
            .background(tokens.cardSurface)
            .then(click)
            .padding(ShellSpacing.cardPadding),
    ) { content() }
}

/** Small popover for row help (Dialog styled as a card, not a full sheet). */
@Composable
private fun HelpPopover(help: String, onDismiss: () -> Unit) {
    val tokens = LocalShellTokens.current
    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(ShellRadii.chip))
                .background(tokens.panelSurface)
                .padding(ShellSpacing.s16),
        ) {
            Text(
                text = help.take(90),
                style = ShellText.help.copy(fontFamily = LocalFont.current),
                color = tokens.textPrimary,
            )
        }
    }
}

/**
 * One lean row: title + live value on the right, optional info button,
 * optional inline control below the header. [value] is display-only text
 * (callers keep it short, ~14 chars); it ellipsises, never wraps.
 * Disabled rows dim and carry a one-line reason under the title — the
 * only always-visible secondary line a row may have.
 */
@Composable
fun ShellRow(
    title: String,
    value: String? = null,
    help: String? = null,
    enabled: Boolean = true,
    disabledReason: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val tokens = LocalShellTokens.current
    var helpOpen by remember { mutableStateOf(false) }
    ShellCard(
        onClick = if (enabled) onClick else null,
        modifier = Modifier.alpha(if (enabled) 1f else 0.45f),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = shellRowHeight())
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                        color = tokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!enabled && disabledReason != null) {
                        Text(
                            text = disabledReason,
                            style = ShellText.help.copy(fontFamily = LocalFont.current),
                            color = tokens.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (value != null) {
                    Spacer(Modifier.width(ShellSpacing.s8))
                    Text(
                        text = value,
                        style = ShellText.value.copy(fontFamily = LocalFont.current),
                        color = tokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (help != null) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Help for $title"; role = Role.Button }
                            .clickable { helpOpen = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "ⓘ",
                            style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                            color = tokens.accent,
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(ShellSpacing.s8))
                    trailing()
                }
            }
            if (content != null) {
                Spacer(Modifier.height(ShellSpacing.s8))
                content()
            }
        }
    }
    if (helpOpen && help != null) {
        HelpPopover(help = help, onDismiss = { helpOpen = false })
    }
}

/** On/off row. The value text ("On"/"Off") states the state in words, never colour alone. */
@Composable
fun ShellSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    help: String? = null,
    enabled: Boolean = true,
    disabledReason: String? = null,
) {
    val tokens = LocalShellTokens.current
    ShellRow(
        title = title,
        value = if (checked) "On" else "Off",
        help = help,
        enabled = enabled,
        disabledReason = disabledReason,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = tokens.accent,
                    uncheckedThumbColor = tokens.textSecondary,
                    uncheckedTrackColor = tokens.cardSurface,
                ),
            )
        },
    )
}

/**
 * Slider row (v2 §6): smooth 4dp track with no tick dots, a 28dp thumb,
 * a value bubble above the thumb only while dragging, and - / + step
 * buttons (48dp targets) for accessibility. [onPreview] fires live while
 * dragging; [onCommit] fires once on release. A haptic tick fires on step
 * buttons only — never continuously while dragging.
 */
@Composable
fun ShellSliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float = 0f,
    valueText: (Float) -> String,
    help: String? = null,
    enabled: Boolean = true,
    disabledReason: String? = null,
    autoLabel: String? = null,
    onAuto: (() -> Unit)? = null,
    onPreview: (Float) -> Unit,
    onCommit: (Float) -> Unit,
) {
    val tokens = LocalShellTokens.current
    val haptics = LocalHapticFeedback.current
    var dragging by remember { mutableStateOf(false) }
    val sliderState = rememberSliderState(
        value = value,
        steps = 0,
        onValueChangeFinished = {},
        valueRange = range,
    )
    // Assigned after creation (the state cannot reference itself in its
    // own initializer lambda); fires once, on release, with the final value.
    sliderState.onValueChangeFinished = { dragging = false; onCommit(sliderState.value) }
    // Observe the state value: this is the live-drag signal. The first
    // emission is the initial value and is skipped (no spurious write).
    var lastSent by remember { mutableStateOf(value) }
    val current = sliderState.value
    LaunchedEffect(current) {
        if (current != lastSent) {
            lastSent = current
            dragging = true
            onPreview(current)
        }
    }
    // Re-sync when the outside value moves (e.g. a Reset) — never mid-drag.
    LaunchedEffect(value) {
        if (!dragging && sliderState.value != value) sliderState.value = value
    }
    val stepSize = if (step > 0f) step else (range.endInclusive - range.start) / 20f
    fun nudge(delta: Float) {
        val next = (sliderState.value + delta).coerceIn(range.start, range.endInclusive)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        sliderState.value = next
        onPreview(next)
        onCommit(next)
    }
    ShellRow(
        title = title,
        value = valueText(value),
        help = help,
        enabled = enabled,
        disabledReason = disabledReason,
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (dragging) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(ShellRadii.chip))
                            .background(tokens.cardSelected)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = valueText(current),
                            style = ShellText.help.copy(
                                fontFamily = LocalFont.current,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = tokens.accent,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "Decrease $title"; role = Role.Button }
                        .clickable(enabled = enabled) { nudge(-stepSize) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "−",
                        style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                        color = tokens.accent,
                    )
                }
                Slider(
                    state = sliderState,
                    enabled = enabled,
                    colors = SliderDefaults.colors(
                        thumbColor = tokens.accent,
                        activeTrackColor = tokens.accent,
                        inactiveTrackColor = tokens.accent.copy(alpha = 0.24f),
                    ),
                    thumb = {
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(tokens.accent),
                        )
                    },
                    track = { st ->
                        val span = (st.valueRange.endInclusive - st.valueRange.start)
                            .takeIf { it > 0f } ?: 1f
                        val fraction = ((st.value - st.valueRange.start) / span).coerceIn(0f, 1f)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(50)),
                        ) {
                            Box(
                                Modifier
                                    .weight(fraction.coerceAtLeast(0.001f))
                                    .height(4.dp)
                                    .background(tokens.accent),
                            )
                            Box(
                                Modifier
                                    .weight((1f - fraction).coerceAtLeast(0.001f))
                                    .height(4.dp)
                                    .background(tokens.accent.copy(alpha = 0.24f)),
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "Increase $title"; role = Role.Button }
                        .clickable(enabled = enabled) { nudge(stepSize) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+",
                        style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                        color = tokens.accent,
                    )
                }
            }
            if (autoLabel != null && onAuto != null) {
                Box(
                    Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(ShellRadii.chip))
                        .background(tokens.cardSelected)
                        .clickable { onAuto() }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = autoLabel,
                        style = ShellText.help.copy(fontFamily = LocalFont.current),
                        color = tokens.accent,
                    )
                }
            }
        }
    }
}

/** Segmented control for 2–4 mutually exclusive options. Selected = accent tint, never an outline. */
@Composable
fun ShellSegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tokens = LocalShellTokens.current
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ShellSpacing.s8),
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(ShellRadii.chip))
                    .background(if (sel) tokens.cardSelected else tokens.cardSurface)
                    .alpha(if (enabled) 1f else 0.45f)
                    .semantics {
                        contentDescription = "$label, ${if (sel) "selected" else "not selected"}"
                        role = Role.Button
                    }
                    .clickable(enabled = enabled) { onSelect(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = ShellText.help.copy(
                        fontFamily = LocalFont.current,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (sel) tokens.accent else tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Swatch row: 48dp targets, 36dp colour dots, a 2dp accent ring on the selected one. */
@Composable
fun ShellSwatchRow(
    swatches: List<Int>,
    selectedArgb: Int,
    onPick: (Int) -> Unit,
    onCustom: (() -> Unit)? = null,
) {
    val tokens = LocalShellTokens.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ShellSpacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (argb in swatches) {
            val sel = argb == selectedArgb
            Box(
                Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .then(
                            if (sel) Modifier.border(2.dp, tokens.accent, CircleShape)
                            else Modifier,
                        )
                        .clickable { onPick(argb) },
                )
            }
        }
        if (onCustom != null) {
            TextButton(onClick = onCustom) {
                Text(
                    text = "Custom…",
                    style = ShellText.help.copy(fontFamily = LocalFont.current),
                    color = tokens.accent,
                )
            }
        }
    }
}

/** Custom colour picker: hex input, validated to exactly 6 hex digits. */
@Composable
fun ShellColorPickerDialog(
    initialArgb: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    val tokens = LocalShellTokens.current
    var text by remember {
        mutableStateOf(
            if (initialArgb != 0) "#%06X".format(initialArgb and 0xFFFFFF) else "#",
        )
    }
    val hex = text.trim().removePrefix("#")
    val parsed = hex.toLongOrNull(16)
    val valid = hex.length == 6 && parsed != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Custom colour",
                style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                color = tokens.textPrimary,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(text = "#RRGGBB") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!valid && hex.isNotEmpty()) {
                    Spacer(Modifier.height(ShellSpacing.s8))
                    Text(
                        text = "Enter 6 hex digits, e.g. #2F6FCE.",
                        style = ShellText.help.copy(fontFamily = LocalFont.current),
                        color = tokens.danger,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { if (valid) onPick((0xFF000000L or (parsed ?: 0L)).toInt()) },
            ) {
                Text(
                    text = "Use colour",
                    style = ShellText.help.copy(fontFamily = LocalFont.current),
                    color = tokens.accent,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = ShellText.help.copy(fontFamily = LocalFont.current),
                    color = tokens.textSecondary,
                )
            }
        },
    )
}

/** "More options" expander: overflow rows for a screen that already shows ~7. */
@Composable
fun ShellMoreOptions(
    title: String = "More options",
    content: @Composable () -> Unit,
) {
    ShellExpander(title = title, content = content)
}

/** Collapsed-by-default fold for rare or technical options. */
@Composable
fun ShellAdvancedFold(
    title: String = "Advanced",
    content: @Composable () -> Unit,
) {
    ShellExpander(title = title, content = content)
}

@Composable
private fun ShellExpander(
    title: String,
    content: @Composable () -> Unit,
) {
    val tokens = LocalShellTokens.current
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable { open = !open }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                color = tokens.accent,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (open) "⌄" else "›",
                style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                color = tokens.textSecondary,
            )
        }
        if (open) content()
    }
}

/** Section label: 12sp caps, muted (v2 §9 type scale). */
@Composable
fun ShellSectionLabel(text: String) {
    val tokens = LocalShellTokens.current
    Text(
        text = text.uppercase(),
        style = ShellText.section.copy(fontFamily = LocalFont.current),
        color = tokens.textSecondary,
    )
}

/** Loading state for a screen: skeleton rows, no fake content. */
@Composable
fun ShellSkeletonRow() {
    val tokens = LocalShellTokens.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(shellRowHeight())
            .clip(RoundedCornerShape(ShellRadii.card))
            .background(tokens.cardSurface.copy(alpha = 0.6f)),
    )
}

/** Empty state: one line plus one action. */
@Composable
fun ShellEmptyState(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val tokens = LocalShellTokens.current
    Column(
        Modifier.fillMaxWidth().padding(vertical = ShellSpacing.s16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ShellSpacing.s8),
    ) {
        Text(
            text = message,
            style = ShellText.value.copy(fontFamily = LocalFont.current),
            color = tokens.textSecondary,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = ShellText.help.copy(fontFamily = LocalFont.current),
                    color = tokens.accent,
                )
            }
        }
    }
}

/** Error state: inline message, state unchanged, retry action. */
@Composable
fun ShellErrorRow(
    message: String,
    onRetry: (() -> Unit)? = null,
) {
    val tokens = LocalShellTokens.current
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ShellSpacing.s8),
    ) {
        Text(
            text = message,
            style = ShellText.help.copy(fontFamily = LocalFont.current),
            color = tokens.danger,
            modifier = Modifier.weight(1f),
        )
        if (onRetry != null) {
            TextButton(onClick = onRetry) {
                Text(
                    text = "Retry",
                    style = ShellText.help.copy(fontFamily = LocalFont.current),
                    color = tokens.accent,
                )
            }
        }
    }
}
