package app.havenos.launcher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.havenos.launcher.theme.LocalPalette

/**
 * M7 Wave 2 (Worker 6) — Accessibility + keyboard/mouse helpers for home.
 *
 * NEW FILE ONLY. Nothing here is attached to any host; the coordinator
 * applies the helpers at the call sites listed in the hook spec
 * (docs/HOME-M7-WAVE2-MOTION-HAPTICS-A11Y-HOOKS.md). This file edits
 * no existing behaviour and adds no new settings keys.
 *
 * Source of truth: HOME-SCREEN-V2-BUILDER-PROMPT.md §10, adapted:
 * - M7 adapted the doc's "Move to page" TalkBack action to "Move to
 *   section" because the M7 home was a scroll flow. M8 restores the
 *   paged home, so that adaptation is STALE: the section-based helper
 *   below is retained unattached (no host calls it), and a page-based
 *   "Move to page" action is the follow-up when TalkBack actions are
 *   attached to PagedHome cells. Do not reintroduce section wording
 *   in new code.
 * - Nothing here requires dragging: every drag-only operation has a
 *   TalkBack action, a keyboard path, or both.
 * - No design-metaphor wording from the doc is used in strings, names, or UI.
 *
 * RTL: no hardcoded left/right anywhere in this file. Visual
 * directions are converted with [visualDxToColumnDelta] (TalkBack and
 * keyboard "left" mean VISUALLY left; in RTL that is the next column,
 * not the previous one). Spacing in host layouts must use
 * start/end padding, never left/right — see the host checklist in
 * the hook spec.
 *
 * Font scale: this file cannot force a host's text to wrap, so the
 * 200% rule is a documented host contract, not code: grid labels use
 * maxLines = 1 + TextOverflow.Ellipsis (they must never push the
 * grid row taller); Calm rows and dialog text use softWrap = true
 * with NO fixed height on the text container (use minHeight, never
 * height); the icon cell itself keeps its size and only the label
 * text scales. Hosts must verify at 200% — the coordinator's device
 * checklist covers it.
 *
 * Touch targets: [HOME_MIN_TOUCH_DP] is the floor (doc §10: >= 48dp).
 * [homeMinTouchTarget] enforces it as a size floor on the host's
 * cell; it does not shrink anything and does not change layout.
 *
 * Focus: the visible ring colour is the accent
 * ([HomeColors.focusRing] == palette.accent, see HomeTokens.kt) and
 * the ring must meet 3:1 against its background (doc §10). Accent-on-
 * surface is the host palette's job; this file only draws the ring.
 * NOTE: an accent that passes on the dock glass may fail on a busy
 * wallpaper — hosts drawing a focus ring directly over wallpaper
 * should put the ring on a scrimmed cell background, not raw
 * wallpaper. That check is the coordinator's, listed in the hook spec.
 */

// ------------------------------------------------------------ constants

/** Minimum touch target, doc §10. Also the dock slot floor. */
const val HOME_MIN_TOUCH_DP: Int = 48

/** Focus ring stroke width. */
val HOME_FOCUS_RING_DP = 2.dp

/** Focus ring corner radius (matches the Minimal card radius family). */
val HOME_FOCUS_RING_SHAPE: Shape = RoundedCornerShape(12.dp)

/** Minimum touch target as a Modifier floor (does not shrink). */
fun Modifier.homeMinTouchTarget(): Modifier =
    this.then(
        sizeIn(
            minWidth = HOME_MIN_TOUCH_DP.dp,
            minHeight = HOME_MIN_TOUCH_DP.dp,
        ),
    )

// ------------------------------------------------------------ TalkBack actions

/**
 * TalkBack actions for one home icon, all supplied by the host as
 * lambdas — this file performs no move, no database write, and no
 * navigation itself. Any null action is simply omitted from the
 * TalkBack menu (hosts omit actions that make no sense in context,
 * e.g. onAddToDock for an icon already on the dock, onHide for a
 * system app that cannot be hidden — the host decides, this file
 * just does not offer what it was not given).
 *
 * - [onMove]: (dx, dy) in VISUAL steps: dx = -1 is visually left,
 *   +1 visually right; dy = -1 up, +1 down. The host converts dx
 *   with [visualDxToColumnDelta] before touching columns.
 * - [onMoveToSection]: opens the host's section chooser (NOT a page
 *   chooser — there are no pages). The chooser itself is host UI.
 * - [onAddToDock] / [onRemove] / [onEditLabel] / [onHide]: exactly
 *   what they say; the host owns undo/toast behaviour (the doc's
 *   5s undo snackbar for remove/move lives with the host).
 */
