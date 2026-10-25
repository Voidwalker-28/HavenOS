package app.havenos.launcher.ui.settings.shell

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.havenos.launcher.data.SettingsState
import app.havenos.launcher.theme.LocalFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// M6 preview aids (v2 §3.4 / §13). All state is in-memory for the session
// except Recent chips, which persist locally (SharedPreferences, no cloud,
// no analytics). Nothing here writes a setting itself: hubs record each
// change with its revert action, and undo / revert / Before all flow
// through those records and the panel-open snapshot.
// ---------------------------------------------------------------------------

/**
 * One logged change. [revert] is the exact write that restores the old
 * value (a typed setter captured by the hub at record time); items with
 * no revert action show no Revert button and cannot be undone.
 */
data class ChangeRecord(
    val key: String,
    val oldText: String,
    val newText: String,
    val revert: (suspend () -> Unit)? = null,
)

/**
 * Session preview state, created once per panel open. The host calls
 * [begin] with the live state when the panel opens; hubs call [record]
 * after every committed write; the panel reads [changes] for the chip
 * and list, [beforeHeld] for the Before hold, and [openSnapshot] for
 * Before / Revert session. Undo keeps the last 20 changes.
 */
class PreviewSession(
    openSnapshot: SettingsState? = null,
) {
    var openSnapshot: SettingsState? by mutableStateOf(openSnapshot)
        private set

    /** True only while the Before button is held down. */
    var beforeHeld: Boolean by mutableStateOf(false)

    /** Every change this session, oldest first (the Changes list). */
    val changes = mutableStateListOf<ChangeRecord>()

    /** Undo stack: the last 20 changes, newest last. */
    private val undoStack = ArrayDeque<ChangeRecord>()

    val snackbarHostState = SnackbarHostState()

    fun begin(snapshot: SettingsState) {
        openSnapshot = snapshot
        beforeHeld = false
        changes.clear()
        undoStack.clear()
    }

    fun record(record: ChangeRecord) {
        changes.add(record)
        undoStack.addLast(record)
        while (undoStack.size > 20) undoStack.removeFirst()
    }

    /**
     * M6 integration: replace the whole list with the host's recomputed
     * session diff (panel-open snapshot vs live state). Undo then pops
     * the newest differing field, and the host rebuilds the list after
     * the revert lands — so a reverted field drops out of the list
     * instead of lingering as a stale entry.
     */
    fun replaceChanges(records: List<ChangeRecord>) {
        changes.clear()
        undoStack.clear()
        for (record in records) {
            changes.add(record)
            undoStack.addLast(record)
        }
        while (undoStack.size > 20) undoStack.removeFirst()
    }

    /** Undo the newest change. Returns false when nothing is undoable. */
    suspend fun undoLast(): Boolean {
        val record = undoStack.removeLastOrNull() ?: return false
        record.revert?.invoke() ?: return false
        changes.remove(record)
        return true
    }

    /** Revert one item from the Changes list. */
    suspend fun revertOne(record: ChangeRecord): Boolean {
        record.revert?.invoke() ?: return false
        changes.remove(record)
        undoStack.remove(record)
        return true
    }

    /** Revert the whole session, newest first. Items without a revert action are skipped. */
    suspend fun revertSession() {
        for (record in changes.asReversed()) {
            record.revert?.invoke()
        }
        changes.clear()
        undoStack.clear()
    }

    /**
     * Record a change and offer Undo for 5 seconds. The snackbar is
     * Indefinite under the hood with a 5s dismiss, because Material3
     * has no 5s duration bucket. Undo reverts via the record's action.
     */
    fun notifyChanged(scope: CoroutineScope, record: ChangeRecord) {
        record(record)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "${record.key} changed",
                actionLabel = "Undo",
                duration = SnackbarDuration.Indefinite,
            )
            if (result == SnackbarResult.ActionPerformed) undoLast()
        }
        scope.launch {
            delay(5_000L)
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
}

@Composable
fun rememberPreviewSession(
    openSnapshot: SettingsState? = null,
): PreviewSession = remember { PreviewSession(openSnapshot) }

