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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
import app.havenos.launcher.theme.LocalAccentRoles
import app.havenos.launcher.theme.LocalFont
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.ui.glass.GlassLevel
import app.havenos.launcher.ui.glass.HavenIcon
import app.havenos.launcher.ui.glass.HavenIconImage
import app.havenos.launcher.ui.glass.HavenSnackbar
import app.havenos.launcher.ui.glass.HavenIconTile
import app.havenos.launcher.ui.glass.HavenType
import app.havenos.launcher.ui.glass.glass
import app.havenos.launcher.ui.glass.glassScrimColor
import app.havenos.launcher.ui.glass.vector
import app.havenos.launcher.theme.LocalState
import app.havenos.launcher.ui.speedDuration
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 3.1 A3/A10: one-shot panel deep link — open [route], scroll the
 * [field] into view, focus it. [nonce] makes a repeat link to the
 * same field a new instance, so the anchor fires again.
 */
data class PanelDeepLink(val route: String, val field: String, val nonce: Int)

/** The panel's current deep link, or null when it opened without one. */
val LocalPanelDeepLink = staticCompositionLocalOf<PanelDeepLink?> { null }

/**
 * 3.1 A3/A10: wraps a row/section a deep link can target. When the
 * panel's deep link names [field], scrolls this anchor into view
 * (works under the shell's vertical-scroll Column) and, when a
 * [focusRequester] is given, focuses it. One-shot by construction:
 * the effect is keyed on the deep-link instance, so navigating away
 * and back does not refire, and a stale link from a previous panel
 * open never fires on a fresh open.
 */
@Composable
fun DeepLinkAnchor(
    field: String,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val deepLink = LocalPanelDeepLink.current
    val bringIntoView = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    LaunchedEffect(deepLink) {
        if (deepLink?.field == field) {
            try {
                bringIntoView.bringIntoView()
            } catch (t: Throwable) {
                // Scroll container gone mid-navigation: focus still attempted below.
            }
            try {
                focusRequester?.requestFocus()
            } catch (t: Throwable) {
                // Focus is best-effort.
            }
        }
    }
    // 3.1 Phase E: a linked row also draws a static 2dp accent ring
    // while the panel is open, so Find/Help pointing is visible on
    // the row itself. This rings existing deep links too (wallpaper
    // pick, coingecko key) — consistent: it shows where you landed.
    val linked = deepLink?.field == field
    Box(
        modifier
            .bringIntoViewRequester(bringIntoView)
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester)
                else Modifier,
            )
            .then(
                if (linked) Modifier.border(
                    2.dp, LocalPalette.current.accent, RoundedCornerShape(12.dp),
                ) else Modifier,
            ),
    ) {
        content()
    }
}

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
    /**
     * 3.1 A8: the header draws ONE title. The crumb renders only at
     * depth three or more (e.g. Look › Wallpaper); at depth one or
     * two it would just repeat the title ("Settings › Settings",
     * "Settings › Look" above "Look") — the ghost title behind the
     * Back control. Empty means "no crumb row".
     */
    val breadcrumb: String
        get() = if (stack.size < 3) {
            ""
        } else {
            buildString {
                for ((i, route) in stack.withIndex()) {
                    if (i > 0) append(" › ")
                    append(registry[route]?.title ?: route)
                }
            }
        }

    /** Last spotlight target, or null. [spotlightNonce] retriggers the same key. */
    var spotlightKey: String? by mutableStateOf(null)
        private set
    var spotlightNonce: Int by mutableStateOf(0)
        private set

    /** 3.1 A3/A10: one-shot deep link for this panel open, or null. */
    var deepLink: PanelDeepLink? by mutableStateOf(null)
        private set

    /** Open [route] (if not current) and arm its [field] deep link. */
    fun deepLinkTo(route: String, field: String) {
        if (registry.contains(route) && route != current) stack.add(route)
        deepLink = PanelDeepLink(route, field, (deepLink?.nonce ?: 0) + 1)
    }

    /** Clear any armed deep link (panel opened without one). */
    fun clearDeepLink() {
        deepLink = null
    }

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

/**
 * 3.1 Phase C: panel surface is Glass2 over the legibility scrim
 * (Phase B §B2: text-carrying glass sits on a ≥40% dark / 60% light
 * scrim, so the panel keeps 4.5:1 over any wallpaper without window
 * blur). The Material setting no longer changes the panel surface;
 * it still drives the home glass tiers.
 */
@Composable
private fun Modifier.panelGlass(shape: Shape): Modifier {
    val dark = LocalPalette.current.bg.let {
        it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f < 0.5f
    }
    return this
        .clip(shape)
        .background(glassScrimColor(dark), shape)
        .glass(GlassLevel.GLASS2, shape)
}

/** Phone scrim over Home behind the panel (drawn only, never dismisses, never eats taps). Coordination fix §6: flat 40%. */
private fun scrimAlpha(material: Int, dark: Boolean): Float = 0.40f