data class HomeIconActions(
    val onMove: ((dx: Int, dy: Int) -> Unit)? = null,
    val onMoveToSection: (() -> Unit)? = null,
    // M9 edit mode (Doc 1 §3.11): page move + widget resize. Nulls are
    // simply omitted from the TalkBack menu, as with the other actions.
    val onMoveToPage: (() -> Unit)? = null,
    val onResize: (() -> Unit)? = null,
    val onAddToDock: (() -> Unit)? = null,
    val onRemove: (() -> Unit)? = null,
    val onEditLabel: (() -> Unit)? = null,
    val onHide: (() -> Unit)? = null,
)

/**
 * Converts a VISUAL horizontal step to a column delta for the host's
 * grid. In LTR they are identical; in RTL visually-left is the next
 * column (+1), not the previous one. Pure; hosts must use this
 * instead of passing TalkBack/keyboard dx straight into columns.
 */
fun visualDxToColumnDelta(dx: Int, isRtl: Boolean): Int = if (isRtl) -dx else dx

/**
 * Adds TalkBack custom actions to an icon (doc §10: Move left / right
 * / up / down, Move to section, Add to dock, Remove, Edit label,
 * Hide), plus label + Button role when [label] is given. Apply as the
 * OUTERMOST semantics modifier on the icon cell so it composes with
 * — not replaces — the host's own clickable semantics (custom
 * actions from multiple semantics modifiers merge; the host's click
 * action survives).
 *
 * The action lambdas return true (handled) so TalkBack announces
 * success; a host lambda that refuses (e.g. move blocked at the
 * edge) should still return normally here — edge feedback (a toast
 * or "at edge" announcement) is the host's job, via its own
 * onMove implementation.
 *
 * Example host wiring (PagedHome icon cell, Drawer row):
 * ```
 * Box(
 *     Modifier.homeIconActions(
 *         label = entry.label,
 *         actions = HomeIconActions(
 *             onMove = { dx, dy -> moveIcon(entry, dx, dy) },
 *             onMoveToSection = { openSectionChooser(entry) },
 *             onAddToDock = { addToDock(entry) },
 *             onRemove = { removeIcon(entry) },
 *             onEditLabel = { openLabelEditor(entry) },
 *             onHide = { hideApp(entry) },
 *         ),
 *     ),
 * ) { … }
 * ```
 * where moveIcon converts dx with visualDxToColumnDelta(dx, isRtl)
 * before applying it to columns.
 */
fun Modifier.homeIconActions(
    label: String?,
    actions: HomeIconActions,
): Modifier = this.semantics {
    if (label != null) contentDescription = label
    role = Role.Button
    val list = mutableListOf<CustomAccessibilityAction>()
    actions.onMove?.let { move ->
        list += CustomAccessibilityAction("Move left") { move(-1, 0); true }
        list += CustomAccessibilityAction("Move right") { move(1, 0); true }
        list += CustomAccessibilityAction("Move up") { move(0, -1); true }
        list += CustomAccessibilityAction("Move down") { move(0, 1); true }
    }
    actions.onMoveToSection?.let { fn ->
        list += CustomAccessibilityAction("Move to section") { fn(); true }
    }
    actions.onMoveToPage?.let { fn ->
        list += CustomAccessibilityAction("Move to page") { fn(); true }
    }
    actions.onResize?.let { fn ->
        list += CustomAccessibilityAction("Resize") { fn(); true }
    }
    actions.onAddToDock?.let { fn ->
        list += CustomAccessibilityAction("Add to dock") { fn(); true }
    }
    actions.onRemove?.let { fn ->
        list += CustomAccessibilityAction("Remove") { fn(); true }
    }
    actions.onEditLabel?.let { fn ->
        list += CustomAccessibilityAction("Edit label") { fn(); true }
    }
    actions.onHide?.let { fn ->
        list += CustomAccessibilityAction("Hide") { fn(); true }
    }
    if (list.isNotEmpty()) customActions = list
}

