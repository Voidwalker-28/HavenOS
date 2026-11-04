package app.havenos.launcher.ui.settings.shell

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.havenos.launcher.HavenAccessibilityService
import app.havenos.launcher.HavenApp
import app.havenos.launcher.ui.glass.HavenIcon
import app.havenos.launcher.ui.glass.vector
import app.havenos.launcher.data.iconDp
import app.havenos.launcher.gestureServicePaused
import app.havenos.launcher.data.parseGestures
import app.havenos.launcher.data.parseMembers
import app.havenos.launcher.data.parseProfiles
import app.havenos.launcher.isNavServiceEnabled
import app.havenos.launcher.theme.LocalAccentRoles
import app.havenos.launcher.theme.LocalFont
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.data.AssistFeatureId
import app.havenos.launcher.ui.assist.AssistAction
import app.havenos.launcher.ui.assist.AssistCommand
import app.havenos.launcher.ui.assist.AssistContext
import app.havenos.launcher.ui.assist.applyAssistSettingAction
import app.havenos.launcher.ui.assist.effectiveIconDp
import app.havenos.launcher.ui.assist.parseAssistCommands
import androidx.compose.ui.text.style.TextOverflow
import app.havenos.launcher.ui.glass.CURATED_ACCENTS
import app.havenos.launcher.ui.glass.GlassCard
import app.havenos.launcher.ui.toLibraryApp
import app.havenos.launcher.ui.glass.GlassLevel
import app.havenos.launcher.ui.glass.GroupedSection
import app.havenos.launcher.ui.glass.HavenBanner
import app.havenos.launcher.ui.glass.HavenIconTile
import app.havenos.launcher.ui.glass.HavenNavRow
import app.havenos.launcher.ui.glass.HavenRadii
import app.havenos.launcher.ui.glass.HavenSegmentedRow
import app.havenos.launcher.ui.glass.HavenSliderRow
import app.havenos.launcher.ui.glass.HavenSpacing
import app.havenos.launcher.ui.glass.HavenSwitchRow
import app.havenos.launcher.ui.glass.HavenType
import app.havenos.launcher.ui.settings.hubs.ApiHubScreen
import app.havenos.launcher.ui.settings.hubs.ApiSearchEntries
import app.havenos.launcher.ui.settings.hubs.DeviceDataHubScreen
import app.havenos.launcher.ui.settings.hubs.DeviceDataEntries
import app.havenos.launcher.ui.settings.hubs.DrawerSearchHubScreen
import app.havenos.launcher.ui.settings.hubs.DrawerSearchEntries
import app.havenos.launcher.ui.settings.hubs.GesturesMotionHubScreen
import app.havenos.launcher.ui.settings.hubs.GesturesMotionEntries
import app.havenos.launcher.ui.settings.hubs.HomeDockHubScreen
import app.havenos.launcher.ui.settings.hubs.HomeDockSearchEntries
import app.havenos.launcher.ui.settings.hubs.LookHubScreen
import app.havenos.launcher.ui.settings.hubs.LookSearchEntries
import app.havenos.launcher.ui.settings.hubs.MascotHubScreen
import app.havenos.launcher.ui.settings.hubs.MascotSearchEntries
import app.havenos.launcher.ui.settings.hubs.PrivacyHubScreen
import app.havenos.launcher.ui.settings.hubs.PrivacySearchEntries
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// M6 integration: hub screen (v2 §5, adapted) + registry.
// Adaptations to the v2 doc, per the user's later directions (see
// docs/ASSUMPTIONS.md): NO preset carousel (Minimal + accent only, no
// Look presets anywhere); NO pages (the scroll home stays in both snap
// modes); the dock is always the floating pill, so the Quick-tune dock
// control is position (Bottom / Left / Right), labelled honestly, and
// there is no Attached / Floating switch. The footer reads "Changed
// this session (N)" — there is no preset left to be changed from.
// ---------------------------------------------------------------------------

/** Draft layer the Home host provides; hubs preview through it (see Home.kt). */
val LocalDraftLayer = staticCompositionLocalOf<DraftLayer?> { null }

