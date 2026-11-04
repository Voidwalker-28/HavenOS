package app.havenos.launcher.ui.settings.hubs

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.havenos.launcher.HavenApp
import app.havenos.launcher.HavenDeviceAdminReceiver
import app.havenos.launcher.theme.LocalAccentRoles
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.ui.glass.HavenBanner
import app.havenos.launcher.ui.lastCrashShareText
import app.havenos.launcher.ui.readFeatureErrors
import app.havenos.launcher.ui.glass.HavenDialog
import app.havenos.launcher.ui.glass.HavenHelp
import app.havenos.launcher.ui.glass.HavenIcon
import app.havenos.launcher.ui.glass.HavenIconTile
import app.havenos.launcher.ui.glass.HavenSwitch
import app.havenos.launcher.ui.glass.vector
import app.havenos.launcher.data.applyBackupJson
import app.havenos.launcher.data.network.ApiDiagnosticsRow
import app.havenos.launcher.data.applyLayoutSnapshot
import app.havenos.launcher.data.backupSummary
import app.havenos.launcher.data.buildBackupJson
import app.havenos.launcher.data.layoutSnapshotJson
import app.havenos.launcher.hideBarGranted
import app.havenos.launcher.isNavServiceEnabled
import app.havenos.launcher.syncNavBarPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import app.havenos.launcher.ui.settings.shell.SearchEntry
import app.havenos.launcher.ui.settings.shell.setBackupFailedFlag
import app.havenos.launcher.data.Profile
import app.havenos.launcher.data.captureProfile
import app.havenos.launcher.data.encodeProfiles
import app.havenos.launcher.data.parseProfiles
import app.havenos.launcher.data.switchProfile

// ---------------------------------------------------------------------------
// Device & Data hub (M6). Only wired controls:
//  - Default launcher: live RoleManager / resolver check, real chooser.
//  - Permissions: every Allow/Manage chip deep-links to the real system
//    screen (runtime prompt for calendar/contacts, system screens for the
//    rest). Sensitive permissions (accessibility, device admin,
//    WRITE_SECURE_SETTINGS) are explained and never auto-requested: their
//    chips only open the system screen. Location is deliberately NOT a
//    row: weather uses a typed city and location is never requested.
//  - Backup: system file picker both ways; import counts changed settings
//    BEFORE applying; an invalid file changes nothing. Import snapshots
//    the pre-import layout into undoJson (also written inside
//    applyBackupJson) and the result banner offers Undo that restores
//    that layout via applyLayoutSnapshot. Undo restores layout/dock and
//    the snapshot's few settings — it is NOT a full settings rollback,
//    and the copy never claims otherwise.
//  - "Hide navigation bar everywhere" lives ONLY here (Advanced), never
//    in Gestures. Its switch stays disabled until the ADB grant exists.
// Not re-added: saved looks / presets (deleted by user direction),
// Replay intro/tutorial (placeholder actions, removed per audit).
// SearchEntry is defined once in the shell
// (ui/settings/shell/SettingsSearch.kt); this hub only lists its rows.
// ---------------------------------------------------------------------------

