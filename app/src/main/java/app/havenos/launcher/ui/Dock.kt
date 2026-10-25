package app.havenos.launcher.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.havenos.launcher.R
import app.havenos.launcher.data.AppEntry
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.LocalPalette

/**
 * M9 — the ONE dock component (Doc 2 §1, adapted to the standing
 * directions: Minimal only, no presets).
 *
 * One component, one content set, every surface: pinned apps · divider ·
 * up to 3 recents (local launch history only) · Search · Library. The
 * material comes from the user's surface setting via [GlassSurface]
 * (which reads LocalTokens / LocalGlass — no preset variants, no preset
 * parameter). Same look on every page, in edit mode, and with the
 * Settings panel open; hidden only in Calm (the host composes Calm
 * over the home), dimmed under the full-screen Library by that
 * overlay's own scrim (the dock stays composed underneath it).
 *
 * Placement (host-owned, from Settings `dockPos` via [dockSideFromKey]):
 * Bottom = horizontal floating pill centred, 16dp clear above the
 * gesture-navigation inset (the host's navigationBarsPadding + dock
 * bottom padding); Left / Right = vertical pill centred on that edge,
 * max 80% of the available height, same material and contents.
 *
 * Capacity is computed, not fixed (about 72dp per slot on a 64dp icon
 * in the doc's terms; here 48dp icons with a 24dp nominal gap
 * compressing to 8dp). Drop order when short: recents first. Pinned
 * apps beyond the visible limit are NOT dropped: they stay in the pill
 * and scroll inside it (horizontal strip for a bottom dock, vertical
 * strip for a side dock). Icons are never shrunk below 48dp.
 *
 * Call-site contract (host = HavenRoot):
 * - `pinned`: the user's dock list resolved to entries, in dock order
 *   (existing behaviour: `state.dock.take(state.dockMax).mapNotNull { appsByKey[it] }`).
 * - `recents`: LOCAL launch history only, already filtered — pinned keys
 *   excluded, capped at 3 (existing behaviour:
 *   `appRepo.recents(3, pinnedKeys)`). OneDock never reads history itself
 *   and caps defensively at 3 again via [computeDockLayout].
 * - `recentsOn`: the `dockRecentsOn` setting (Boolean, default true).
 *   Passing an empty recents list hides recents on its own; the flag is
 *   belt-and-braces so a stale list can never reappear when the user
 *   turns recents off.
 * - `onAppClick` launches the app; `onAppLongPress` opens the icon menu.
 *   Both pinned and recents route through the same two callbacks.
 * - `side` / `sizeClass`: placement + the Doc 2 §1 size table. The host
 *   maps `state.dockPos` with [dockSideFromKey] and passes
 *   [rememberHomeSizeClass].
 * - `animSpeed` / `reduceMotion`: press + hover motion. Under reduced
 *   motion there is no press scale and no magnify animation (instant
 *   only); the host passes the live settings values.
 * - `hapticsOn`: the `pressFeedback && hapticStrength > 0` gate (see
 *   HomeHaptics.kt). Long-press on a dock icon fires the confirm pulse.
 *
 * Icon loading note (M7 Wave 2): by default icons are loaded here via
 * PackageManager, which ignores icon packs and per-app overrides. A host
 * that has an AppRepository passes [iconProvider] (additive, optional,
 * default null = the old PackageManager behaviour exactly): typically
 * `{ appRepo.icon(it) }`, which resolves the global pack first, then
 * the per-app override, then the system icon. The provider is read
 * inside remember(entry.key, iconProvider), so the host must keep the
 * lambda instance stable (remember it) and force a remount (key()) when
 * the pack or per-app overrides change, or icons go stale after a pack
 * switch. Null provider = unchanged behaviour, no repository needed.
 *
 * Folder extension point (NOT built here): `pinned` is List<AppEntry>
 * only. Folders in the dock need a sealed item type (e.g.
 * `DockItem.App(entry)` / `DockItem.Folder(...)`) passed in place of
 * AppEntry, plus a dock persistence change — do NOT fake a folder as
 * an AppEntry with a synthetic package. Dock-internal drag reorder is
 * likewise not built here: the shipped reorder paths are the dock
 * menu's Move left / Move right rows (host-owned dialog), which are
 * TalkBack- and keyboard-safe. Drag-out-to-home IS built (v10,
 * [dragOutEnabled]): edit-mode long-press drags a dock icon onto the
 * home grid via [DOCK_DRAG_MIME]; the host drop side removes it from
 * the dock. The menu's "Move to home" row remains the normal-mode
 * (and TalkBack/keyboard) move path.
 *
 * No dim/disabled states: every icon, pinned or recent, draws at full
 * alpha. Never dim an icon unless it is disabled by the system. No
 * network calls, no analytics.
 */