/**
 * Announcement text for a scroll-home section, for hosts to use as a
 * section header contentDescription ("Favorites, section 1 of 3").
 * The doc's pager announcement ("Page 2 of 4") does not apply — there
 * is no pager — so sections announce instead. Pure.
 */
fun homeSectionAnnouncement(sectionName: String, index: Int, total: Int): String =
    "$sectionName, section ${index + 1} of $total"

// ------------------------------------------------------------ keyboard / D-pad

/**
 * Keyboard / D-pad contract for one focusable icon (doc §10), as a
 * Modifier the host attaches to the icon cell. The host must make the
 * cell focusable (a clickable/combinedClickable already is) and must
 * ensure focus can actually reach the cell (focusable containers,
 * no focus-stealing overlay above it).
 *
 * - Arrows: move focus (via FocusManager) in that direction. In edit
 *   mode with Alt held, arrows instead call [onMoveInEditMode] with
 *   VISUAL steps (convert with [visualDxToColumnDelta]).
 * - Enter / D-pad centre: [onOpen].
 * - Menu key OR Shift+F10: [onOpenMenu] (the icon menu).
 * - Esc: [onCloseOverlay] if given (closes Library, Search, folder,
 *   Calm — the host passes whichever close applies at that site;
 *   Drawer.kt already handles Esc at the overlay root, so hosts
 *   must not double-handle it: attach this Esc branch ONLY where no
 *   overlay-root Esc handler already exists).
 * - All other keys pass through untouched. Only KeyDown is consumed;
 *   KeyUp is never consumed, so no key gets stuck.
 *
 * Example host wiring (icon cell):
 * ```
 * Box(
 *     Modifier.homeIconKeys(
 *         onOpen = { onLaunch(entry) },
 *         onOpenMenu = { openMenu(entry) },
 *         onCloseOverlay = onClose,          // omit where Drawer root handles Esc
 *         onMoveInEditMode = { dx, dy -> moveIcon(entry, dx, dy) },
 *         editMode = editMode,
 *     ),
 * ) { … }
 * ```
 */
@Composable
fun Modifier.homeIconKeys(
    onOpen: () -> Unit,
    onOpenMenu: () -> Unit,
    onCloseOverlay: (() -> Unit)? = null,
    onMoveInEditMode: ((dx: Int, dy: Int) -> Unit)? = null,
    editMode: Boolean = false,
): Modifier {
    val focusManager = LocalFocusManager.current
    // RTL note: FocusDirection.Left/Right are already VISUAL
    // directions, so arrow-key focus moves visually left/right in
    // both layouts with no conversion. Only onMoveInEditMode's dx
    // needs conversion, and that is the host's moveIcon job (see
    // homeIconActions docs) — nothing to convert here.
    return this.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionLeft -> {
                if (editMode && event.isAltPressed) {
                    onMoveInEditMode?.invoke(-1, 0) ?: return@onPreviewKeyEvent false
                } else {
                    focusManager.moveFocus(FocusDirection.Left)
                }
                true
            }
            Key.DirectionRight -> {
                if (editMode && event.isAltPressed) {
                    onMoveInEditMode?.invoke(1, 0) ?: return@onPreviewKeyEvent false
                } else {
                    focusManager.moveFocus(FocusDirection.Right)
                }
                true
            }
            Key.DirectionUp -> {
                if (editMode && event.isAltPressed) {
                    onMoveInEditMode?.invoke(0, -1) ?: return@onPreviewKeyEvent false
                } else {
                    focusManager.moveFocus(FocusDirection.Up)
                }
                true
            }
            Key.DirectionDown -> {
                if (editMode && event.isAltPressed) {
                    onMoveInEditMode?.invoke(0, 1) ?: return@onPreviewKeyEvent false
                } else {
                    focusManager.moveFocus(FocusDirection.Down)
                }
                true
            }
            Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                onOpen()
                true
            }
            Key.Menu -> {
                onOpenMenu()
                true
            }
            Key.F10 -> {
                if (event.isShiftPressed) {
                    onOpenMenu()
                    true
                } else {
                    false
                }
            }
            Key.Escape -> {
                if (onCloseOverlay != null) {
                    onCloseOverlay()
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }
}

