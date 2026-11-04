package app.havenos.launcher.ui.glass

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import app.havenos.launcher.ui.speedDuration

// ---------------------------------------------------------------------------
// HavenOS 3.1 Phase B6 — motion tokens. Durations stay: app open 320ms,
// folders 280ms, dock 240ms; hub transitions 220ms shared axis (already in
// SettingsPanel). One spring for switch/segmented. Haptic ticks on
// switch/step/snap (call sites use the home tick helper). Motion speed
// (Off/Fast/Normal/Slow) scales all; Reduce Motion → 100ms fades;
// Reduce Effects flattens glass to a translucent tint (see glass()).
// Precedence: Reduce Motion wins over Motion Off (100ms fade, not snap).
// ---------------------------------------------------------------------------

object HavenDurations {
    const val APP_OPEN_MS: Int = 320
    const val FOLDER_MS: Int = 280
    const val DOCK_MS: Int = 240
    const val HUB_MS: Int = 220
    const val REDUCE_MOTION_FADE_MS: Int = 100
}

/** Scaled duration: Reduce Motion → 100ms fade; Motion Off → instant; else scaled. */
fun havenDurationMs(baseMs: Int, animSpeed: Int, reduceMotion: Boolean): Int = when {
    reduceMotion -> HavenDurations.REDUCE_MOTION_FADE_MS
    animSpeed == 0 -> 0
    else -> speedDuration(baseMs, animSpeed)
}

/** One spring for switch/segmented thumbs. Reduce Motion or Off → snap (a thumb has no fade target). */
fun havenDpSpring(animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Dp> =
    if (reduceMotion || animSpeed == 0) {
        snap()
    } else {
        val stiffness = when (animSpeed) {
            1 -> 600f
            3 -> 150f
            else -> 300f
        }
        spring(dampingRatio = 1f, stiffness = stiffness)
    }

/** Fade spec honouring Reduce Motion (100ms) and Motion Off (instant). */
fun havenFadeSpec(animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float> = when {
    reduceMotion -> tween(HavenDurations.REDUCE_MOTION_FADE_MS)
    animSpeed == 0 -> snap()
    else -> tween(speedDuration(HavenDurations.HUB_MS, animSpeed))
}
