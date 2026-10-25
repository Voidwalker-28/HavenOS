package app.havenos.launcher.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * M6 motion speed (animSpeed, values from SettingsState: 0 Off, 1 Fast,
 * 2 Normal, 3 Slow). One mapping for every real transition so the
 * Gestures & Motion speed control is not a demo: Home's dock fade and
 * edit-mode shrink, the Quick Panel slide, and the settings panel slide
 * all scale from the same base durations. Reduce motion still wins
 * over every speed (instant), and Off is instant too. Spring
 * transitions scale stiffness inversely (fast = stiffer, slow = softer)
 * because a spring has no duration to scale.
 */
fun speedDuration(baseMs: Int, animSpeed: Int): Int = when (animSpeed) {
    0 -> 0
    1 -> baseMs / 2
    3 -> baseMs * 2
    else -> baseMs
}

fun speedTween(baseMs: Int, animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float> =
    if (reduceMotion || animSpeed == 0) snap() else tween(speedDuration(baseMs, animSpeed))

fun speedSpring(animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float> =
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
