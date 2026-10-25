package app.havenos.launcher.ui.settings.hubs

import android.app.Activity
import android.app.KeyguardManager
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.encodeMembers
import app.havenos.launcher.data.hiddenApps
import app.havenos.launcher.data.parseMembers
import kotlinx.coroutines.launch
import app.havenos.launcher.ui.settings.shell.SearchEntry

// ---------------------------------------------------------------------------
// Privacy hub (M6). Only controls that change live behaviour are shown:
//  - hiddenJson: hidden apps vanish from home, drawer AND search together
//    (Drawer filters search results from the hidden-filtered list), so
//    there is deliberately NO separate hide-from-search switch — it would
//    drift out of sync with the one hidden list. The row copy says so.
//  - hiddenLock: the device-credential gate on the hidden list (the shipped
//    Cat12 behaviour). No androidx.biometric dependency exists in this
//    project, so "biometrics exist" is checked with the platform feature
//    flags (fingerprint / face / iris) plus a device credential being set;
//    the unlock prompt itself is the device-credential prompt.
//  - lockLayout: the one and only place for this switch in the new UI
//    (it was duplicated across two old categories).
// Not re-added here: saved looks / presets (deleted by user direction),
// Replay intro/tutorial (placeholder actions, removed per audit).
// SearchEntry is defined once in the shell
// (ui/settings/shell/SettingsSearch.kt); this hub only lists its rows.
// ---------------------------------------------------------------------------

val PrivacySearchEntries: List<SearchEntry> = listOf(
    SearchEntry("Hidden apps", listOf("hide", "hidden", "private", "secret", "apps"), "Privacy", "hiddenApps"),
    SearchEntry("Biometric lock", listOf("biometric", "lock", "hidden", "credential", "fingerprint", "face"), "Privacy", "biometricLock"),
    SearchEntry("Lock layout", listOf("lock", "layout", "prevent", "move", "edit"), "Privacy", "lockLayout"),
    SearchEntry("Unhide all", listOf("unhide", "all", "hidden", "restore", "show"), "Privacy", "unhideAll"),
)

