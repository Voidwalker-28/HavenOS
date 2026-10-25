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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.iconDp
import app.havenos.launcher.data.parseGestures
import app.havenos.launcher.data.parseMembers
import app.havenos.launcher.data.parseProfiles
import app.havenos.launcher.isNavServiceEnabled
import app.havenos.launcher.theme.LocalFont
import app.havenos.launcher.theme.LocalPalette
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

/** One hub row on the hub screen: route, display title, glyph (never a letter tile). */
data class HubRowDef(val route: String, val title: String, val glyph: String)

val HUB_ROWS: List<HubRowDef> = listOf(
    HubRowDef("look", "Look", "🎨"),
    HubRowDef("home-dock", "Home & Dock", "🏠"),
    HubRowDef("drawer-search", "Drawer & Search", "🔍"),
    HubRowDef("gestures-motion", "Gestures & Motion", "👆"),
    HubRowDef("privacy", "Privacy", "🔒"),
    HubRowDef("device-data", "Device & Data", "💾"),
)

/** Hub route for a search entry, from its (normalized) hub display name. */
fun hubRouteForEntry(entry: SearchEntry): String = when (entry.hub) {
    "Look" -> "look"
    "Home & Dock" -> "home-dock"
    "Drawer & Search" -> "drawer-search"
    "Gestures & Motion" -> "gestures-motion"
    "Privacy" -> "privacy"
    "Device & Data" -> "device-data"
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
)

/**
 * The filled registry: the hub screen plus all six hubs. The Gestures
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
): HubScreenRegistry = HubScreenRegistry(
    mapOf(
        "home" to HubScreenDef(
            route = "home",
            title = "Settings",
            content = { _, onSpotlight ->
                HubScreen(navigator = navigator, session = session, onSpotlight = onSpotlight)
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
 * backup/import failed), the Minimal hero card with its accent swatch
 * strip, Quick tune, recent hub chips (local only, up to 4), the six
 * hub rows with live one-line summaries, and the Changes footer.
 */