/**
 * Draft layer for preview-only state (v2 §3.1/§13): preset previews,
 * grid shrink, wallpaper adjust. The draft is a full [SettingsState]
 * copy the visible Home renders instead of the live state
 * ([effective]); nothing is persisted until the hub commits it with
 * its own typed setters and calls [discard]. Dismissing the panel or
 * Cancel discards without writing anything.
 */
class DraftLayer {
    var draft: SettingsState? by mutableStateOf(null)
        private set

    val hasDraft: Boolean get() = draft != null

    fun begin(current: SettingsState) {
        if (draft == null) draft = current
    }

    fun preview(transform: (SettingsState) -> SettingsState) {
        draft = draft?.let(transform)
    }

    fun discard() {
        draft = null
    }

    /** What Home renders: the draft while previewing, else the live state. */
    fun effective(live: SettingsState): SettingsState = draft ?: live
}

@Composable
fun rememberDraftLayer(): DraftLayer = remember { DraftLayer() }

/** Spotlight state: the Home host wraps the target element in [SpotlightRing]. */
class SpotlightState {
    var target: String? by mutableStateOf(null)
        private set
    var visible: Boolean by mutableStateOf(false)
        private set

    /** Pulse once (~600ms) on [key]; re-firing the same key restarts it. */
    fun pulse(scope: CoroutineScope, key: String) {
        target = key
        visible = true
        scope.launch {
            delay(600L)
            if (target == key) visible = false
        }
    }
}

@Composable
fun rememberSpotlightState(): SpotlightState = remember { SpotlightState() }

/**
 * Accent ring around a Home element. Under reduced motion there is no
 * pulse: a static 2dp ring shows for the same 600ms, then hides.
 */
@Composable
fun SpotlightRing(
    visible: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tokens = LocalShellTokens.current
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible && !reducedMotion) {
            pulse.snapTo(0f)
            pulse.animateTo(1f, animationSpec = tween(durationMillis = 600))
        }
    }
    val scale = if (reducedMotion || !visible) 1f else 1f + 0.04f * pulse.value
    val alpha = if (reducedMotion || !visible) 1f else 1f - pulse.value
    Box(
        modifier
            .then(
                if (visible) Modifier.border(
                    2.dp,
                    tokens.accent.copy(alpha = alpha),
                    RoundedCornerShape(ShellRadii.card),
                ) else Modifier,
            )
            .graphicsLayer(scaleX = scale, scaleY = scale),
    ) { content() }
}

/** "Before" button: while held, the host shows [PreviewSession.openSnapshot]. */
@Composable
fun BeforeHoldButton(
    onHoldChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalShellTokens.current
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(ShellRadii.chip))
            .background(tokens.cardSelected)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onHoldChange(true)
                        tryAwaitRelease()
                        onHoldChange(false)
                    },
                )
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Before",
            style = ShellText.help.copy(fontFamily = LocalFont.current),
            color = tokens.accent,
        )
    }
}

/** Toolbar chip opening the Changes list. Rendered only when count > 0. */
@Composable
fun ChangesChip(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalShellTokens.current
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(ShellRadii.chip))
            .background(tokens.cardSelected)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Changes ($count)",
            style = ShellText.help.copy(fontFamily = LocalFont.current),
            color = tokens.accent,
        )
    }
}

