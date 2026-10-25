package app.havenos.launcher.ui.settings.shell

// ===========================================================================
// HUB HOST CONTRACT (M6 shell — read this before writing a hub screen)
// ---------------------------------------------------------------------------
// A hub screen is a plain composable with exactly this signature:
//
//   @Composable fun XxxHubScreen(
//       onBack: () -> Unit,
//       onSpotlight: (String) -> Unit,
//   )
//
// - `onBack` pops the panel stack (the panel also wires system Back to it).
// - `onSpotlight` fires the spotlight on a Home element: pass the key the
//   Home host recognises for that element (the panel stores it in
//   SettingsPanelState.spotlightKey; the Home host wraps the matching
//   element in SpotlightRing from PreviewAids.kt). Hubs also call it from
//   their own controls, per v2 §3.4.
// - Read state with `(LocalContext.current.applicationContext as
//   HavenApp).settings.state` via collectAsState — never a cached copy.
// - Write with `settings.update { ... }` or the typed setters in
//   data/Settings.kt. Do NOT change Settings.kt. Do NOT invent a control
//   for a key the M6 audit (docs/AUDIT-M6.md §3) found not wired.
// - After a committed write, record it for Undo / Changes:
//   `panelState.aids.notifyChanged(scope, ChangeRecord(key, old, new) {
//   settings.<setter>(old) })` — the revert lambda is what makes Undo
//   and per-item revert work. The panel renders the Undo snackbar.
// - Render inside ShellScreenColumn(route) as the root: it scrolls, and
//   every screen opens at the top. Keep a screen to ~7 rows; overflow
//   goes in ShellMoreOptions, rare options in ShellAdvancedFold.
// - Reset: a screen gets AT MOST one Reset, via HubScreenDef.reset
//   (count + suspend action). The panel renders it in the title-bar
//   overflow with a confirm naming how many settings change. Screens
//   without a reset supply null — never add a second reset row.
// - The panel hosts hub content; it never renders hub rows itself.
//
// The integration step fills the registry — it does not edit this file:
//
//   val registry = HubScreenRegistry(mapOf(
//       "look" to HubScreenDef("look", "Look", { onBack, onSpotlight ->
//           LookHubScreen(onBack, onSpotlight) }),
//       ...
//   ))
//   val panelState = rememberSettingsPanelState(registry, startRoute = "home")
//   SettingsPanel(state = panelState, homeContent = { HomeContent() },
//       onClose = { showSettings = false })
//
// Requirements on the host: SettingsPanel must be composed inside
// HavenTheme (it reads LocalPalette / LocalState for accent, material
// and reduced motion) and above the live Home composition, so Home
// keeps updating behind it. Tapping Home behind the panel never
// dismisses it — only the close button, system Back past the root, or
// the host's own close action does.
// ===========================================================================

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.theme.LocalFont
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.theme.LocalState
import app.havenos.launcher.ui.speedDuration
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Hub screen content: back + spotlight callbacks only, per the contract above. */
typealias HubScreenContent = @Composable (onBack: () -> Unit, onSpotlight: (String) -> Unit) -> Unit

/** The single Reset a screen may offer: how many settings it changes + the action. */
data class HubReset(
    val affectedCount: Int,
    val onReset: suspend () -> Unit,
)

/** One hub screen in the registry. */
data class HubScreenDef(
    val route: String,
    val title: String,
    val content: HubScreenContent,
    val reset: HubReset? = null,
)

/** The registry type the integration step fills: route -> hub screen. */
data class HubScreenRegistry(
    val screens: Map<String, HubScreenDef>,
) {
    operator fun get(route: String): HubScreenDef? = screens[route]
    fun contains(route: String): Boolean = screens.containsKey(route)
}

/** Bottom-sheet detents (v2 §3.3): peek ~30%, half ~58%, full ~92%. */
enum class SheetDetent(val fraction: Float) {
    Peek(0.30f),
    Half(0.58f),
    Full(0.92f),
}

/**
 * Panel navigation + sheet state. The stack holds routes; [current]
 * is the top. [spotlightKey] is the last spotlight target for the
 * Home host to ring. Sheet detents are remembered per route.
 */
