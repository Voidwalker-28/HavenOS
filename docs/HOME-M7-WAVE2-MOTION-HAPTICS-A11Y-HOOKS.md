# HavenOS M7 Wave 2 — Motion + haptics + accessibility: hook spec (Worker 6)

Coordinator-only application guide. All helpers live in three NEW files
under `app/src/main/java/app/havenos/launcher/ui/` — no existing file
was edited by this worker, and no helper is attached to any host yet.
Apply the snippets below at the named hosts; delete nothing else.

## Files

- `HomeMotion.kt` — motion tokens + helpers (doc §9, Minimal only).
- `HomeHaptics.kt` — haptic helpers (doc §9).
- `HomeA11y.kt` — TalkBack / keyboard / mouse / focus helpers (doc §10).

## Settings referenced (by name, no new keys)

| Setting (`SettingsState`) | Values | Used by |
|---|---|---|
| `animSpeed` | 0 Off, 1 Fast, 2 Normal, 3 Slow | Every motion spec, via MotionSpeed.kt's `speedDuration` / `speedSpring` (reused, not forked) |
| `reduceMotion` | Boolean | Every motion spec; also see `isReduceMotionEnabled()` for the system animator-scale OR |
| `pressFeedback` | Boolean, default true | Haptic gate, with `hapticStrength` |
| `hapticStrength` | Int 0..100, default 50, 0 = off | Haptic gate (same gate Drawer.kt already uses: `state.pressFeedback && state.hapticStrength > 0`) |
| `wpParallax` | Boolean | NOT used here — stays host-owned; hosts must force it off under reduced motion |

NOT used: `navHaptics` (navigation-gesture service only — do not gate
home haptics on it). No amplitude scaling exists: Compose's
`HapticFeedback` has fixed types only, and there is no VIBRATE
permission in the manifest, so `hapticStrength` gates on/off only.
A tick stays a tick at every strength (same `TextHandleMove` the
Drawer scrubber already ships).

## Reduced-motion rule (one deviation, stated plainly)

MotionSpeed.kt snaps transitions to instant under reduced motion.
These new helpers instead use 100ms fades under reduced motion, per
doc §9 ("100ms fades only, no parallax, no wiggle, no springs").
`animSpeed == 0` (Off) is still instant, exactly as in M6. Hosts that
already use `speedTween`/`speedSpring` directly (Home.kt dock fade,
edit-mode shrink) are untouched — do not "fix" them to 100ms fades
in this wave; that is a separate consistency decision for the
coordinator.

## Helper signatures (complete list)

### HomeMotion.kt

```kotlin
const val APP_OPEN_BASE_MS: Int = 320
const val LIBRARY_BASE_MS: Int = 360
const val FOLDER_BASE_MS: Int = 280
const val CALM_BASE_MS: Int = 400
const val PRESS_BASE_MS: Int = 80
const val PRESS_SCALE: Float = 0.94f
val CALM_DROP_DP: Dp = 12.dp
const val REDUCED_MOTION_FADE_MS: Int = 100

fun isReduceMotionEnabled(userReduceMotion: Boolean, animatorScale: Float): Boolean
fun homeFadeSpec(baseMs: Int, animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float>
fun homeSpringSpec(animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float>
fun appOpenSpec(animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float>
fun librarySpec(animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float>
fun folderSpec(animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float>
fun calmSpec(animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float>
fun pressSpec(animSpeed: Int, reduceMotion: Boolean): AnimationSpec<Float>
fun pressTargetScale(pressed: Boolean): Float
@Composable fun Modifier.homePressScale(pressed: Boolean, animSpeed: Int, reduceMotion: Boolean): Modifier

data class AppOpenReveal(scaleX: Float, scaleY: Float, translationX: Float, translationY: Float, scrimAlpha: Float)
fun appOpenReveal(progress: Float, iconBounds: Rect, containerBounds: Rect, scrimMaxAlpha: Float = 0.35f): AppOpenReveal
fun Modifier.homeAppOpenReveal(reveal: AppOpenReveal): Modifier

fun libraryBlurRadius(progress: Float, maxBlurDp: Dp): Dp
fun libraryScrimAlpha(progress: Float, maxAlpha: Float = 0.55f): Float
fun libraryDragProgress(offsetPx: Float, heightPx: Float, dismissFraction: Float = 0.35f): Float
fun libraryDragDismissed(offsetPx: Float, heightPx: Float, dismissFraction: Float = 0.35f): Boolean
class LibraryDragCloseState { var offsetPx: Float; var heightPx: Float } // both State
@Composable fun rememberLibraryDragCloseState(): LibraryDragCloseState
fun Modifier.homeLibraryDragVisuals(offsetPx: Float, heightPx: Float, dismissFraction: Float = 0.35f): Modifier
fun Modifier.homeLibraryDragVisuals(state: LibraryDragCloseState, dismissFraction: Float = 0.35f): Modifier
fun Modifier.homeLibraryDragToClose(state: LibraryDragCloseState, onClose: () -> Unit, dismissFraction: Float = 0.35f, canStartDrag: () -> Boolean = { true }): Modifier

fun folderTransformOrigin(iconCenter: Offset, containerSize: IntSize): TransformOrigin
fun Modifier.homeFolderPanel(progress: Float, origin: TransformOrigin = TransformOrigin.Center): Modifier
@Composable fun Modifier.homeCalmItems(calmOn: Boolean, animSpeed: Int, reduceMotion: Boolean): Modifier
```