/**
 * The settings panel over the live Home. Tablet landscape (width >=
 * 840dp): Home left (re-laid out in the free area, never under the
 * panel), panel docked right at ~56% clamped to 640–920dp with a 12dp
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
 *
 * 3.1.1 (item 5): clamp(640dp, 56% of window, 920dp). The old ~46% /
 * 480–660dp panel left ~240dp for the detail pane beside the 260dp
 * rail, so hub rows wrapped one character per line — that was the
 * root cause of the Privacy / Device & Data / Mascot symptoms.
 */
fun tabletPanelWidthDp(screenWidthDp: Float): Float =
    (screenWidthDp * 0.56f).coerceIn(640f, 920f).coerceAtMost(screenWidthDp - 24f)

/**
 * 3.1.1 (item 5): rail width for a panel. Icon-only 72dp when the
 * panel is under 900dp (label as tooltip + accessibility label),
 * expanded 200dp with labels at 900dp or more.
 */
fun settingsRailWidthDp(panelWidthDp: Float): Float =
    if (panelWidthDp < 900f) 72f else 200f

/**
 * 3.1.1 (item 5): detail width = panel − rail − 16dp side padding ×2
 * − 1dp divider − 4dp divider padding ×2. With the new clamp the
 * detail is always ≥ 360dp in master-detail (640 − 72 − 41 = 527dp
 * at the narrow end); narrower panels use single-pane navigation.
 */
fun settingsDetailWidthDp(panelWidthDp: Float): Float =
    panelWidthDp - settingsRailWidthDp(panelWidthDp) - 41f
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
                )
            } else {
                SheetPanel(
                    state = state,
                    onClose = onClose,
                    reducedMotion = reducedMotion,
                    maxHeight = maxHeight,
                    // 3.1.1 (item 5): phone sheets are under 560dp
                    // wide, so their headers collapse the same way.
                    compactHeader = maxWidth < 560.dp,
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
    // 3.1.1 (item 5): under 560dp Before/Changes collapse to
    // icon-only (with content descriptions); the title keeps
    // priority and is never ellipsized.
    compactHeader: Boolean = false,
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
                HavenIconImage(
                    HavenIcon.Back,
                    "Back",
                    tint = tokens.textPrimary,
                    size = 24.dp,
                )
            }
        }
        Column(Modifier.weight(1f)) {
            if (state.breadcrumb.isNotBlank()) {
                Text(
                    text = state.breadcrumb,
                    style = ShellText.help.copy(fontFamily = LocalFont.current),
                    color = tokens.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = current?.title ?: "Settings",
                style = HavenType.title.copy(fontFamily = LocalFont.current),
                color = tokens.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Clip,
            )
        }
        if (state.aids.openSnapshot != null) {
            BeforeHoldButton(
                onHoldChange = { state.aids.beforeHeld = it },
                iconOnly = compactHeader,
            )
        }
        if (state.aids.changes.isNotEmpty()) {
            ChangesChip(
                count = state.aids.changes.size,
                onClick = { changesOpen = true },
                iconOnly = compactHeader,
            )
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
            Icon(
                HavenIcon.Close.vector(),
                contentDescription = null,
                tint = tokens.textPrimary,
                modifier = Modifier.size(20.dp),
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
    // 3.1 A3/A10: the deep link is provided only on its own route, so
    // an anchor in another hub can never fire on a stale link.
    val providedLink = state.deepLink?.takeIf { it.route == route }
    return PanelScreenContentWithLink(providedLink, state, reducedMotion, modifier, content)
}

@Composable
private fun PanelScreenContentWithLink(
    providedLink: PanelDeepLink?,
    state: SettingsPanelState,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPanelDeepLink provides providedLink) {
    val route = state.current
    // 3.1 A8: one shared-axis hub transition, 220 ms at Normal speed
    // (scaled by the Gestures & Motion speed control). The slide uses
    // FULL-width offsets — outgoing to -width, incoming from +width —
    // so the two contents touch at the boundary but never overlap:
    // the old 1/3-width offsets left a middle third where both rows
    // drew on top of each other (the APIs-hub ghost). AnimatedContent
    // removes the outgoing content from composition when it finishes.
    // Reduce Motion is a 100 ms fade (not instant, not a slide);
    // Motion Off stays instant.
    val animSpeed = LocalState.current.animSpeed
    if (reducedMotion) {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 100))
                    .togetherWith(fadeOut(animationSpec = tween(durationMillis = 100)))
            },
            modifier = modifier,
        ) { content() }
    } else if (animSpeed == 0) {
        Box(modifier) { content() }
    } else {
        val ms = speedDuration(220, animSpeed)
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                (slideInHorizontally(animationSpec = tween(durationMillis = ms)) { it } +
                    fadeIn(animationSpec = tween(durationMillis = ms)))
                    .togetherWith(
                        slideOutHorizontally(animationSpec = tween(durationMillis = ms)) { -it } +
                            fadeOut(animationSpec = tween(durationMillis = ms)),
                    )
            },
            modifier = modifier,
        ) { content() }
    }
    }
}

