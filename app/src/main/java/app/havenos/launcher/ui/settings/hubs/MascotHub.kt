package app.havenos.launcher.ui.settings.hubs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.HavenApp
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.ui.assist.ModelPackManager
import app.havenos.launcher.ui.assist.ModelPackStatus
import app.havenos.launcher.ui.mascot.MascotCharacter
import app.havenos.launcher.ui.mascot.MascotExpression
import app.havenos.launcher.ui.mascot.MascotFigure
import app.havenos.launcher.ui.mascot.PixelSprite
import app.havenos.launcher.ui.mascot.USE_PIXEL_MASCOT
import app.havenos.launcher.ui.mascot.rememberSpriteSet
import app.havenos.launcher.ui.mascot.spriteScaleFor
import app.havenos.launcher.ui.mascot.targetDpForMascotSize
import app.havenos.launcher.ui.settings.shell.DeepLinkAnchor
import app.havenos.launcher.ui.settings.shell.SearchEntry
import app.havenos.launcher.ui.settings.shell.ShellRow
import app.havenos.launcher.ui.settings.shell.ShellSectionLabel
import app.havenos.launcher.ui.settings.shell.ShellSegmentedControl
import app.havenos.launcher.ui.settings.shell.ShellSwitchRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Mascot hub (v3 Track A). Built with the EXACT existing shell components
// (ShellSwitchRow / ShellSegmentedControl / ShellSliderRow / ShellRow /
// ShellSectionLabel) and the existing hub pattern (panel title bar + scroll
// column). No other Settings screen is touched. The live preview reuses
// the same MascotFigure the home layer draws — idle + happy, static.
// ---------------------------------------------------------------------------

val MascotSearchEntries: List<SearchEntry> = listOf(
    SearchEntry("Mascot", listOf("mascot", "companion", "milo", "mila"), "Mascot", "mascot"),
    SearchEntry("Mascot character", listOf("milo", "mila", "character"), "Mascot", "mascot"),
    SearchEntry("Mascot roaming", listOf("roam", "roaming", "wander"), "Mascot", "mascot"),
    SearchEntry("Mascot Find", listOf("find", "search"), "Mascot", "mascot"),
    SearchEntry("Mascot quiet hours", listOf("quiet", "quiet hours"), "Mascot", "mascot"),
    SearchEntry("Reset mascot", listOf("reset", "mascot"), "Mascot", "mascot"),
    SearchEntry("Chat model", listOf("chat", "model", "llm"), "Mascot", "mascot"),
)

private fun fmtHour(hour: Int): String = "%02d:00".format(hour)