Spring note: a spring has no exact duration — 320/360/280ms are the
Normal-stiffness settle targets of the critically-damped springs
(dampingRatio = 1f, no overshoot). Do not convert them to tweens to
hit the number exactly.

### HomeHaptics.kt

```kotlin
fun hapticsEnabled(pressFeedback: Boolean, hapticStrength: Int): Boolean
fun hapticTickType(): HapticFeedbackType // TextHandleMove
fun hapticConfirmType(): HapticFeedbackType // LongPress
fun HapticFeedback.homeScrubberTick(enabled: Boolean)
fun HapticFeedback.homeGridSnapTick(enabled: Boolean)
fun HapticFeedback.homeLongPressConfirm(enabled: Boolean)
class HomeHaptics(val enabled: Boolean, private val feedback: HapticFeedback) {
    fun scrubberTick()
    fun gridSnapTick()
    fun longPressConfirm()
}
@Composable fun rememberHomeHaptics(pressFeedback: Boolean, hapticStrength: Int): HomeHaptics
```

Fire ticks on CHANGE only (new letter, new grid cell), never per
pointer move. Fire the long-press pulse once, at recognition time.
Inside `pointerInput` blocks, capture the remembered `HomeHaptics`
from the enclosing composable — never call `LocalHapticFeedback`
inside the gesture block.

### HomeA11y.kt

```kotlin
const val HOME_MIN_TOUCH_DP: Int = 48
val HOME_FOCUS_RING_DP = 2.dp
val HOME_FOCUS_RING_SHAPE: Shape = RoundedCornerShape(12.dp)
fun Modifier.homeMinTouchTarget(): Modifier

data class HomeIconActions(
    val onMove: ((dx: Int, dy: Int) -> Unit)? = null,
    val onMoveToSection: (() -> Unit)? = null,
    val onAddToDock: (() -> Unit)? = null,
    val onRemove: (() -> Unit)? = null,
    val onEditLabel: (() -> Unit)? = null,
    val onHide: (() -> Unit)? = null,
)
fun visualDxToColumnDelta(dx: Int, isRtl: Boolean): Int
fun Modifier.homeIconActions(label: String?, actions: HomeIconActions): Modifier
fun homeSectionAnnouncement(sectionName: String, index: Int, total: Int): String

@Composable fun Modifier.homeIconKeys(
    onOpen: () -> Unit,
    onOpenMenu: () -> Unit,
    onCloseOverlay: (() -> Unit)? = null,
    onMoveInEditMode: ((dx: Int, dy: Int) -> Unit)? = null,
    editMode: Boolean = false,
): Modifier

fun homeFocusRingColor(homeColors: HomeColors): Color
@Composable fun Modifier.homeFocusRing(
    interactionSource: MutableInteractionSource,
    accent: Color = LocalPalette.current.accent,
    shape: Shape = HOME_FOCUS_RING_SHAPE,
): Modifier

fun Modifier.homeRightClickMenu(onMenu: () -> Unit): Modifier
@Composable fun HomeHoverTooltip(label: String, visible: Boolean, modifier: Modifier = Modifier)
```

`onMove` dx/dy are VISUAL steps (left = -1). Hosts MUST convert dx
with `visualDxToColumnDelta(dx, isRtl)` before touching columns.
Keyboard arrows need no conversion (`FocusDirection` is already
visual). "Move to section" replaces the doc's "Move to page" — there
are no pages; the host opens a section chooser.

## Hook snippets per host

### ScrollHome rows (favorites + A–Z icon cells)