/** Pushes a hub route; set by the host once the panel state exists. */
class HubNavigator {
    var push: ((String) -> Unit)? = null
    fun open(route: String) {
        push?.invoke(route)
    }
}

/** One hub row on the hub screen: route, display title, icon (never a letter tile). */
data class HubRowDef(val route: String, val title: String, val icon: HavenIcon)

val HUB_ROWS: List<HubRowDef> = listOf(
    HubRowDef("look", "Look", HavenIcon.HubLook),
    HubRowDef("home-dock", "Home & Dock", HavenIcon.HubHomeDock),
    HubRowDef("drawer-search", "Drawer & Search", HavenIcon.HubDrawerSearch),
    HubRowDef("gestures-motion", "Gestures & Motion", HavenIcon.HubGesturesMotion),
    HubRowDef("privacy", "Privacy", HavenIcon.HubPrivacy),
    HubRowDef("device-data", "Device & Data", HavenIcon.HubDeviceData),
    HubRowDef("apis", "APIs", HavenIcon.HubApis),
    // v3 Track A: Mascot hub (opt-in companion; defaults off).
    HubRowDef("mascot", "Mascot", HavenIcon.HubMascot),
)

/** Hub route for a search entry, from its (normalized) hub display name. */
fun hubRouteForEntry(entry: SearchEntry): String = when (entry.hub) {
    "Look" -> "look"
    "Home & Dock" -> "home-dock"
    "Drawer & Search" -> "drawer-search"
    "Gestures & Motion" -> "gestures-motion"
    "Privacy" -> "privacy"
    "Device & Data" -> "device-data"
    "APIs" -> "apis"
    "Mascot" -> "mascot"
    else -> "home"
}

/** Every hub's SearchEntries, aggregated through the shell search index. */
val allHubSearchEntries: List<SearchEntry> = searchEntries(
    LookSearchEntries,
    HomeDockSearchEntries,
    DrawerSearchEntries,
    GesturesMotionEntries,
    PrivacySearchEntries,
    DeviceDataEntries,
    ApiSearchEntries,
    MascotSearchEntries,
)

/**
 * The filled registry: the hub screen plus all seven hubs (v2 M1 adds APIs). The Gestures
 * hub gets the host's real gesture dispatcher as its Test action, so
 * the Test button fires a real gesture instead of sitting dead.
 * Look gets the one panel Reset (its M5 category-1 reset, 20 fields);
 * the other hubs keep their own in-screen reset actions and get none
 * here, per the panel contract (at most one Reset per screen).
 */
fun createHubRegistry(
    app: HavenApp,
    session: PreviewSession,
    navigator: HubNavigator,
    onTestAction: (String) -> Unit,
    // 3.1 Phase F (F2/F3): the host's Calm entry, so a "calm" command
    // typed in panel search can open Calm like any other action.
    onEnterCalm: () -> Unit = {},
): HubScreenRegistry = HubScreenRegistry(
    mapOf(
        "home" to HubScreenDef(
            route = "home",
            title = "Settings",
            content = { _, onSpotlight ->
                HubScreen(
                    navigator = navigator,
                    session = session,
                    onSpotlight = onSpotlight,
                    onEnterCalm = onEnterCalm,
                )
            },
        ),
        "look" to HubScreenDef(
            route = "look",
            title = "Look",
            content = { onBack, onSpotlight -> LookHubScreen(onBack, onSpotlight) },
            reset = HubReset(
                affectedCount = 20,
                onReset = { resetNewCategory(app, 1) },
            ),
        ),
        "home-dock" to HubScreenDef(
            route = "home-dock",
            title = "Home & Dock",
            content = { onBack, onSpotlight -> HomeDockHubScreen(onBack, onSpotlight) },
        ),
        "drawer-search" to HubScreenDef(
            route = "drawer-search",
            title = "Drawer & Search",
            content = { onBack, onSpotlight -> DrawerSearchHubScreen(onBack, onSpotlight) },
        ),
        "gestures-motion" to HubScreenDef(
            route = "gestures-motion",
            title = "Gestures & Motion",
            content = { onBack, onSpotlight ->
                GesturesMotionHubScreen(onBack, onSpotlight, onTestAction)
            },
        ),
        "privacy" to HubScreenDef(
            route = "privacy",
            title = "Privacy",
            content = { onBack, onSpotlight -> PrivacyHubScreen(onBack, onSpotlight) },
        ),
        "device-data" to HubScreenDef(
            route = "device-data",
            title = "Device & Data",
            content = { onBack, onSpotlight -> DeviceDataHubScreen(onBack, onSpotlight) },
        ),
        // v2 M1: APIs hub. Its panel Reset restores the 7 toggles only;
        // keys/username are deliberately not wiped (see resetSection(10)).
        "apis" to HubScreenDef(
            route = "apis",
            title = "APIs",
            content = { onBack, onSpotlight -> ApiHubScreen(onBack, onSpotlight) },
            reset = HubReset(
                affectedCount = 7,
                onReset = { app.settings.resetSection(10) },
            ),
        ),
        // v3 Track A: Mascot hub. No panel Reset: the hub's own
        // "Reset mascot tips & tutorials" row owns mascot resets.
        "mascot" to HubScreenDef(
            route = "mascot",
            title = "Mascot",
            content = { onBack, onSpotlight -> MascotHubScreen(onBack, onSpotlight) },
        ),
    ),
)

