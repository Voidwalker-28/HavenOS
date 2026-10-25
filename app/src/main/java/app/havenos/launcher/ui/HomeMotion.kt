package app.havenos.launcher.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectVerticalDragGestures

/**
 * M7 Wave 2 (Worker 6) — Motion tokens for the home experience.
 *
 * NEW FILE ONLY. Nothing here is attached to any host; the coordinator
 * applies the helpers at the call sites listed in the hook spec
 * (docs/HOME-M7-WAVE2-MOTION-HAPTICS-A11Y-HOOKS.md). This file edits no
 * existing behaviour.
 *
 * Source of truth: HOME-SCREEN-V2-BUILDER-PROMPT.md §9, adapted to the
 * standing M7 directions:
 * - Minimal motion ONLY. The doc's per-preset motion table is NOT built:
 *   every spring here is critically damped (dampingRatio = 1f, via
 *   [speedSpring]), so there is no overshoot and no bounce anywhere.
 * - NO pages. There is no page-snap helper, no page-scroll parallax and
 *   no "Move to page" anything in this file (the TalkBack equivalent is
 *   "Move to section" in HomeA11y.kt).
 * - Transform/opacity ONLY. Every Modifier below touches graphicsLayer
 *   scale / translation / alpha. Nothing animates size, padding,
 *   or any other layout property per frame, and no animation blocks
 *   input: all specs are non-blocking animateFloatAsState / spring
 *   specs, never a delay or a lock.
 *
 * Speed mechanism — REUSED, not forked: [speedDuration] / [speedSpring]
 * from MotionSpeed.kt (M6) are the single mapping from
 * SettingsState.animSpeed (0 Off, 1 Fast, 2 Normal, 3 Slow) to timing.
 * The one deliberate deviation from MotionSpeed.kt is reduced motion:
 * there, reduceMotion snaps transitions to instant; here, per doc §9,
 * reduced motion means 100ms FADES (see [REDUCED_MOTION_FADE_MS]).
 * A fade is still a transition the eye can follow, and it is what the
 * doc specifies, so [homeFadeSpec] / [homeSpringSpec] return
 * tween(100ms) under reduced motion instead of snap(). "Off"
 * (animSpeed == 0) still means instant, exactly as in M6. Hosts that
 * already use speedTween/speedSpring directly (Home.kt dock fade,
 * edit-mode shrink) are untouched; new hosts use the helpers below.
 *
 * Reduced motion also kills: parallax (there is none here to kill —
 * [wpParallax] stays host-owned and must be forced off by the host
 * under reduced motion), wiggle (PagedHome's jiggle is host-owned
 * and must already be off under reduced motion), and springs (every
 * spring below degrades to the 100ms fade).
 *
 * "App open 320ms spring": a spring has no exact duration, so the 320ms
 * is the approximate settle time of the critically-damped spring at
 * Normal stiffness (300f in [speedSpring]). Fast (600f) settles in
 * roughly half that, Slow (150f) in roughly double — the same
 * direction as [speedDuration]'s scaling, without inventing a second
 * mapping. The base-ms constants below are therefore the tween bases
 * for the fade/press specs and the documented settle targets for the
 * springs; hosts must not "tween-ify" a spring to hit 320ms exactly.
 */

// ------------------------------------------------------------ bases (doc §9)

/** App open: 320ms spring from the icon's bounds + scrim return. */
const val APP_OPEN_BASE_MS: Int = 320

/** Library open: 360ms spring + blur ramp + drag-down to close. */
const val LIBRARY_BASE_MS: Int = 360

/** Folder open: 280ms scale from the icon. */
const val FOLDER_BASE_MS: Int = 280

/** Calm enter/exit: 400ms cross-fade, home items fade + drop 12dp. */
const val CALM_BASE_MS: Int = 400

/** Icon press: scale to [PRESS_SCALE] in 80ms (doc §5.1). */
const val PRESS_BASE_MS: Int = 80