```kotlin
// At the top of the ScrollHome composable (or the row composable):
val homeHaptics = rememberHomeHaptics(state.pressFeedback, state.hapticStrength)
val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
val cellInteraction = remember { MutableInteractionSource() }

// On each icon cell — order matters for the right-click hook
// (homeRightClickMenu BEFORE combinedClickable, as in Dock.kt):
Box(
    Modifier
        .homeMinTouchTarget()
        .homeIconActions(
            label = entry.label,
            actions = HomeIconActions(
                onMove = { dx, dy -> moveIcon(entry, visualDxToColumnDelta(dx, isRtl), dy) },
                onMoveToSection = { openSectionChooser(entry) },
                onAddToDock = { addToDock(entry) },
                onRemove = { removeIcon(entry) },
                onEditLabel = { openLabelEditor(entry) },
                onHide = { hideApp(entry) },
            ),
        )
        .homeIconKeys(
            onOpen = { onLaunch(entry) },
            onOpenMenu = { openMenu(entry) },
            onCloseOverlay = null, // no overlay at this site; Esc passes through
            onMoveInEditMode = { dx, dy -> moveIcon(entry, visualDxToColumnDelta(dx, isRtl), dy) },
            editMode = editMode,
        )
        .homeFocusRing(cellInteraction)
        .homeRightClickMenu { openMenu(entry) }
        .combinedClickable(
            interactionSource = cellInteraction,
            onClick = { onLaunch(entry) },
            onLongClick = { homeHaptics.longPressConfirm(); openMenu(entry) },
        )
        .hoverable(cellInteraction),
) {
    // …icon + label…
    HomeHoverTooltip(label = entry.label, visible = hovered)
}
// Edit-mode drag: call homeHaptics.gridSnapTick() ONLY when the
// dragged icon snaps to a different cell (not on every drag delta).
// Press scale (optional, only if the host tracks pressed state):
//   val pressed by cellInteraction.collectIsPressedAsState()
//   Modifier.homePressScale(pressed, state.animSpeed, state.reduceMotion)
```

`moveIcon`, `openSectionChooser`, `addToDock`, `removeIcon`,
`openLabelEditor`, `hideApp`, `openMenu` are host functions to write
or reuse — this worker supplies no implementations, only the
lambda slots. `hovered` comes from
`cellInteraction.collectIsHoveredAsState()`.

### Drawer rows (LibraryListRow / LibraryGridCell)

Same three modifiers as ScrollHome (`homeIconActions`,
`homeIconKeys`, `homeFocusRing`, `homeRightClickMenu`), with
`onCloseOverlay = onClose` OMITTED — Drawer.kt already handles Esc
at the overlay root (`onPreviewKeyEvent`, Drawer.kt lines ~532 and
~963), so attaching Esc again on rows would double-close. Row
actions that make no sense in the Library (e.g. `onRemove` for an
installed app) are passed as null and simply do not appear in the
TalkBack menu. `onAddToDock` maps to the existing `onAddToHome`
path (favorites/dock pinning). The scrubber already ticks per
letter change — replace its inline gate with
`rememberHomeHaptics(...)` + `homeHaptics.scrubberTick()` only if
the coordinator wants the single gate; do not change scrubber
behaviour otherwise.

### Folder panel (FolderSheet)

```kotlin
// Panel container, driven by the host's open progress (0..1):
val folderProgress by animateFloatAsState(
    targetValue = if (open) 1f else 0f,
    animationSpec = folderSpec(state.animSpeed, state.reduceMotion),
    label = "folderOpen",
)
Box(
    Modifier.homeFolderPanel(
        progress = folderProgress,
        origin = folderTransformOrigin(iconCenterInPanelCoords, panelSize),
    ),
) { /* …folder grid… */ }
// Folder member cells get the same homeIconActions/homeIconKeys/
// homeFocusRing/homeRightClickMenu treatment as ScrollHome cells,
// with onMove moving within the folder grid and onMoveToSection null.
```

The host owns measuring `iconCenterInPanelCoords` /
`panelSize` (via `onGloballyPositioned`, as FolderSheet already
does for cells). If the host cannot supply an origin, the default
`TransformOrigin.Center` is acceptable — do not block on it.

### Library overlay (LibraryOverlay / DrawerOverlay)

