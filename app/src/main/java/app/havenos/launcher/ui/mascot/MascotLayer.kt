package app.havenos.launcher.ui.mascot

import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.havenos.launcher.R
import app.havenos.launcher.data.AppEntry
import app.havenos.launcher.data.AssistFeatureId
import app.havenos.launcher.data.Folder
import app.havenos.launcher.data.HomeItem
import app.havenos.launcher.data.ItemKind
import app.havenos.launcher.data.Settings
import app.havenos.launcher.data.SettingsState
import app.havenos.launcher.data.parseMembers
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.ui.DockSide
import app.havenos.launcher.ui.glass.HavenIcon
import app.havenos.launcher.ui.glass.HavenIconImage
import app.havenos.launcher.ui.FeatureId
import app.havenos.launcher.ui.FeatureSessionState
import app.havenos.launcher.ui.LibraryApp
import app.havenos.launcher.ui.supervisedChildScope
import app.havenos.launcher.ui.assist.LocalModelBrain
import app.havenos.launcher.ui.hapticsEnabled
import app.havenos.launcher.ui.toLibraryApp
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * HavenOS v3 Track A — MascotLayer (prompt §4/§5/§7).
 *
 * ONE composable slot inside the launcher's home UI (hosted by HavenRoot).
 * Pointer-transparent everywhere except the mascot's own bounds and the
 * bubble: the root Box takes no input modifier, the mascot Box and the
 * bubble are the only clickable surfaces, and the spotlight ring draws
 * without any input modifier — icons stay tappable throughout guidance.
 *
 * Honest deviations from the prompt, stated plainly:
 * - Walk-to + spotlight use PagedHome's real cell rects (plumbed out via
 *   [cellRects]) for apps placed on home. Folder/drawer targets cannot
 *   be ringed (no rect exists for them), so the bubble names the
 *   location and offers Open/Launch chips instead — the prompt's own
 *   drawer fallback, applied to folders too.
 * - Guidance ends when the bubble closes, times out (~8s), or the
 *   mascot is tapped again. There is deliberately NO full-screen tap
 *   catcher: one would swallow home taps and break acceptance rule 1
 *   (zero blocked taps).
 * - Tutorial offers appear as one line inside the bubble menu on first
 *   open (the host has no first-edit/first-folder/first-widget signal
 *   to trigger on); any choice (Yes / Not now / Don't show again)
 *   consumes the one offer, per "offered once".
 * - Media/call sleep detection is skipped (it would need extra
 *   permissions); quiet hours, battery saver, overlays, and home
 *   visibility still sleep the mascot.
 */

private data class ChipAction(val label: String, val onClick: () -> Unit)

private sealed class BubbleMode {
    object Menu : BubbleMode()
    object FindInput : BubbleMode()
    data class Message(val text: String, val chips: List<ChipAction> = emptyList()) : BubbleMode()
    data class Choices(val candidates: List<FindCandidate>) : BubbleMode()
    object HelpList : BubbleMode()
    data class TutorialOffer(val tutorial: Tutorial) : BubbleMode()
    data class TutorialSteps(val tutorial: Tutorial, val step: Int) : BubbleMode()
    data class Confirm(val actionId: String, val title: String) : BubbleMode()
}

private data class GuideState(val appKey: String, val label: String, val itemId: String, val page: Int?)

/**
 * 3.1.1 (item 4): the sprite hitbox IS the drawn sprite rect. The body
 * Box carries its graphicsLayer translation AND its input modifiers on
 * the same node, so touch bounds move with the draw; this helper is
 * the single definition both the root hit test and the unit test use.
 * (x, y) is the animated top-left; sizePx is the sprite's pixel size.
 */
fun spriteHitRect(x: Float, y: Float, sizePx: Float): Rect =
    Rect(x, y, x + sizePx, y + sizePx)

/**
 * 3.1.1 (item 7, E3): pixel-stepped corner points for ONE corner,
 * from (radius, 0) to (0, radius) along the quarter arc, each
 * coordinate snapped down to the [stepPx] grid. Pure and
 * unit-tested: x is non-increasing, y is non-decreasing, and every
 * consecutive pair differs in at most one axis (a true staircase,
 * never a diagonal). An intermediate point is inserted whenever a
 * sample moves in both axes at once.
 */
fun steppedCornerPoints(radiusPx: Float, stepPx: Float): List<Offset> {
    if (radiusPx <= 0f) return emptyList()
    val step = stepPx.coerceAtLeast(1f)
    val n = maxOf(16, (radiusPx / step).toInt() * 8)
    val pts = mutableListOf<Offset>()
    var last: Offset? = null
    fun add(p: Offset) {
        val prev = last
        if (prev != null && p.x != prev.x && p.y != prev.y) {
            val mid = Offset(p.x, prev.y)
            if (mid != prev) pts += mid
        }
        if (p != prev) {
            pts += p
            last = p
        }
    }
    for (i in 0..n) {
        val a = (Math.PI.toFloat() / 2f) * i / n
        val x = (radiusPx * kotlin.math.cos(a) / step).toInt() * step
        val y = (radiusPx * kotlin.math.sin(a) / step).toInt() * step
        add(Offset(x, y))
    }
    return pts
}

/**
 * 3.1.1 (item 7, E3): rounded rectangle whose corners are staircases
 * of [stepPx] device pixels instead of smooth arcs — the pixel-art
 * bubble border. Used once, for the mascot bubble, so it lives here
 * rather than in a new file.
 */
class SteppedRoundedShape(
    private val corner: Dp,
    private val stepPx: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = with(density) { corner.toPx() }
            .coerceAtMost(minOf(size.width, size.height) / 2f)
        if (r <= 0f) return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        val pts = steppedCornerPoints(r, stepPx)
        val path = Path().apply {
            moveTo(r, 0f)
            // Top-left corner, then down the left edge.
            for (p in pts) lineTo(p.x, p.y)
            lineTo(0f, size.height - r)
            // Bottom-left corner (mirrored vertically, reversed).
            for (p in pts.asReversed()) lineTo(p.x, size.height - p.y)
            lineTo(size.width - r, size.height)
            // Bottom-right corner.
            for (p in pts) lineTo(size.width - p.x, size.height - p.y)
            lineTo(size.width, r)
            // Top-right corner (mirrored horizontally, reversed);
            // close() draws the top edge back to (r, 0).
            for (p in pts.asReversed()) lineTo(size.width - p.x, p.y)
            close()
        }
        return Outline.Generic(path)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MascotLayer(
    settings: Settings,
    state: SettingsState,
    apps: List<AppEntry>,
    stats: Map<String, Pair<Int, Long>>,
    homeItems: List<HomeItem>,
    foldersMap: Map<String, Folder>,
    dockKeys: List<String>,
    currentGridPage: Int?,
    cellRects: Map<String, Rect>,
    dockSide: DockSide,
    dockThicknessDp: androidx.compose.ui.unit.Dp,
    railRect: Rect?,
    hideRequested: Boolean,
    onHideComplete: () -> Unit,
    onSettingsPointingChange: (Boolean) -> Unit,
    onNavigateToPage: (Int) -> Unit,
    onOpenDrawer: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onOpenSettings: (String, String?) -> Unit,
    onEnterEditMode: () -> Unit,
    onOpenWidgetSheet: () -> Unit,
    onAddToDock: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val palette = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    val viewConfig = LocalViewConfiguration.current
    val scope = rememberCoroutineScope()
    // 3.1.1 (item 2): supervised child scopes — a mascot failure is
    // logged, disables the mascot for the session, and toasts once; a
    // Find failure only closes the bubble. Neither cancels siblings
    // nor reaches the Activity's uncaught path.
    val mascotGuardedScope = remember(scope) {
        supervisedChildScope(scope, context, FeatureId.MASCOT)
    }
    val findGuardedScope = remember(scope) {
        supervisedChildScope(scope, context, FeatureId.FIND)
    }
    val character = if (state.mascotCharacter == 1) MascotCharacter.MILA else MascotCharacter.MILO
    val mascotName = stringResource(
        if (character == MascotCharacter.MILA) R.string.mascot_name_mila else R.string.mascot_name_milo,
    )
    // 3.1 Phase E (E1): integer sprite scale from the size setting
    // (Small 56 / Medium 72 / Large 96 dp targets); on-screen px is
    // exactly 24 * scale, never fractional.
    val spriteScale = spriteScaleFor(targetDpForMascotSize(state.mascotSize), density.density)
    val sizePx = 24f * spriteScale
    val sizeDp = with(density) { sizePx.toDp() }
    val spriteSet = rememberSpriteSet(character, palette.accent.toArgb())

    // Reduced motion: system animator scale 0, app setting, or mascot setting.
    val animatorOff = try {
        AndroidSettings.Global.getFloat(context.contentResolver, AndroidSettings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    } catch (t: Throwable) {
        false
    }
    val reducedMotion = state.reduceMotion || state.mascotReduceMotion || animatorOff
    val screenReaderOn = try {
        (context.getSystemService(AccessibilityManager::class.java))?.isTouchExplorationEnabled == true
    } catch (t: Throwable) {
        false
    }
    val batterySaverOn = try {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    } catch (t: Throwable) {
        false
    }
    var nowMin by remember { mutableStateOf(minuteOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            nowMin = minuteOfDay()
        }
    }
    val quietNow = isQuietHour(nowMin, state.mascotQuietStart, state.mascotQuietEnd)
    val sleeping = shouldSleep(
        userBusy = false, // host gates the layer off whenever an overlay is open
        quietNow = quietNow,
        homeVisible = true,
        batterySaverOn = batterySaverOn,
    )
    val roamingOn = state.mascotRoaming && !screenReaderOn && !sleeping

    val appsByKey = remember(apps) { apps.associateBy { it.key } }
    val libraryApps = remember(apps) { apps.map { it.toLibraryApp() } }
    val placementOf: (String) -> HomePlacement = { key ->
        val homeItem = homeItems.firstOrNull { it.kind == ItemKind.APP && it.payload == key }
        // 3.1.1: firstNotNullOfOrNull — a key in no folder used to
        // throw NoSuchElementException here (vc13 crash class); a miss
        // now falls through to the dock/empty branches below.
        val folderHit = foldersMap.values.firstNotNullOfOrNull { folder ->
            if (key in parseMembers(folder.membersJson)) folder.id to folder.name else null
        }
        when {
            homeItem != null -> HomePlacement(itemId = homeItem.id, page = homeItem.page)
            folderHit != null -> HomePlacement(folderId = folderHit.first, folderName = folderHit.second)
            key in dockKeys -> HomePlacement(inDock = true)
            else -> HomePlacement()
        }
    }
    // 3.1 Phase F (F5): the pack-aware seam. USE_MODEL_BRAIN is false
    // and no pack exists, so this delegates to ScriptedBrain exactly.
    val brain = remember { LocalModelBrain() }
    val widgetEntries = remember(homeItems) {
        homeItems
            .filter { it.kind != ItemKind.APP && it.kind != ItemKind.FOLDER }
            .map { WidgetFindEntry(it.id, widgetFindLabel(it.kind), it.page) }
    }
    val brainCtx = BrainContext(
        libraryApps, stats, placementOf, state.mascotFindOn, widgetEntries,
        chatEnabled = state.assistChat,
    )

    var mascotState by remember { mutableStateOf(MascotState.IDLE) }
    var expression by remember { mutableStateOf(MascotExpression.IDLE) }
    var blink by remember { mutableStateOf(false) }
    var bubble by remember { mutableStateOf<BubbleMode?>(null) }
    var bubbleKey by remember { mutableStateOf(0) }
    var findQuery by remember { mutableStateOf("") }
    var finding by remember { mutableStateOf(false) }
    var guide by remember { mutableStateOf<GuideState?>(null) }
    var spotlightRect by remember { mutableStateOf<Rect?>(null) }
    var layerOrigin by remember { mutableStateOf(Offset.Zero) }
    var layerSize by remember { mutableStateOf(Offset.Zero) }
    var bubbleSize by remember { mutableStateOf(Offset.Zero) }
    var sessionNudges by remember { mutableStateOf(0) }
    var targetPos by remember { mutableStateOf<Offset?>(null) }
    // 3.1 Phase E (E2): pixel engine state. [overrideAnim] wins over
    // the expression-driven animation while moving/pointing/carried;
    // null means the expression mapping drives the sprite.
    var overrideAnim by remember { mutableStateOf<String?>(null) }
    var facingRight by remember { mutableStateOf(true) }
    var pixelState by remember { mutableStateOf(PixelMascotState.Idle) }
    var moveDurationMs by remember { mutableStateOf(1000L) }
    var currentLedge by remember { mutableStateOf<LedgePoint?>(null) }
    var carriedPrev by remember { mutableStateOf<LedgePoint?>(null) }
    var findFlow by remember { mutableStateOf(FindFlowState.Idle) }
    var settingsPointing by remember { mutableStateOf<String?>(null) }
    fun goPixelState(next: PixelMascotState) {
        if (pixelState.canTransitionTo(next)) pixelState = next
    }
    // Home stopped: pause timers + frame loop (loops below key on this
    // and restart on resume; they never fire while stopped).
    val lifecycleOwner = LocalLifecycleOwner.current
    var homeResumed by remember { mutableStateOf(true) }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            homeResumed = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Updated-state refs for gesture lambdas: pointerInput(Unit)
    // captures its first composition, so plain composition vals
    // (state, density, sleeping) would go stale inside it. State
    // objects themselves always read current; only plain vals need refs.
    val stateNow by rememberUpdatedState(state)
    val densityNow by rememberUpdatedState(density)
    fun closeBubble() {
        bubble = null
        guide = null
        spotlightRect = null
        // Closing the bubble during Settings pointing ends the
        // pointing too (otherwise the layer would stay composed and
        // the planner paused with no way back). endFind also clears
        // this; the double clear is a harmless no-op.
        if (settingsPointing != null) {
            settingsPointing = null
            onSettingsPointingChange(false)
            findFlow = nextFindFlowState(findFlow, FindFlowEvent.Cancel)
        }
        if (mascotState == MascotState.GUIDING || mascotState == MascotState.REACTING) {
            val quietNow = isQuietHour(
                nowMin, stateNow.mascotQuietStart, stateNow.mascotQuietEnd,
            )
            mascotState = if (quietNow || batterySaverOn) {
                MascotState.SLEEPING
            } else {
                MascotState.IDLE
            }
        }
    }

    fun openMenu() {
        bubbleKey++
        bubble = BubbleMode.Menu
    }

    fun tapToggle() {
        if (bubble != null) {
            closeBubble()
        } else {
            mascotState = MascotState.REACTING
            expression = MascotExpression.HAPPY
            goPixelState(PixelMascotState.Reacting)
            openMenu()
        }
    }

    /** End Find cleanly: flow cancelled, ring + pointing cleared, the
     * Settings panel (if open) is left alone for the user to close. */
    fun endFind() {
        findFlow = nextFindFlowState(findFlow, FindFlowEvent.Cancel)
        guide = null
        spotlightRect = null
        if (settingsPointing != null) {
            settingsPointing = null
            onSettingsPointingChange(false)
        }
        overrideAnim = null
        goPixelState(PixelMascotState.Idle)
        closeBubble()
    }

    // 3.1 Phase E (E2): ledge graph from the live layout. Cell rects
    // arrive in window coords; the graph is layer-local. Only the
    // visible page's rects count (pager neighbours are ignored).
    val pageItemIds = remember(homeItems, currentGridPage) {
        homeItems.filter { it.page == currentGridPage }.map { it.id }.toSet()
    }
    val pageRectsLocal: List<FloatRect> = remember(
        cellRects, layerOrigin, pageItemIds,
    ) {
        cellRects.entries
            .filter { (id, _) -> id in pageItemIds }
            .map { (_, r) ->
                FloatRect(
                    r.left - layerOrigin.x, r.top - layerOrigin.y,
                    r.right - layerOrigin.x, r.bottom - layerOrigin.y,
                )
            }
    }
    // Real grid pitch / cell width from the live rects (jump height is
    // two icon rows, jump gap is three cells — never a dp guess).
    val rowPitchPx: Float = remember(pageRectsLocal, density) {
        val tops = pageRectsLocal.map { it.top }.sorted()
        val diffs = tops.zipWithNext { a, b -> b - a }.filter { it > 1f }
        if (diffs.isEmpty()) 120f * density.density else diffs.sorted()[diffs.size / 2]
    }
    val cellWidthPx: Float = remember(pageRectsLocal, density) {
        pageRectsLocal.firstOrNull()?.let { it.right - it.left }
            ?: (120f * density.density)
    }
    val graph: List<Ledge> = remember(
        pageRectsLocal, layerSize, currentGridPage, homeItems, dockSide,
        dockThicknessDp, railRect, spriteScale, density,
    ) {
        if (layerSize.x <= 0 || layerSize.y <= 0) return@remember emptyList()
        val pageRects = pageRectsLocal
        val dockThicknessPx = with(density) { dockThicknessDp.toPx() }
        val dockRect = when (dockSide) {
            DockSide.Bottom -> FloatRect(
                0f, layerSize.y - dockThicknessPx, layerSize.x, layerSize.y,
            )
            DockSide.Left -> FloatRect(0f, 0f, dockThicknessPx, layerSize.y)
            DockSide.Right -> FloatRect(
                layerSize.x - dockThicknessPx, 0f, layerSize.x, layerSize.y,
            )
        }
        val dockTopY = if (dockSide == DockSide.Bottom) {
            layerSize.y - dockThicknessPx
        } else null
        val railLocal = railRect?.let {
            FloatRect(
                it.left - layerOrigin.x, it.top - layerOrigin.y,
                it.right - layerOrigin.x, it.bottom - layerOrigin.y,
            )
        }
        val segments = buildSegments(
            pageRects, layerSize.x, layerSize.y, sizePx, dockTopY, railLocal,
        )
        val obstacles = pageRects + dockRect + listOfNotNull(railLocal)
        buildLedges(segments, obstacles, sizePx, sizePx)
    }
    val dockTopPoint: LedgePoint? = remember(graph, layerSize) {
        graph.firstOrNull { it.kind == LedgeKind.DockTop }?.let {
            LedgePoint(it.id, (it.left + it.right) / 2f, it.y)
        }
    }
    fun pointToPos(point: LedgePoint): Offset =
        Offset(point.x - sizePx / 2f, point.y - sizePx)
    fun fallbackPoint(): Offset {
        val y = if (dockSide == DockSide.Bottom) {
            layerSize.y - with(density) { dockThicknessDp.toPx() } - sizePx
        } else {
            layerSize.y - sizePx
        }
        return Offset(
            (layerSize.x / 2f - sizePx / 2f).coerceAtLeast(0f),
            y.coerceAtLeast(0f),
        )
    }
    /** Execute one planned route: walk/jump/drop steps, transform-only
     * moves at [speedPxPerSec]. Shared by the planner loop and Find
     * guidance so the two never drift apart. [isBusy] aborts the route
     * at the next step boundary (the planner passes its busy check so
     * a Find guide starting mid-trip wins within one step). */
    suspend fun executeRoute(
        from: LedgePoint,
        route: List<RouteStep>,
        speedPxPerSec: Float,
        isBusy: () -> Boolean = { false },
    ) {
        var cur = from
        for (step in route) {
            if (isBusy()) return
            when (step) {
                is RouteStep.WalkTo -> {
                    val dist = abs(step.x - cur.x)
                    facingRight = step.x >= cur.x
                    overrideAnim = "walk"
                    goPixelState(PixelMascotState.Walking)
                    moveDurationMs = (dist / speedPxPerSec * 1000).toLong().coerceAtLeast(200)
                    val ledge = graph.firstOrNull { it.id == cur.ledgeId }
                    val y = ledge?.y ?: cur.y
                    targetPos = Offset(step.x - sizePx / 2f, y - sizePx)
                    delay(moveDurationMs)
                    cur = cur.copy(x = step.x)
                }
                is RouteStep.JumpTo -> {
                    val ledge = graph.firstOrNull { it.id == step.ledgeId } ?: continue
                    val dist = abs(step.x - cur.x) + abs(ledge.y - cur.y)
                    facingRight = step.x >= cur.x
                    overrideAnim = "jump"
                    goPixelState(PixelMascotState.Jumping)
                    moveDurationMs = (dist / speedPxPerSec * 1000).toLong().coerceAtLeast(300)
                    targetPos = Offset(step.x - sizePx / 2f, ledge.y - sizePx)
                    delay(moveDurationMs)
                    cur = LedgePoint(ledge.id, step.x, ledge.y)
                }
                is RouteStep.DropTo -> {
                    val ledge = graph.firstOrNull { it.id == step.ledgeId } ?: continue
                    val dist = abs(ledge.y - cur.y)
                    overrideAnim = "jump"
                    goPixelState(PixelMascotState.Falling)
                    moveDurationMs = (dist / speedPxPerSec * 1000).toLong().coerceAtLeast(300)
                    targetPos = Offset(step.x - sizePx / 2f, ledge.y - sizePx)
                    delay(moveDurationMs)
                    cur = LedgePoint(ledge.id, step.x, ledge.y)
                }
            }
        }
    }

    // Initial placement: persisted home spot if it lands on a ledge,
    // else the dock top, else the fallback point. Never on an icon:
    // the home spot is snapped through the same clearance graph.
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(layerSize, graph) {
        if (layerSize.x > 0 && !placed && graph.isNotEmpty()) {
            placed = true
            val homePoint = snapDropToLedge(
                graph,
                state.mascotHomeX * layerSize.x,
                state.mascotHomeY * layerSize.y,
                96f * density.density,
            )
            val start = homePoint ?: dockTopPoint
            if (start != null) {
                currentLedge = start
                targetPos = pointToPos(start)
            } else {
                targetPos = fallbackPoint()
            }
        }
    }
    // Sleeping / screen-reader: park at the dock top, no roaming.
    LaunchedEffect(sleeping, screenReaderOn, layerSize, dockTopPoint) {
        if (layerSize.x <= 0) return@LaunchedEffect
        if (sleeping || screenReaderOn) {
            dockTopPoint?.let {
                currentLedge = it
                targetPos = pointToPos(it)
            }
            expression = if (sleeping) MascotExpression.SLEEPY else MascotExpression.IDLE
            mascotState = if (sleeping) MascotState.SLEEPING else MascotState.IDLE
        }
    }
    // Ledge loss: the current ledge vanished (layout/page change) —
    // fall to the nearest valid ledge below, else poof + dock top.
    val currentLedgeNow = currentLedge
    LaunchedEffect(graph, currentLedgeNow) {
        val cur = currentLedgeNow ?: return@LaunchedEffect
        if (graph.none { it.id == cur.ledgeId }) {
            val below = dropTarget(graph, cur.x, cur.y)
            if (below != null) {
                val point = LedgePoint(below.id, cur.x.coerceIn(below.left, below.right), below.y)
                overrideAnim = "jump"
                goPixelState(PixelMascotState.Falling)
                targetPos = pointToPos(point)
                delay(600)
                currentLedge = point
                overrideAnim = null
                goPixelState(PixelMascotState.Idle)
            } else {
                overrideAnim = "poof"
                goPixelState(PixelMascotState.Despawning)
                delay(700)
                dockTopPoint?.let {
                    currentLedge = it
                    targetPos = pointToPos(it)
                } ?: run { targetPos = fallbackPoint() }
                overrideAnim = null
                goPixelState(PixelMascotState.Idle)
            }
        }
    }
    // Planner loop: pick a weighted ledge point, walk/jump there at
    // 40–60 dp/s, dwell 3–12s, sit after 60–120s idle, sleep after
    // 5 min. Cancelled on stop/hide/interaction by key change.
    // Reduce Motion, battery saver, and roaming-off all park the
    // mascot on the dock top in a static idle pose (taps still work).
    val parkOnDock = !roamingOn || reducedMotion || batterySaverOn
    LaunchedEffect(parkOnDock, graph, layerSize, homeResumed, hideRequested) {
        if (parkOnDock) {
            if (layerSize.x > 0) {
                dockTopPoint?.let {
                    currentLedge = it
                    targetPos = pointToPos(it)
                }
                overrideAnim = null
                goPixelState(PixelMascotState.Idle)
            }
            return@LaunchedEffect
        }
        if (!homeResumed || layerSize.x <= 0 || graph.isEmpty()) return@LaunchedEffect
        val random = Random.Default
        while (true) {
            if (bubble != null || guide != null || settingsPointing != null || hideRequested) {
                delay(1000)
                continue
            }
            val target = pickTargetLedgePoint(graph, random)
            if (target == null) {
                delay(3000)
                continue
            }
            val from = currentLedge
            if (from == null) {
                delay(1000)
                continue
            }
            val route = planRoute(from, target, graph, 2f * rowPitchPx, 3f * cellWidthPx)
            if (route == null) {
                delay(1000)
                continue
            }
            executeRoute(from, route, tripSpeedDpPerSec(random) * density.density) {
                bubble != null || guide != null || settingsPointing != null || hideRequested
            }
            if (bubble != null || guide != null || settingsPointing != null || hideRequested) continue
            currentLedge = target
            overrideAnim = null
            goPixelState(PixelMascotState.Idle)
            val dwell = dwellMs(random)
            delay(dwell)
            if (bubble != null || guide != null || settingsPointing != null || hideRequested) continue
            val sitDelay = sitDelayMs(random)
            delay((sitDelay - dwell).coerceAtLeast(0L))
            if (bubble != null || guide != null || settingsPointing != null || hideRequested) continue
            overrideAnim = "sit"
            goPixelState(PixelMascotState.Sitting)
            delay((300_000L - sitDelay).coerceAtLeast(0L))
            if (bubble != null || guide != null || settingsPointing != null || hideRequested) continue
            overrideAnim = "sleep"
            goPixelState(PixelMascotState.Sleeping)
            delay(30_000)
        }
    }
    // Hide with poof: the host keeps the layer composed with
    // hideRequested until the poof finishes, then removes it.
    LaunchedEffect(hideRequested) {
        if (hideRequested) {
            overrideAnim = "poof"
            goPixelState(PixelMascotState.Despawning)
            delay(700)
            onHideComplete()
        } else {
            overrideAnim = null
            goPixelState(PixelMascotState.Idle)
        }
    }

    // Find progress timeout: 20 s without a transition out of the
    // waiting states (Searching, Choosing) ends Find. The effect
    // restarts on every flow change, so the clock measures time
    // since last progress. Navigating/Pointing are not timed out:
    // movement and the ring are themselves progress, and both end
    // on user action (tap, Done, back, close).
    LaunchedEffect(findFlow) {
        if (findFlow == FindFlowState.Searching || findFlow == FindFlowState.Choosing) {
            delay(FIND_PROGRESS_TIMEOUT_MS)
            endFind()
        }
    }

    // Find switched off mid-flow: cancel the flow (bubble, ring,
    // pointing all clear; the panel, if open, stays for the user).
    LaunchedEffect(state.mascotFindOn) {
        if (!state.mascotFindOn) endFind()
    }
    // Blink loop: no frame loop at rest — one-shot delays only.
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000 + Random.nextLong(4000))
            if (expression == MascotExpression.IDLE) {
                blink = true
                delay(120)
                blink = false
            }
        }
    }
    // Bubble auto-hide (~8s of no interaction).
    LaunchedEffect(bubbleKey, bubble) {
        if (bubble != null) {
            delay(8000)
            closeBubble()
        }
    }
    // Emote visibility: one emote at a time, ~1.2s.
    var emoteVisible by remember { mutableStateOf(false) }
    LaunchedEffect(expression) {
        val emote = emoteFor(expression)
        if (emote != null) {
            emoteVisible = true
            delay(1200)
            emoteVisible = false
        } else {
            emoteVisible = false
        }
    }
    // One auto-nudge per session (prompt §7.3), after 10s of calm.
    LaunchedEffect(Unit) {
        delay(10_000)
        if (state.mascotTips != 0 && bubble == null && !sleeping && sessionNudges == 0) {
            val muted = parseMascotStringList(state.mascotMutedJson).toSet()
            val pin = suggestPinCandidate(libraryApps, stats, dockKeys, state.dockMax)
            // Normal (2) adds a help-tip fallback when no pin/widget
            // candidate exists; Rare (1) keeps today's behavior exactly.
            val tipEntry = if (pin == null && !suggestWidgetCandidate(homeItems.size) &&
                state.mascotTips == 2
            ) {
                HELP_ENTRIES.firstOrNull { it.id !in muted }
            } else null
            val suggestionId = when {
                pin != null -> "pin:${pin.key}"
                suggestWidgetCandidate(homeItems.size) -> "widget-room"
                tipEntry != null -> "tip:${tipEntry.id}"
                else -> null
            }
            if (suggestionId != null && !muted.contains(suggestionId) &&
                canNudge(0, state.mascotLastNudgeMs, System.currentTimeMillis(), false, quietNow)
            ) {
                sessionNudges = 1
                mascotGuardedScope.launch {
                    settings.setMascotLastNudgeMs(System.currentTimeMillis())
                    settings.recordAssistUse(AssistFeatureId.SUGGESTIONS, System.currentTimeMillis())
                }
                bubbleKey++
                bubble = BubbleMode.Message(
                    text = when {
                        pin != null -> "You open ${pin.label} a lot — pin it to the dock?"
                        tipEntry != null -> tipEntry.answer
                        else -> "Your home has room for a clock widget. Want one?"
                    },
                    chips = listOfNotNull(
                        when {
                            tipEntry != null -> tipEntry.settingRoute?.let { route ->
                                ChipAction("Open") {
                                    onOpenSettings(route, tipEntry.controlKey)
                                    closeBubble()
                                }
                            }
                            pin != null -> ChipAction("Do it") {
                                appsByKey[pin.key]?.let { onAddToDock(it) }
                                closeBubble()
                            }
                            else -> ChipAction("Do it") {
                                onOpenWidgetSheet()
                                closeBubble()
                            }
                        },
                        ChipAction("Don't suggest this") {
                            mascotGuardedScope.launch {
                                settings.setMascotMutedJson(
                                    encodeMascotStringList(muted + suggestionId),
                                )
                            }
                            closeBubble()
                        },
                    ),
                )
            }
        }
    }

    // Guidance: page navigation, then walk adjacent + spotlight ring.
    val currentGuide = guide
    LaunchedEffect(currentGuide, currentGridPage) {
        if (currentGuide != null && currentGuide.page != null && currentGridPage != null &&
            currentGuide.page != currentGridPage
        ) {
            onNavigateToPage(currentGuide.page)
        }
    }
    LaunchedEffect(currentGuide, cellRects, layerOrigin, layerSize, currentGridPage, graph) {
        val g = currentGuide ?: return@LaunchedEffect
        // Never ring a stale rect: only when actually on the target page.
        if (g.page != null && currentGridPage != null && g.page != currentGridPage) return@LaunchedEffect
        val rect = cellRects[g.itemId] ?: return@LaunchedEffect
        val local = Rect(
            rect.left - layerOrigin.x, rect.top - layerOrigin.y,
            rect.right - layerOrigin.x, rect.bottom - layerOrigin.y,
        )
        spotlightRect = local
        // Walk/jump to the nearest valid ledge by the icon, face it,
        // then Point with the ring. No route -> direct move to the
        // nearest ledge point; no ledge at all -> point in place
        // (the ring still shows; documented fallback).
        val near = nearestLedgePoint(graph, local.center.x, local.center.y)
        val from = currentLedge
        if (near != null && from != null) {
            val route = planRoute(from, near, graph, 2f * rowPitchPx, 3f * cellWidthPx)
            if (route != null) {
                executeRoute(from, route, 50f * density.density)
                currentLedge = near
            } else {
                val dist = hypot(near.x - from.x, near.y - from.y)
                facingRight = near.x >= from.x
                overrideAnim = "walk"
                goPixelState(PixelMascotState.Walking)
                moveDurationMs = (dist / (50f * density.density) * 1000).toLong()
                    .coerceAtLeast(300)
                targetPos = pointToPos(near)
                delay(moveDurationMs)
                currentLedge = near
            }
        } else if (near != null) {
            targetPos = pointToPos(near)
            currentLedge = near
        }
        val spriteCenterX = (targetPos?.x ?: 0f) + sizePx / 2f
        facingRight = local.center.x >= spriteCenterX
        overrideAnim = "point"
        goPixelState(PixelMascotState.Pointing)
        findFlow = nextFindFlowState(findFlow, FindFlowEvent.Arrived)
        expression = MascotExpression.FOUND
    }

    fun applyCandidate(candidate: FindCandidate) {
        findFlow = nextFindFlowState(findFlow, FindFlowEvent.CandidatePicked)
        when (val target = candidate.target) {
            is FindTarget.App -> {
                val placement = placementOf(target.key)
                val entry = appsByKey[target.key]
                val launchChip = entry?.let {
                    ChipAction("Launch") { onLaunch(it); closeBubble() }
                }
                when {
                    placement.itemId != null -> {
                        mascotState = MascotState.GUIDING
                        expression = MascotExpression.SEARCHING
                        guide = GuideState(target.key, target.label, placement.itemId, placement.page)
                        bubbleKey++
                        bubble = BubbleMode.Message(
                            "Found ${target.label} — walking over to point at it.",
                            listOfNotNull(launchChip),
                        )
                    }
                    placement.folderId != null -> {
                        expression = MascotExpression.FOUND
                        bubbleKey++
                        bubble = BubbleMode.Message(
                            "Found ${target.label} inside the folder “${placement.folderName ?: "Folder"}”.",
                            listOfNotNull(launchChip),
                        )
                    }
                    placement.inDock -> {
                        expression = MascotExpression.FOUND
                        bubbleKey++
                        bubble = BubbleMode.Message(
                            "Found ${target.label} in your dock.",
                            listOfNotNull(launchChip),
                        )
                    }
                    else -> {
                        expression = MascotExpression.FOUND
                        bubbleKey++
                        bubble = BubbleMode.Message(
                            "Found ${target.label} in the app drawer.",
                            listOfNotNull(
                                ChipAction("Open drawer") { onOpenDrawer(); closeBubble() },
                                launchChip,
                            ),
                        )
                    }
                }
            }
            is FindTarget.Widget -> {
                mascotState = MascotState.GUIDING
                expression = MascotExpression.SEARCHING
                guide = GuideState(
                    target.label, target.label, target.itemId,
                    homeItems.firstOrNull { it.id == target.itemId }?.page,
                )
                bubbleKey++
                bubble = BubbleMode.Message(
                    "Found the ${target.label} widget on your home — walking over to point at it.",
                    emptyList(),
                )
            }
            is FindTarget.Setting -> {
                // The ONE exception to the hide rule: the mascot stays
                // composed while pointing at a Settings row, and the
                // panel opens at the hub with the row deep-linked.
                // Ending Find closes nothing; the user closes Settings.
                expression = MascotExpression.FOUND
                settingsPointing = target.controlKey
                onSettingsPointingChange(true)
                onOpenSettings(target.route, target.controlKey)
                findFlow = nextFindFlowState(findFlow, FindFlowEvent.Arrived)
                overrideAnim = "point"
                goPixelState(PixelMascotState.Pointing)
                bubbleKey++
                bubble = BubbleMode.Message(
                    "There's “${target.title}” — highlighted in Settings.",
                    listOf(ChipAction("Done") { endFind() }),
                )
            }
            is FindTarget.Action -> {
                expression = MascotExpression.CURIOUS
                bubbleKey++
                bubble = BubbleMode.Confirm(target.id, target.title)
            }
        }
    }

    fun applyResponse(response: MascotResponse) {
        expression = response.expression
        when (val action = response.action) {
            is MascotAction.GuideHomeApp -> {
                mascotState = MascotState.GUIDING
                guide = GuideState(action.appKey, action.label, action.itemId, action.page)
                bubbleKey++
                bubble = BubbleMode.Message(
                    response.text,
                    listOfNotNull(appsByKey[action.appKey]?.let { entry ->
                        ChipAction("Launch") { onLaunch(entry); closeBubble() }
                    }),
                )
            }
            is MascotAction.GuideFolder -> {
                bubbleKey++
                bubble = BubbleMode.Message(
                    response.text,
                    listOfNotNull(appsByKey[action.appKey]?.let { entry ->
                        ChipAction("Launch") { onLaunch(entry); closeBubble() }
                    }),
                )
            }
            is MascotAction.GuideDockApp -> {
                bubbleKey++
                bubble = BubbleMode.Message(
                    response.text,
                    listOfNotNull(appsByKey[action.appKey]?.let { entry ->
                        ChipAction("Launch") { onLaunch(entry); closeBubble() }
                    }),
                )
            }
            is MascotAction.GuideDrawerApp -> {
                bubbleKey++
                bubble = BubbleMode.Message(
                    response.text,
                    listOfNotNull(
                        ChipAction("Open drawer") { onOpenDrawer(); closeBubble() },
                        appsByKey[action.appKey]?.let { entry ->
                            ChipAction("Launch") { onLaunch(entry); closeBubble() }
                        },
                    ),
                )
            }
            is MascotAction.GuideWidget -> {
                mascotState = MascotState.GUIDING
                expression = MascotExpression.SEARCHING
                guide = GuideState(
                    action.label, action.label, action.itemId,
                    homeItems.firstOrNull { it.id == action.itemId }?.page,
                )
                bubbleKey++
                bubble = BubbleMode.Message(response.text, emptyList())
            }
            is MascotAction.GuideSetting -> {
                // Typed-question help: the user chooses Open. The row
                // ring still works through the deep-link alone (no
                // layer-pointing state needed once the bubble closes).
                bubbleKey++
                bubble = BubbleMode.Message(
                    response.text,
                    listOf(ChipAction("Open") {
                        onOpenSettings(action.route, action.controlKey)
                        closeBubble()
                    }),
                )
            }
            is MascotAction.DoAction -> {
                bubbleKey++
                bubble = BubbleMode.Confirm(action.actionId, response.text)
            }
            is MascotAction.ShowChoices -> {
                bubbleKey++
                bubble = BubbleMode.Choices(action.candidates)
            }
            is MascotAction.StartTutorial -> {
                val tutorial = TUTORIALS.firstOrNull { it.id == action.tutorialId }
                bubbleKey++
                bubble = if (tutorial != null) BubbleMode.TutorialSteps(tutorial, 0)
                else BubbleMode.Message("I don't have that tour.", emptyList())
            }
            is MascotAction.ShowTutorialOffer -> {
                val unoffered = TUTORIALS.firstOrNull {
                    it.id !in parseMascotStringList(state.mascotDontShowJson)
                }
                bubbleKey++
                bubble = if (unoffered != null) BubbleMode.TutorialOffer(unoffered)
                else BubbleMode.Message("No tours left — you've seen them all.", emptyList())
            }
            is MascotAction.ShowHelpList -> {
                bubbleKey++
                bubble = BubbleMode.HelpList
            }
            else -> {
                bubbleKey++
                bubble = BubbleMode.Message(response.text, emptyList())
            }
        }
    }

    fun submitFind() {
        val query = findQuery.trim()
        if (query.isEmpty()) return
        finding = true
        findFlow = nextFindFlowState(findFlow, FindFlowEvent.QuerySubmitted)
        expression = MascotExpression.THINKING
        overrideAnim = "think"
        findGuardedScope.launch {
            val response = brain.handle(BrainInput.TextQuery(query), brainCtx)
            // 3.1 Phase F (F4): ledger, counts/times only. A Find
            // decision (or a Find attempt while Find is off) counts as
            // Find; any other TextQuery answer counts as chat.
            val feature = if (response.findDecision != null || !state.mascotFindOn) {
                AssistFeatureId.MASCOT_FIND
            } else {
                AssistFeatureId.MASCOT_CHAT
            }
            settings.recordAssistUse(feature, System.currentTimeMillis())
            finding = false
            val decision = response.findDecision
            if (decision != null) {
                findFlow = nextFindFlowState(findFlow, FindFlowEvent.Results(decision))
            } else {
                // Non-Find answer (e.g. Find switched off): terminal.
                findFlow = nextFindFlowState(findFlow, FindFlowEvent.Done)
            }
            if (response.action is MascotAction.None &&
                response.expression == MascotExpression.CONFUSED
            ) {
                // No match: say so plainly, offer drawer search or
                // another try. Never guess.
                expression = MascotExpression.SURPRISED
                bubbleKey++
                bubble = BubbleMode.Message(
                    "I can't find that.",
                    listOf(
                        ChipAction("Search in drawer") { onOpenDrawer(); closeBubble() },
                        ChipAction("Try again") {
                            findFlow = nextFindFlowState(findFlow, FindFlowEvent.Start)
                            bubbleKey++
                            bubble = BubbleMode.FindInput
                        },
                    ),
                )
            } else {
                applyResponse(response)
            }
        }
    }

    fun doConfirmedAction(actionId: String) {
        when (actionId) {
            "add-widget" -> onOpenWidgetSheet()
            "edit-mode" -> onEnterEditMode()
            "open-drawer" -> onOpenDrawer()
            "open-settings" -> onOpenSettings("home", null)
            "change-accent" -> onOpenSettings("look", "accent")
        }
        closeBubble()
    }

    fun markTutorialOffered(id: String) {
        val current = parseMascotStringList(state.mascotDontShowJson).toSet()
        if (id !in current) {
            mascotGuardedScope.launch { settings.setMascotDontShowJson(encodeMascotStringList(current + id)) }
        }
    }

    // Animated position: transform-only (graphicsLayer translation).
    // The animated values are read ONLY inside graphicsLayer lambdas,
    // so a move invalidates the draw phase, never recomposition.
    // Reduced motion or carried: instant teleport, no tween.
    val pos = targetPos ?: Offset(0f, 0f)
    val isCarried = pixelState == PixelMascotState.Carried
    val animSpecX = if (isCarried || reducedMotion) {
        tween<Float>(durationMillis = 0)
    } else {
        tween<Float>(durationMillis = moveDurationMs.toInt().coerceAtLeast(0))
    }
    val animX by animateFloatAsState(targetValue = pos.x, animationSpec = animSpecX, label = "mascotX")
    val animY by animateFloatAsState(targetValue = pos.y, animationSpec = animSpecX, label = "mascotY")

    // pointerInput(Unit) captures its first composition's values, so
    // the gesture lambdas must read live state through updated-state
    // refs — otherwise a drop would persist a stale position.
    val posNow by rememberUpdatedState(pos)
    val layerSizeNow by rememberUpdatedState(layerSize)
    val homeXNow by rememberUpdatedState(state.mascotHomeX)
    val homeYNow by rememberUpdatedState(state.mascotHomeY)
    val graphNow by rememberUpdatedState(graph)
    val carriedPrevNow by rememberUpdatedState(carriedPrev)
    val sizePxNow by rememberUpdatedState(sizePx)

    // Spotlight pulse: one animated float, read only by the ring
    // while a guide is active. Kept at top level because the
    // animation call cannot be conditional; with no ring composed
    // nothing reads it, so it costs one clock tick and no frames.
    val spotlightTransition = rememberInfiniteTransition(label = "spotlightPulse")
    val pulseScale by spotlightTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "spotlightPulse",
    )

    // Back closes the bubble (ending Find if one is in flight).
    BackHandler(enabled = bubble != null) { endFind() }

    // 3.1.1 (item 2): a failed Find closes the bubble (and clears the
    // thinking state); the mascot itself keeps walking.
    LaunchedEffect(FeatureSessionState.disabled) {
        if (FeatureId.FIND in FeatureSessionState.disabled) {
            finding = false
            endFind()
        }
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInWindow()
                layerOrigin = bounds.topLeft
                layerSize = Offset(bounds.width, bounds.height)
            }
            .pointerInput(Unit) {
                // Touch outside the bubble cancels Find (menu bubbles
                // just close). Never consumes: the touch still does
                // its home action. Downs inside the sprite or the
                // bubble are ignored — those handlers own them.
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        if (bubble == null) continue
                        val sp = sizePxNow
                        val sx = animX
                        val sy = animY
                        // 3.1.1 (item 4): the hitbox is the drawn
                        // sprite rect, by the shared helper below.
                        val inSprite = spriteHitRect(sx, sy, sp).contains(down.position)
                        val bx = (animX + sp / 2f - bubbleSize.x / 2f)
                            .coerceIn(8f, (layerSize.x - bubbleSize.x - 8f).coerceAtLeast(8f))
                        val aboveY = animY - bubbleSize.y - 8f
                        val by = (if (aboveY >= 8f) aboveY else (animY + sp + 8f))
                            .coerceIn(8f, (layerSize.y - bubbleSize.y - 8f).coerceAtLeast(8f))
                        val inBubble = down.position.x in bx..(bx + bubbleSize.x) &&
                            down.position.y in by..(by + bubbleSize.y)
                        if (!inSprite && !inBubble) endFind()
                    }
                }
            },
    ) {
        // Spotlight: 40% dim everywhere except the target, a 2dp
        // accent ring (soft pulse; static under Reduce Motion).
        // Any tap on the dim dismisses the guide — the dim consumes
        // exactly that tap, so the first tap never reaches the icon.
        val ring = spotlightRect
        if (ring != null && guide != null) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .pointerInput(ring) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown()
                                // 3.1.1 (item 4): the dim consumes
                                // ONLY the dismiss tap. A swipe must
                                // still scroll the pager: movement
                                // past slop ends the guide WITHOUT
                                // consuming, so the pager (which also
                                // saw the unconsumed down) scrolls.
                                // A tap consumes its UP, so the icon
                                // beneath never receives the click —
                                // the first tap only dismisses (its
                                // pressed flash may still show).
                                val slop = viewConfig.touchSlop
                                var done = false
                                while (!done) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes
                                        .firstOrNull { it.id == down.id } ?: continue
                                    if (!change.pressed) {
                                        change.consume()
                                        endFind()
                                        done = true
                                    } else if (
                                        (change.position - down.position)
                                            .getDistance() > slop
                                    ) {
                                        endFind()
                                        done = true
                                    }
                                }
                            }
                        }
                    },
            ) {
                drawRect(Color.Black.copy(alpha = 0.4f))
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(ring.left - 6f, ring.top - 6f),
                    size = Size(ring.width + 12f, ring.height + 12f),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    blendMode = BlendMode.Clear,
                )
            }
            val scale = if (reducedMotion) 1f else pulseScale
            Box(
                Modifier
                    .graphicsLayer {
                        translationX = ring.left - 6f
                        translationY = ring.top - 6f
                        scaleX = scale
                        scaleY = scale
                    }
                    .size(
                        width = with(density) { (ring.width + 12f).toDp() },
                        height = with(density) { (ring.height + 12f).toDp() },
                    )
                    .border(2.dp, palette.accent, RoundedCornerShape(16.dp)),
            )
        }
        // Mascot body: the only home-area input surface besides the
        // bubble. Short tap toggles the menu; a 350 ms long-press
        // picks the sprite up (Carried, haptic tick); dropping on a
        // valid ledge places it, anywhere else returns it to the
        // previous ledge. Nothing here consumes taps meant for home:
        // the wait loop below never consumes, and the drag loop only
        // consumes moves after the long-press has fired.
        val displayAnim = overrideAnim
            ?: animationForExpression(if (sleeping) MascotExpression.SLEEPY else expression)
        Box(
            Modifier
                .graphicsLayer {
                    translationX = animX
                    translationY = animY
                }
                .semantics {
                    contentDescription = "$mascotName, mascot. Double-tap to open the mascot menu."
                    role = Role.Button
                    onClick { tapToggle(); true }
                }
                .pointerInput(Unit) {
                    try {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown()
                            val slop = viewConfig.touchSlop
                            var released = false
                            var cancelled = false
                            val tapped = withTimeoutOrNull(350) {
                                while (!released && !cancelled) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: continue
                                    if ((change.position - down.position).getDistance() > slop) {
                                        cancelled = true
                                    } else if (!change.pressed) {
                                        released = true
                                    }
                                }
                                released
                            }
                            when {
                                tapped == null -> {
                                    // Long-press fired: carried.
                                    if (hapticsEnabled(
                                            stateNow.pressFeedback, stateNow.hapticStrength,
                                        )
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    carriedPrev = currentLedge
                                    goPixelState(PixelMascotState.Carried)
                                    mascotState = MascotState.DRAGGED
                                    expression = MascotExpression.SURPRISED
                                    var done = false
                                    while (!done) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes
                                            .firstOrNull { it.id == down.id } ?: continue
                                        if (change.pressed) {
                                            change.consume()
                                            val size = layerSizeNow
                                            val sp = sizePxNow
                                            val nx = (posNow.x + change.position.x - change.previousPosition.x)
                                                .coerceIn(0f, (size.x - sp).coerceAtLeast(0f))
                                            val ny = (posNow.y + change.position.y - change.previousPosition.y)
                                                .coerceIn(0f, (size.y - sp).coerceAtLeast(0f))
                                            targetPos = Offset(nx, ny)
                                        } else {
                                            done = true
                                        }
                                    }
                                    // Drop: snap to a ledge within 48dp
                                    // vertically, else return to previous.
                                    val cur = posNow
                                    val sp = sizePxNow
                                    val dens = densityNow.density
                                    val drop = snapDropToLedge(
                                        graphNow,
                                        cur.x + sp / 2f, cur.y + sp,
                                        48f * dens,
                                    )
                                    if (drop != null) {
                                        currentLedge = drop
                                        targetPos = Offset(
                                            drop.x - sp / 2f, drop.y - sp,
                                        )
                                        val size = layerSizeNow
                                        val fx = if (size.x > 0) drop.x / size.x else homeXNow
                                        val fy = if (size.y > 0) drop.y / size.y else homeYNow
                                        mascotGuardedScope.launch { settings.setMascotHomeSpot(fx, fy) }
                                    } else {
                                        carriedPrevNow?.let { prev ->
                                            currentLedge = prev
                                            targetPos = Offset(
                                                prev.x - sp / 2f, prev.y - sp,
                                            )
                                        }
                                    }
                                    overrideAnim = null
                                    goPixelState(PixelMascotState.Idle)
                                    mascotState = MascotState.IDLE
                                    expression = MascotExpression.HAPPY
                                }
                                tapped -> tapToggle()
                                else -> {
                                    // Moved before the long-press fired:
                                    // not our gesture, ignore it.
                                }
                            }
                        }
                    }
                    } finally {
                        // Gesture scope died mid-carry (cancel/dispose):
                        // restore the previous ledge, never hang Carried.
                        if (pixelState == PixelMascotState.Carried) {
                            carriedPrev?.let { prev ->
                                currentLedge = prev
                                targetPos = Offset(
                                    prev.x - sizePxNow / 2f, prev.y - sizePxNow,
                                )
                            }
                            overrideAnim = null
                            goPixelState(PixelMascotState.Idle)
                            mascotState = MascotState.IDLE
                        }
                    }
                },
        ) {
            // 3.1.1: single read + smart cast — no !! on the
            // sprite set (it is loaded once, but never assumed).
            val set = spriteSet
            if (USE_PIXEL_MASCOT && set != null) {
                if (isCarried) {
                    // No dedicated dangling frame in the atlas: the
                    // jump apex frame (legs tucked) reads as dangling.
                    StaticSpriteFrame(set, 11, spriteScale)
                } else {
                    PixelSprite(set, displayAnim, spriteScale, facingRight)
                }
            } else {
                MascotFigure(
                    character = character,
                    expression = if (sleeping) MascotExpression.SLEEPY else expression,
                    size = sizeDp,
                    body = palette.onSurface,
                    eye = palette.surface,
                    accent = palette.accent,
                    blink = blink,
                )
                // Emote chip (vector figure only — the pixel sprite's
                // faces carry the expression): one at a time, ~1.2s.
                val emote = emoteFor(if (sleeping) MascotExpression.SLEEPY else expression)
                if (emoteVisible && emote != null) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(bottom = 2.dp)
                            .background(palette.surface, RoundedCornerShape(50))
                            .border(1.dp, palette.accent, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .semantics { contentDescription = "Mascot feels $emote" },
                    ) {
                        Text(emote, color = palette.onSurface, fontSize = 12.sp)
                    }
                }
            }
        }
        // Bubble: above the mascot when room, else below; clamped
        // on-screen and above the IME, so the Find field is never
        // covered by the keyboard.
        val mode = bubble
        if (mode != null) {
            // 3.1.1 (item 4): position math lives in the draw phase.
            // animX/animY change every frame while moving; reading
            // them in composition recomposed the bubble subtree per
            // frame. Now a move only invalidates draw. The IME read
            // stays in composition on purpose: it subscribes this
            // scope to keyboard changes, so the lambda below re-runs
            // (and the bubble stays above the keyboard) when the
            // IME opens or closes.
            val imeBottomPx = WindowInsets.ime.getBottom(density).toFloat()
            // 3.1.1 (item 7, E3): one stepped shape for the bubble
            // background and border (2px steps at sprite scale).
            val steppedBubbleShape = remember(spriteScale) {
                SteppedRoundedShape(20.dp, 2f * spriteScale)
            }
            Box(
                Modifier
                    .graphicsLayer {
                        translationX = (animX + sizePx / 2 - bubbleSize.x / 2)
                            .coerceIn(8f, (layerSize.x - bubbleSize.x - 8f).coerceAtLeast(8f))
                        val aboveY = animY - bubbleSize.y - 8f
                        translationY = (if (aboveY >= 8f) aboveY else (animY + sizePx + 8f))
                            .coerceIn(8f, (layerSize.y - bubbleSize.y - 8f).coerceAtLeast(8f))
                            .coerceAtMost(
                                (layerSize.y - imeBottomPx - bubbleSize.y - 8f).coerceAtLeast(8f),
                            )
                    }
                    .widthIn(max = 320.dp)
                    .onGloballyPositioned { coords ->
                        val b = coords.boundsInWindow()
                        bubbleSize = Offset(b.width, b.height)
                    }
                    // 3.1.1 (item 7, E3): pixel-stepped border — 2px
                    // steps at the sprite's scale, 20dp corner kept
                    // from Phase C. The stepped shape replaces the
                    // smooth rounded border; text stays 14sp+.
                    .background(palette.surface, steppedBubbleShape)
                    .border(
                        with(density) { (2f * spriteScale).toDp() },
                        palette.accent.copy(alpha = 0.5f),
                        steppedBubbleShape,
                    )
                    .padding(12.dp),
            ) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$mascotName",
                            color = palette.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = { closeBubble() },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text("Close", color = palette.onSurface, fontSize = 14.sp)
                        }
                    }
                    when (mode) {
                        is BubbleMode.Menu -> {
                            val unoffered = TUTORIALS.firstOrNull {
                                it.id !in parseMascotStringList(state.mascotDontShowJson)
                            }
                            if (unoffered != null && state.mascotTips != 0) {
                                Text(
                                    "Want a quick tour of ${unoffered.title.lowercase()}? Offered once.",
                                    color = palette.onSurface, fontSize = 14.sp,
                                )
                                Row {
                                    TextButton(
                                        onClick = {
                                            markTutorialOffered(unoffered.id)
                                            bubbleKey++
                                            bubble = BubbleMode.TutorialSteps(unoffered, 0)
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) { Text("Yes", color = palette.accent, fontSize = 14.sp) }
                                    TextButton(
                                        onClick = {
                                            markTutorialOffered(unoffered.id)
                                            closeBubble()
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) { Text("Not now", color = palette.onSurface, fontSize = 14.sp) }
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                            // 3.1.1 (item 7, E3): four 48dp icon
                            // chips in a wrapping row (wraps if the
                            // bubble narrows; at 320dp max width the
                            // four fit one row).
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                            MenuChip(HavenIcon.Search, "Find") {
                                findFlow = nextFindFlowState(findFlow, FindFlowEvent.Start)
                                bubbleKey++
                                bubble = BubbleMode.FindInput
                            }
                            MenuChip(HavenIcon.Help, "Help") {
                                mascotGuardedScope.launch {
                                    settings.recordAssistUse(AssistFeatureId.MASCOT_CHAT, System.currentTimeMillis())
                                    applyResponse(brain.handle(BrainInput.HelpMenu, brainCtx))
                                }
                            }
                            MenuChip(HavenIcon.Star, "Suggest") {
                                if (state.mascotTips == 0) {
                                    bubbleKey++
                                    bubble = BubbleMode.Message(
                                        "Suggestions are off. Switch Tips on in Settings, then Mascot.",
                                    )
                                } else {
                                    val muted = parseMascotStringList(state.mascotMutedJson).toSet()
                                    val pin = suggestPinCandidate(libraryApps, stats, dockKeys, state.dockMax)
                                    // Normal (2) adds the same help-tip
                                    // fallback as the auto-nudge.
                                    val tipEntry = if (pin == null &&
                                        !suggestWidgetCandidate(homeItems.size) &&
                                        state.mascotTips == 2
                                    ) {
                                        HELP_ENTRIES.firstOrNull { it.id !in muted }
                                    } else null
                                    val suggestionId = when {
                                        pin != null -> "pin:${pin.key}"
                                        suggestWidgetCandidate(homeItems.size) -> "widget-room"
                                        tipEntry != null -> "tip:${tipEntry.id}"
                                        else -> null
                                    }
                                    if (suggestionId == null || muted.contains(suggestionId) ||
                                        !canNudge(
                                            sessionNudges, state.mascotLastNudgeMs,
                                            System.currentTimeMillis(), false, quietNow,
                                        )
                                    ) {
                                        bubbleKey++
                                        bubble = BubbleMode.Message(
                                            "Nothing to suggest right now — I only nudge when something is genuinely useful.",
                                        )
                                    } else {
                                        sessionNudges = 1
                                        mascotGuardedScope.launch {
                                            settings.setMascotLastNudgeMs(System.currentTimeMillis())
                                            settings.recordAssistUse(AssistFeatureId.SUGGESTIONS, System.currentTimeMillis())
                                        }
                                        bubbleKey++
                                        bubble = BubbleMode.Message(
                                            text = when {
                                                pin != null -> "You open ${pin.label} a lot — pin it to the dock?"
                                                tipEntry != null -> tipEntry.answer
                                                else -> "Your home has room for a clock widget. Want one?"
                                            },
                                            chips = listOfNotNull(
                                                when {
                                                    tipEntry != null -> tipEntry.settingRoute?.let { route ->
                                                        ChipAction("Open") {
                                                            onOpenSettings(route, tipEntry.controlKey)
                                                            closeBubble()
                                                        }
                                                    }
                                                    pin != null -> ChipAction("Do it") {
                                                        appsByKey[pin.key]?.let { onAddToDock(it) }
                                                        closeBubble()
                                                    }
                                                    else -> ChipAction("Do it") {
                                                        onOpenWidgetSheet()
                                                        closeBubble()
                                                    }
                                                },
                                                ChipAction("Don't suggest this") {
                                                    mascotGuardedScope.launch {
                                                        settings.setMascotMutedJson(
                                                            encodeMascotStringList(muted + suggestionId),
                                                        )
                                                    }
                                                    closeBubble()
                                                },
                                            ),
                                        )
                                    }
                                }
                            }
                            MenuChip(HavenIcon.Close, "Dismiss") {
                                mascotGuardedScope.launch { settings.setMascotDismissed(true) }
                                closeBubble()
                            }
                            }
                        }
                        is BubbleMode.FindInput -> {
                            Text(
                                "What are you looking for? Apps, folders, settings.",
                                color = palette.onSurface, fontSize = 14.sp,
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = findQuery,
                                onValueChange = { findQuery = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Search apps, settings…", fontSize = 14.sp) },
                                placeholder = { Text("Type here…", fontSize = 14.sp) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { submitFind() },
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.accent,
                                    unfocusedBorderColor = palette.onSurface.copy(alpha = 0.5f),
                                    cursorColor = palette.accent,
                                    focusedLabelColor = palette.accent,
                                ),
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { submitFind() },
                                enabled = !finding,
                                modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth(),
                            ) {
                                Text(if (finding) "Thinking…" else "Find", fontSize = 14.sp)
                            }
                        }
                        is BubbleMode.Message -> {
                            Text(mode.text, color = palette.onSurface, fontSize = 14.sp)
                            mode.chips.forEach { chip ->
                                TextButton(
                                    onClick = chip.onClick,
                                    modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth(),
                                ) {
                                    Text(chip.label, color = palette.accent, fontSize = 14.sp)
                                }
                            }
                        }
                        is BubbleMode.Choices -> {
                            Text(
                                "I found a few matches — pick one and I will point to it.",
                                color = palette.onSurface, fontSize = 14.sp,
                            )
                            mode.candidates.forEach { candidate ->
                                val label = when (val t = candidate.target) {
                                    is FindTarget.App -> t.label
                                    is FindTarget.Widget -> t.label
                                    is FindTarget.Setting -> t.title
                                    is FindTarget.Action -> t.title
                                }
                                TextButton(
                                    onClick = { applyCandidate(candidate) },
                                    modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth(),
                                ) {
                                    Text(label, color = palette.accent, fontSize = 14.sp)
                                }
                            }
                        }
                        is BubbleMode.HelpList -> {
                            Text(
                                "Pick a question — I will open the right Settings screen.",
                                color = palette.onSurface, fontSize = 14.sp,
                            )
                            HELP_ENTRIES.forEach { entry ->
                                TextButton(
                                    onClick = {
                                        expression = MascotExpression.HAPPY
                                        bubbleKey++
                                        bubble = BubbleMode.Message(
                                            entry.answer,
                                            listOfNotNull(
                                                entry.settingRoute?.let { route ->
                                                    ChipAction("Open") {
                                                        onOpenSettings(route, entry.controlKey)
                                                        closeBubble()
                                                    }
                                                },
                                            ),
                                        )
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth(),
                                ) {
                                    Text(entry.question, color = palette.accent, fontSize = 14.sp)
                                }
                            }
                            Text(
                                "Tours:",
                                color = palette.onSurface, fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            TUTORIALS.forEach { tutorial ->
                                TextButton(
                                    onClick = {
                                        bubbleKey++
                                        bubble = BubbleMode.TutorialSteps(tutorial, 0)
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth(),
                                ) {
                                    Text("Tour: ${tutorial.title}", color = palette.accent, fontSize = 14.sp)
                                }
                            }
                        }
                        is BubbleMode.TutorialOffer -> {
                            Text(
                                "Want a quick tour of ${mode.tutorial.title.lowercase()}?",
                                color = palette.onSurface, fontSize = 14.sp,
                            )
                            Row {
                                TextButton(
                                    onClick = {
                                        markTutorialOffered(mode.tutorial.id)
                                        bubbleKey++
                                        bubble = BubbleMode.TutorialSteps(mode.tutorial, 0)
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) { Text("Yes", color = palette.accent, fontSize = 14.sp) }
                                TextButton(
                                    onClick = {
                                        markTutorialOffered(mode.tutorial.id)
                                        closeBubble()
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) { Text("Not now", color = palette.onSurface, fontSize = 14.sp) }
                                TextButton(
                                    onClick = {
                                        markTutorialOffered(mode.tutorial.id)
                                        closeBubble()
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) { Text("Don't show again", color = palette.onSurface, fontSize = 14.sp) }
                            }
                        }
                        is BubbleMode.TutorialSteps -> {
                            val tutorial = mode.tutorial
                            Text(
                                "${tutorial.title} — step ${mode.step + 1} of ${tutorial.steps.size}",
                                color = palette.onSurface, fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(tutorial.steps[mode.step], color = palette.onSurface, fontSize = 14.sp)
                            Row {
                                if (mode.step + 1 < tutorial.steps.size) {
                                    TextButton(
                                        onClick = {
                                            bubbleKey++
                                            bubble = BubbleMode.TutorialSteps(tutorial, mode.step + 1)
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) { Text("Next", color = palette.accent, fontSize = 14.sp) }
                                } else {
                                    TextButton(
                                        onClick = {
                                            markTutorialOffered(tutorial.id)
                                            expression = MascotExpression.CELEBRATE
                                            closeBubble()
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) { Text("Done", color = palette.accent, fontSize = 14.sp) }
                                }
                                if (tutorial.actionId != null && mode.step == 0) {
                                    TextButton(
                                        onClick = { doConfirmedAction(tutorial.actionId) },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) { Text("Try it", color = palette.accent, fontSize = 14.sp) }
                                }
                                TextButton(
                                    onClick = { closeBubble() },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) { Text("Skip", color = palette.onSurface, fontSize = 14.sp) }
                            }
                        }
                        is BubbleMode.Confirm -> {
                            Text(
                                "“${mode.title}” — do exactly this?",
                                color = palette.onSurface, fontSize = 14.sp,
                            )
                            Text(
                                "Nothing changes until you choose Do it.",
                                color = palette.onSurface, fontSize = 12.sp,
                            )
                            Row {
                                TextButton(
                                    onClick = { doConfirmedAction(mode.actionId) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) { Text("Do it", color = palette.accent, fontSize = 14.sp) }
                                TextButton(
                                    onClick = { closeBubble() },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) { Text("No", color = palette.onSurface, fontSize = 14.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuChip(icon: HavenIcon, label: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(percent = 50))
            .border(
                1.dp,
                palette.accent.copy(alpha = 0.5f),
                RoundedCornerShape(percent = 50),
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HavenIconImage(icon, label, size = 20.dp)
            Spacer(Modifier.width(6.dp))
            Text(label, color = palette.onSurface, fontSize = 14.sp)
        }
    }
}

private fun minuteOfDay(): Int {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
}