@Composable
private fun DockedPanel(
    state: SettingsPanelState,
    onClose: () -> Unit,
    reducedMotion: Boolean,
    panelWidth: Dp,
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
                .panelGlass(RoundedCornerShape(ShellRadii.panel))
                .padding(ShellSpacing.s16),
        ) {
            Column {
                PanelTitleBar(
                    state = state,
                    onClose = onClose,
                    onPeek = null,
                    compactHeader = panelWidth < 560.dp,
                )
                Spacer(Modifier.height(ShellSpacing.s12))
                // 3.1.1 (item 5): under 560dp the rail would squeeze
                // the detail below a usable width, so the panel goes
                // single-pane: hub list, then detail with the title
                // bar's back button (PanelScreenContent at the root
                // route IS the hub list). At 560dp+ the master-detail
                // rail stays, with the adaptive rail width below.
                if (panelWidth < 560.dp) {
                    PanelScreenContent(
                        state = state,
                        reducedMotion = reducedMotion,
                        modifier = Modifier.weight(1f),
                    )
                    SnackbarHost(
                        hostState = state.aids.snackbarHostState,
                        modifier = Modifier.fillMaxWidth(),
                        snackbar = { data ->
                            HavenSnackbar(
                                message = data.visuals.message,
                                actionLabel = data.visuals.actionLabel,
                                onAction = { data.performAction() },
                                onDismiss = { data.dismiss() },
                            )
                        },
                    )
                } else {
                    // M10 master-detail (BUG-026/027/028): the hub rail
                    // stays visible beside the detail, so categories and
                    // controls are on screen together. Hubs, rows, and
                    // copy are unchanged — only the presentation gains
                    // the rail. The detail is PanelScreenContent exactly
                    // as the phone sheet renders it.
                    Row(Modifier.weight(1f)) {
                        MasterDetailRail(
                            state = state,
                            railWidth = settingsRailWidthDp(panelWidth.value).dp,
                            iconOnly = panelWidth < 900.dp,
                        )
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
                                snackbar = { data ->
                                    HavenSnackbar(
                                        message = data.visuals.message,
                                        actionLabel = data.visuals.actionLabel,
                                        onAction = { data.performAction() },
                                        onDismiss = { data.dismiss() },
                                    )
                                },
                            )
                        }
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
private fun MasterDetailRail(
    state: SettingsPanelState,
    railWidth: Dp,
    iconOnly: Boolean,
) {
    Column(
        Modifier
            .width(railWidth)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
    ) {
        RailRow(
            icon = HavenIcon.HubSettings,
            title = "All settings",
            selected = state.current == "home",
            onClick = { state.toRoot() },
            iconOnly = iconOnly,
        )
        for (hub in HUB_ROWS) {
            RailRow(
                icon = hub.icon,
                title = hub.title,
                selected = state.current == hub.route,
                onClick = {
                    if (state.current != hub.route) {
                        state.toRoot()
                        state.push(hub.route)
                    }
                },
                iconOnly = iconOnly,
            )
        }
    }
}

@Composable
private fun RailRow(
    icon: HavenIcon,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    iconOnly: Boolean = false,
) {
    val tokens = LocalShellTokens.current
    val roles = LocalAccentRoles.current
    // 3.1.1 (item 5): icon-only rail — the label is the tooltip AND
    // the accessibility label (semantics below); no text is drawn.
    val row: @Composable () -> Unit = {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(
                    if (selected) Color(roles.accentContainer)
                    else Color.Transparent,
                )
                .clickable(onClick = onClick)
                .semantics { contentDescription = title; role = Role.Button }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                HavenIconTile(icon, null, iconSize = 24.dp)
            } else {
                Box(
                    Modifier.size(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    HavenIconImage(icon, null, tint = tokens.textPrimary, size = 24.dp)
                }
            }
            if (!iconOnly) {
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    color = if (selected) Color(roles.accent) else tokens.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    if (iconOnly) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(title) } },
            state = rememberTooltipState(),
            content = row,
        )
    } else {
        row()
    }
}

@Composable
private fun SheetPanel(
    state: SettingsPanelState,
    onClose: () -> Unit,
    reducedMotion: Boolean,
    maxHeight: Dp,
    compactHeader: Boolean = false,
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
                .panelGlass(RoundedCornerShape(topStart = ShellRadii.panel, topEnd = ShellRadii.panel))
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
                    compactHeader = compactHeader,
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
                    snackbar = { data ->
                        HavenSnackbar(
                            message = data.visuals.message,
                            actionLabel = data.visuals.actionLabel,
                            onAction = { data.performAction() },
                            onDismiss = { data.dismiss() },
                        )
                    },
                )
            }
        }
    }
}