val DeviceDataEntries: List<SearchEntry> = listOf(
    SearchEntry("Default launcher", listOf("default", "home", "launcher", "role", "make", "default"), "Device & Data", "defaultLauncher"),
    SearchEntry("Calendar permission", listOf("calendar", "permission", "events", "access"), "Device & Data", "permCalendar"),
    SearchEntry("Contacts permission", listOf("contacts", "permission", "access", "people"), "Device & Data", "permContacts"),
    SearchEntry("Notification access", listOf("notification", "access", "badges", "listener"), "Device & Data", "permNotif"),
    SearchEntry("Usage access", listOf("usage", "access", "stats", "suggestions"), "Device & Data", "permUsage"),
    SearchEntry("Accessibility", listOf("accessibility", "service", "navigation", "gestures"), "Device & Data", "permA11y"),
    SearchEntry("Draw over apps", listOf("overlay", "draw", "over", "other", "apps", "gesture", "strips"), "Device & Data", "permOverlay"),
    SearchEntry("Device admin", listOf("device", "admin", "lock", "screen", "off"), "Device & Data", "permAdmin"),
    SearchEntry("Export backup", listOf("backup", "export", "save", "file", "restore"), "Device & Data", "backupExport"),
    SearchEntry("Import backup", listOf("backup", "import", "restore", "file"), "Device & Data", "backupImport"),
    SearchEntry("Hide navigation bar everywhere", listOf("hide", "navigation", "bar", "buttons", "immersive", "adb"), "Device & Data", "navHideBar"),
    SearchEntry("Diagnostics", listOf("diagnostics", "device", "info", "copy", "report"), "Device & Data", "diagnostics"),
    SearchEntry("Developer options", listOf("developer", "advanced", "adb", "command", "secure", "settings"), "Device & Data", "developer"),
    SearchEntry("Licenses", listOf("licenses", "credits", "open", "source", "apache"), "Device & Data", "licenses"),
    SearchEntry("Version", listOf("version", "package", "about", "build"), "Device & Data", "version"),
)

private const val NAV_GRANT_COMMAND =
    "adb shell pm grant app.havenos.launcher android.permission.WRITE_SECURE_SETTINGS"

/** Settings keys counted for "N settings will change" (Cat13 definition). */
private fun countBackupChanged(json: String, state: app.havenos.launcher.data.SettingsState): Int? {
    val root = try {
        JSONObject(json)
    } catch (t: Throwable) {
        return null
    }
    if (root.optString("type") != "havenos-backup") return null
    val s = root.optJSONObject("settings") ?: return null
    val current = mapOf(
        "darkMode" to state.darkMode.toString(), "density" to state.density.toString(),
        "iconShape" to state.iconShape.toString(),
        "iconSizeOverride" to state.iconSizeOverride.toString(),
        "labelsOn" to state.labelsOn.toString(), "labelSize" to state.labelSize.toString(),
        "fontChoice" to state.fontChoice.toString(),
        "reduceEffects" to state.reduceEffects.toString(),
        "reduceMotion" to state.reduceMotion.toString(),
        "lockLayout" to state.lockLayout.toString(),
        "dockMax" to state.dockMax.toString(), "dockPos" to state.dockPos.toString(),
        "drawerMode" to state.drawerMode.toString(),
        "drawerSort" to state.drawerSort.toString(),
        "hiddenJson" to state.hiddenJson, "hiddenLock" to state.hiddenLock.toString(),
        "weatherCity" to state.weatherCity, "weatherUnits" to state.weatherUnits.toString(),
        "weatherNetwork" to state.weatherNetwork.toString(),
        "iconPack" to state.iconPack, "monoIcons" to state.monoIcons.toString(),
        "badgeMode" to state.badgeMode.toString(),
        "gesturesJson" to state.gesturesJson,
        "navGesturesOn" to state.navGesturesOn.toString(),
        "navMapJson" to state.navMapJson, "navEdgeDp" to state.navEdgeDp.toString(),
        "navSensitivity" to state.navSensitivity.toString(),
        "navHaptics" to state.navHaptics.toString(), "navHideBar" to state.navHideBar.toString(),
        "searchContacts" to state.searchContacts.toString(),
        "searchCalendar" to state.searchCalendar.toString(),
        "searchSettings" to state.searchSettings.toString(),
        "searchCalc" to state.searchCalc.toString(),
        "wallpaperMode" to state.wallpaperMode.toString(),
        "wpKind" to state.wpKind.toString(), "wpUri" to state.wpUri,
        "wpDim" to state.wpDim.toString(), "wpZoom" to state.wpZoom.toString(),
        "wpTint" to state.wpTint.toString(), "wpSolid" to state.wpSolid.toString(),
    )
    var changed = 0
    for (key in s.keys()) {
        val known = current[key] ?: continue
        if (s.opt(key).toString() != known) changed++
    }
    return changed
}