/** Which edge the dock sits on (Settings `dockPos`: 0 bottom, 1 left, 2 right). */
enum class DockSide { Bottom, Left, Right }

/** Map the stored `dockPos` int to a [DockSide]; unknown values are Bottom. */
fun dockSideFromKey(dockPos: Int): DockSide = when (dockPos) {
    1 -> DockSide.Left
    2 -> DockSide.Right
    else -> DockSide.Bottom
}

/** Dock icon size. Never reduced to fit — slots scroll instead. */
const val DOCK_ICON_DP = 48f

/** Nominal slot pitch: 48dp icon + 24dp gap (doc §1 "~72dp per slot"). */
const val DOCK_SLOT_PITCH_DP = 72f

/** Minimum gap between dock items; gaps compress to this before scrolling. */
const val DOCK_MIN_GAP_DP = 8f

/** Padding inside the pill, each end along its long axis. */
const val DOCK_PILL_PADDING_DP = 16f

/** Maximum recents shown, per doc §1. */
const val DOCK_MAX_RECENTS = 3

/**
 * Dock thickness (height for a bottom dock, width for a side dock) per
 * size class, from the Doc 2 §1 table: Expanded 76/80dp, Medium 72/76dp,
 * Compact 64/64dp (bottom / side).
 */
fun dockThicknessFor(sizeClass: HomeSizeClass, side: DockSide): Dp = when (side) {
    DockSide.Bottom -> when (sizeClass) {
        HomeSizeClass.Expanded -> 76.dp
        HomeSizeClass.Medium -> 72.dp
        HomeSizeClass.Compact -> 64.dp
    }
    DockSide.Left, DockSide.Right -> when (sizeClass) {
        HomeSizeClass.Expanded -> 80.dp
        HomeSizeClass.Medium -> 76.dp
        HomeSizeClass.Compact -> 64.dp
    }
}

/**
 * Max width of a bottom-dock pill per size class, from the Doc 2 §1
 * table: Expanded ~720dp (≈10 visible app slots), Medium ~584dp
 * (≈8 visible app slots), Compact uncapped (full width, 4–5 visible
 * pinned on a phone). Null = no cap. The cap is what makes the table's
 * slot counts fall out of [dockCapacity]: 720dp holds 12 slots total
 * (10 apps + Search + Library); 584dp holds 10 total (8 apps + 2).
 */
fun dockMaxWidthFor(sizeClass: HomeSizeClass): Dp? = when (sizeClass) {
    HomeSizeClass.Expanded -> 720.dp
    HomeSizeClass.Medium -> 584.dp
    HomeSizeClass.Compact -> null
}

/** How many 48dp icons fit at [widthDp] with gaps compressed to the minimum. */
fun dockCapacity(widthDp: Float): Int {
    if (widthDp <= 0f) return 0
    // 2 * padding + n * icon + (n - 1) * minGap <= width
    val n = ((widthDp - 2f * DOCK_PILL_PADDING_DP + DOCK_MIN_GAP_DP) /
        (DOCK_ICON_DP + DOCK_MIN_GAP_DP)).toInt()
    return n.coerceAtLeast(0)
}

/** Result of [computeDockLayout]: how many of each section to show. */
data class DockLayout(
    /**
     * Pinned apps rendered in the pill's scroll strip: ALL pinned apps,
     * never a dropped subset. When [pinnedScrolls] is false they all fit
     * visibly; when true the strip scrolls and only [visibleAppSlots]
     * of them are visible at once.
     */
    val pinnedShown: Int,
    val recentsShown: Int,
    /** True when pinned apps exceed the visible app slots: the strip scrolls. */
    val pinnedScrolls: Boolean,
    /** App slots visible without scrolling (capacity minus Search + Library). */
    val visibleAppSlots: Int,
    /** Search + Library buttons are always shown, even when capacity is 0. */
    val showSearch: Boolean = true,
    val showLibrary: Boolean = true,
)

