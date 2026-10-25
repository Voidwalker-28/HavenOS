package app.havenos.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * M7 Wave 2 (Worker 6) — Haptic helpers for home interactions.
 *
 * NEW FILE ONLY. Nothing here is attached to any host; the coordinator
 * wires the calls at the sites listed in the hook spec
 * (docs/HOME-M7-WAVE2-MOTION-HAPTICS-A11Y-HOOKS.md).
 *
 * Setting names referenced (SettingsState, data/Settings.kt — read
 * only, no new keys):
 * - `pressFeedback` (Boolean, default true): the master haptic switch
 *   for home/drawer feedback. When false, NOTHING in this file fires.
 * - `hapticStrength` (Int 0..100, default 50): 0 = no vibration.
 *   The gate is exactly the one Drawer.kt already uses for its
 *   scrubber (`state.pressFeedback && state.hapticStrength > 0`,
 *   Drawer.kt lines 657 and 722) — this file just names it so every
 *   new call site gates identically instead of re-deriving it.
 * - NOT used here: `navHaptics`. That switch belongs to the
 *   accessibility navigation-gesture service only (Settings →
 *   Gestures & Motion "Haptics" row) and must not be confused with
 *   home haptics. Do not gate home ticks on it.
 *
 * Honest limitation, stated plainly: Compose's HapticFeedback API
 * exposes fixed feedback TYPES, not an amplitude. There is no
 * VIBRATE permission in the manifest and this file deliberately does
 * NOT reach for android.os.Vibrator, so `hapticStrength` cannot scale
 * vibration amplitude — it gates on/off only (0 = off, anything above
 * = on at the platform's fixed strength for that type). A tick stays
 * a tick at every strength: upgrading a Strong tick to a LongPress
 * buzz would feel wrong and would break consistency with the
 * scrubber tick Drawer.kt already ships (always TextHandleMove).
 * If a future worker wants true amplitude scaling it needs the
 * permission, a Vibrator path, and a settings change; that is
 * coordinator work, not a silent addition here.
 *
 * Doc §9 events covered: scrubber-letter tick, edit-mode grid-snap
 * tick, long-press confirm pulse. Deliberately NOT covered: page snap
 * (no pages in M7 — there is no page-snap haptic to fire).
 */

// ------------------------------------------------------------ gate

/**
 * The single haptic gate. Hosts pass their SettingsState fields
 * verbatim: `hapticsEnabled(state.pressFeedback, state.hapticStrength)`.
 * Pure; never throws. Out-of-range strengths are clamped, not trusted.
 */
fun hapticsEnabled(pressFeedback: Boolean, hapticStrength: Int): Boolean =
    pressFeedback && hapticStrength.coerceIn(0, 100) > 0

/** Tick type: TextHandleMove, the same tick Drawer.kt's scrubber already uses. */
fun hapticTickType(): HapticFeedbackType = HapticFeedbackType.TextHandleMove

/** Long-press confirm type: always the platform long-press pulse, at any enabled strength. */
fun hapticConfirmType(): HapticFeedbackType = HapticFeedbackType.LongPress

// ------------------------------------------------------------ perform

/**
 * Scrubber-letter tick. Call ONLY when the letter actually changes
 * (the host tracks last-ticked letter, as Drawer.kt's AlphabetScrubber
 * already does) — firing per pointer move, not per change, would buzz
 * continuously down the strip. No-op when [enabled] is false.
 */
fun HapticFeedback.homeScrubberTick(enabled: Boolean) {
    if (!enabled) return
    performHapticFeedback(hapticTickType())
}

/**
 * Edit-mode grid-snap tick. Call when a dragged icon snaps to a new
 * grid cell (not on every drag delta — only on cell change, same
 * change-only rule as the scrubber tick). No-op when [enabled] is false.
 */
fun HapticFeedback.homeGridSnapTick(enabled: Boolean) {
    if (!enabled) return
    performHapticFeedback(hapticTickType())
}

/**
 * Long-press confirm pulse. Call once, at the moment long-press
 * recognition fires (not on release, not on drag start). No-op when
 * [enabled] is false.
 */
fun HapticFeedback.homeLongPressConfirm(enabled: Boolean) {
    if (!enabled) return
    performHapticFeedback(hapticConfirmType())
}

/**
 * Page-turn tick (M9, Doc 2 §7). Call once when the pager settles on a
 * new page — not per scroll delta. Same tick type as the scrubber tick.
 * No-op when [enabled] is false.
 */
fun HapticFeedback.homePageTurnTick(enabled: Boolean) {
    if (!enabled) return
    performHapticFeedback(hapticTickType())
}

// ------------------------------------------------------------ remember

/**
 * Composable handle bundling the gate with the platform feedback, so
 * call sites read cleanly. The host passes its SettingsState fields
 * once; every method no-ops when disabled.
 *
 * Example host wiring (inside a composable that already has `state`):
 * ```
 * val homeHaptics = rememberHomeHaptics(state.pressFeedback, state.hapticStrength)
 * // in a scrubber letter-change handler:
 * homeHaptics.scrubberTick()
 * // in a drag cell-change handler:
 * homeHaptics.gridSnapTick()
 * // in a long-press handler, before opening the menu:
 * homeHaptics.longPressConfirm()
 * ```
 * Inside pointerInput gesture blocks (non-composable scope), capture
 * `homeHaptics` from the enclosing composable — do not call
 * LocalHapticFeedback.current inside the gesture block.
 */
class HomeHaptics(
    val enabled: Boolean,
    private val feedback: HapticFeedback,
) {
    fun scrubberTick() = feedback.homeScrubberTick(enabled)
    fun gridSnapTick() = feedback.homeGridSnapTick(enabled)
    fun longPressConfirm() = feedback.homeLongPressConfirm(enabled)
    fun pageTurnTick() = feedback.homePageTurnTick(enabled)
}

@Composable
fun rememberHomeHaptics(pressFeedback: Boolean, hapticStrength: Int): HomeHaptics {
    val feedback = LocalHapticFeedback.current
    val enabled = hapticsEnabled(pressFeedback, hapticStrength)
    return remember(feedback, enabled) {
        HomeHaptics(enabled, feedback)
    }
}