private data class PermStatus(
    val home: Boolean,
    val calendar: Boolean,
    val contacts: Boolean,
    val notif: Boolean,
    val usage: Boolean,
    val a11y: Boolean,
    val overlay: Boolean,
    val admin: Boolean,
    val hideBar: Boolean,
)

@Composable
fun DeviceDataHubScreen(onBack: () -> Unit, onSpotlight: (String) -> Unit) {
    val context = LocalContext.current
    val app = HavenApp.of(context)
    val settings = app.settings
    val state by settings.state.collectAsState()
    val scope = rememberCoroutineScope()

    // Live permission state, re-read on every return from system screens.
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val status = remember(tick, context) {
        val pkg = context.packageName
        val isHome = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                context.getSystemService(RoleManager::class.java)
                    ?.isRoleHeld(RoleManager.ROLE_HOME) == true
            } catch (t: Throwable) {
                false
            }
        } else {
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                context.packageManager.resolveActivity(homeIntent, 0)
                    ?.activityInfo?.packageName == pkg
            } catch (t: Throwable) {
                false
            }
        }
        val usage = try {
            val ops = context.getSystemService(AppOpsManager::class.java)
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ops?.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), pkg,
                )
            } else {
                @Suppress("DEPRECATION")
                ops?.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), pkg)
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (t: Throwable) {
            false
        }
        val admin = try {
            val dpm = context.getSystemService(android.app.admin.DevicePolicyManager::class.java)
            dpm?.isAdminActive(ComponentName(context, HavenDeviceAdminReceiver::class.java)) == true
        } catch (t: Throwable) {
            false
        }
        PermStatus(
            home = isHome,
            calendar = context.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED,
            contacts = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED,
            notif = try {
                NotificationManagerCompat.getEnabledListenerPackages(context).contains(pkg)
            } catch (t: Throwable) {
                false
            },
            usage = usage,
            a11y = isNavServiceEnabled(context),
            overlay = AndroidSettings.canDrawOverlays(context),
            admin = admin,
            hideBar = hideBarGranted(context),
        )
    }

    fun startSystem(intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (t: Throwable) {
            // System screen unavailable on this device; row state is unchanged.
        }
    }
    fun openAppInfo() = startSystem(
        Intent(
            AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + context.packageName),
        ),
    )
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { tick++ }
    fun requestHomeRole() {
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
        if (!launched) startSystem(Intent(AndroidSettings.ACTION_HOME_SETTINGS))
    }

    // Runtime permissions: asked only when the user taps Allow (point of
    // use), after a one-sentence reason. A denial keeps the row visible
    // with its reason and a Settings chip to App info.
    var deniedCalendar by remember { mutableStateOf(false) }
    var deniedContacts by remember { mutableStateOf(false) }
    var rationaleFor by remember { mutableStateOf<String?>(null) }
    val calLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) tick++ else deniedCalendar = true
    }
    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) tick++ else deniedContacts = true
    }

    // Backup state.
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var importText by remember { mutableStateOf<String?>(null) }
    var importSummary by remember { mutableStateOf<String?>(null) }
    var importChanged by remember { mutableStateOf<Int?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var resultMsg by remember { mutableStateOf<String?>(null) }
    var showImportUndo by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = result.data?.data
        val content = pendingExport
        pendingExport = null
        if (result.resultCode == Activity.RESULT_OK && uri != null && content != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(content.toByteArray(Charsets.UTF_8))
                        }
                        resultMsg = "Backup exported."
                    } catch (t: Throwable) {
                        resultMsg = "Export failed — the file could not be written."
                    }
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    } catch (t: Throwable) {
                        null
                    }
                }
                val summary = text?.let { backupSummary(it) }
                val changed = text?.let { countBackupChanged(it, state) }
                if (text == null || summary == null || changed == null) {
                    setBackupFailedFlag(context, true)
                    importError = "Not a HavenOS backup. Nothing changed."
                } else {
                    importText = text
                    importSummary = summary
                    importChanged = changed
                }
            }
        }
    }
    fun undoLastImport() {
        scope.launch {
            val snap = settings.persistedState().undoJson
            if (snap.isBlank()) {
                resultMsg = "Nothing to undo."
                return@launch
            }
            applyLayoutSnapshot(app, snap)
            settings.setUndoJson("")
            syncNavBarPolicy(context, settings)
            showImportUndo = false
            resultMsg = "Previous layout restored."
        }
    }

    var showMore by remember { mutableStateOf(false) }
    // Profiles state (hoisted out of the showMore block: remember calls
    // must not appear or disappear with a toggle inside the Column).
    val profilesList = remember(state.profilesJson) { parseProfiles(state.profilesJson) }
    var newProfileName by remember { mutableStateOf("") }
    var profileMsg by remember { mutableStateOf<String?>(null) }
    var showAdvanced by remember { mutableStateOf(false) }
    // 3.1 Phase C: inline help expansions replaced by HavenHelp popovers; no helpFor state.
    var licensesOpen by remember { mutableStateOf(false) }
    var copiedWhat by remember { mutableStateOf<String?>(null) }

    val pkgInfo = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (t: Throwable) {
            null
        }
    }
    val versionText = if (pkgInfo != null) {
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION") pkgInfo.versionCode.toLong()
        }
        "${pkgInfo.versionName} ($code)"
    } else {
        "HavenOS"
    }
    val diagnostics = remember(context, state) {
        val dm = context.resources.displayMetrics
        listOf(
            "Device: ${Build.MANUFACTURER} ${Build.MODEL}",
            "Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            "Screen: ${dm.widthPixels} × ${dm.heightPixels} px · ${dm.densityDpi} dpi",
            "Look: Minimal · Density: ${state.density}",
            "Version: $versionText · ${context.packageName}",
        ).joinToString("\n")
    }
    val autoBackupText = if (state.lastBackupDay > 0) {
        val date = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(state.lastBackupDay * 86_400_000L))
        "Daily · last $date"
    } else {
        "Daily · never yet"
    }
    val navBarReason = when {
        !status.hideBar -> "Needs ADB grant below."
        !state.navGesturesOn -> "Turn navigation gestures on first."
        !status.a11y -> "Needs accessibility service on."
        !status.overlay -> "Needs draw-over-apps permission."
        else -> null
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {

        // Default launcher banner form only while NOT default.
        if (!status.home) {
            HavenBanner(
                icon = HavenIcon.Home,
                text = "Default launcher\nHavenOS is not the default launcher.",
                actionLabel = "Make default",
                onAction = {
                    onSpotlight("defaultLauncher")
                    requestHomeRole()
                },
            )
        } else {
            DeviceValueRow(title = "Default launcher", value = "HavenOS")
        }

        Spacer(Modifier.height(16.dp))
        DeviceSectionLabel("Permissions")
        // Compact rows: icon + name + purpose (≤6 words) + dot + text + chip.
        DevicePermRow(
            icon = HavenIcon.PermCalendar, title = "Calendar", purpose = "Shows events in widgets, search",
            granted = status.calendar,
            help = null,
            chipLabel = if (status.calendar) "Manage" else if (deniedCalendar) "Settings" else "Allow",
            onChip = {
                if (status.calendar || deniedCalendar) openAppInfo()
                else rationaleFor = "calendar"
            },
        )
        DevicePermRow(
            icon = HavenIcon.PermContacts, title = "Contacts", purpose = "Finds contacts in drawer search",
            granted = status.contacts,
            help = null,
            chipLabel = if (status.contacts) "Manage" else if (deniedContacts) "Settings" else "Allow",
            onChip = {
                if (status.contacts || deniedContacts) openAppInfo()
                else rationaleFor = "contacts"
            },
        )
        DevicePermRow(
            icon = HavenIcon.PermNotifications, title = "Notification access", purpose = "Powers badges, Now Playing",
            granted = status.notif,
            help = null,
            chipLabel = if (status.notif) "Manage" else "Allow",
            onChip = {
                startSystem(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
        )
        DevicePermRow(
            icon = HavenIcon.PermUsage, title = "Usage access", purpose = "Powers optional app suggestions",
            granted = status.usage,
            help = null,
            chipLabel = if (status.usage) "Manage" else "Allow",
            onChip = { startSystem(Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS)) },
        )
        DevicePermRow(
            icon = HavenIcon.PermAccessibility, title = "Accessibility", purpose = "Powers navigation edge gestures",
            granted = status.a11y,
            help = "System screen only — never enabled automatically.",
            chipLabel = if (status.a11y) "Manage" else "Allow",
            onChip = { startSystem(Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)) },
        )
        DevicePermRow(
            icon = HavenIcon.PermOverlay, title = "Draw over apps", purpose = "Lets gesture strips overlay apps",
            granted = status.overlay,
            help = null,
            chipLabel = if (status.overlay) "Manage" else "Allow",
            onChip = {
                startSystem(
                    Intent(
                        AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.packageName),
                    ),
                )
            },
        )
        DevicePermRow(
            icon = HavenIcon.PermDeviceAdmin, title = "Device admin", purpose = "Lets lock turn screen off",
            granted = status.admin,
            help = "System screen only — never enabled automatically.",
            chipLabel = if (status.admin) "Manage" else "Allow",
            onChip = {
                if (status.admin) {
                    openAppInfo()
                } else {
                    startSystem(
                        Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                            .putExtra(
                                android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                ComponentName(context, HavenDeviceAdminReceiver::class.java),
                            ),
                    )
                }
            },
        )
        if (deniedCalendar || deniedContacts) {
            Text(
                "Denied — allow in App info.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (resultMsg != null) {
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp),
                ) {
                    Text(resultMsg!!, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    if (showImportUndo) {
                        TextButton(onClick = { undoLastImport() }) { Text("Undo") }
                    }
                    TextButton(onClick = {
                        resultMsg = null
                        showImportUndo = false
                    }) { Text("Dismiss") }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { showMore = !showMore }) {
            Text(if (showMore) "Fewer options" else "More options")
        }
        if (showMore) {
            DeviceSectionLabel("Backup")
            DeviceActionRow(
                title = "Export backup", value = "File",
                actionLabel = "Export",
                onAction = {
                    scope.launch {
                        pendingExport = buildBackupJson(app, state, state.backupIncludeWp)
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("application/json")
                            .putExtra(Intent.EXTRA_TITLE, "havenos-backup.json")
                        try {
                            exportLauncher.launch(intent)
                        } catch (t: Throwable) {
                            pendingExport = null
                            resultMsg = "No file picker is available on this device."
                        }
                    }
                },
            )
            DeviceActionRow(
                title = "Import backup", value = "File",
                actionLabel = "Import",
                onAction = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/json")
                    try {
                        importLauncher.launch(intent)
                    } catch (t: Throwable) {
                        importError = "No file picker is available on this device."
                    }
                },
            )
            if (state.undoJson.isNotBlank() && !showImportUndo) {
                DeviceActionRow(
                    title = "Undo last import", value = "Layout",
                    actionLabel = "Undo",
                    onAction = { undoLastImport() },
                )
            }
            DeviceSwitchRow(
                title = "Include wallpapers",
                value = if (state.backupIncludeWp) "On" else "Off",
                checked = state.backupIncludeWp,
                enabled = true,
                reason = null,
                help = "Daily backups include wallpaper choice.",
                onChange = { scope.launch { settings.setBackupIncludeWp(it) } },
            )
            DeviceValueRow(title = "Auto backup", value = autoBackupText)

            // Profiles (M6 integration): the feature worked but had no
            // hub UI after the M5 categories were deleted — it lives
            // here, next to Backup, not dropped. Switching auto-saves
            // the profile being left (switchProfile); the schedule row
            // below only shows when a schedule is actually set.
            Spacer(Modifier.height(16.dp))
            DeviceSectionLabel("Profiles")
            val profiles = profilesList
            if (state.profileSchedule) {
                DeviceValueRow(
                    title = "Schedule",
                    value = "%02d:%02d–%02d:%02d → ${state.schedProfile}".format(
                        state.schedStart / 60, state.schedStart % 60,
                        state.schedEnd / 60, state.schedEnd % 60,
                    ),
                )
            }
            for (profile in profiles) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(profile.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        if (profile.name == state.activeProfile) {
                            Text("Active", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (profile.name != state.activeProfile) {
                        TextButton(onClick = {
                            scope.launch {
                                switchProfile(app, state, profile.name)
                                profileMsg = "Switched to ${profile.name}."
                            }
                        }) { Text("Switch") }
                    }
                    TextButton(onClick = {
                        val base = "${profile.name} copy"
                        var candidate = base
                        var n = 2
                        while (profiles.any { it.name == candidate }) {
                            candidate = "$base $n"
                            n++
                        }
                        scope.launch {
                            settings.setProfilesJson(
                                encodeProfiles(profiles + Profile(candidate, profile.snapshotJson)),
                            )
                            profileMsg = "Duplicated as $candidate."
                        }
                    }) { Text("Duplicate") }
                    TextButton(onClick = {
                        val next = profiles.filterNot { it.name == profile.name }
                        scope.launch {
                            settings.setProfilesJson(encodeProfiles(next))
                            if (profile.name == state.activeProfile) {
                                settings.setActiveProfile("Default")
                            }
                            profileMsg = "Deleted ${profile.name}."
                        }
                    }) { Text("Delete") }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                TextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    placeholder = { Text("New profile name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    val name = newProfileName.trim()
                    when {
                        name.isBlank() -> profileMsg = "Enter a name first."
                        profiles.any { it.name == name } -> profileMsg = "A profile with that name already exists."
                        else -> {
                            scope.launch {
                                val saved = captureProfile(app, state, name)
                                settings.setProfilesJson(encodeProfiles(profiles + saved))
                                newProfileName = ""
                                profileMsg = "Saved $name."
                            }
                        }
                    }
                }) { Text("Save current") }
            }
            if (profileMsg != null) {
                Text(profileMsg!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(16.dp))
            DeviceSectionLabel("About")
            DeviceValueRow(title = "Version", value = versionText)
            DeviceActionRow(
                title = "Licenses", value = "",
                actionLabel = "View",
                onAction = { licensesOpen = true },
            )
            DeviceActionRow(
                title = "App info", value = "",
                actionLabel = "Manage",
                onAction = { openAppInfo() },
            )

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) "Hide Advanced" else "Advanced")
            }
            if (showAdvanced) {
                DeviceSectionLabel("Diagnostics")
                Text(diagnostics, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                TextButton(onClick = {
                    try {
                        val cm = context.getSystemService(ClipboardManager::class.java)
                        cm?.setPrimaryClip(
                            ClipData.newPlainText("HavenOS diagnostics", diagnostics),
                        )
                        copiedWhat = "Diagnostics"
                    } catch (t: Throwable) {
                        copiedWhat = null
                    }
                }) { Text("Copy") }
                if (copiedWhat == "Diagnostics") {
                    Text("Copied.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // 3.1.1 (item 3): last crash, local only — class,
                // message, time, versionCode, with user-triggered
                // Copy/Share (nothing is uploaded automatically).
                // Feature errors (item 2) live in their own section
                // below; this is only truly uncaught crashes.
                DeviceSectionLabel("Last crash")
                val lastCrash = remember { app.readLastCrash() }
                if (lastCrash == null) {
                    Text(
                        "No crash recorded.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "${lastCrash.clazz}\n${lastCrash.message}\n" +
                            "${lastCrash.timeText} · versionCode ${lastCrash.versionCode}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Row {
                        TextButton(onClick = {
                            try {
                                val cm = context.getSystemService(ClipboardManager::class.java)
                                cm?.setPrimaryClip(
                                    ClipData.newPlainText(
                                        "HavenOS last crash",
                                        lastCrashShareText(app),
                                    ),
                                )
                                copiedWhat = "Last crash"
                            } catch (t: Throwable) {
                                copiedWhat = null
                            }
                        }) { Text("Copy") }
                        TextButton(onClick = {
                            try {
                                val shareText = lastCrashShareText(app) ?: return@TextButton
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(
                                    Intent.createChooser(intent, "Share last crash")
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            } catch (t: Throwable) {
                            }
                        }) { Text("Share") }
                    }
                    if (copiedWhat == "Last crash") {
                        Text("Copied.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // 3.1.1 (item 2): feature errors — soft failures that
                // disabled one feature for the session (never crashes).
                DeviceSectionLabel("Feature errors")
                val featureErrors = remember { readFeatureErrors(context) }
                if (featureErrors.isEmpty()) {
                    Text(
                        "No feature errors recorded.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    for (err in featureErrors) {
                        Text(
                            "${err.feature}: ${err.clazz} · ${err.message}",
                            fontSize = 13.sp,
                        )
                        if (err.frames.isNotBlank()) {
                            Text(
                                err.frames,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // 3.1 A2: per-API last-result rows — class, HTTP
                // code, time, and the first 200 chars of the body
                // for parse errors only (the store never records a
                // snippet otherwise, and keys travel in headers, so
                // no key/token/personal data can appear here).
                // Loaded when Advanced opens; a row exists only
                // after that API attempted a refresh.
                var apiDiags by remember { mutableStateOf<List<ApiDiagnosticsRow>>(emptyList()) }
                val diagTimeFmt = remember {
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                }
                LaunchedEffect(Unit) {
                    apiDiags = try {
                        app.apiStore.allDiagnostics()
                    } catch (t: Throwable) {
                        emptyList()
                    }
                }
                Spacer(Modifier.height(16.dp))
                DeviceSectionLabel("API diagnostics")
                if (apiDiags.isEmpty()) {
                    Text(
                        "No API results recorded yet.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    for (row in apiDiags) {
                        Text(
                            row.api + ": " + row.resultClass +
                                (if (row.httpCode >= 0) " · HTTP " + row.httpCode else "") +
                                " · " + diagTimeFmt.format(java.util.Date(row.timeMs)),
                            fontSize = 13.sp,
                        )
                        if (row.bodySnippet.isNotBlank()) {
                            Text(
                                row.bodySnippet,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                DeviceSectionLabel("Developer options")
                Text(
                    "For advanced users",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DeviceSwitchRow(
                    // Named verbatim per spec; the one row over the
                    // 3-word title guideline in this hub.
                    title = "Hide navigation bar everywhere",
                    value = if (status.hideBar) {
                        if (state.navHideBar) "On" else "Off"
                    } else {
                        "Not granted"
                    },
                    checked = state.navHideBar,
                    enabled = status.hideBar,
                    reason = navBarReason,
                    help = "Hides the button bar in every app.",
                    onChange = {
                        onSpotlight("navHideBar")
                        scope.launch {
                            settings.setNavHideBar(it)
                            syncNavBarPolicy(context, settings)
                        }
                    },
                )
                Text(
                    NAV_GRANT_COMMAND,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                TextButton(onClick = {
                    try {
                        val cm = context.getSystemService(ClipboardManager::class.java)
                        cm?.setPrimaryClip(
                            ClipData.newPlainText("Hide navigation bar command", NAV_GRANT_COMMAND),
                        )
                        copiedWhat = "Command"
                    } catch (t: Throwable) {
                        copiedWhat = null
                    }
                }) { Text("Copy command") }
                if (copiedWhat == "Command") {
                    Text("Copied.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "Run once from a computer with ADB.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // One-sentence reason, then the real system prompt — point of use only.
    if (rationaleFor != null) {
        val isCalendar = rationaleFor == "calendar"
        HavenDialog(
            onDismiss = { rationaleFor = null },
            title = "Allow this permission?",
            body = if (isCalendar) {
                "HavenOS shows your events in widgets and search."
            } else {
                "HavenOS finds contacts from drawer search."
            },
            dismissLabel = "Not now",
            confirmLabel = "Allow",
            onConfirm = {
                rationaleFor = null
                if (isCalendar) calLauncher.launch(Manifest.permission.READ_CALENDAR)
                else contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
        )
    }

    // Import confirm: count shown BEFORE anything is applied.
    if (importSummary != null && importText != null) {
        HavenDialog(
            onDismiss = {
                importSummary = null; importText = null; importChanged = null
            },
            title = "Import backup?",
            body = "${importChanged ?: 0} ${if (importChanged == 1) "setting" else "settings"} " +
                "will change. " + importSummary!!,
            dismissLabel = "Cancel",
            confirmLabel = "Apply",
            onConfirm = {
                val text = importText!!
                importSummary = null; importText = null; importChanged = null
                scope.launch {
                    // Snapshot the pre-import layout first; the same
                    // write also happens inside applyBackupJson.
                    val items = app.db.homeItems().all()
                    val folders = app.db.folders().all()
                    settings.setUndoJson(layoutSnapshotJson(items, folders, state))
                    val ok = applyBackupJson(app, text)
                    if (ok) syncNavBarPolicy(context, settings)
                    setBackupFailedFlag(context, !ok)
                    showImportUndo = ok
                    resultMsg = if (ok) {
                        "Backup applied."
                    } else {
                        "Backup could not be applied."
                    }
                }
            },
        )
    }
    if (importError != null) {
        HavenDialog(
            onDismiss = { importError = null },
            title = "Import backup",
            body = importError!!,
            dismissLabel = "OK",
        )
    }
    if (licensesOpen) {
        HavenDialog(
            onDismiss = { licensesOpen = false },
            title = "Open-source licenses",
            body = "AndroidX Compose, Room and DataStore — Apache License 2.0\n" +
                "Kotlin — Apache License 2.0\n" +
                "Weather data by Open-Meteo.com — CC BY 4.0",
            dismissLabel = "Close",
        )
    }
}

@Composable
private fun DeviceSectionLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun DeviceValueRow(title: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp),
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DeviceActionRow(
    title: String,
    value: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 2.dp),
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (value.isNotBlank()) {
            Text(
                value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

/** Permission row: icon tile + name + purpose + status chip + Manage action. */
@Composable
private fun DevicePermRow(
    icon: HavenIcon,
    title: String,
    purpose: String,
    granted: Boolean,
    help: String?,
    chipLabel: String,
    onChip: () -> Unit,
) {
    val roles = LocalAccentRoles.current
    val palette = LocalPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 2.dp),
    ) {
        HavenIconTile(icon, null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (help != null) HavenHelp(helpText = help)
            }
            Text(purpose, fontSize = 13.sp, color = palette.muted)
        }
        Box(
            Modifier.clip(RoundedCornerShape(percent = 50))
                .background(
                    if (granted) Color(roles.accentContainer)
                    else palette.onSurface.copy(alpha = 0.08f),
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                if (granted) "Allowed" else "Not allowed",
                fontSize = 12.sp,
                color = if (granted) Color(roles.accent) else palette.muted,
            )
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onChip) { Text(chipLabel) }
    }
}

@Composable
private fun DeviceSwitchRow(
    title: String,
    value: String,
    checked: Boolean,
    enabled: Boolean,
    reason: String?,
    help: String?,
    onChange: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 56.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    if (help != null) HavenHelp(helpText = help)
                }
                if (reason != null) {
                    Text(reason, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
            HavenSwitch(checked = checked, enabled = enabled, onChange = onChange)
        }
    }
}