/** Changes list: every session change with per-item revert + Revert session. */
@Composable
fun ChangesDialog(
    session: PreviewSession,
    onDismiss: () -> Unit,
) {
    val tokens = LocalShellTokens.current
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(ShellRadii.card))
                .background(tokens.panelSurface)
                .padding(ShellSpacing.s16),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ShellSpacing.s12)) {
                Text(
                    text = "Changes (${session.changes.size})",
                    style = ShellText.rowTitle.copy(
                        fontFamily = LocalFont.current,
                    ),
                    color = tokens.textPrimary,
                )
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(ShellSpacing.s8),
                ) {
                    items(session.changes.toList()) { record ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = record.key,
                                    style = ShellText.value.copy(
                                        fontFamily = LocalFont.current,
                                    ),
                                    color = tokens.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${record.oldText} → ${record.newText}",
                                    style = ShellText.help.copy(
                                        fontFamily = LocalFont.current,
                                    ),
                                    color = tokens.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (record.revert != null) {
                                TextButton(onClick = {
                                    scope.launch { session.revertOne(record) }
                                }) {
                                    Text(
                                        text = "Revert",
                                        style = ShellText.help.copy(
                                            fontFamily = LocalFont.current,
                                        ),
                                        color = tokens.accent,
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        scope.launch { session.revertSession(); onDismiss() }
                    }) {
                        Text(
                            text = "Revert session",
                            style = ShellText.help.copy(
                                fontFamily = LocalFont.current,
                            ),
                            color = tokens.danger,
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Done",
                            style = ShellText.help.copy(
                                fontFamily = LocalFont.current,
                            ),
                            color = tokens.accent,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The ONE Reset per screen (v2 §3.4): title-bar overflow menu with a
 * single "Reset this screen…" item. The confirm names exactly how many
 * settings change; dismissing writes nothing. Hubs supply the count
 * and the reset action; the panel renders this only when supplied.
 */
@Composable
fun ShellResetOverflow(
    affectedCount: Int,
    onReset: suspend () -> Unit,
) {
    val tokens = LocalShellTokens.current
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmOpen by remember { mutableStateOf(false) }
    Box(
        Modifier
            .size(48.dp)
            .semantics { contentDescription = "More options"; role = Role.Button }
            .clickable { menuOpen = true },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "⋮",
            style = ShellText.rowTitle.copy(
                fontFamily = LocalFont.current,
            ),
            color = tokens.textPrimary,
        )
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(
            text = {
                Text(
                    text = "Reset this screen…",
                    style = ShellText.value.copy(
                        fontFamily = LocalFont.current,
                    ),
                    color = tokens.textPrimary,
                )
            },
            onClick = { menuOpen = false; confirmOpen = true },
        )
    }
    if (confirmOpen) {
        AlertDialog(
            onDismissRequest = { confirmOpen = false },
            title = {
                Text(
                    text = "Reset this screen",
                    style = ShellText.rowTitle.copy(
                        fontFamily = LocalFont.current,
                    ),
                    color = tokens.textPrimary,
                )
            },
            text = {
                Text(
                    text = "$affectedCount settings will change.",
                    style = ShellText.value.copy(
                        fontFamily = LocalFont.current,
                    ),
                    color = tokens.textSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmOpen = false
                    scope.launch { onReset() }
                }) {
                    Text(
                        text = "Reset",
                        style = ShellText.help.copy(
                            fontFamily = LocalFont.current,
                        ),
                        color = tokens.danger,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmOpen = false }) {
                    Text(
                        text = "Cancel",
                        style = ShellText.help.copy(
                            fontFamily = LocalFont.current,
                        ),
                        color = tokens.textSecondary,
                    )
                }
            },
        )
    }
}

/**
 * Recent chips, persisted locally (SharedPreferences, this device only).
 * Stores up to 4 labels, most recent first; [clear] wipes them.
 */
class RecentSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("havenos_shell_recent", Context.MODE_PRIVATE)

    fun recent(): List<String> =
        prefs.getString(KEY, "").orEmpty().split("\n").filter { it.isNotEmpty() }

    fun record(label: String) {
        val next = (listOf(label) + recent()).distinct().take(4)
        prefs.edit().putString(KEY, next.joinToString("\n")).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "recent_settings"
    }
}

@Composable
fun rememberRecentSettingsStore(): RecentSettingsStore {
    val context = LocalContext.current
    return remember { RecentSettingsStore(context.applicationContext) }
}

/** Horizontally scrolling recent chips with a Clear action. */
@Composable
fun RecentChips(
    recent: List<String>,
    onChipClick: (String) -> Unit,
    onClear: () -> Unit,
) {
    val tokens = LocalShellTokens.current
    if (recent.isEmpty()) return
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = ShellSpacing.s4),
        horizontalArrangement = Arrangement.spacedBy(ShellSpacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (label in recent) {
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(ShellRadii.chip))
                    .background(tokens.cardSelected)
                    .clickable { onChipClick(label) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = ShellText.help.copy(
                        fontFamily = LocalFont.current,
                    ),
                    color = tokens.accent,
                    maxLines = 1,
                )
            }
        }
        TextButton(onClick = onClear) {
            Text(
                text = "Clear",
                style = ShellText.help.copy(
                    fontFamily = LocalFont.current,
                ),
                color = tokens.textSecondary,
            )
        }
    }
}
