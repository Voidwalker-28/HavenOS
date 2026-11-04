package app.havenos.launcher.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.HavenApp
import app.havenos.launcher.theme.LocalPalette

/**
 * 3.1.1 (item 3): Safe Mode banner. Rendered only while HavenApp
 * reports Safe Mode; sits above home content, below the drawer and
 * Settings overlays (those are composed after the home shell). The
 * banner text is exactly the brief's wording. Re-enable lifts the
 * runtime overrides — stored settings were never changed, so it
 * restores exactly what was on. View details shows the last crash,
 * local only, with user-triggered Copy/Share (no automatic upload).
 */
@Composable
fun SafeModeBanner(app: HavenApp) {
    val safeMode by app.safeMode.collectAsState()
    if (!safeMode) return
    val palette = LocalPalette.current
    val context = LocalContext.current
    var showDetails by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .background(palette.surface, RoundedCornerShape(16.dp))
                .border(1.dp, palette.accent, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                "HavenOS recovered from a crash. Mascot turned off",
                color = palette.onSurface,
                fontSize = 14.sp,
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { showDetails = true }) { Text("View details") }
                TextButton(onClick = { app.clearSafeMode() }) { Text("Re-enable") }
            }
        }
    }
    if (showDetails) {
        val crash = remember { app.readLastCrash() }
        val body = if (crash == null) {
            "No crash recorded."
        } else {
            "${crash.clazz}\n${crash.message}\n${crash.timeText} · versionCode ${crash.versionCode}"
        }
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text("Last crash") },
            text = { Text(body) },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        try {
                            val cm = context.getSystemService(ClipboardManager::class.java)
                            cm?.setPrimaryClip(ClipData.newPlainText("HavenOS last crash", body))
                            copied = true
                        } catch (_: Exception) {
                        }
                    }) { Text(if (copied) "Copied" else "Copy") }
                    TextButton(onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "HavenOS last crash\n$body")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(
                                Intent.createChooser(intent, "Share last crash")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        } catch (_: Exception) {
                        }
                    }) { Text("Share") }
                    TextButton(onClick = { showDetails = false }) { Text("Close") }
                }
            },
        )
    }
}

/** Shared last-crash share text (Diagnostics block reuses the same shape). */
fun lastCrashShareText(app: HavenApp): String? {
    val crash = app.readLastCrash() ?: return null
    return "HavenOS last crash\n${crash.clazz}\n${crash.message}\n" +
        "${crash.timeText} · versionCode ${crash.versionCode}"
}