/** True when HavenOS currently holds the default-home role. */
fun isDefaultLauncher(context: Context): Boolean {
    val pkg = context.packageName
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            context.getSystemService(RoleManager::class.java)
                ?.isRoleHeld(RoleManager.ROLE_HOME) == true
        } catch (t: Throwable) {
            false
        }
    } else {
        try {
            context.packageManager.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                0,
            )?.activityInfo?.packageName == pkg
        } catch (t: Throwable) {
            false
        }
    }
}

// Backup/import failure flag (local only): set by the Device & Data hub
// when an import fails, read by the hub screen's needs-attention banner,
// cleared when the user opens the hub from the banner or an import
// later succeeds. There is no persistent failure record anywhere else,
// which is why the banner's third priority is wired through this flag.
private const val ATTENTION_PREFS = "havenos_shell_attention"
private const val KEY_BACKUP_FAILED = "backup_failed"

fun backupFailedFlag(context: Context): Boolean =
    context.getSharedPreferences(ATTENTION_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_BACKUP_FAILED, false)

fun setBackupFailedFlag(context: Context, failed: Boolean) {
    context.getSharedPreferences(ATTENTION_PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_BACKUP_FAILED, failed).apply()
}

private fun fmtMinutes(mins: Int): String =
    "%02d:%02d".format(mins / 60, mins % 60)

/**
 * The hub screen itself: search field, at most one needs-attention
 * banner (priority: not default → missing navigation permission →
 * gestures paused → backup/import failed), the Minimal hero card with its accent swatch
 * strip, Quick tune, recent hub chips (local only, up to 4), the six
 * hub rows with live one-line summaries, and the Changes footer.
 */