/**
 * Slot calculation, pure and unit-tested (no Android types).
 *
 * Drop order: recents first — recents are dropped entirely as soon as
 * pinned apps alone fill the visible slots. Pinned apps are NEVER
 * dropped: beyond the visible limit they scroll inside the pill, and
 * icons are never shrunk ([DOCK_ICON_DP] is a constant, and capacity
 * comes from [dockCapacity], which compresses gaps (24dp nominal →
 * 8dp minimum) before anything scrolls).
 *
 * Worked examples (pinned = 6, recents = 3, recents on):
 * - 390dp (Compact phone): capacity 6, 4 app slots → pinned scroll
 *   (6 rendered, 4 visible), no recents.
 * - 430dp: capacity 7, 5 app slots → pinned scroll, no recents.
 * - 500dp: capacity 8, 6 app slots → all 6 pinned visible, no recents
 *   (recents dropped first).
 * - 560dp: capacity 9, 7 app slots → 6 pinned + 1 recent.
 * - 1000dp (Expanded tablet): capacity 17 → all 6 pinned + 3 recents.
 */
fun computeDockLayout(
    availableWidthDp: Float,
    pinnedCount: Int,
    recentsCount: Int,
    recentsOn: Boolean,
): DockLayout {
    val capacity = dockCapacity(availableWidthDp)
    // Two slots are always reserved for Search + Library.
    val appSlots = (capacity - 2).coerceAtLeast(0)
    val pinned = pinnedCount.coerceAtLeast(0)
    val scrolls = pinned > appSlots
    val recentsShown = if (!recentsOn || scrolls) {
        0
    } else {
        recentsCount.coerceAtLeast(0)
            .coerceAtMost(DOCK_MAX_RECENTS)
            .coerceAtMost((appSlots - pinned).coerceAtLeast(0))
    }
    return DockLayout(
        pinnedShown = pinned,
        recentsShown = recentsShown,
        pinnedScrolls = scrolls,
        visibleAppSlots = appSlots,
    )
}

/**
 * Derived icon backdrop, pure ARGB arithmetic (unit-tested, no Android types).
 *
 * No existing icon normalization was found in the project (the only
 * "backdrop" code is the glass surface pipeline), so this is the small
 * backdrop the task calls for: a non-adaptive icon is drawn inset on a
 * tile mixed 80% from the dock surface toward the on-surface colour.
 * In light themes that turns a stray white square into a visibly bounded
 * light-grey tile (white icon background on a grey tile + hairline
 * border); in dark themes the same mix gives a mid-grey tile the white
 * square reads against. Adaptive icons are drawn full-bleed instead —
 * they already fill their shape and need no tile.
 *
 * Returns an ARGB Long in the same 0xAARRGGBB layout as Compose Color.
 */