private fun fmtMinutes(min: Int): String = "%02d:%02d".format(min / 60, min % 60)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MascotHubScreen(onBack: () -> Unit, onSpotlight: (String) -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HavenApp
    val settings = app.settings
    val state by settings.state.collectAsState()
    val scope = rememberCoroutineScope()
    val palette = LocalPalette.current
    val scroll = rememberScrollState()
    val character = if (state.mascotCharacter == 1) MascotCharacter.MILA else MascotCharacter.MILO

    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            ShellSectionLabel("Companion")
            // Preview: the pixel sprite animating in a preview box
            // (vector figure when the pixel set is unavailable).
            // Preview plays idle → walk → jump → wave once, then
            // rests on idle.
            val previewSet = rememberSpriteSet(character, palette.accent.toArgb())
            var previewAnim by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(previewAnim) {
                if (previewAnim != null) {
                    delay(1500)
                    previewAnim = "walk"
                    delay(1500)
                    previewAnim = "jump"
                    delay(1500)
                    previewAnim = "wave"
                    delay(1500)
                    previewAnim = null
                }
            }
            Box(
                Modifier.fillMaxWidth().height(120.dp)
                    .background(palette.surface, RoundedCornerShape(16.dp))
                    .border(1.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (USE_PIXEL_MASCOT && previewSet != null) {
                    PixelSprite(
                        previewSet,
                        previewAnim ?: "idle",
                        spriteScaleFor(
                            targetDpForMascotSize(state.mascotSize),
                            LocalDensity.current.density,
                        ),
                    )
                } else {
                    MascotFigure(
                        character = character,
                        expression = MascotExpression.IDLE,
                        size = targetDpForMascotSize(state.mascotSize).dp,
                        body = palette.onSurface,
                        eye = palette.surface,
                        accent = palette.accent,
                    )
                }
            }
            TextButton(
                onClick = { previewAnim = "idle" },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("Preview", color = palette.accent, fontSize = 14.sp) }
            DeepLinkAnchor(field = "mascot") {
                ShellSwitchRow(
                    title = "Mascot",
                    checked = state.mascotOn,
                    onCheckedChange = { scope.launch { settings.setMascotOn(it) } },
                    help = "Opt-in companion on the home screen only. Default off. Never floats over other apps.",
                )
            }
            ShellSectionLabel("Character")
            ShellSegmentedControl(
                options = listOf("Milo", "Mila"),
                selected = state.mascotCharacter,
                onSelect = { scope.launch { settings.setMascotCharacter(it) } },
            )
            Spacer(Modifier.height(8.dp))
            ShellSwitchRow(
                title = "Roaming",
                checked = state.mascotRoaming,
                onCheckedChange = { scope.launch { settings.setMascotRoaming(it) } },
                help = "Wanders along ledges on home. Off parks it on the dock top.",
            )
            ShellSectionLabel("Size")
            ShellSegmentedControl(
                options = listOf("Small", "Medium", "Large"),
                selected = state.mascotSize,
                onSelect = { scope.launch { settings.setMascotSize(it) } },
            )
            Spacer(Modifier.height(8.dp))
            DeepLinkAnchor(field = "find") {
                ShellSwitchRow(
                    title = "Find",
                    checked = state.mascotFindOn,
                    onCheckedChange = { scope.launch { settings.setMascotFindOn(it) } },
                    help = "Lets the mascot locate apps, folders, and settings and point to them.",
                )
            }
            ShellSectionLabel("Tips & suggestions")
            ShellSegmentedControl(
                options = listOf("Off", "Rare", "Normal"),
                selected = state.mascotTips,
                onSelect = { scope.launch { settings.setMascotTips(it) } },
            )
            Spacer(Modifier.height(8.dp))
            var quietStartPicker by remember { mutableStateOf(false) }
            var quietEndPicker by remember { mutableStateOf(false) }
            DeepLinkAnchor(field = "quiet") {
                ShellRow(
                    title = "Quiet hours start",
                    value = fmtMinutes(state.mascotQuietStart),
                    help = "The mascot sleeps during quiet hours and never nudges then. Default 22:00–07:00.",
                    onClick = { quietStartPicker = true },
                )
            }
            ShellRow(
                title = "Quiet hours end",
                value = fmtMinutes(state.mascotQuietEnd),
                help = "End of quiet hours. Default 22:00–07:00.",
                onClick = { quietEndPicker = true },
            )
            if (quietStartPicker) {
                val pickerState = rememberTimePickerState(
                    initialHour = state.mascotQuietStart / 60,
                    initialMinute = state.mascotQuietStart % 60,
                    is24Hour = true,
                )
                AlertDialog(
                    onDismissRequest = { quietStartPicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                settings.setMascotQuiet(
                                    pickerState.hour * 60 + pickerState.minute,
                                    state.mascotQuietEnd,
                                )
                            }
                            quietStartPicker = false
                        }) { Text("Set", color = palette.accent, fontSize = 14.sp) }
                    },
                    dismissButton = {
                        TextButton(onClick = { quietStartPicker = false }) {
                            Text("Cancel", color = palette.onSurface, fontSize = 14.sp)
                        }
                    },
                    text = { TimePicker(state = pickerState) },
                )
            }
            if (quietEndPicker) {
                val pickerState = rememberTimePickerState(
                    initialHour = state.mascotQuietEnd / 60,
                    initialMinute = state.mascotQuietEnd % 60,
                    is24Hour = true,
                )
                AlertDialog(
                    onDismissRequest = { quietEndPicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                settings.setMascotQuiet(
                                    state.mascotQuietStart,
                                    pickerState.hour * 60 + pickerState.minute,
                                )
                            }
                            quietEndPicker = false
                        }) { Text("Set", color = palette.accent, fontSize = 14.sp) }
                    },
                    dismissButton = {
                        TextButton(onClick = { quietEndPicker = false }) {
                            Text("Cancel", color = palette.onSurface, fontSize = 14.sp)
                        }
                    },
                    text = { TimePicker(state = pickerState) },
                )
            }
            ShellSwitchRow(
                title = "Reduce motion (mascot)",
                checked = state.mascotReduceMotion,
                onCheckedChange = { scope.launch { settings.setMascotReduceMotion(it) } },
                help = "Replaces mascot motion with static poses and teleport moves. The system setting is honored as well.",
            )
            ShellSectionLabel("Chat model (Phase 3)")
            // 3.1 Phase F (F5): the row shows the REAL pack state from
            // the app-private model-pack dir. No model is bundled and
            // the URL/checksum placeholders are still empty, so today
            // this reads "Not set up" and the scripted brain answers.
            // There is deliberately no download button that does
            // nothing; when a pack exists, Delete appears here.
            val packManager = remember(ctx) { ModelPackManager(ctx.filesDir) }
            var packInfo by remember { mutableStateOf(packManager.packState()) }
            ShellRow(
                title = "Chat model",
                value = when (packInfo.status) {
                    ModelPackStatus.NOT_SET_UP -> "Not set up"
                    ModelPackStatus.DOWNLOADING -> "Downloading ${(packInfo.progress * 100).toInt()}%"
                    ModelPackStatus.READY -> "Ready"
                    ModelPackStatus.FAILED -> "Failed: ${packInfo.error ?: "unknown error"}"
                },
                help = "Free-form chat stays scripted until an on-device model is configured. No model is downloaded, no data leaves the device. The model URL and checksum are still setup gaps. A future pack downloads on request only, over Wi-Fi, with checksum verification, and can be deleted here.",
                trailing = if (packInfo.status == ModelPackStatus.READY) {
                    {
                        TextButton(onClick = {
                            scope.launch {
                                packManager.delete()
                                packInfo = packManager.packState()
                            }
                        }) { Text("Delete", color = palette.accent, fontSize = 14.sp) }
                    }
                } else {
                    null
                },
            )
            ShellSectionLabel("Reset")
            DeepLinkAnchor(field = "reset") {
                ShellRow(
                    title = "Reset mascot",
                    help = "Clears dismissed tours and muted suggestions so they can be offered again. Mascot on/off, character, and size are untouched.",
                    onClick = { scope.launch { settings.resetMascotTips() } },
                )
            }
            if (state.mascotDismissed) {
                ShellRow(
                    title = "Bring back mascot",
                    help = "The mascot was dismissed. This clears the dismiss so it shows on home again (when Mascot is on).",
                    onClick = { scope.launch { settings.setMascotDismissed(false) } },
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
