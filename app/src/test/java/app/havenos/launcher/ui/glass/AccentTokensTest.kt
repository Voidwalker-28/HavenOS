package app.havenos.launcher.ui.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 3.1 Phase B7: accent roles, contrast rules, migration, determinism.
 * Written, not run (sandbox test executor broken, pre-existing).
 */
class AccentTokensTest {

    // Contrast rules -----------------------------------------------------

    @Test
    fun everyCuratedColor_passesContrast_darkAndLight() {
        for (curated in CURATED_ACCENTS) {
            for (isDark in listOf(true, false)) {
                val roles = accentRolesFor(curated.argb, isDark, AccentStrength.SUBTLE)
                val bg = if (isDark) DARK_GLASS_BG else LIGHT_GLASS_BG
                assertTrue(
                    "${curated.name} dark=$isDark accent ratio",
                    contrastRatio(roles.accent, bg) >= 4.5,
                )
                assertTrue(
                    "${curated.name} dark=$isDark outline ratio",
                    contrastRatio(roles.accentOutline, bg) >= 3.0,
                )
                assertTrue(
                    "${curated.name} dark=$isDark onAccent ratio",
                    contrastRatio(roles.onAccent, roles.accent) >= 4.5,
                )
            }
        }
    }

    @Test
    fun curatedCount_isTwelve() {
        assertEquals(12, CURATED_ACCENTS.size)
    }

    @Test
    fun roleGeneration_isDeterministic() {
        val a = accentRolesFor(0xFF9B8CFF.toInt(), true, AccentStrength.VIVID)
        val b = accentRolesFor(0xFF9B8CFF.toInt(), true, AccentStrength.VIVID)
        assertEquals(a, b)
    }

    @Test
    fun strength_setsContainerAlpha() {
        assertEquals(0.10f, containerAlpha(AccentStrength.SUBTLE), 0.001f)
        assertEquals(0.18f, containerAlpha(AccentStrength.BALANCED), 0.001f)
        assertEquals(0.30f, containerAlpha(AccentStrength.VIVID), 0.001f)
        val roles = accentRolesFor(0xFFF0A35E.toInt(), true, AccentStrength.BALANCED)
        assertEquals(0.18f, ((roles.accentContainer ushr 24) and 0xFF) / 255f, 0.01f)
    }

    @Test
    fun lightSeed_clampsForReadability() {
        // Sand on a light background cannot pass 4.5:1 unclamped.
        val roles = accentRolesFor(0xFFE3CFA0.toInt(), false, AccentStrength.SUBTLE)
        assertTrue(roles.clamped)
        assertTrue(contrastRatio(roles.accent, LIGHT_GLASS_BG) >= 4.5)
    }

    @Test
    fun darkSeed_needsNoClamp() {
        val roles = accentRolesFor(0xFF6FA0FF.toInt(), true, AccentStrength.SUBTLE)
        assertFalse(roles.clamped)
    }

    // Migration ----------------------------------------------------------

    @Test
    fun migrate_mode0_isDefault() {
        assertEquals(AccentSource.DEFAULT, migrateAccent(0, 0).source)
    }

    @Test
    fun migrate_palette_mapsToNearestCurated() {
        // Old eight swatches (LookHub ACCENT_SWATCHES), pinned by redmean distance.
        assertEquals("Cobalt", migrateAccent(1, 0xFF2F6FCE.toInt()).curated)
        assertEquals("Lagoon", migrateAccent(1, 0xFF0F766E.toInt()).curated)
        assertEquals("Iris", migrateAccent(1, 0xFF7C3AED.toInt()).curated)
        assertEquals("Clay", migrateAccent(1, 0xFFDB2777.toInt()).curated)
        assertEquals("Ember", migrateAccent(1, 0xFFD97706.toInt()).curated)
        assertEquals("Lagoon", migrateAccent(1, 0xFF16A34A.toInt()).curated)
        assertEquals("Clay", migrateAccent(1, 0xFFB3541E.toInt()).curated)
        assertEquals("Clay", migrateAccent(1, 0xFF44586C.toInt()).curated)
    }