/** Pressed icon scale (doc §5.1). */
const val PRESS_SCALE: Float = 0.94f

/** Calm drop distance for home items (doc §9). */
val CALM_DROP_DP: Dp = 12.dp

/** Reduced-motion fade length (doc §9): 100ms fades only. */
const val REDUCED_MOTION_FADE_MS: Int = 100

/**
 * Reduced-motion decision, combining the user's setting with the
 * system animator scale. [animatorScale] is
 * Settings.Global.ANIMATOR_DURATION_SCALE read by the host (1f =
 * normal, 0f = system "Remove animations"); hosts that do not read it
 * pass 1f and the user setting alone decides. Pure; never throws.
 */
fun isReduceMotionEnabled(userReduceMotion: Boolean, animatorScale: Float): Boolean =
    userReduceMotion || animatorScale == 0f

/**
 * Fade spec: [speedDuration]-scaled tween of [baseMs], except reduced
 * motion forces a [REDUCED_MOTION_FADE_MS] fade, and animSpeed == 0
 * (Off) is instant — same Off rule as [speedTween].
 */
fun homeFadeSpec(baseMs: Int, animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float> =
    when {
        animSpeed == 0 -> snap()
        reduceMotion -> tween(REDUCED_MOTION_FADE_MS)
        else -> tween(speedDuration(baseMs, animSpeed))
    }

/**
 * Spring spec: critically damped, no overshoot (Minimal motion).
 * Reduced motion degrades to a [REDUCED_MOTION_FADE_MS] fade instead of
 * a spring (doc §9: no springs under reduced motion); animSpeed == 0
 * is instant. Stiffness scaling is [speedSpring]'s, reused verbatim.
 */
fun homeSpringSpec(animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float> =
    when {
        animSpeed == 0 -> snap()
        reduceMotion -> tween(REDUCED_MOTION_FADE_MS)
        else -> speedSpring(animSpeed, false)
    }

/** App-open spring (see note above: 320ms is the Normal settle target). */
fun appOpenSpec(animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float> =
    homeSpringSpec(animSpeed, reduceMotion)

/**
 * M10 — the ONE page-transition spec (BUG-032/033: stutter + no easing
 * model + no spatial continuity). Every PROGRAMMATIC page change
 * (indicator taps, arrow keys, drag-edge auto-scroll, add-page
 * scroll, move-to-page) uses this critically-damped spring, so pages
 * always move with the same curve and duration. Finger swipes keep
 * the pager's native fling, which cannot take a custom spec without
 * forking fling behavior — that is the one documented exception.
 * Under reduced motion this degrades to the 100ms fade (via
 * [homeSpringSpec]); host indicator taps additionally keep their
 * instant scrollToPage branch under reduced motion, which is
 * stricter than the fade rule and still compliant (no movement).
 */
fun homePageSpec(animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float> =
    homeSpringSpec(animSpeed, reduceMotion)

/** Library-open spring (360ms settle target at Normal). */
fun librarySpec(animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float> =
    homeSpringSpec(animSpeed, reduceMotion)

/** Folder-open spring (280ms settle target at Normal). */
fun folderSpec(animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float> =
    homeSpringSpec(animSpeed, reduceMotion)

/** Calm cross-fade spec (400ms tween, 100ms fade under reduced motion). */
fun calmSpec(animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float> =
    homeFadeSpec(CALM_BASE_MS, animSpeed, reduceMotion)

/** Press spec: 80ms tween scaled by speed; instant under reduced motion. */
fun pressSpec(animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float> =
    if (reduceMotion || animSpeed == 0) snap() else tween(speedDuration(PRESS_BASE_MS, animSpeed))

// ------------------------------------------------------------ press

/**
 * Pressed-state scale for an icon. Pure: pressed -> [PRESS_SCALE],
 * released -> 1f. Under reduced motion the host's [homePressScale]
 * snaps to this instantly instead of animating it.
 */
fun pressTargetScale(pressed: Boolean): Float = if (pressed) PRESS_SCALE else 1f

/**
 * Animated press scale. Apply to the icon container (graphicsLayer
 * scale only). The host owns the pressed boolean (its own
 * interaction source / combinedClickable state); this helper only
 * animates it. Example host wiring:
 * ```
 * val pressed by interaction.collectIsPressedAsState()
 * Box(Modifier.homePressScale(pressed, state.animSpeed, state.reduceMotion)) { … }
 * ```
 */
@Composable
fun Modifier.homePressScale(
    pressed: Boolean,
    animSpeed: Int,
    reduceMotion: Boolean,
): Modifier {
    val scale by animateFloatAsState(
        targetValue = pressTargetScale(pressed),
        animationSpec = pressSpec(animSpeed, reduceMotion),
        label = "homePressScale",
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

// ------------------------------------------------------------ app open

/**
 * Geometry for an app-open placeholder reveal. The real app window
 * animation is owned by the OS and cannot be driven from here; this is
 * for the in-launcher scrim/placeholder the host draws while the app
 * launches, so the eye still sees an expansion from the icon. At
 * progress 0 the full-size placeholder is scaled/translated to cover
 * exactly [iconBounds]; at progress 1 it fills [containerBounds].
 * Scrim alpha ramps 0 -> [scrimMaxAlpha]. Pure; never throws.
 */
data class AppOpenReveal(
    val scaleX: Float,
    val scaleY: Float,
    val translationX: Float,
    val translationY: Float,
    val scrimAlpha: Float,
)

fun appOpenReveal(
    progress: Float,
    iconBounds: Rect,
    containerBounds: Rect,
    scrimMaxAlpha: Float = 0.35f,
): AppOpenReveal {
    val p = progress.coerceIn(0f, 1f)
    val cw = containerBounds.width.coerceAtLeast(1f)
    val ch = containerBounds.height.coerceAtLeast(1f)
    val cx = containerBounds.center.x
    val cy = containerBounds.center.y
    // graphicsLayer pivots on the container centre by default, so the
    // translation that maps the container rect onto the icon rect is
    // iconEdge - (centre + scale * (edge - centre)) per axis.
    val sx0 = (iconBounds.width / cw).coerceIn(0f, 1f)
    val sy0 = (iconBounds.height / ch).coerceIn(0f, 1f)
    val tx0 = iconBounds.left - (cx + sx0 * (containerBounds.left - cx))
    val ty0 = iconBounds.top - (cy + sy0 * (containerBounds.top - cy))
    return AppOpenReveal(
        scaleX = sx0 + (1f - sx0) * p,
        scaleY = sy0 + (1f - sy0) * p,
        translationX = tx0 * (1f - p),
        translationY = ty0 * (1f - p),
        scrimAlpha = scrimMaxAlpha * p,
    )
}

/** Applies an [AppOpenReveal] via graphicsLayer (transform only). */
fun Modifier.homeAppOpenReveal(reveal: AppOpenReveal): Modifier =
    this.graphicsLayer {
        scaleX = reveal.scaleX
        scaleY = reveal.scaleY
        translationX = reveal.translationX
        translationY = reveal.translationY
    }

// ------------------------------------------------------------ library

/**
 * Library blur ramp: 0 at closed -> [maxBlurDp] at fully open.
 * [maxBlurDp] comes from the host's own resolved blur (the glass tier
 * in Glass.kt decides what blur is actually available; this helper
 * never invents a blur radius). Pure.
 */
fun libraryBlurRadius(progress: Float, maxBlurDp: Dp): Dp =
    maxBlurDp * progress.coerceIn(0f, 1f)

/**
 * Library scrim alpha ramp: 0 at closed -> [maxAlpha] at fully open.
 * The doc requires text to stay >= 4.5:1 over the scrim; the host
 * picks [maxAlpha] against its own dim setting. Pure.
 */
fun libraryScrimAlpha(progress: Float, maxAlpha: Float = 0.55f): Float =
    maxAlpha * progress.coerceIn(0f, 1f)

/**
 * Drag-down-to-close progress: 0 at rest -> 1 when the drag reaches
 * [dismissFraction] of the overlay height. Pure.
 */
fun libraryDragProgress(offsetPx: Float, heightPx: Float, dismissFraction: Float = 0.35f): Float {
    if (heightPx <= 0f) return 0f
    return (offsetPx / (heightPx * dismissFraction)).coerceIn(0f, 1f)
}

/** True once a drag-down has passed the dismiss threshold. Pure. */
fun libraryDragDismissed(offsetPx: Float, heightPx: Float, dismissFraction: Float = 0.35f): Boolean =
    libraryDragProgress(offsetPx, heightPx, dismissFraction) >= 1f

/**
 * Drag-down-to-close state, owned by the host (remember it with
 * [rememberLibraryDragCloseState]). [offsetPx] follows the finger
 * (downward only, clamped at 0); [heightPx] is filled in by
 * [homeLibraryDragToClose] from the overlay's measured height.
 * Both are State, deliberately: the graphicsLayer block in
 * [homeLibraryDragVisuals] reads them, so a finger move re-runs the
 * block and the overlay follows without a recomposition. A plain
 * Float field would freeze at the value captured when the modifier
 * chain was built.
 * On release without dismiss, the offset snaps back to 0 instantly —
 * if the host wants an animated return it should drive its own
 * animated float and apply the Float overload of
 * [homeLibraryDragVisuals] instead.
 */
class LibraryDragCloseState {
    var offsetPx: Float by mutableStateOf(0f)
    var heightPx: Float by mutableStateOf(0f)
}

@Composable
fun rememberLibraryDragCloseState(): LibraryDragCloseState =
    remember { LibraryDragCloseState() }

/**
 * Visuals for a drag-down close: the overlay translates with the
 * finger and fades to half alpha at the dismiss point. Transform /
 * opacity only. Two forms:
 * - Float form: for hosts that animate the offset themselves (the
 *   host reads its animated float during composition, so the modifier
 *   chain rebuilds as the value changes).
 * - State form: for [homeLibraryDragToClose], where the gesture
 *   mutates the state directly — the graphicsLayer block reads the
 *   State, so it re-runs on every finger move with no recomposition.
 * Do NOT pass a plain field read once at chain-build time to the
 * Float form; it would freeze at the build-time value.
 */
fun Modifier.homeLibraryDragVisuals(
    offsetPx: Float,
    heightPx: Float,
    dismissFraction: Float = 0.35f,
): Modifier {
    val progress = libraryDragProgress(offsetPx, heightPx, dismissFraction)
    return this.graphicsLayer {
        translationY = offsetPx.coerceAtLeast(0f)
        alpha = 1f - 0.5f * progress
    }
}

fun Modifier.homeLibraryDragVisuals(
    state: LibraryDragCloseState,
    dismissFraction: Float = 0.35f,
): Modifier = this.graphicsLayer {
    // State reads inside the graphicsLayer block: the block re-runs
    // whenever offsetPx/heightPx change, so the overlay follows the
    // finger live. Computing progress here (not at chain-build time)
    // is what makes that true.
    val progress = libraryDragProgress(state.offsetPx, state.heightPx, dismissFraction)
    translationY = state.offsetPx.coerceAtLeast(0f)
    alpha = 1f - 0.5f * progress
}

/**
 * Gesture + visuals for drag-down to close. Apply to the Library
 * overlay root. Dragging only moves the overlay down (never up);
 * releasing past [dismissFraction] of the height calls [onClose],
 * releasing short of it snaps back to rest.
 *
 * HOST RESPONSIBILITY (cannot be decided here): only attach this
 * where a drag-down unambiguously means "close" — if the overlay
 * contains a scrollable list, the host must gate the drag start on
 * its list being at the top (scrollState == 0), otherwise a list
 * scroll would close the Library. Pass [canStartDrag] for that gate;
 * it defaults to always true for non-scrolling overlays.
 */
fun Modifier.homeLibraryDragToClose(
    state: LibraryDragCloseState,
    onClose: () -> Unit,
    dismissFraction: Float = 0.35f,
    canStartDrag: () -> Boolean = { true },
): Modifier = this
    .onSizeChanged { state.heightPx = it.height.toFloat() }
    .homeLibraryDragVisuals(state, dismissFraction)
    .pointerInput(dismissFraction) {
        detectVerticalDragGestures(
            onVerticalDrag = { _, dragAmount ->
                if (canStartDrag()) {
                    state.offsetPx = (state.offsetPx + dragAmount).coerceAtLeast(0f)
                }
            },
            onDragEnd = {
                if (libraryDragDismissed(state.offsetPx, state.heightPx, dismissFraction)) {
                    onClose()
                }
                state.offsetPx = 0f
            },
            onDragCancel = { state.offsetPx = 0f },
        )
    }

// ------------------------------------------------------------ folder

/**
 * Transform origin for a folder panel "scaled from the icon": the
 * fraction of the panel's size where the tapped icon's centre sits,
 * so the panel appears to grow out of the icon. The host passes the
 * icon centre in the panel container's coordinates. Pure.
 */
fun folderTransformOrigin(iconCenter: Offset, containerSize: IntSize): TransformOrigin {
    if (containerSize.width <= 0 || containerSize.height <= 0) {
        return TransformOrigin.Center
    }
    return TransformOrigin(
        pivotFractionX = (iconCenter.x / containerSize.width).coerceIn(0f, 1f),
        pivotFractionY = (iconCenter.y / containerSize.height).coerceIn(0f, 1f),
    )
}

/**
 * Folder panel transform: scale + alpha driven by the host's open
 * progress (0 closed -> 1 open), with [folderSpec] applied by the
 * host's animateFloatAsState. Combine with [folderTransformOrigin]
 * for the grow-from-icon effect. Transform/opacity only.
 */
fun Modifier.homeFolderPanel(
    progress: Float,
    origin: TransformOrigin = TransformOrigin.Center,
): Modifier {
    val p = progress.coerceIn(0f, 1f)
    return this.graphicsLayer {
        transformOrigin = origin
        // Closed panel reads as a shrunken, faded version of the open
        // panel; the 0.9 floor keeps it legible mid-transition.
        scaleX = 0.9f + 0.1f * p
        scaleY = 0.9f + 0.1f * p
        alpha = p
    }
}

// ------------------------------------------------------------ calm

/**
 * Home-items Calm transition: when [calmOn], home items fade out and
 * drop [CALM_DROP_DP]; when Calm exits they return. Under reduced
 * motion the drop is 0 (fade only, per doc §9). Apply to the home
 * content container, NOT to the Calm overlay itself — the overlay
 * fades in with [calmSpec] on its own alpha. Transform/opacity only.
 *
 * Example host wiring (home content):
 * ```
 * Box(Modifier.homeCalmItems(calmOn, state.animSpeed, state.reduceMotion)) { …home… }
 * ```
 */
@Composable
fun Modifier.homeCalmItems(
    calmOn: Boolean,
    animSpeed: Int,
    reduceMotion: Boolean,
): Modifier {
    val spec = calmSpec(animSpeed, reduceMotion)
    val alpha by animateFloatAsState(
        targetValue = if (calmOn) 0f else 1f,
        animationSpec = spec,
        label = "homeCalmAlpha",
    )
    val dropPx = with(LocalDensity.current) {
        if (reduceMotion) 0.dp.toPx() else CALM_DROP_DP.toPx()
    }
    val drop by animateFloatAsState(
        targetValue = if (calmOn) dropPx else 0f,
        animationSpec = spec,
        label = "homeCalmDrop",
    )
    return this.graphicsLayer {
        this.alpha = alpha
        // graphicsLayer translation, not layout offset: the drop must
        // not reflow siblings or resize the container.
        translationY = drop
    }
}