class SettingsPanelState(
    val registry: HubScreenRegistry,
    startRoute: String,
    val aids: PreviewSession,
) {
    private val stack = mutableStateListOf(startRoute)

    val current: String get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1
    val breadcrumb: String
        get() = buildString {
            append("Settings")
            for (route in stack) {
                append(" › ")
                append(registry[route]?.title ?: route)
            }
        }

    /** Last spotlight target, or null. [spotlightNonce] retriggers the same key. */
    var spotlightKey: String? by mutableStateOf(null)
        private set
    var spotlightNonce: Int by mutableStateOf(0)
        private set

    private val detents = mutableMapOf<String, SheetDetent>()

    fun push(route: String) {
        if (registry.contains(route) && route != current) stack.add(route)
    }

    /** Pop one level. Returns false at the root (the host then closes). */
    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun toRoot() {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun spotlight(key: String) {
        spotlightKey = key
        spotlightNonce++
    }

    fun sheetDetentFor(route: String): SheetDetent = detents[route] ?: SheetDetent.Half
    fun setSheetDetent(route: String, detent: SheetDetent) {
        detents[route] = detent
    }
}

@Composable
fun rememberSettingsPanelState(
    registry: HubScreenRegistry,
    startRoute: String,
    aids: PreviewSession,
): SettingsPanelState = remember(registry, startRoute) {
    SettingsPanelState(registry, startRoute, aids)
}

@Composable
fun rememberSettingsPanelState(
    registry: HubScreenRegistry,
    startRoute: String,
): SettingsPanelState {
    val aids = rememberPreviewSession()
    return rememberSettingsPanelState(registry, startRoute, aids)
}

/**
 * Root for hub screens: scrolls, opens at the top on every route change,
 * 16dp side padding, 12dp gaps between cards.
 */
@Composable
fun ShellScreenColumn(
    route: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    LaunchedEffect(route) { scroll.scrollTo(0) }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = ShellSpacing.s16, vertical = ShellSpacing.s12),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
            ShellSpacing.cardGap,
        ),
    ) { content() }
}

/** Panel background for the user's material: near-opaque so text contrast holds. */
private fun panelBackground(material: Int, tokens: ShellTokens): Color = when (material) {
    0 -> tokens.panelSurface // Solid
    1 -> tokens.panelSurface.copy(alpha = 0.96f) // Translucent
    else -> tokens.panelSurface.copy(alpha = 0.92f) // Frosted
}

/** Adaptive scrim over Home behind the panel (drawn only, never dismisses, never eats taps). */
private fun scrimAlpha(material: Int, dark: Boolean): Float {
    val base = if (dark) 0.30f else 0.12f
    return base + material * 0.06f
}

/**
 * The settings panel over the live Home. Tablet landscape (width >=
 * 840dp): Home left (re-laid out in the free area, never under the
 * panel), panel docked right at ~46% clamped to 480–660dp with a 12dp
 * inset and 28dp radius, and — M10 master-detail (BUG-026/027/028) —
 * a permanent left rail of the six hubs + All settings beside the
 * detail, so categories and controls are visible simultaneously and
 * the panel no longer leaves most of the tablet as dead space. On
 * tablets the full-screen scrim is dropped (the panel is opaque and
 * home no longer sits under it, so dimming it would only hide the
 * live home the panel is meant to preview against). Anything
 * narrower: a bottom sheet with peek / half / full detents, a drag
 * handle, springy snap and per-screen detent memory. System Back steps
 * up the stack; at the root it closes. Tapping Home behind never
 * dismisses. Under reduced motion there is no slide and no spring:
 * screens switch instantly and the sheet snaps.
 */

/**
 * Docked panel width (dp) for a tablet of [screenWidthDp]. Shared by
 * SettingsPanel and the home free-area layout so the two never
 * disagree about where the panel begins.
 */
fun tabletPanelWidthDp(screenWidthDp: Float): Float =
    (screenWidthDp * 0.46f).coerceIn(480f, 660f).coerceAtMost(screenWidthDp - 24f)