    @Test
    fun migrate_paletteZeroArg_isDefault() {
        assertEquals(AccentSource.DEFAULT, migrateAccent(1, 0).source)
    }

    @Test
    fun migrate_custom_keepsColourAndRecents() {
        val sel = migrateAccent(2, 0xFF123456.toInt())
        assertEquals(AccentSource.CUSTOM, sel.source)
        assertEquals(0xFF123456.toInt(), sel.customArgb)
        assertEquals(listOf(0xFF123456.toInt()), sel.recents)
    }

    @Test
    fun migrate_wallpaperRoundTrip_isWallpaper() {
        assertEquals(AccentSource.WALLPAPER, migrateAccent(3, 0).source)
    }

    // Resolution and names ----------------------------------------------

    @Test
    fun resolveSeed_default_isNull() {
        assertNull(resolveAccentSeed(AccentSelection(AccentSource.DEFAULT), 0xFF000000.toInt(), 0xFF000000.toInt()))
    }

    @Test
    fun resolveSeed_wallpaperWithoutSeed_isNull() {
        assertNull(resolveAccentSeed(AccentSelection(AccentSource.WALLPAPER), null, null))
    }

    @Test
    fun resolveSeed_curatedUnknownName_fallsBackToEmber() {
        assertEquals(
            curatedArgb("Ember"),
            resolveAccentSeed(AccentSelection(AccentSource.CURATED, "Nope"), null, null),
        )
    }

    @Test
    fun displayName_neverRawInt() {
        assertEquals("Default", accentDisplayName(AccentSelection(AccentSource.DEFAULT)))
        assertEquals("Wallpaper", accentDisplayName(AccentSelection(AccentSource.WALLPAPER)))
        assertEquals("System", accentDisplayName(AccentSelection(AccentSource.SYSTEM)))
        assertEquals("Iris", accentDisplayName(AccentSelection(AccentSource.CURATED, "Iris")))
        assertEquals("Ember", accentDisplayName(AccentSelection(AccentSource.CURATED, "Nope")))
        assertEquals("Custom #123456", accentDisplayName(AccentSelection(AccentSource.CUSTOM, customArgb = 0xFF123456.toInt())))
        assertEquals("Custom", accentDisplayName(AccentSelection(AccentSource.CUSTOM, customArgb = 0)))
    }

    @Test
    fun recents_roundTrip_capsAtSix() {
        val encoded = encodeRecents(listOf(1, 2, 3, 4, 5, 6, 7))
        assertEquals(6, parseRecents(encoded).size)
        assertEquals(
            listOf(0xFF000001.toInt(), 0xFF000002.toInt()),
            parseRecents("000001,000002"),
        )
        assertEquals(listOf(0xFF000003.toInt()), pushRecent(listOf(0xFF000001.toInt(), 0xFF000002.toInt()), 0xFF000003.toInt()).take(1))
        assertEquals(6, pushRecent(listOf(1, 2, 3, 4, 5, 6), 7).size)
    }

    // HSV round-trip (Custom picker) -------------------------------------

    @Test
    fun hsv_roundTrip() {
        for (argb in listOf(0xFFF0A35E.toInt(), 0xFF6FA0FF.toInt(), 0xFFE8E8EA.toInt(), 0xFF5CCBC0.toInt())) {
            val (h, s, v) = argbToHsv(argb)
            val back = hsvToArgb(h, s, v)
            // Rounding through HSV may shift a channel by 1.
            for (shift in listOf(16, 8, 0)) {
                val a = (argb shr shift) and 0xFF
                val b = (back shr shift) and 0xFF
                assertTrue("channel $shift: $a vs $b", kotlin.math.abs(a - b) <= 1)
            }
        }
    }
}
