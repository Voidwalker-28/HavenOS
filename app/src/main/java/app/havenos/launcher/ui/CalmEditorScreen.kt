package app.havenos.launcher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.data.AppEntry
import app.havenos.launcher.data.CALM_EDITOR_MAX
import app.havenos.launcher.data.CalmEditorState
import app.havenos.launcher.data.CalmEntryOptions
import app.havenos.launcher.data.validate
import app.havenos.launcher.data.withAdded
import app.havenos.launcher.data.withDayToggled
import app.havenos.launcher.data.withMoved
import app.havenos.launcher.data.withRemoved

/**
 * Worker D (M9) — the Calm editor screen, Doc 1 §5.9.
 *
 * This REPLACES the M8 "Choose apps" placeholder (which opened the
 * Library instead of editing anything). Worker A owns the toolbar Calm
 * button; the coordinator shows this screen from that button and from
 * CalmMode's "Choose apps" action, and persists the saved state with
 * the Settings setters (calmFavoritesJson / calmScheduleJson /
 * calmEntryJson). Calm never changes the layout — saving here writes
 * only those three keys.
 *
 * The screen is deliberately reorder-button based (up/down) rather
 * than drag based: it is TalkBack/keyboard usable without depending on
 * Worker A's drag UI, and drag reorder of favorites is a later polish
 * item, not a correctness one.
 *
 * Day values are Calendar.DAY_OF_WEEK (SUNDAY = 1 … SATURDAY = 7),
 * matching CalmMode.kt's schedule format exactly.
 */

private fun minutesLabel(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "%02d:%02d".format(h, m)
}

private val DAY_LABELS = listOf(
    1 to "Sun", 2 to "Mon", 3 to "Tue", 4 to "Wed", 5 to "Thu", 6 to "Fri", 7 to "Sat",
)

@Composable
fun CalmEditorScreen(
    apps: List<AppEntry>,
    initial: CalmEditorState,
    onSave: (CalmEditorState) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember(initial) { mutableStateOf(initial) }
    val errors = remember(state) { state.validate() }
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val scroll = rememberScrollState()

    Column(
        modifier.fillMaxSize().verticalScroll(scroll).padding(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Calm favorites", fontSize = 22.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Cancel") }
            Button(
                onClick = { onSave(state) },
                enabled = errors.isEmpty(),
            ) { Text("Save") }
        }
        Text(
            "Pick up to $CALM_EDITOR_MAX favorites. Calm never changes your home layout.",
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(16.dp))

        // ---- chosen favorites, in order, with up/down/remove ----
        Text("Favorites (${state.favorites.size}/$CALM_EDITOR_MAX)", fontSize = 17.sp)
        if (state.favorites.isEmpty()) {
            Text("None chosen yet — pick apps below.", fontSize = 14.sp)
        }
        for ((index, key) in state.favorites.withIndex()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    byKey[key]?.label ?: key,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { state = state.withMoved(index, index - 1) },
                    enabled = index > 0,
                ) { Text("↑") }
                TextButton(
                    onClick = { state = state.withMoved(index, index + 1) },
                    enabled = index < state.favorites.size - 1,
                ) { Text("↓") }
                TextButton(onClick = { state = state.withRemoved(key) }) { Text("Remove") }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---- app picker ----
        Text("All apps", fontSize = 17.sp)
        for (entry in apps) {
            val checked = entry.key in state.favorites
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { on ->
                        state = if (on) state.withAdded(entry.key) else state.withRemoved(entry.key)
                    },
                    enabled = checked || !state.isFull,
                )
                Text(entry.label, fontSize = 16.sp, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---- schedule ----
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Schedule", fontSize = 17.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = state.scheduleEnabled,
                onCheckedChange = { state = state.copy(scheduleEnabled = it) },
            )
        }
        if (state.scheduleEnabled) {
            Row(Modifier.fillMaxWidth()) {
                for ((day, label) in DAY_LABELS) {
                    TextButton(
                        onClick = { state = state.withDayToggled(day) },
                    ) {
                        Text(if (day in state.days) "[$label]" else label, fontSize = 13.sp)
                    }
                }
            }
            Text("From ${minutesLabel(state.startMinutes)}", fontSize = 14.sp)
            Slider(
                value = state.startMinutes.toFloat(),
                onValueChange = {
                    state = state.copy(startMinutes = (it.toInt() / 15 * 15).coerceIn(0, 1439))
                },
                valueRange = 0f..1439f,
            )
            Text("To ${minutesLabel(state.endMinutes)}", fontSize = 14.sp)
            Slider(
                value = state.endMinutes.toFloat(),
                onValueChange = {
                    state = state.copy(endMinutes = (it.toInt() / 15 * 15).coerceIn(0, 1439))
                },
                valueRange = 0f..1439f,
            )
            Text(
                "An overnight window (end before start) wraps past midnight. " +
                    "With no days selected the schedule never activates.",
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(16.dp))

        // ---- entry options ----
        Text("Entry", fontSize = 17.sp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Auto-enter on schedule", fontSize = 15.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = state.entry.autoEnter,
                onCheckedChange = { state = state.copy(entry = state.entry.copy(autoEnter = it)) },
            )
        }
        Text("Dim ${if (state.entry.dimPct > 0) "${state.entry.dimPct}%" else "(automatic)"}", fontSize = 14.sp)
        Slider(
            value = state.entry.dimPct.toFloat(),
            onValueChange = {
                state = state.copy(entry = CalmEntryOptions(state.entry.autoEnter, it.toInt().coerceIn(0, 100)))
            },
            valueRange = 0f..100f,
        )
        if (errors.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            for (error in errors) Text(error, fontSize = 14.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}