@Composable
fun HubScreen(
    navigator: HubNavigator,
    session: PreviewSession,
    onSpotlight: (String) -> Unit,
    onEnterCalm: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as HavenApp
    val settings = app.settings
    val state by settings.state.collectAsState()
    val scope = rememberCoroutineScope()
    val tokens = LocalShellTokens.current
    val palette = LocalPalette.current
    val recentStore = rememberRecentSettingsStore()
    var recentVersion by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var changesOpen by remember { mutableStateOf(false) }
    val recentQueries = rememberRecentSearchQueries()

    fun openHub(route: String, title: String) {
        recentStore.record(title)
        recentVersion++
        navigator.open(route)
    }

    // 3.1 Phase F (F2/F3): panel-search assist commands. The panel
    // session is active here, so setting actions applied through the
    // typed setters land in Before/Changes/Undo automatically; the row
    // stays on the results so Changes stays visible. App launches use
    // the system launcher intent; a hub command navigates, except the
    // root ("Settings") which is already on screen.
    val panelApps by app.appRepo.apps.collectAsState()
    val panelStats by app.appRepo.stats.collectAsState()
    fun launchAssistApp(pkg: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) context.startActivity(intent)
        } catch (t: Throwable) {
            // No launch intent for this package; row does nothing.
        }
    }
    fun executeAssistCommands(commands: List<AssistCommand>) {
        recentQueries.record(query)
        scope.launch {
            settings.recordAssistUse(AssistFeatureId.SEARCH_COMMANDS, System.currentTimeMillis())
        }
        for (cmd in commands) {
            when (val action = cmd.action) {
                is AssistAction.OpenApp -> launchAssistApp(action.pkg)
                is AssistAction.OpenHub -> {
                    if (action.route != "home") {
                        onSpotlight(action.route)
                        openHub(action.route, action.title)
                    }
                }
                is AssistAction.ToggleCalm -> onEnterCalm()
                else -> scope.launch { applyAssistSettingAction(settings, action) }
            }
        }
    }

    var defaultTick by remember { mutableStateOf(0) }
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { defaultTick++ }
    fun requestDefaultRole() {
        var launched = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val rm = context.getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    launched = true
                }
            } catch (t: Throwable) {
                launched = false
            }
        }
        if (!launched) {
            try {
                context.startActivity(
                    Intent(AndroidSettings.ACTION_HOME_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (t: Throwable) {
                // No home-settings screen on this device; row state unchanged.
            }
        }
    }

    ShellScreenColumn(route = "home") {
        SettingsSearchField(query = query, onQueryChange = { query = it })
        if (query.isNotBlank()) {
            val results = searchSettingsEntries(query, allHubSearchEntries)
            // 3.1 Phase F (F2): command rows above the settings hits;
            // null means "not a command" and changes nothing.
            val assistCommands = remember(query, panelApps, panelStats, state) {
                parseAssistCommands(
                    query,
                    AssistContext(
                        apps = panelApps.map { it.toLibraryApp() },
                        stats = panelStats,
                        darkMode = state.darkMode,
                        effectiveIconDp = effectiveIconDp(state.iconSizeOverride, state.density),
                        curatedNames = CURATED_ACCENTS.map { it.name },
                        commandsEnabled = state.assistSearchCommands,
                    ),
                )
            }
            if (assistCommands != null && assistCommands.isNotEmpty()) {
                ShellSectionLabel(text = "Assist commands")
                GlassCard(shape = HavenRadii.groupShape(), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { executeAssistCommands(assistCommands) }
                            .padding(HavenSpacing.s16),
                        verticalArrangement = Arrangement.spacedBy(ShellSpacing.s8),
                    ) {
                        Text(
                            text = assistCommands.joinToString(" · ") { it.label },
                            style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                            color = tokens.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Tap to apply",
                            style = ShellText.help.copy(fontFamily = LocalFont.current),
                            color = tokens.accent,
                        )
                    }
                }
            }
            if (results.isEmpty()) {
                val hub = closestHub(query, allHubSearchEntries)
                Text(
                    text = "No settings match",
                    style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                    color = tokens.textPrimary,
                )
                if (hub != null) {
                    Text(
                        text = "Closest match: $hub",
                        style = ShellText.value.copy(fontFamily = LocalFont.current),
                        color = tokens.textSecondary,
                    )
                    val first = allHubSearchEntries.firstOrNull { it.hub == hub }
                    if (first != null) {
                        TextButton(onClick = {
                            recentQueries.record(query)
                            onSpotlight(first.route)
                            openHub(hubRouteForEntry(first), hub)
                        }) {
                            Text(
                                text = "Open $hub",
                                style = ShellText.help.copy(fontFamily = LocalFont.current),
                                color = tokens.accent,
                            )
                        }
                    }
                }
            } else {
                val settingsResults = results.filter { !it.isAction }
                val actionResults = results.filter { it.isAction }
                if (settingsResults.isNotEmpty()) {
                    ShellSectionLabel(text = "Settings")
                    for (entry in settingsResults) {
                        SearchResultRow(
                            entry = entry,
                            onOpenEntry = {
                                recentQueries.record(query)
                                onSpotlight(entry.route)
                                openHub(hubRouteForEntry(entry), entry.hub)
                            },
                        )
                    }
                }
                if (actionResults.isNotEmpty()) {
                    ShellSectionLabel(text = "Actions")
                    for (entry in actionResults) {
                        SearchResultRow(
                            entry = entry,
                            onOpenEntry = {
                                recentQueries.record(query)
                                onSpotlight(entry.route)
                                openHub(hubRouteForEntry(entry), entry.hub)
                            },
                        )
                    }
                }
            }
            return@ShellScreenColumn
        }

        // Needs-attention banner: at most one, fixed priority.
        val isDefault = remember(defaultTick) { isDefaultLauncher(context) }
        val navPermissionMissing = state.navGesturesOn && !isNavServiceEnabled(context)
        // 3.1 A13: permission granted but the service itself is not
        // connected (system stopped it). Refreshed on resume, same
        // pattern as the gestures hub's navTick, so returning from
        // the system screen clears the banner without extra taps.
        var navTick by remember { mutableIntStateOf(0) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { navTick++ }
        val gesturesPaused = remember(state.navGesturesOn, navTick) {
            gestureServicePaused(
                state.navGesturesOn,
                isNavServiceEnabled(context),
                HavenAccessibilityService.isConnected,
            )
        }
        val backupFailed = backupFailedFlag(context)
        when {
            !isDefault -> {
                HavenBanner(
                    icon = HavenIcon.Home,
                    text = "HavenOS is not the default launcher.\nHome-button presses still open the old launcher.",
                    actionLabel = "Make default",
                    onAction = { requestDefaultRole() },
                )
            }
            navPermissionMissing -> {
                HavenBanner(
                    icon = HavenIcon.Accessibility,
                    text = "Navigation gestures need accessibility access.",
                    actionLabel = "Allow",
                    onAction = {
                        try {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        } catch (t: Throwable) {
                            // System screen unavailable; row state unchanged.
                        }
                    },
                )
            }
            gesturesPaused -> {
                HavenBanner(
                    icon = HavenIcon.Accessibility,
                    text = "Gestures are paused.\nThe system stopped the gesture service. Reconnect it to restore edge swipes.",
                    actionLabel = "Reconnect",
                    onAction = {
                        try {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        } catch (t: Throwable) {
                            // System screen unavailable; row state unchanged.
                        }
                    },
                )
            }
            backupFailed -> {
                HavenBanner(
                    icon = HavenIcon.Info,
                    text = "The last backup import failed. Nothing changed.",
                    actionLabel = "View in Device & Data",
                    onAction = {
                        setBackupFailedFlag(context, false)
                        openHub("device-data", "Device & Data")
                    },
                )
            }
        }

        // Coordination fix §6: one Changes banner under the
        // needs-attention banner. It carries the count (the footer
        // count row below is deleted), Review, and Revert session.
        if (session.changes.isNotEmpty()) {
            GlassCard(level = GlassLevel.GLASS1, shape = HavenRadii.groupShape()) {
                Row(
                    Modifier.fillMaxWidth().padding(HavenSpacing.s16),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${session.changes.size} changes",
                        style = HavenType.rowLabel.copy(fontFamily = LocalFont.current),
                        color = palette.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { changesOpen = true }) { Text("Review") }
                    TextButton(onClick = { scope.launch { session.revertSession() } }) {
                        Text("Revert session")
                    }
                }
            }
        }

        // Look hero: a live miniature of the current look, built from
        // tokens only (accent, mode, material, icon shape, grid) — not
        // a scaled-down copy of the real home. No carousel: there are
        // no presets to cycle through.
        val modeName = when (state.darkMode) {
            1 -> "Light"
            2 -> "Dark"
            3 -> "Auto"
            else -> "System"
        }
        val materialName = when (state.surfaceMaterial) {
            0 -> "Solid"
            1 -> "Translucent"
            else -> "Frosted"
        }
        MiniLookPreview(
            modeName = modeName,
            materialName = materialName,
            gridText = if (state.colsPort < 0) "Follows density" else "${state.colsPort} × ${state.rowsPort}",
            iconShapeExt = state.iconShapeExt,
            onClick = { openHub("look", "Look") },
        )

        // Quick tune: the controls people reach for most, writing the
        // same keys as their hubs (icon size, grid, mode, labels, dock
        // position), as ONE grouped section. The dock control is
        // position only — the dock is always the floating pill, so
        // there is no Attached/Floating switch to offer. Mode lists
        // System explicitly: HavenSegmented always shows a selection,
        // so the old "no selection" state for System would lie.
        val isAutoIcon = state.iconSizeOverride < 0
        val shownIcon = if (isAutoIcon) state.iconDp().toFloat() else state.iconSizeOverride.toFloat()
        val gridPort = state.colsPort to state.rowsPort
        val gridSelected = when (gridPort) {
            -1 to -1 -> 0
            4 to 6 -> 1
            5 to 6 -> 2
            else -> 3
        }
        GroupedSection(
            label = "Quick tune",
            rows = listOf(
                {
                    HavenSliderRow(
                        label = "Icon size",
                        value = shownIcon,
                        valueRange = 40f..68f,
                        valueText = { if (isAutoIcon) "Auto" else "${it.roundToInt()} dp" },
                        onCommit = { scope.launch { settings.setIconSizeOverride(it.roundToInt().coerceIn(40, 68)) } },
                        autoActive = isAutoIcon,
                        onAutoClick = { scope.launch { settings.setIconSizeOverride(-1) } },
                        autoLabel = "Auto",
                    )
                },
                {
                    HavenSegmentedRow(
                        label = "Grid",
                        options = listOf("Density", "4 × 6", "5 × 6", "Custom"),
                        selected = gridSelected,
                        onSelect = { index ->
                            when (index) {
                                0 -> scope.launch {
                                    settings.clearGridOverride(false)
                                    settings.clearGridOverride(true)
                                }
                                // M10 (BUG-030): quick tune used to write the
                                // PORTRAIT override only, so on a landscape
                                // tablet tapping 4x6/5x6 changed nothing
                                // visible. A quick-tune grid is global: it
                                // writes both orientations (the per-orientation
                                // picker in Home & Dock still splits them).
                                1 -> scope.launch {
                                    settings.setGridOverride(false, 4, 6)
                                    settings.setGridOverride(true, 4, 6)
                                }
                                2 -> scope.launch {
                                    settings.setGridOverride(false, 5, 6)
                                    settings.setGridOverride(true, 5, 6)
                                }
                                else -> openHub("home-dock", "Home & Dock")
                            }
                        },
                    )
                },
                {
                    HavenSegmentedRow(
                        label = "Mode",
                        options = listOf("Light", "Dark", "Auto", "System"),
                        selected = when (state.darkMode) {
                            1 -> 0
                            2 -> 1
                            3 -> 2
                            else -> 3
                        },
                        onSelect = { index ->
                            scope.launch { settings.setDarkMode(if (index == 3) 0 else index + 1) }
                        },
                    )
                },
                {
                    HavenSwitchRow(
                        label = "Labels",
                        checked = state.labelsOn,
                        onChange = { on ->
                            scope.launch { settings.setLabels(on, state.labelSize) }
                        },
                    )
                },
                {
                    HavenSegmentedRow(
                        label = "Dock position",
                        options = listOf("Bottom", "Left", "Right"),
                        selected = state.dockPos.coerceIn(0, 2),
                        onSelect = { index ->
                            scope.launch { settings.setDockPos(index) }
                        },
                    )
                },
            ),
        )

        // Recent hub chips, local only, up to 4.
        val recent by remember(recentVersion) { mutableStateOf(recentStore.recent()) }
        if (recent.isNotEmpty()) {
            ShellSectionLabel(text = "Recent")
            RecentChips(
                recent = recent,
                onChipClick = { label ->
                    val route = HUB_ROWS.firstOrNull { it.title == label }?.route ?: "home"
                    openHub(route, label)
                },
                onClear = {
                    recentStore.clear()
                    recentVersion++
                },
            )
        }

        // Hub rows as ONE grouped section: icon tile, title + live
        // one-line summary + chevron (HavenNavRow).
        val hiddenCount = remember(state.hiddenJson) { parseMembers(state.hiddenJson).size }
        val boundGestures = remember(state.gesturesJson) {
            parseGestures(state.gesturesJson).count { it.value != "none" }
        }
        val profileCount = remember(state.profilesJson) { parseProfiles(state.profilesJson).size }
        val searchOnCount = listOf(
            state.searchContacts, state.searchCalendar, state.searchSettings, state.searchCalc,
        ).count { it }
        val summaries = mapOf(
            "look" to "$modeName · $materialName",
            "home-dock" to "Dock: ${dockPosName(state.dockPos)} · ${state.dockMax} slots · labels ${if (state.labelsOn) "on" else "off"}",
            "drawer-search" to "Sort: ${drawerSortName(state.drawerSort)} · $searchOnCount search sources on",
            "gestures-motion" to "Navigation ${if (state.navGesturesOn) "on" else "off"} · $boundGestures gestures bound",
            "privacy" to "$hiddenCount hidden apps · layout ${if (state.lockLayout) "locked" else "unlocked"}",
            "device-data" to "$profileCount profiles · backup ${if (state.backupIncludeWp) "with" else "without"} wallpaper",
        )
        GroupedSection(
            label = "All settings",
            rows = HUB_ROWS.map { hub ->
                {
                    HavenNavRow(
                        label = hub.title,
                        value = summaries[hub.route] ?: "",
                        icon = hub.icon,
                        onClick = {
                            onSpotlight(hub.route)
                            openHub(hub.route, hub.title)
                        },
                    )
                }
            },
        )

        Spacer(Modifier.height(ShellSpacing.s16))
    }
    if (changesOpen) {
        ChangesDialog(session = session, onDismiss = { changesOpen = false })
    }
}

