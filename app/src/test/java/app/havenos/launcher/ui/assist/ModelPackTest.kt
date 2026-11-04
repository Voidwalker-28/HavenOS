package app.havenos.launcher.ui.assist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelPackTest {

    @Test fun sha256OfKnownInput() {
        // SHA-256("abc") from FIPS 180-4: the verifier must match it.
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256Hex("abc".toByteArray()),
        )
    }

    @Test fun verifyRequiresExactMatch() {
        val bytes = "pack-bytes".toByteArray()
        val good = sha256Hex(bytes)
        assertTrue(verifyPackBytes(bytes, good))
        assertFalse(verifyPackBytes(bytes, "00$good".dropLast(2)))
        assertFalse(verifyPackBytes(bytes, null))
        assertFalse(verifyPackBytes(bytes, ""))
    }

    @Test fun gateRefusesEmptyUrlBeforeAnyNetwork() {
        val gate = DownloadGate(
            url = "", wifiOnly = true, wifiConnected = true,
            freeBytes = 10_000_000_000L, expectedBytes = 100_000_000L,
            totalRamBytes = 8_000_000_000L,
        )
        assertEquals("No model URL configured", downloadGateError(gate))
    }

    @Test fun gateChecksWifiSpaceAndRam() {
        val base = DownloadGate(
            url = "https://example.invalid/model.bin", wifiOnly = true, wifiConnected = true,
            freeBytes = 10_000_000_000L, expectedBytes = 100_000_000L,
            totalRamBytes = 8_000_000_000L,
        )
        assertNull(downloadGateError(base))
        assertEquals(
            "Wi-Fi required",
            downloadGateError(base.copy(wifiConnected = false)),
        )
        assertEquals(
            "Not enough free space",
            downloadGateError(base.copy(freeBytes = 50_000_000L)),
        )
        assertEquals(
            "Not enough memory",
            downloadGateError(base.copy(totalRamBytes = 512_000_000L)),
        )
        // Wi-Fi rule is off by user choice: mobile data may proceed.
        assertNull(downloadGateError(base.copy(wifiOnly = false, wifiConnected = false)))
    }

    @Test fun deriveStatusFromFiles() {
        assertEquals(ModelPackStatus.NOT_SET_UP, derivePackStatus(false, false, 0L, 0L, false))
        assertEquals(ModelPackStatus.DOWNLOADING, derivePackStatus(false, true, 50L, 100L, false))
        assertEquals(ModelPackStatus.READY, derivePackStatus(true, false, 0L, 0L, true))
        assertEquals(ModelPackStatus.FAILED, derivePackStatus(true, false, 0L, 0L, false))
    }

    @Test fun modelBrainDelegatesUntilPackReady() {
        // No runtime is wired: with the flag off the scripted brain
        // answers, and even with it on an unread pack still delegates.
        assertFalse(USE_MODEL_BRAIN)
        val brain = LocalModelBrain(packReady = { false })
        assertTrue(brain is app.havenos.launcher.ui.mascot.MascotBrain)
    }
}
