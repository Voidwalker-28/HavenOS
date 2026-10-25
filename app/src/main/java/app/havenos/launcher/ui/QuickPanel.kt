package app.havenos.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.SettingsState
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.LocalPalette
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Quick panel (v2.2 §6): drops from the top over home. Live Density
 * slider, dark-mode chips, reduce-effects/motion switches, and shortcuts
 * into Settings / Wallpaper / Edit mode. Every control writes the same
 * Settings the Settings screens edit — nothing is faked. The Look chips
 * were removed with the Looks system (Minimal-only since 2026-10-23).
 * Reduce Motion makes the panel appear/disappear instantly.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickPanel(
    app: HavenApp,
    state: SettingsState,
    onClose: () -> Unit,
    onOpenSettings: (section: Int) -> Unit,
    onEditHome: () -> Unit,
) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    val visible = remember { MutableTransitionState(false) }.apply { targetState = true }
    // Motion speed (animSpeed): the slide scales with the Gestures &
    // Motion speed control; reduce motion or Off makes it instant.
    val enter: EnterTransition = if (state.reduceMotion || state.animSpeed == 0) {
        EnterTransition.None
    } else {
        slideInVertically(tween(speedDuration(220, state.animSpeed))) { -it / 2 } +
            fadeIn(tween(speedDuration(220, state.animSpeed)))
    }
    val exit: ExitTransition = if (state.reduceMotion || state.animSpeed == 0) {
        ExitTransition.None
    } else {
        slideOutVertically(tween(speedDuration(180, state.animSpeed))) { -it / 2 } +
            fadeOut(tween(speedDuration(180, state.animSpeed)))
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(onClick = onClose),
        )
        AnimatedVisibility(
            visibleState = visible,
            enter = enter,
            exit = exit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            GlassSurface(shape = RoundedCornerShape(28.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                        .pointerInput(Unit) {
                            var total = 0f
                            detectVerticalDragGestures(
                                onDragStart = { total = 0f },
                                onVerticalDrag = { _, amount -> total += amount },
                                onDragEnd = { if (total < -70.dp.toPx()) onClose() },
                            )
                        },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Quick panel",
                            color = palette.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onClose) { Text("Close") }
                    }

                    PanelLabel("Density — ${state.density}")
                    // M10 density levels (redesign doc, adapted):
                    // Spacious (airy, fewer/larger), Balanced
                    // (default), Dense (more apps, smaller gaps),
                    // Custom (manual slider / grid overrides). The
                    // airy level is "Spacious", never "Calm", so it
                    // cannot collide with Calm mode (the focus list).
                    // A level is meaningless under a grid override
                    // (the override wins over density), so selecting
                    // one clears both overrides — undo restores the
                    // previous grid. Any other density, or any grid
                    // override, reads as Custom.
                    val hasGridOverride = state.colsPort > 0 || state.rowsPort > 0 ||
                        state.colsLand > 0 || state.rowsLand > 0
                    val densityLevel = when {
                        hasGridOverride -> 3
                        state.density == 15 -> 0
                        state.density == 40 -> 1
                        state.density == 75 -> 2
                        else -> 3
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((index, label) in listOf(
                            0 to "Spacious",
                            1 to "Balanced",
                            2 to "Dense",
                            3 to "Custom",
                        )) {
                            PanelChip(
                                label = label,
                                selected = densityLevel == index,
                                onClick = {
                                    if (index == 3) return@PanelChip
                                    scope.launch {
                                        val value = when (index) {
                                            0 -> 15
                                            1 -> 40
                                            else -> 75
                                        }
                                        app.settings.setDensity(value)
                                        app.settings.clearGridOverride(false)
                                        app.settings.clearGridOverride(true)
                                    }
                                },
                            )
                        }
                    }
                    Slider(
                        value = state.density.toFloat(),
                        onValueChange = { v ->
                            scope.launch { app.settings.setDensity(v.roundToInt()) }
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    PanelLabel("Dark mode")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((mode, label) in listOf(
                            0 to "System", 1 to "Light", 2 to "Dark", 3 to "Auto",
                        )) {
                            PanelChip(
                                label = label,
                                selected = state.darkMode == mode,
                                onClick = { scope.launch { app.settings.setDarkMode(mode) } },
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    PanelSwitch(
                        label = "Reduce effects",
                        checked = state.reduceEffects,
                        onChange = { scope.launch { app.settings.setReduceEffects(it) } },
                    )
                    PanelSwitch(
                        label = "Reduce motion",
                        checked = state.reduceMotion,
                        onChange = { scope.launch { app.settings.setReduceMotion(it) } },
                    )

                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onOpenSettings(0) }) { Text("Settings") }
                        // M5 registry: Wallpaper is category 3 (was 2 in the old 13).
                        TextButton(onClick = { onOpenSettings(3) }) { Text("Wallpaper") }
                        TextButton(onClick = onEditHome) { Text("Edit home") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelLabel(text: String) {
    val palette = LocalPalette.current
    Text(
        text,
        color = palette.muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
    )
}

@Composable
private fun PanelChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .padding(vertical = 3.dp)
            .clip(shape)
            .background(
                if (selected) palette.accent else palette.surface.copy(alpha = 0.55f),
            )
            .then(
                if (selected) Modifier
                else Modifier.border(1.dp, palette.onSurface.copy(alpha = 0.18f), shape),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            color = if (selected) {
                if (palette.accent.luminance() > 0.5f) Color.Black else Color.White
            } else {
                palette.onSurface
            },
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun PanelSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = palette.onSurface,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