@Composable
fun PrivacyHubScreen(onBack: () -> Unit, onSpotlight: (String) -> Unit) {
    val context = LocalContext.current
    val app = HavenApp.of(context)
    val settings = app.settings
    val state by settings.state.collectAsState()
    val apps by app.appRepo.apps.collectAsState()
    val scope = rememberCoroutineScope()

    val hiddenKeys = remember(state.hiddenJson) { parseMembers(state.hiddenJson) }
    val hiddenSet = remember(hiddenKeys) { hiddenKeys.toSet() }
    val appsByKey = remember(apps) { apps.associateBy { it.key } }

    var query by remember { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }

    // Device-credential gate on the hidden list.
    var unlocked by remember { mutableStateOf(!state.hiddenLock) }
    LaunchedEffect(state.hiddenLock) { unlocked = !state.hiddenLock }
    var testMsg by remember { mutableStateOf<String?>(null) }
    val credentialLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        unlocked = result.resultCode == Activity.RESULT_OK
        testMsg = if (result.resultCode == Activity.RESULT_OK) {
            "Confirmed."
        } else {
            "Not confirmed — the lock stays on."
        }
    }
    fun runCredentialPrompt() {
        val km = context.getSystemService(KeyguardManager::class.java)
        val intent = km?.createConfirmDeviceCredentialIntent(
            "Hidden apps", "Confirm it's you to view hidden apps",
        )
        if (intent != null) {
            try {
                credentialLauncher.launch(intent)
                testMsg = null
            } catch (t: Throwable) {
                testMsg = "Could not open the credential prompt on this device."
            }
        } else {
            testMsg = "No device credential is set — set a screen lock first."
        }
    }

    // Biometric availability, checked BEFORE the switch is ever enabled.
    // No androidx.biometric dependency: platform feature flags + secure lock.
    val biometricAvailable = remember(context) {
        val pm = context.packageManager
        val hasHardware = try {
            pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) ||
                pm.hasSystemFeature(PackageManager.FEATURE_FACE) ||
                pm.hasSystemFeature(PackageManager.FEATURE_IRIS)
        } catch (t: Throwable) {
            false
        }
        val secure = try {
            context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
        } catch (t: Throwable) {
            false
        }
        hasHardware && secure
    }
    val biometricReason = remember(context) {
        val pm = context.packageManager
        val hasHardware = try {
            pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) ||
                pm.hasSystemFeature(PackageManager.FEATURE_FACE) ||
                pm.hasSystemFeature(PackageManager.FEATURE_IRIS)
        } catch (t: Throwable) {
            false
        }
        when {
            !hasHardware -> "No biometric hardware on this device."
            else -> "Set a screen lock first."
        }
    }

    // One-line result summary + Undo for the last privacy change.
    var resultMsg by remember { mutableStateOf<String?>(null) }
    var undoHidden by remember { mutableStateOf<String?>(null) }
    var undoLayout by remember { mutableStateOf<Boolean?>(null) }
    fun recordChange(prevHidden: String, prevLayout: Boolean, msg: String) {
        undoHidden = prevHidden
        undoLayout = prevLayout
        resultMsg = msg
    }
    fun hiddenCountMsg(count: Int) =
        "$count ${if (count == 1) "app" else "apps"} hidden"

    var showMore by remember { mutableStateOf(false) }
    var helpFor by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        PrivacyHubHeader("Privacy", onBack)
        Spacer(Modifier.height(12.dp))

        // Hidden apps, with search. Blank query = the hidden list; a query
        // searches every installed app so an app can be hidden from here.
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search apps") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(searchFocus),
        )
        Spacer(Modifier.height(16.dp))

        PrivacySectionLabel("Hidden apps")
        if (query.isBlank()) {
            if (hiddenKeys.isEmpty()) {
                Text(
                    "Nothing hidden",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
                TextButton(onClick = { searchFocus.requestFocus() }) {
                    Text("Find app to hide")
                }
            } else if (state.hiddenLock && !unlocked) {
                PrivacyActionRow(
                    title = "Hidden apps locked",
                    value = "${hiddenKeys.size} hidden",
                    help = null,
                    helpOpen = false,
                    onHelp = {},
                    actionLabel = "Unlock",
                    onAction = { runCredentialPrompt() },
                )
            } else {
                for (key in hiddenKeys) {
                    val label = appsByKey[key]?.label ?: key
                    PrivacyActionRow(
                        title = label,
                        value = "Hidden",
                        help = null,
                        helpOpen = false,
                        onHelp = {},
                        actionLabel = "Unhide",
                        onAction = {
                            val prev = state.hiddenJson
                            val next = encodeMembers(hiddenKeys - key)
                            val count = (hiddenKeys - key).size
                            recordChange(prev, state.lockLayout, hiddenCountMsg(count))
                            onSpotlight("hiddenApps")
                            scope.launch { settings.setHiddenJson(next) }
                        },
                    )
                }
            }
            if (hiddenKeys.isNotEmpty()) {
                Text(
                    "Hidden apps also leave search.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val q = query.trim().lowercase()
            val matches = apps.filter { it.label.lowercase().contains(q) }
            if (matches.isEmpty()) {
                Text("No apps match.", fontSize = 15.sp)
            } else {
                for (entry in matches) {
                    val isHidden = entry.key in hiddenSet
                    PrivacyActionRow(
                        title = entry.label,
                        value = if (isHidden) "Hidden" else "Shown",
                        help = null,
                        helpOpen = false,
                        onHelp = {},
                        actionLabel = if (isHidden) "Unhide" else "Hide",
                        onAction = {
                            val prev = state.hiddenJson
                            val nextSet = if (isHidden) hiddenSet - entry.key else hiddenSet + entry.key
                            val next = encodeMembers(nextSet.toList())
                            recordChange(prev, state.lockLayout, hiddenCountMsg(nextSet.size))
                            onSpotlight("hiddenApps")
                            scope.launch { settings.setHiddenJson(next) }
                        },
                    )
                }
            }
        }

        if (resultMsg != null) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    resultMsg!!,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                if (undoHidden != null) {
                    TextButton(onClick = {
                        val restoreHidden = undoHidden!!
                        val restoreLayout = undoLayout ?: state.lockLayout
                        undoHidden = null
                        undoLayout = null
                        resultMsg = "Change undone."
                        scope.launch {
                            settings.setHiddenJson(restoreHidden)
                            settings.setLockLayout(restoreLayout)
                        }
                    }) { Text("Undo") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        PrivacySectionLabel("Lock")
        PrivacySwitchRow(
            title = "Biometric lock",
            value = if (state.hiddenLock) "On" else "Off",
            checked = state.hiddenLock,
            enabled = biometricAvailable,
            reason = if (biometricAvailable) null else biometricReason,
            help = "Needs device credential to view hidden apps.",
            helpOpen = helpFor == "biometricLock",
            onHelp = { helpFor = if (helpFor == "biometricLock") null else "biometricLock" },
            onChange = {
                onSpotlight("biometricLock")
                scope.launch { settings.setHiddenLock(it) }
            },
        )
        PrivacyActionRow(
            title = "Test lock",
            value = if (state.hiddenLock) "Locked" else "Off",
            help = "Runs the real device-credential prompt.",
            helpOpen = helpFor == "testLock",
            onHelp = { helpFor = if (helpFor == "testLock") null else "testLock" },
            actionLabel = "Test",
            onAction = { runCredentialPrompt() },
        )
        if (testMsg != null) {
            Text(testMsg!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PrivacySwitchRow(
            title = "Lock layout",
            value = if (state.lockLayout) "Locked" else "Unlocked",
            checked = state.lockLayout,
            enabled = true,
            reason = null,
            help = "Stops moving, adding or removing home items.",
            helpOpen = helpFor == "lockLayout",
            onHelp = { helpFor = if (helpFor == "lockLayout") null else "lockLayout" },
            onChange = {
                val prevHidden = state.hiddenJson
                val prevLayout = state.lockLayout
                recordChange(
                    prevHidden, prevLayout,
                    if (it) "Layout locked." else "Layout unlocked.",
                )
                onSpotlight("lockLayout")
                scope.launch { settings.setLockLayout(it) }
            },
        )

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { showMore = !showMore }) {
            Text(if (showMore) "Fewer options" else "More options")
        }
        if (showMore) {
            PrivacyActionRow(
                title = "Unhide all",
                value = hiddenCountMsg(state.hiddenApps().size),
                help = "Shows every hidden app again.",
                helpOpen = helpFor == "unhideAll",
                onHelp = { helpFor = if (helpFor == "unhideAll") null else "unhideAll" },
                actionLabel = "Unhide all",
                onAction = {
                    recordChange(state.hiddenJson, state.lockLayout, "0 apps hidden")
                    onSpotlight("hiddenApps")
                    scope.launch { settings.setHiddenJson("[]") }
                },
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PrivacyHubHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text("‹ Back") }
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PrivacySectionLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

/** Title + live value + optional ⓘ help (≤90 chars) + Switch. Never a dead switch. */
@Composable
private fun PrivacySwitchRow(
    title: String,
    value: String,
    checked: Boolean,
    enabled: Boolean,
    reason: String?,
    help: String?,
    helpOpen: Boolean,
    onHelp: () -> Unit,
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
                    if (help != null) {
                        TextButton(onClick = onHelp) { Text("ⓘ", fontSize = 14.sp) }
                    }
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
            Switch(checked = checked, enabled = enabled, onCheckedChange = onChange)
        }
        if (helpOpen && help != null) {
            Text(help, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Title + live value + one action button. */
@Composable
private fun PrivacyActionRow(
    title: String,
    value: String,
    help: String?,
    helpOpen: Boolean,
    onHelp: () -> Unit,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 56.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (help != null) {
                        TextButton(onClick = onHelp) { Text("ⓘ", fontSize = 14.sp) }
                    }
                }
            }
            Text(
                value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp),
            )
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
        if (helpOpen && help != null) {
            Text(help, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
