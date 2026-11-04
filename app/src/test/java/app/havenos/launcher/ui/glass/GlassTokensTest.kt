package app.havenos.launcher.ui.glass

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 3.1 Phase B2–B3: glass tint/hairline/scrim values and radii.
 * Written, not run (sandbox test executor broken, pre-existing).
 */
class GlassTokensTest {

    @Test
    fun tintAlphas_matchPrompt() {
        assertEquals(0.07f, glassTintAlpha(GlassLevel.GLASS1, true), 0.001f)
        assertEquals(0.05f, glassTintAlpha(GlassLevel.GLASS1, false), 0.001f)
        assertEquals(0.12f, glassTintAlpha(GlassLevel.GLASS2, true), 0.001f)
        assertEquals(0.70f, glassTintAlpha(GlassLevel.GLASS2, false), 0.001f)
        assertEquals(0.16f, glassTintAlpha(GlassLevel.GLASS3, true), 0.001f)
    }

    @Test
    fun hairlineAlphas_matchPrompt() {
        assertEquals(0.12f, glassHairlineAlpha(GlassLevel.GLASS1), 0.001f)
        assertEquals(0.16f, glassHairlineAlpha(GlassLevel.GLASS2), 0.001f)
        assertEquals(0.18f, glassHairlineAlpha(GlassLevel.GLASS3), 0.001f)
    }

    @Test
    fun fallbackBump_isSixPoints() {
        assertEquals(0.06f, GLASS_FALLBACK_BUMP, 0.001f)
    }

    @Test
    fun scrim_meetsLegibilityFloor() {
        assertEquals(0.40f, glassScrimAlpha(true), 0.001f)
        assertEquals(0.60f, glassScrimAlpha(false), 0.001f)
    }

    @Test
    fun radii_matchPrompt() {
        assertEquals(28f, HavenRadii.card.value, 0.001f)
        assertEquals(20f, HavenRadii.group.value, 0.001f)
        assertEquals(14f, HavenRadii.control.value, 0.001f)
        assertEquals(12f, HavenRadii.iconTile.value, 0.001f)
    }

    @Test
    fun spacingScale_isExact() {
        assertEquals(4f, HavenSpacing.s4.value, 0.001f)
        assertEquals(8f, HavenSpacing.s8.value, 0.001f)
        assertEquals(12f, HavenSpacing.s12.value, 0.001f)
        assertEquals(16f, HavenSpacing.s16.value, 0.001f)
        assertEquals(24f, HavenSpacing.s24.value, 0.001f)
        assertEquals(32f, HavenSpacing.s32.value, 0.001f)
    }
}