```kotlin
val dragState = rememberLibraryDragCloseState()
Box(
    Modifier.homeLibraryDragToClose(
        state = dragState,
        onClose = onClose,
        canStartDrag = { listScrollState.firstVisibleItemIndex == 0 &&
            listScrollState.firstVisibleItemScrollOffset == 0 },
    ),
) { /* …overlay content… */ }
// Open transition: drive the overlay's enter progress with
// librarySpec(state.animSpeed, state.reduceMotion); apply
// libraryBlurRadius(progress, hostResolvedMaxBlur) to the backdrop
// and libraryScrimAlpha(progress) to the scrim. The host's resolved
// max blur comes from its glass tier (Glass.kt) — this file invents none.
```

`canStartDrag` is REQUIRED where the overlay scrolls: without the
at-top gate, a list scroll would close the Library. The snap-back
on release-short-of-threshold is instant; if the coordinator wants
an animated return, drive an animated float and use the Float
overload of `homeLibraryDragVisuals` instead.

### Calm (CalmMode overlay + home content)

```kotlin
// Home content container (in Home.kt, around the scroll home):
Box(
    Modifier.homeCalmItems(calmOn, state.animSpeed, state.reduceMotion),
) { /* …home… */ }
// Calm overlay itself: fade its own alpha with
// calmSpec(state.animSpeed, state.reduceMotion) — do NOT also apply
// homeCalmItems to the overlay (that helper is for the home items
// fading OUT and dropping 12dp; the overlay fades IN).
// Calm favorite rows: large text list — softWrap = true, NO fixed
// height on the row (minHeight only), so 200% font scale cannot clip.
// Calm rows get homeIconActions (onMove null — order is favorites
// order, changed via the chooser — onOpenAllApps is the row action)
// and homeIconKeys with onCloseOverlay = onExitCalm.
```

App-open (for whichever host launches apps): the real window
animation is OS-owned. If the host draws a launch placeholder/scrim,
drive it with `appOpenReveal(progress, iconBoundsInWindow,
containerBounds)` + `homeAppOpenReveal(...)` and
`appOpenSpec(state.animSpeed, state.reduceMotion)`; otherwise skip
— do not fake an app-open animation the OS already performs.

## Host checklist the coordinator must verify (not code here)

1. 200% font scale: grid labels `maxLines = 1` + ellipsis (never
   taller rows); Calm/dialog text wraps with no fixed heights;
   icon cells keep size, only label text scales.
2. Touch targets: every icon cell, scrubber strip (already 40dp —
   note: doc §6 wants ≥ 40dp for the scrubber, §10 wants ≥ 48dp
   for touch targets generally; the scrubber strip stays 40dp as
   shipped, its letters are a slider, not buttons), dock slots,
   and menu rows all ≥ 48dp except the scrubber strip itself.
   Apply `homeMinTouchTarget()` where a cell could shrink below.
3. RTL: start/end padding everywhere; no left/right padding on
   touched layouts; `visualDxToColumnDelta` at every move site.
4. Focus ring contrast 3:1: accent-on-surface is palette work;
   rings drawn directly over wallpaper need a scrimmed cell
   background, not raw wallpaper.
5. Reduced motion: `wpParallax` forced off, ScrollHome jiggle
   off, no springs — verify, not just the new helpers.
6. No page-snap haptic, no page-scroll parallax, no "Move to
   page" string anywhere in the hooked UI (grep before shipping).
7. Mouse: right-click and touch long-press call the SAME menu
   lambda at every site; hover tooltip shows the app name only.

## Could NOT implement without editing hosts (coordinator applies)

- All hook attachments above (this worker was read-only on hosts).
- Host-owned functions behind the lambdas: `moveIcon`,
  `openSectionChooser`, section chooser UI, `addToDock`,
  `removeIcon`, `openLabelEditor`, `hideApp`, `openMenu`,
  folder icon-centre measurement, Library at-top scroll gate,
  Library enter-progress plumbing, Calm exit wiring.
- The section chooser itself (dialog/list UI for "Move to section").
- 200% font-scale layout verification and any resulting host
  layout fixes (fixed heights, non-wrapping text).
- Focus-ring contrast verification against live wallpapers.
- True haptic amplitude scaling (needs VIBRATE permission +
  Vibrator path + settings change — deliberately not added).
- Animated (non-instant) Library drag snap-back, if wanted.
- `speedTween`/`speedSpring` hosts (Home.kt dock fade, edit-mode
  shrink) left as-is: they snap under reduced motion rather than
  fading 100ms. Unifying that is a coordinator decision, not
  something this worker could do without editing hosts.
- No build was run: full release builds are the coordinator's,
  and these files are uncompiled until integration. Compile
  verification is therefore an explicit coordinator step, not a
  claim made here.
