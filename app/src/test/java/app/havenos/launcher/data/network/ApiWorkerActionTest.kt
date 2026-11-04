package app.havenos.launcher.data.network

import app.havenos.launcher.data.network.ApiRefreshScheduler.ApiWorkerAction
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 3.1 Phase A11 flow 16 + A12: toggle edit → worker action.
 * Written, not run (sandbox test executor broken, pre-existing).
 */
class ApiWorkerActionTest {

    @Test
    fun disabled_alwaysCancel() {
        for (api in ApiId.entries) {
            assertEquals(
                ApiWorkerAction.CANCEL,
                ApiRefreshScheduler.apiWorkerAction(api, false),
            )
        }
    }

    @Test
    fun enabled_withCadence_schedule() {
        for (api in ApiId.entries) {
            if (api == ApiId.DICTIONARY) continue
            assertEquals(
                ApiWorkerAction.SCHEDULE,
                ApiRefreshScheduler.apiWorkerAction(api, true),
            )
        }
    }

    @Test
    fun enabled_dictionary_none() {
        // Dictionary has no cadence: fetched on demand only, so a
        // toggle edit schedules nothing and cancels nothing.
        assertEquals(
            ApiWorkerAction.NONE,
            ApiRefreshScheduler.apiWorkerAction(ApiId.DICTIONARY, true),
        )
    }
}
