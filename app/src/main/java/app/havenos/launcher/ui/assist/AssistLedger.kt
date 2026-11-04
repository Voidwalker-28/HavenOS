package app.havenos.launcher.ui.assist

import app.havenos.launcher.data.AppRepository
import app.havenos.launcher.data.AssistFeatureId
import app.havenos.launcher.data.Settings
import app.havenos.launcher.data.clearAssistLedgerJsons
import app.havenos.launcher.data.encodeLaunchStats
import app.havenos.launcher.data.parseStringIntMap
import app.havenos.launcher.data.parseStringLongMap

// 3.1 Phase F (F4): the AI Privacy Ledger's display model. One row per
// assist feature, with what local data it reads, an on-device badge, the
// last used time and a per-feature switch. Rows record COUNTS and TIMES
// ONLY, never content — that is a property of the stored JSONs (see
// data/AssistLedger.kt), not of this display layer.

/** Switch values the host reads from SettingsState (no Android import). */
data class AssistLedgerState(
    val searchCommandsEnabled: Boolean,
    val predictionEnabled: Boolean,
    val chatEnabled: Boolean,
    val findEnabled: Boolean,
    val suggestionsEnabled: Boolean,
    val lastUsedJson: String,
    val countsJson: String,
)

data class AssistLedgerEntry(
    val feature: AssistFeatureId,
    val title: String,
    val dataReads: String,
    val lastUsedMs: Long,
    val useCount: Int,
    val enabled: Boolean,
    /** Always true: every feature here runs on the device, by §F0. */
    val onDevice: Boolean = true,
)

/**
 * Builds the five ledger rows. App prediction's last-used/count come from
 * the launch stats themselves (its input data) — no per-render ledger
 * write — so its row is honest without a write on every suggestion.
 */
fun ledgerEntries(
    state: AssistLedgerState,
    stats: Map<String, Pair<Int, Long>>,
): List<AssistLedgerEntry> {
    val lastUsed = parseStringLongMap(state.lastUsedJson)
    val counts = parseStringIntMap(state.countsJson)
    val predictionLast = stats.values.maxOfOrNull { it.second } ?: 0L
    val predictionCount = stats.values.sumOf { it.first }
    return listOf(
        AssistLedgerEntry(
            AssistFeatureId.SEARCH_COMMANDS,
            "Search commands",
            "Reads: your typed query and the installed-app list, on this device.",
            lastUsed[AssistFeatureId.SEARCH_COMMANDS.id] ?: 0L,
            counts[AssistFeatureId.SEARCH_COMMANDS.id] ?: 0,
            state.searchCommandsEnabled,
        ),
        AssistLedgerEntry(
            AssistFeatureId.APP_PREDICTION,
            "App prediction",
            "Reads: local launch history (count, recency, same-hour boost).",
            predictionLast,
            predictionCount,
            state.predictionEnabled,
        ),
        AssistLedgerEntry(
            AssistFeatureId.MASCOT_FIND,
            "Mascot Find",
            "Reads: your typed query and the installed-app list, on this device.",
            lastUsed[AssistFeatureId.MASCOT_FIND.id] ?: 0L,
            counts[AssistFeatureId.MASCOT_FIND.id] ?: 0,
            state.findEnabled,
        ),
        AssistLedgerEntry(
            AssistFeatureId.MASCOT_CHAT,
            "Mascot chat",
            "Reads: your typed query and the scripted help map, on this device.",
            lastUsed[AssistFeatureId.MASCOT_CHAT.id] ?: 0L,
            counts[AssistFeatureId.MASCOT_CHAT.id] ?: 0,
            state.chatEnabled,
        ),
        AssistLedgerEntry(
            AssistFeatureId.SUGGESTIONS,
            "Suggestions",
            "Reads: local launch history and the scripted tip list, on this device.",
            lastUsed[AssistFeatureId.SUGGESTIONS.id] ?: 0L,
            counts[AssistFeatureId.SUGGESTIONS.id] ?: 0,
            state.suggestionsEnabled,
        ),
    )
}

/**
 * "Clear learned data" (prompt §F2 + §F4): wipes the launch stats (in
 * memory AND persisted, so they cannot come back on relaunch) and resets
 * both ledger JSONs. Mascot muted/don't-show lists are user preferences,
 * not learned data, and are deliberately left alone.
 */
suspend fun clearAssistLearnedData(settings: Settings, appRepo: AppRepository) {
    appRepo.clearStats()
    settings.setLaunchStatsJson(encodeLaunchStats(emptyMap()))
    val (lastUsed, counts) = clearAssistLedgerJsons()
    settings.setAssistLastUsedJson(lastUsed)
    settings.setAssistCountsJson(counts)
}
