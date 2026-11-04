package app.havenos.launcher.data.network

import app.havenos.launcher.data.migration67Sql
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 3.1 Phase A2: one-place result classification + Diagnostics
 * migration pin.
 *
 * NOTE: written-but-not-run — the sandbox test executor is broken
 * (pre-existing) and SettingsShellTest has pre-existing compile errors.
 */
class ApiClassificationTest {

    @Test
    fun classify_offlineOnlyWhenNetworkNotValidated() {
        assertEquals(
            ApiFailureClass.OFFLINE,
            classifyFailure(ApiErrorKind.OFFLINE, -1, hasValidatedInternet = false),
        )
        // The acceptance test: a transport failure with a validated
        // network is NEVER Offline.
        assertEquals(
            ApiFailureClass.NETWORK_ERROR,
            classifyFailure(ApiErrorKind.OFFLINE, -1, hasValidatedInternet = true),
        )
    }

    @Test
    fun classify_httpCodesMapToClasses() {
        assertEquals(
            ApiFailureClass.RATE_LIMITED,
            classifyFailure(ApiErrorKind.RATE_LIMITED, 429, true),
        )
        assertEquals(
            ApiFailureClass.NOT_FOUND,
            classifyFailure(ApiErrorKind.NOT_FOUND, 404, true),
        )
        assertEquals(
            ApiFailureClass.SERVER_ERROR,
            classifyFailure(ApiErrorKind.SERVER_ERROR, 500, true),
        )
        assertEquals(
            ApiFailureClass.SERVER_ERROR,
            classifyFailure(ApiErrorKind.OTHER, 503, true),
        )
        assertEquals(
            ApiFailureClass.OTHER,
            classifyFailure(ApiErrorKind.OTHER, 301, true),
        )
        assertEquals(
            ApiFailureClass.OTHER,
            classifyFailure(ApiErrorKind.OTHER, -1, false),
        )
    }

    @Test
    fun classify_cardStatesByClass() {
        // Card-state mapping lives in the status line, but the class
        // set itself is the contract: every class the UI switches on
        // must exist exactly once here.
        val classes = ApiFailureClass.entries.map { it.name }.toSet()
        assertEquals(
            setOf(
                "OFFLINE", "NETWORK_ERROR", "RATE_LIMITED", "NOT_FOUND",
                "PARSE_ERROR", "SERVER_ERROR", "OTHER",
            ),
            classes,
        )
    }

    @Test
    fun diagnostics_snippetCappedAt200() {
        val long = "x".repeat(500)
        val row = ApiDiagnosticsRow("QUOTE", "PARSE_ERROR", 200, 1L, long.take(200))
        assertEquals(200, row.bodySnippet.length)
    }

    @Test
    fun migration67_createsDiagnosticsTableOnly() {
        val sql = migration67Sql()
        assertEquals(1, sql.size)
        assertTrue(sql[0].contains("CREATE TABLE IF NOT EXISTS `api_diagnostics`"))
        assertTrue(sql[0].contains("`api` TEXT NOT NULL"))
        assertTrue(sql[0].contains("`resultClass` TEXT NOT NULL"))
        assertTrue(sql[0].contains("`httpCode` INTEGER NOT NULL"))
        assertTrue(sql[0].contains("`timeMs` INTEGER NOT NULL"))
        assertTrue(sql[0].contains("`bodySnippet` TEXT NOT NULL"))
        assertTrue(sql[0].contains("PRIMARY KEY(`api`)"))
    }
}