@Composable
fun SettingsPanel(
    state: SettingsPanelState,
    homeContent: @Composable () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val liveState = LocalState.current
    val tokens = shellTokensFor(LocalPalette.current)
    val dark = LocalPalette.current.bg.let {
        it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f < 0.5f
    }
    val reducedMotion = liveState.reduceMotion
    CompositionLocalProvider(LocalShellTokens provides tokens) {
        BoxWithConstraints(modifier.fillMaxSize()) {
            homeContent()
            // Adaptive scrim (phones only): darkens Home behind the
            // sheet so panel text keeps >= 4.5:1 contrast on any
            // wallpaper. No click handler: taps pass through, and
            // tapping Home never dismisses. Tablets skip it — the
            // panel is opaque, home is re-laid out in the free area
            // (nothing sits under the panel), and dimming the free
            // area would only hide the live home (BUG-029).
            if (maxWidth < 840.dp) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = scrimAlpha(liveState.surfaceMaterial, dark))),
                )
            }
            BackHandler {
                if (!state.pop()) onClose()
            }
            if (maxWidth >= 840.dp) {
                DockedPanel(
                    state = state,
                    onClose = onClose,
                    reducedMotion = reducedMotion,
                    panelWidth = tabletPanelWidthDp(maxWidth.value).dp,
                    panelBackground = panelBackground(liveState.surfaceMaterial, tokens),
                )
            } else {
                SheetPanel(
                    state = state,
                    onClose = onClose,
                    reducedMotion = reducedMotion,
                    maxHeight = maxHeight,
                    panelBackground = panelBackground(liveState.surfaceMaterial, tokens),
                )
            }
        }
    }
}

@Composable
private fun PanelTitleBar(
    state: SettingsPanelState,
    onClose: () -> Unit,
    onPeek: (() -> Unit)?,
) {
    val tokens = LocalShellTokens.current
    var changesOpen by remember { mutableStateOf(false) }
    val current = state.registry[state.current]
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.canGoBack) {
            Box(
                Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Back"; role = Role.Button }
                    .clickable { state.pop() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "‹",
                    style = ShellText.title.copy(fontFamily = LocalFont.current),
                    color = tokens.textPrimary,
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = state.breadcrumb,
                style = ShellText.help.copy(fontFamily = LocalFont.current),
                color = tokens.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = current?.title ?: "Settings",
                style = ShellText.title.copy(fontFamily = LocalFont.current),
                color = tokens.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (state.aids.openSnapshot != null) {
            BeforeHoldButton(onHoldChange = { state.aids.beforeHeld = it })
        }
        if (state.aids.changes.isNotEmpty()) {
            ChangesChip(count = state.aids.changes.size, onClick = { changesOpen = true })
        }
        val reset = current?.reset
        if (reset != null) {
            ShellResetOverflow(
                affectedCount = reset.affectedCount,
                onReset = reset.onReset,
            )
        }
        if (onPeek != null) {
            TextButton(onClick = onPeek) {
                Text(
                    text = "Peek",
                    style = ShellText.help.copy(fontFamily = LocalFont.current),
                    color = tokens.accent,
                )
            }
        }
        Box(
            Modifier
                .size(48.dp)
                .semantics { contentDescription = "Close settings"; role = Role.Button }
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✕",
                style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                color = tokens.textPrimary,
            )
        }
    }
    if (changesOpen) {
        ChangesDialog(session = state.aids, onDismiss = { changesOpen = false })
    }
}

@Composable
private fun PanelScreenContent(
    state: SettingsPanelState,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val route = state.current
    val content: @Composable () -> Unit = {
        val def = state.registry[route]
        if (def == null) {
            ShellEmptyState(message = "Screen not available")
        } else {
            def.content(
                { state.pop() },
                { key -> state.spotlight(key) },
            )
        }
    }
    // Motion speed (animSpeed): the panel slide scales with the
    // Gestures & Motion speed control; reduce motion or Off is instant.
    val animSpeed = LocalState.current.animSpeed
    if (reducedMotion || animSpeed == 0) {
        Box(modifier) { content() }
    } else {
        val ms = speedDuration(240, animSpeed)
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                (slideInHorizontally(animationSpec = tween(durationMillis = ms)) { it / 3 } +
                    fadeIn(animationSpec = tween(durationMillis = ms)))
                    .togetherWith(
                        slideOutHorizontally(animationSpec = tween(durationMillis = ms)) { -it / 3 } +
                            fadeOut(animationSpec = tween(durationMillis = ms)),
                    )
            },
            modifier = modifier,
        ) { content() }
    }
}