fun derivedIconBackdropArgb(surfaceArgb: Long, onSurfaceArgb: Long): Long {
    fun mix(s: Int, o: Int): Int = ((s * 4 + o) / 5).coerceIn(0, 255)
    val a = (surfaceArgb shr 24) and 0xFF
    val r = mix(((surfaceArgb shr 16) and 0xFF).toInt(), ((onSurfaceArgb shr 16) and 0xFF).toInt())
    val g = mix(((surfaceArgb shr 8) and 0xFF).toInt(), ((onSurfaceArgb shr 8) and 0xFF).toInt())
    val b = mix((surfaceArgb and 0xFF).toInt(), (onSurfaceArgb and 0xFF).toInt())
    return (a shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}

/**
 * v10 dock correction — adaptive-icon contrast (pure, unit-tested).
 *
 * Adaptive icons used to draw full-bleed with no backdrop, so a
 * white-foreground adaptive icon vanished on a light dock (and a
 * black-foreground one on a dark dock). When the icon's centre
 * luminance is within [ADAPTIVE_BACKDROP_LUMINANCE_GAP] of the dock
 * surface luminance, the icon gets the same derived tonal backdrop
 * tile as a non-adaptive icon; otherwise it keeps drawing full-bleed
 * (its own background already separates it from the dock).
 */
const val ADAPTIVE_BACKDROP_LUMINANCE_GAP = 0.22f

fun adaptiveIconNeedsBackdrop(iconLuminance: Float, surfaceLuminance: Float): Boolean =
    kotlin.math.abs(iconLuminance - surfaceLuminance) < ADAPTIVE_BACKDROP_LUMINANCE_GAP

/**
 * One sRGB channel → linear light, the standard formula Compose's
 * `luminance()` applies before weighting. Kept as a pure top-level
 * helper so it is unit-testable. Without this step the raw sRGB
 * centre value was compared against a linear dock luminance: a
 * dark-grey icon centre (sRGB 0.30) on a near-black dock (linear
 * 0.004) read as a 0.30 gap — above the backdrop threshold — while
 * its true linear gap is only ~0.07, so the icon lost its backdrop
 * and was nearly invisible.
 */
fun srgbChannelToLinear(value: Float): Float =
    if (value <= 0.04045f) value / 12.92f
    else Math.pow(((value + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()

/**
 * Mean linear-light luminance of the opaque pixels in the centre
 * half of a rendered icon bitmap (edges excluded: adaptive
 * backgrounds live there, the foreground lives in the centre).
 * Channels are linearised with [srgbChannelToLinear] first so the
 * result is comparable with Compose's `luminance()` of the dock
 * surface. Transparent pixels are skipped — a sparse white logo on
 * a transparent background must read as white, not as the black
 * that transparent pixels decode to. Null when there are no opaque
 * centre pixels or sampling fails, in which case the caller keeps
 * the old full-bleed behaviour.
 */
private fun centreIconLuminance(bitmap: ImageBitmap): Float? {
    return try {
        val androidBitmap = bitmap.asAndroidBitmap()
        val w = androidBitmap.width
        val h = androidBitmap.height
        if (w <= 0 || h <= 0) return null
        var sum = 0.0
        var count = 0
        var y = h / 4
        while (y < (3 * h) / 4) {
            var x = w / 4
            while (x < (3 * w) / 4) {
                val pixel = androidBitmap.getPixel(x, y)
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha > 128) {
                    val r = ((pixel ushr 16) and 0xFF) / 255f
                    val g = ((pixel ushr 8) and 0xFF) / 255f
                    val b = (pixel and 0xFF) / 255f
                    sum += 0.2126 * srgbChannelToLinear(r) +
                        0.7152 * srgbChannelToLinear(g) +
                        0.0722 * srgbChannelToLinear(b)
                    count++
                }
                x += 4
            }
            y += 4
        }
        if (count == 0) null else (sum / count).toFloat()
    } catch (t: Throwable) {
        null
    }
}

/**
 * v10 dock correction — dock drag-out MIME. Distinct from
 * [LIBRARY_DRAG_MIME] so the home drop target can tell a dock move
 * (remove from dock on a successful drop) from a Library add (dock
 * untouched). The ClipData item text is the same tab-separated
 * [encodeLibraryDragPayload] format, decoded by
 * [decodeLibraryDragPayload].
 */
const val DOCK_DRAG_MIME = "application/vnd.havenos.dock-app"

/**
 * Drag-out source for dock icons, mirroring [libraryDragSource] in
 * Drawer.kt: long-press starts a platform drag carrying
 * [DOCK_DRAG_MIME]. Only applied in edit mode (see [OneDock]'s
 * [dragOutEnabled]): in normal mode the same long-press opens the
 * icon menu, and the two gestures cannot share one long-press — the
 * menu is the only touch route to Remove / Move left / Move right /
 * Edit label, so normal-mode long-press keeps the menu and the
 * functional move stays available there via the menu's "Move to
 * home" row. Grid dragging is likewise edit-mode-only (PagedHome's
 * item gesture is keyed on editMode).
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.dockDragSource(
    payload: LibraryDragPayload,
): Modifier = this.then(
    Modifier.dragAndDropSource(
        drawDragDecoration = {},
        block = {
            detectTapGestures(
                onLongPress = {
                    startTransfer(
                        DragAndDropTransferData(
                            clipData = ClipData(
                                ClipDescription(payload.label, arrayOf(DOCK_DRAG_MIME)),
                                ClipData.Item(encodeLibraryDragPayload(payload)),
                            ),
                        ),
                    )
                },
            )
        },
    ),
)

/**
 * The single dock. Horizontal floating glass pill for [DockSide.Bottom],
 * vertical pill for [DockSide.Left] / [DockSide.Right] — same component,
 * same contents, same material; only the axis changes.
 *
 * Must be composed under the existing theme providers (LocalPalette /
 * LocalTokens / LocalGlass), as all other home UI is.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OneDock(
    pinned: List<AppEntry>,
    recents: List<AppEntry>,
    recentsOn: Boolean,
    onAppClick: (AppEntry) -> Unit,
    onAppLongPress: (AppEntry) -> Unit,
    onSearchClick: () -> Unit,
    onLibraryClick: () -> Unit,
    // M7 integration, additive + optional: long-press on the Library
    // button enters Calm mode. Null = tap-only, as before.
    onLibraryLongPress: (() -> Unit)? = null,
    // M7 Wave 2, additive + optional: repository icon resolution
    // (icon packs + per-app overrides). Null = PackageManager as before.
    iconProvider: ((AppEntry) -> Drawable?)? = null,
    // M9, additive + optional: placement + motion + haptics. Defaults
    // preserve the old horizontal Expanded behaviour for any caller
    // that does not pass them.
    side: DockSide = DockSide.Bottom,
    sizeClass: HomeSizeClass = HomeSizeClass.Expanded,
    animSpeed: Int = 2,
    reduceMotion: Boolean = false,
    hapticsOn: Boolean = true,
    // v10: edit-mode dock drag-out to the home grid (see dockDragSource).
    // False = today's behaviour exactly (long-press opens the menu).
    dragOutEnabled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val thickness = dockThicknessFor(sizeClass, side)
    // Along-axis padding inside the pill: the cross-axis padding is
    // (thickness - icon) / 2 so the pill is exactly `thickness` thick.
    val crossPad = (thickness.value - DOCK_ICON_DP) / 2f
    if (side == DockSide.Bottom) {
        BoxWithConstraints(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val maxWidthDp = maxWidth.value
            val cap = dockMaxWidthFor(sizeClass)?.value
            val effectiveWidth = if (cap != null) minOf(maxWidthDp, cap) else maxWidthDp
            val layout = remember(effectiveWidth, pinned.size, recents.size, recentsOn) {
                computeDockLayout(effectiveWidth, pinned.size, recents.size, recentsOn)
            }
            val shownRecents = recents.take(layout.recentsShown)
            // Visible items drive the gap: pinned visible count +
            // recents + 2 buttons. Gaps compress 24dp → 8dp; the pill
            // hugs its content (never wider than the cap above).
            val visibleItems =
                minOf(pinned.size, layout.visibleAppSlots) + shownRecents.size + 2
            val dividerCount = (if (pinned.isNotEmpty() && shownRecents.isNotEmpty()) 1 else 0) +
                (if (pinned.isNotEmpty() || shownRecents.isNotEmpty()) 1 else 0)
            val gapDp = if (visibleItems <= 1) {
                12.dp
            } else {
                ((effectiveWidth - 2f * DOCK_PILL_PADDING_DP - DOCK_ICON_DP * visibleItems - dividerCount) /
                    (visibleItems - 1)).coerceIn(DOCK_MIN_GAP_DP, DOCK_SLOT_PITCH_DP - DOCK_ICON_DP).dp
            }
            val divider: @Composable () -> Unit = {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(palette.glassBorder),
                )
            }
            // Scroll case: the pill is fixed to the cap width so the
            // weighted strip has leftover width to take. Wrap case: the
            // pill hugs its content and the outer box centres it.
            val pillWidthMod = if (layout.pinnedScrolls) {
                Modifier.width(effectiveWidth.dp)
            } else {
                Modifier
            }
            GlassSurface(shape = RoundedCornerShape(50), modifier = pillWidthMod) {
                Row(
                    Modifier
                        .padding(
                            horizontal = DOCK_PILL_PADDING_DP.dp,
                            vertical = crossPad.dp,
                        )
                        .then(if (layout.pinnedScrolls) Modifier.fillMaxWidth() else Modifier),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(gapDp),
                ) {
                    if (layout.pinnedScrolls) {
                        // Overflow scrolls inside the pill: the strip
                        // takes the leftover width (pill width is the
                        // cap, so this never grows past it). Pinned
                        // overflow is what makes this branch scroll, so
                        // the strip is always full here.
                        Row(
                            Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(gapDp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            pinned.forEach { entry ->
                                DockAppIcon(entry, onAppClick, onAppLongPress, iconProvider, animSpeed, reduceMotion, hapticsOn, dragOutEnabled)
                            }
                        }
                    } else {
                        pinned.forEach { entry ->
                            DockAppIcon(entry, onAppClick, onAppLongPress, iconProvider, animSpeed, reduceMotion, hapticsOn, dragOutEnabled)
                        }
                    }
                    if (pinned.isNotEmpty() && shownRecents.isNotEmpty()) divider()
                    shownRecents.forEach { entry ->
                        DockAppIcon(entry, onAppClick, onAppLongPress, iconProvider, animSpeed, reduceMotion, hapticsOn, dragOutEnabled)
                    }
                    if (pinned.isNotEmpty() || shownRecents.isNotEmpty()) divider()
                    DockActionButton(
                        contentDescription = "Search",
                        iconRes = R.drawable.ic_search,
                        iconSizeDp = 22,
                        onClick = onSearchClick,
                        reduceMotion = reduceMotion,
                    )
                    DockActionButton(
                        contentDescription = "All apps",
                        iconRes = R.drawable.ic_apps,
                        iconSizeDp = 26,
                        onClick = onLibraryClick,
                        onLongClick = onLibraryLongPress,
                        reduceMotion = reduceMotion,
                    )
                }
            }
        }
    } else {
        // Vertical pill: same contents, same material, column axis.
        // Height budget is 80% of the available height (Doc 2 §1); the
        // pill hugs its content below that budget and scrolls at it.
        BoxWithConstraints(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            val heightBudget = maxHeight.value * 0.8f
            val layout = remember(heightBudget, pinned.size, recents.size, recentsOn) {
                computeDockLayout(heightBudget, pinned.size, recents.size, recentsOn)
            }
            val shownRecents = recents.take(layout.recentsShown)
            val visibleItems =
                minOf(pinned.size, layout.visibleAppSlots) + shownRecents.size + 2
            val dividerCount = (if (pinned.isNotEmpty() && shownRecents.isNotEmpty()) 1 else 0) +
                (if (pinned.isNotEmpty() || shownRecents.isNotEmpty()) 1 else 0)
            val gapDp = if (visibleItems <= 1) {
                12.dp
            } else {
                ((heightBudget - 2f * DOCK_PILL_PADDING_DP - DOCK_ICON_DP * visibleItems - dividerCount) /
                    (visibleItems - 1)).coerceIn(DOCK_MIN_GAP_DP, DOCK_SLOT_PITCH_DP - DOCK_ICON_DP).dp
            }
            val divider: @Composable () -> Unit = {
                Box(
                    Modifier
                        .height(1.dp)
                        .width(32.dp)
                        .background(palette.glassBorder),
                )
            }
            // Pill height: content height when everything fits, the 80%
            // budget when the pinned strip scrolls.
            val contentHeight = 2f * DOCK_PILL_PADDING_DP +
                DOCK_ICON_DP * visibleItems +
                gapDp.value * (visibleItems - 1).coerceAtLeast(0) +
                dividerCount
            val pillHeight = if (layout.pinnedScrolls) heightBudget else minOf(contentHeight, heightBudget)
            GlassSurface(
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(pillHeight.dp),
            ) {
                Column(
                    Modifier
                        .padding(
                            horizontal = crossPad.dp,
                            vertical = DOCK_PILL_PADDING_DP.dp,
                        )
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(gapDp),
                ) {
                    if (layout.pinnedScrolls) {
                        Column(
                            Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(gapDp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            pinned.forEach { entry ->
                                DockAppIcon(entry, onAppClick, onAppLongPress, iconProvider, animSpeed, reduceMotion, hapticsOn, dragOutEnabled)
                            }
                        }
                    } else {
                        pinned.forEach { entry ->
                            DockAppIcon(entry, onAppClick, onAppLongPress, iconProvider, animSpeed, reduceMotion, hapticsOn, dragOutEnabled)
                        }
                    }
                    if (pinned.isNotEmpty() && shownRecents.isNotEmpty()) divider()
                    shownRecents.forEach { entry ->
                        DockAppIcon(entry, onAppClick, onAppLongPress, iconProvider, animSpeed, reduceMotion, hapticsOn, dragOutEnabled)
                    }
                    if (pinned.isNotEmpty() || shownRecents.isNotEmpty()) divider()
                    DockActionButton(
                        contentDescription = "Search",
                        iconRes = R.drawable.ic_search,
                        iconSizeDp = 22,
                        onClick = onSearchClick,
                        reduceMotion = reduceMotion,
                    )
                    DockActionButton(
                        contentDescription = "All apps",
                        iconRes = R.drawable.ic_apps,
                        iconSizeDp = 26,
                        onClick = onLibraryClick,
                        onLongClick = onLibraryLongPress,
                        reduceMotion = reduceMotion,
                    )
                }
            }
        }
    }
}

/**
 * One dock app icon: tap launches, touch long-press and mouse right-click
 * both open the icon menu ([onAppLongPress]), hover shows a tooltip with
 * the app name and magnifies to 1.12, press scales to 0.94 (not under
 * reduced motion). Full alpha always — never dimmed. Long-press fires
 * the haptic confirm pulse when [hapticsOn].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockAppIcon(
    entry: AppEntry,
    onAppClick: (AppEntry) -> Unit,
    onAppLongPress: (AppEntry) -> Unit,
    iconProvider: ((AppEntry) -> Drawable?)? = null,
    animSpeed: Int = 2,
    reduceMotion: Boolean = false,
    hapticsOn: Boolean = true,
    dragOutEnabled: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val hoverScale by animateFloatAsState(
        targetValue = if (hovered) 1.12f else 1f,
        animationSpec = if (reduceMotion) snap() else spring(),
        label = "dockHoverMagnify",
    )
    // Press scale is a transform, not a fade: under reduced motion it
    // is omitted entirely (Doc 2 §7: 100ms fades only).
    val pressMod = if (reduceMotion) {
        Modifier
    } else {
        Modifier.homePressScale(pressed, animSpeed, false)
    }
    Box(
        Modifier
            .size(DOCK_ICON_DP.dp)
            .then(pressMod)
            .graphicsLayer { scaleX = hoverScale; scaleY = hoverScale }
            .hoverable(interaction)
            .pointerInput(entry.key) {
                // Mouse right-click opens the icon menu. Touch long-press
                // is handled by combinedClickable below; this block only
                // consumes secondary-button presses, so primary taps pass
                // through untouched.
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press &&
                            event.buttons.isSecondaryPressed
                        ) {
                            event.changes.forEach { it.consume() }
                            onAppLongPress(entry)
                        }
                    }
                }
            }
            // v10 drag-out (edit mode only): the drag source sits
            // outside combinedClickable, and combinedClickable's
            // long-press is off while dragging is enabled, so one
            // long-press cannot both drag and open the menu. Tap
            // still launches; mouse right-click above still opens
            // the menu.
            .then(
                if (dragOutEnabled) {
                    Modifier.dockDragSource(entry.toLibraryDragPayload())
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                interactionSource = interaction,
                onClick = { onAppClick(entry) },
                onLongClick = if (dragOutEnabled) {
                    null
                } else {
                    {
                        haptic.homeLongPressConfirm(hapticsOn)
                        onAppLongPress(entry)
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        DockIconImage(entry, iconProvider)
    }
    if (hovered) {
        val palette = LocalPalette.current
        Popup(
            alignment = Alignment.TopCenter,
            offset = IntOffset(0, -8),
            properties = PopupProperties(focusable = false),
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.glassBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(entry.label, color = palette.onSurface, maxLines = 1)
            }
        }
    }
}

/** Search / Library button: 48dp target, icon-only, hover magnify + tooltip. */
@Composable
private fun DockActionButton(
    contentDescription: String,
    iconRes: Int,
    iconSizeDp: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    reduceMotion: Boolean = false,
) {
    val palette = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (hovered) 1.12f else 1f,
        animationSpec = if (reduceMotion) snap() else spring(),
        label = "dockButtonHoverMagnify",
    )
    Box(
        Modifier
            .size(DOCK_ICON_DP.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(palette.surface.copy(alpha = 0.55f))
            .hoverable(interaction)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = palette.onSurface,
            modifier = Modifier.size(iconSizeDp.dp),
        )
    }
    if (hovered) {
        Popup(
            alignment = Alignment.TopCenter,
            offset = IntOffset(0, -8),
            properties = PopupProperties(focusable = false),
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.glassBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(contentDescription, color = palette.onSurface, maxLines = 1)
            }
        }
    }
}

/**
 * App icon with the derived-backdrop normalization (see
 * [derivedIconBackdropArgb]). Adaptive icons draw full-bleed clipped to
 * the Minimal icon shape, EXCEPT when their centre luminance is too
 * close to the dock surface (v10: [adaptiveIconNeedsBackdrop]) — those
 * draw inset (40dp) on the backdrop tile like non-adaptive icons, so a
 * white-foreground adaptive icon never vanishes on a light dock (nor a
 * black-foreground one on a dark dock). Non-adaptive icons always draw
 * inset (40dp) on the backdrop tile so a white-background icon never
 * appears as a stray white square on the light dock. Fallback is a tonal initial-letter tile
 * (surface + on-surface letter, never an accent block — accent is
 * reserved for interactive and selected states).
 */
@Composable
private fun DockIconImage(
    entry: AppEntry,
    iconProvider: ((AppEntry) -> Drawable?)? = null,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    // Provider first (icon packs + per-app overrides via the repository);
    // PackageManager fallback exactly as before when it is null or misses.
    // The host keeps the lambda stable and remounts on pack changes, so
    // keying on the provider instance here never reloads spuriously.
    val drawable: Drawable? = remember(entry.key, iconProvider) {
        iconProvider?.invoke(entry) ?: try {
            context.packageManager.getActivityIcon(
                ComponentName(entry.pkg, entry.cls),
            )
        } catch (t: Throwable) {
            try {
                context.packageManager.defaultActivityIcon
            } catch (t2: Throwable) {
                null
            }
        }
    }
    val bitmap = remember(drawable) {
        if (drawable == null) {
            null
        } else {
            try {
                val bmp = Bitmap.createBitmap(144, 144, Bitmap.Config.ARGB_8888)
                drawable.setBounds(0, 0, 144, 144)
                drawable.draw(Canvas(bmp))
                bmp.asImageBitmap()
            } catch (t: Throwable) {
                null
            }
        }
    }
    val iconShape = RoundedCornerShape(14.dp)
    // v10: adaptive icons whose centre luminance is too close to the
    // dock surface get the derived backdrop tile (same tile as the
    // non-adaptive path below); otherwise they keep full-bleed.
    val adaptiveNeedsBackdrop = bitmap != null && drawable is AdaptiveIconDrawable &&
        centreIconLuminance(bitmap)?.let { adaptiveIconNeedsBackdrop(it, palette.surface.luminance()) } == true
    if (bitmap != null && drawable is AdaptiveIconDrawable && !adaptiveNeedsBackdrop) {
        Box(
            Modifier.size(DOCK_ICON_DP.dp).clip(iconShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = entry.label,
                modifier = Modifier.size(DOCK_ICON_DP.dp),
            )
        }
    } else if (bitmap != null) {
        val backdrop = Color(derivedIconBackdropArgb(
            surfaceArgb = palette.surface.value.toLong(),
            onSurfaceArgb = palette.onSurface.value.toLong(),
        ))
        Box(
            Modifier
                .size(DOCK_ICON_DP.dp)
                .clip(iconShape)
                .background(backdrop)
                .border(1.dp, palette.glassBorder, iconShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = entry.label,
                modifier = Modifier.size(40.dp),
            )
        }
    } else {
        Box(
            Modifier.size(DOCK_ICON_DP.dp).clip(iconShape).background(palette.surface)
                .border(1.dp, palette.glassBorder, iconShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                entry.label.firstOrNull()?.toString() ?: "?",
                color = palette.onSurface,
            )
        }
    }
}