// ------------------------------------------------------------ focus ring

/** Focus ring colour: the accent (HomeColors.focusRing, HomeTokens.kt). */
fun homeFocusRingColor(homeColors: HomeColors): Color = homeColors.focusRing

/**
 * Visible focus ring, drawn only while the host's interaction source
 * reports focus. The host creates ONE MutableInteractionSource, passes
 * it to its clickable AND here — a second source would never see
 * focus and the ring would never appear. Ring colour defaults to the
 * current palette accent (same value as HomeColors.focusRing).
 *
 * Example host wiring:
 * ```
 * val interaction = remember { MutableInteractionSource() }
 * Box(
 *     Modifier
 *         .homeFocusRing(interaction)
 *         .combinedClickable(
 *             interactionSource = interaction,
 *             indication = null, // host's choice; ring replaces ripple only if host wants
 *             onClick = { … }, onLongClick = { … },
 *         ),
 * ) { … }
 * ```
 * Do NOT pass indication = null just for the ring unless the host
 * design says so — the ring is an addition to, not a replacement
 * for, the press indication.
 */
@Composable
fun Modifier.homeFocusRing(
    interactionSource: MutableInteractionSource,
    accent: Color = LocalPalette.current.accent,
    shape: Shape = HOME_FOCUS_RING_SHAPE,
): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()
    return if (focused) {
        this.border(BorderStroke(HOME_FOCUS_RING_DP, accent), shape)
    } else {
        this
    }
}

// ------------------------------------------------------------ mouse

/**
 * Right-click menu hook for grid icons, mirroring the pattern Dock.kt
 * already ships for dock icons (pointerInput loop consuming only
 * secondary-button presses; primary taps pass through untouched to
 * the clickable below). The host's touch long-press menu and this
 * right-click menu must call the SAME onMenu lambda, or mouse and
 * touch users get different menus.
 *
 * Example host wiring (icon cell, alongside combinedClickable):
 * ```
 * Box(
 *     Modifier.homeRightClickMenu { openMenu(entry) }
 *         .combinedClickable(onClick = { … }, onLongClick = { openMenu(entry) }),
 * ) { … }
 * ```
 * Order matters: this modifier must come BEFORE the clickable in the
 * chain (as in Dock.kt), so the secondary press is consumed before
 * the clickable sees it.
 */
fun Modifier.homeRightClickMenu(onMenu: () -> Unit): Modifier =
    this.pointerInput(onMenu) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press &&
                    event.buttons.isSecondaryPressed
                ) {
                    event.changes.forEach { it.consume() }
                    onMenu()
                }
            }
        }
    }

/**
 * Hover tooltip content, mirroring Dock.kt's dock tooltip (non-
 * focusable Popup above the icon, surface background, glass border).
 * The host owns hover state via its own interaction source:
 * ```
 * val interaction = remember { MutableInteractionSource() }
 * val hovered by interaction.collectIsHoveredAsState()
 * Box(Modifier.hoverable(interaction)) {
 *     …icon…
 *     HomeHoverTooltip(label = entry.label, visible = hovered)
 * }
 * ```
 * Tooltips show the app name only — never a description, never an
 * action hint (the long-press hint is tutorial-only per doc §8.1).
 */
@Composable
fun HomeHoverTooltip(
    label: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val palette = LocalPalette.current
    Popup(
        alignment = androidx.compose.ui.Alignment.TopCenter,
        offset = androidx.compose.ui.unit.IntOffset(0, -8),
        properties = PopupProperties(focusable = false),
    ) {
        Box(
            modifier
                .background(palette.surface, RoundedCornerShape(8.dp))
                .border(
                    BorderStroke(1.dp, palette.glassBorder),
                    RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(label, color = palette.onSurface, maxLines = 1)
        }
    }
}