/**
 * 3.1 Phase C: the hub-screen hero as a live miniature of the current
 * look, built from tokens only — a mini grid in the current icon
 * shape (one accent tile), a dock pill, and the mode/material/grid
 * line. Never a scaled-down copy of the real home.
 */
@Composable
private fun MiniLookPreview(
    modeName: String,
    materialName: String,
    gridText: String,
    iconShapeExt: Int,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val roles = LocalAccentRoles.current
    val tileShape = when (iconShapeExt) {
        0 -> RoundedCornerShape(4.dp)
        2 -> RoundedCornerShape(16.dp)
        3 -> RoundedCornerShape(percent = 50)
        4 -> RoundedCornerShape(topStart = 9.dp, topEnd = 2.dp, bottomEnd = 9.dp, bottomStart = 2.dp)
        else -> RoundedCornerShape(12.dp)
    }
    GlassCard(
        shape = HavenRadii.cardShape(),
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(HavenSpacing.s16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(18.dp).clip(tileShape).background(palette.onSurface.copy(alpha = 0.15f)))
                    Box(Modifier.size(18.dp).clip(tileShape).background(Color(roles.accent)))
                    Box(Modifier.size(18.dp).clip(tileShape).background(palette.onSurface.copy(alpha = 0.15f)))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(18.dp).clip(tileShape).background(palette.onSurface.copy(alpha = 0.15f)))
                    Box(Modifier.size(18.dp).clip(tileShape).background(palette.onSurface.copy(alpha = 0.15f)))
                    Box(Modifier.size(18.dp).clip(tileShape).background(palette.onSurface.copy(alpha = 0.15f)))
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.size(62.dp, 12.dp).clip(RoundedCornerShape(percent = 50))
                        .background(Color(roles.accentContainer)),
                )
            }
            Spacer(Modifier.size(HavenSpacing.s16))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Minimal",
                    style = HavenType.title.copy(fontFamily = LocalFont.current),
                    color = palette.onSurface,
                )
                Text(
                    text = "$modeName · $materialName · $gridText",
                    style = HavenType.caption.copy(fontFamily = LocalFont.current),
                    color = palette.muted,
                )
            }
            HavenIconTile(HavenIcon.HubLook, null)
        }
    }
}

private fun dockPosName(v: Int): String = when (v) {
    1 -> "Left"
    2 -> "Right"
    else -> "Bottom"
}

private fun drawerSortName(v: Int): String = when (v) {
    1 -> "Most used"
    2 -> "Recently installed"
    else -> "A–Z"
}