@Composable
fun HubScreen(
    navigator: HubNavigator,
    session: PreviewSession,
    onSpotlight: (String) -> Unit,
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
            if (results.isEmpty()) {
                val hub = closestHub(query, allHubSearchEntries)
                Text(
                    text = "No results",
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
        val backupFailed = backupFailedFlag(context)
        when {
            !isDefault -> {
                ShellCard {
                    Column {
                        Text(
                            text = "HavenOS is not the default launcher.",
                            style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                            color = tokens.textPrimary,
                        )
                        Text(
                            text = "Home-button presses still open the old launcher.",
                            style = ShellText.help.copy(fontFamily = LocalFont.current),
                            color = tokens.textSecondary,
                        )
                        TextButton(onClick = { requestDefaultRole() }) {
                            Text(
                                text = "Make default",
                                style = ShellText.help.copy(fontFamily = LocalFont.current),
                                color = tokens.accent,
                            )
                        }
                    }
                }
            }
            navPermissionMissing -> {
                ShellCard {
                    Column {
                        Text(
                            text = "Navigation gestures need accessibility access.",
                            style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                            color = tokens.textPrimary,
                        )
                        TextButton(onClick = {
                            try {
                                context.startActivity(
                                    Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            } catch (t: Throwable) {
                                // System screen unavailable; row state unchanged.
                            }
                        }) {
                            Text(
                                text = "Allow",
                                style = ShellText.help.copy(fontFamily = LocalFont.current),
                                color = tokens.accent,
                            )
                        }
                    }
                }
            }
            backupFailed -> {
                ShellCard {
                    Column {
                        Text(
                            text = "The last backup import failed. Nothing changed.",
                            style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                            color = tokens.textPrimary,
                        )
                        TextButton(onClick = {
                            setBackupFailedFlag(context, false)
                            openHub("device-data", "Device & Data")
                        }) {
                            Text(
                                text = "View in Device & Data",
                                style = ShellText.help.copy(fontFamily = LocalFont.current),
                                color = tokens.accent,
                            )
                        }
                    }
                }
            }
        }

        // Look hero card: Minimal + accent swatch strip. No carousel:
        // there are no presets to cycle through.
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
        ShellCard(onClick = { openHub("look", "Look") }) {
            Column {
                Text(
                    text = "Minimal",
                    style = ShellText.title.copy(fontFamily = LocalFont.current),
                    color = tokens.textPrimary,
                )
                Text(
                    text = "$modeName · $materialName",
                    style = ShellText.help.copy(fontFamily = LocalFont.current),
                    color = tokens.textSecondary,
                )
                Spacer(Modifier.height(ShellSpacing.s12))
                Row(horizontalArrangement = Arrangement.spacedBy(ShellSpacing.s8)) {
                    for (dot in listOf(palette.accent, palette.surface, palette.onSurface)) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape)
                                .background(dot),
                        )
                    }
                }
            }
        }

        // Quick tune: the controls people reach for most, writing the
        // same keys as their hubs (icon size, grid, mode, labels, dock
        // position). The dock control is position only — the dock is
        // always the floating pill, so there is no Attached/Floating
        // switch to offer.
        ShellSectionLabel(text = "Quick tune")
        val isAutoIcon = state.iconSizeOverride < 0
        val shownIcon = if (isAutoIcon) state.iconDp().toFloat() else state.iconSizeOverride.toFloat()
        ShellSliderRow(
            title = "Icon size",
            value = shownIcon,
            range = 40f..68f,
            valueText = { if (isAutoIcon) "Auto" else "${it.roundToInt()} dp" },
            autoLabel = "Auto",
            onAuto = { scope.launch { settings.setIconSizeOverride(-1) } },
            onPreview = {},
            onCommit = { scope.launch { settings.setIconSizeOverride(it.roundToInt().coerceIn(40, 68)) } },
        )
        val gridPort = state.colsPort to state.rowsPort
        val gridSelected = when (gridPort) {
            -1 to -1 -> 0
            4 to 6 -> 1
            5 to 6 -> 2
            else -> 3
        }
        ShellRow(title = "Grid", value = if (gridPort.first < 0) "Follows density" else "${gridPort.first} × ${gridPort.second}") {
            ShellSegmentedControl(
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
        }
        ShellRow(title = "Mode") {
            ShellSegmentedControl(
                options = listOf("Light", "Dark", "Auto"),
                selected = when (state.darkMode) {
                    1 -> 0
                    2 -> 1
                    3 -> 2
                    else -> -1
                },
                onSelect = { index ->
                    scope.launch { settings.setDarkMode(index + 1) }
                },
            )
        }
        ShellSwitchRow(
            title = "Labels",
            checked = state.labelsOn,
            onCheckedChange = { on ->
                scope.launch { settings.setLabels(on, state.labelSize) }
            },
        )
        ShellRow(title = "Dock position") {
            ShellSegmentedControl(
                options = listOf("Bottom", "Left", "Right"),
                selected = state.dockPos.coerceIn(0, 2),
                onSelect = { index ->
                    scope.launch { settings.setDockPos(index) }
                },
            )
        }

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

        // Six hub rows: glyph tile tinted with the real glyph (never a
        // letter tile), title + live one-line summary + chevron.
        ShellSectionLabel(text = "All settings")
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
        for (hub in HUB_ROWS) {
            ShellCard(onClick = {
                onSpotlight(hub.route)
                openHub(hub.route, hub.title)
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(ShellRadii.chip))
                            .background(tokens.accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = hub.glyph,
                            style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                            color = tokens.accent,
                        )
                    }
                    Spacer(Modifier.size(ShellSpacing.s12))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = hub.title,
                            style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                            color = tokens.textPrimary,
                        )
                        Text(
                            text = summaries[hub.route] ?: "",
                            style = ShellText.help.copy(fontFamily = LocalFont.current),
                            color = tokens.textSecondary,
                        )
                    }
                    Text(
                        text = "›",
                        style = ShellText.title.copy(fontFamily = LocalFont.current),
                        color = tokens.textSecondary,
                    )
                }
            }
        }

        // Footer: changed-this-session count opens the Changes list.
        // (Panel title bar also shows the same count as a chip.)
        ShellCard(onClick = { changesOpen = true }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Changed this session (${session.changes.size})",
                    style = ShellText.rowTitle.copy(fontFamily = LocalFont.current),
                    color = tokens.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "›",
                    style = ShellText.title.copy(fontFamily = LocalFont.current),
                    color = tokens.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(ShellSpacing.s16))
    }
    if (changesOpen) {
        ChangesDialog(session = session, onDismiss = { changesOpen = false })
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