@Composable
private fun DockedPanel(
    state: SettingsPanelState,
    onClose: () -> Unit,
    reducedMotion: Boolean,
    panelWidth: Dp,
    panelBackground: Color,
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            Modifier
                .padding(ShellSpacing.s12)
                .width(panelWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(ShellRadii.panel))
                .background(panelBackground)
                .padding(ShellSpacing.s16),
        ) {
            Column {
                PanelTitleBar(state = state, onClose = onClose, onPeek = null)
                Spacer(Modifier.height(ShellSpacing.s12))
                // M10 master-detail (BUG-026/027/028): the hub rail
                // stays visible beside the detail, so categories and
                // controls are on screen together. Hubs, rows, and
                // copy are unchanged — only the presentation gains
                // the rail. The detail is PanelScreenContent exactly
                // as the phone sheet renders it.
                Row(Modifier.weight(1f)) {
                    MasterDetailRail(state = state)
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(LocalShellTokens.current.textPrimary.copy(alpha = 0.12f)),
                    )
                    Column(Modifier.weight(1f)) {
                        PanelScreenContent(
                            state = state,
                            reducedMotion = reducedMotion,
                            modifier = Modifier.weight(1f),
                        )
                        SnackbarHost(
                            hostState = state.aids.snackbarHostState,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * M10 master-detail rail (tablet docked panel only): All settings +
 * the six hubs. Tapping a hub resets the stack to that hub (the same
 * reset opening the panel performs); tapping All settings returns to
 * the hub list. Selection follows the current route; deeper screens
 * inside a hub leave no rail row selected, which is honest — the rail
 * navigates hubs, it does not pretend to track every sub-screen.
 */
@Composable
private fun MasterDetailRail(state: SettingsPanelState) {
    Column(
        Modifier
            .width(180.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
    ) {
        RailRow(
            glyph = "⚙️",
            title = "All settings",
            selected = state.current == "home",
            onClick = { state.toRoot() },
        )
        for (hub in HUB_ROWS) {
            RailRow(
                glyph = hub.glyph,
                title = hub.title,
                selected = state.current == hub.route,
                onClick = {
                    if (state.current != hub.route) {
                        state.toRoot()
                        state.push(hub.route)
                    }
                },
            )
        }
    }
}

@Composable
private fun RailRow(glyph: String, title: String, selected: Boolean, onClick: () -> Unit) {
    val tokens = LocalShellTokens.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) tokens.textPrimary.copy(alpha = 0.10f)
                else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = title; role = Role.Button }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(glyph, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            color = tokens.textPrimary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SheetPanel(
    state: SettingsPanelState,
    onClose: () -> Unit,
    reducedMotion: Boolean,
    maxHeight: Dp,
    panelBackground: Color,
) {
    val tokens = LocalShellTokens.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
    val route = state.current
    val sheetAnim = remember {
        Animatable(state.sheetDetentFor(route).fraction)
    }
    // Re-snap to the remembered detent whenever the screen changes.
    LaunchedEffect(route) {
        val target = state.sheetDetentFor(route).fraction
        if (reducedMotion) sheetAnim.snapTo(target)
        else sheetAnim.animateTo(
            target,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }
    fun setDetent(detent: SheetDetent) {
        state.setSheetDetent(route, detent)
        scope.launch {
            if (reducedMotion) sheetAnim.snapTo(detent.fraction)
            else sheetAnim.animateTo(
                detent.fraction,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(maxHeight * sheetAnim.value)
                .clip(RoundedCornerShape(topStart = ShellRadii.panel, topEnd = ShellRadii.panel))
                .background(panelBackground)
                .padding(horizontal = ShellSpacing.s16, vertical = ShellSpacing.s12),
        ) {
            Column {
                // Drag handle: the only drag surface, so sheet content
                // scrolling never fights the sheet gesture.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { _, dragAmount ->
                                    scope.launch {
                                        sheetAnim.snapTo(
                                            (sheetAnim.value - dragAmount / heightPx)
                                                .coerceIn(0.15f, 0.97f),
                                        )
                                    }
                                },
                                onDragEnd = {
                                    val nearest = SheetDetent.entries.minByOrNull { entry ->
                                        abs(entry.fraction - sheetAnim.value)
                                    } ?: SheetDetent.Half
                                    setDetent(nearest)
                                },
                                onDragCancel = {
                                    setDetent(state.sheetDetentFor(route))
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .width(48.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(tokens.textSecondary.copy(alpha = 0.5f)),
                    )
                }
                PanelTitleBar(
                    state = state,
                    onClose = onClose,
                    onPeek = { setDetent(SheetDetent.Peek) },
                )
                Spacer(Modifier.height(ShellSpacing.s12))
                PanelScreenContent(
                    state = state,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.weight(1f),
                )
                SnackbarHost(
                    hostState = state.aids.snackbarHostState,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
