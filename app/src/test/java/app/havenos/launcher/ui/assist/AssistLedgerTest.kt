package app.havenos.launcher.ui.assist

import app.havenos.launcher.data.AssistFeatureId
import app.havenos.launcher.data.clearAssistLedgerJsons
import app.havenos.launcher.data.recordAssistUse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssistLedgerTest {

    @Test fun recordKeepsCountsAndTimesOnly() {
        val (lastUsed, counts) = recordAssistUse("{}", "{}", AssistFeatureId.MASCOT_FIND, 1700L)
        val (lastUsed2, counts2) = recordAssistUse(lastUsed, counts, AssistFeatureId.MASCOT_FIND, 2900L)
        assertEquals("{\"mascot_find\":2900}", lastUsed2)
        assertEquals("{\"mascot_find\":2}", counts2)
        // No content anywhere: the JSONs hold ids, counts, times, nothing else.
        assertFalse(lastUsed2.contains("camera"))
        assertFalse(counts2.contains("camera"))
    }

    @Test fun recordIsTolerantOfGarbage() {
        val (lastUsed, counts) = recordAssistUse("not json", "{bad", AssistFeatureId.SEARCH_COMMANDS, 5L)
        assertEquals("{\"search_commands\":5}", lastUsed)
        assertEquals("{\"search_commands\":1}", counts)
    }

    @Test fun clearResetsBothJsons() {
        val (lastUsed, counts) = clearAssistLedgerJsons()
        assertEquals("{}", lastUsed)
        assertEquals("{}", counts)
    }

    @Test fun ledgerEntriesDerivePredictionFromStats() {
        val state = AssistLedgerState(
            searchCommandsEnabled = true,
            predictionEnabled = true,
            chatEnabled = false,
            findEnabled = true,
            suggestionsEnabled = false,
            lastUsedJson = "{\"mascot_find\":900}",
            countsJson = "{\"mascot_find\":4}",
        )
        val stats = mapOf("camera" to (3 to 700L), "clock" to (2 to 1200L))
        val entries = ledgerEntries(state, stats)
        assertEquals(5, entries.size)
        val prediction = entries.first { it.feature == AssistFeatureId.APP_PREDICTION }
        assertEquals(1200L, prediction.lastUsedMs)
        assertEquals(5, prediction.useCount)
        assertTrue(prediction.enabled)
        val chat = entries.first { it.feature == AssistFeatureId.MASCOT_CHAT }
        assertFalse(chat.enabled)
        val find = entries.first { it.feature == AssistFeatureId.MASCOT_FIND }
        assertEquals(900L, find.lastUsedMs)
        assertEquals(4, find.useCount)
        // Every entry names what it reads and carries the on-device badge.
        for (entry in entries) {
            assertTrue(entry.dataReads.isNotBlank())
            assertTrue(entry.onDevice)
        }
    }

    @Test fun ledgerEntriesWithNoStatsAreNeverUsed() {
        val state = AssistLedgerState(
            searchCommandsEnabled = true,
            predictionEnabled = true,
            chatEnabled = true,
            findEnabled = true,
            suggestionsEnabled = true,
            lastUsedJson = "{}",
            countsJson = "{}",
        )
        val prediction = ledgerEntries(state, emptyMap())
            .first { it.feature == AssistFeatureId.APP_PREDICTION }
        assertEquals(0L, prediction.lastUsedMs)
        assertEquals(0, prediction.useCount)
    }
}
