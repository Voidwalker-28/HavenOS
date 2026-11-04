package app.havenos.launcher.ui.settings.shell

import app.havenos.launcher.ui.glass.SegmentedMode
import app.havenos.launcher.ui.glass.segmentedLayoutMode
import app.havenos.launcher.ui.settings.hubs.AdaptiveRowPlan
import app.havenos.launcher.ui.settings.hubs.adaptiveRowArrangement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 3.1.1 (item 5): settings layout matrix, JUnit4 against current APIs.
 * Written, not run (sandbox test executor broken + test tree does not
 * compile, pre-existing — see the hotfix report for the exact status).
 *
 * What this covers: the pure decision functions every hub shares
 * (HubRow / HubSwitchRow / HavenSegmented / ApiKeyField all route
 * through them), enumerated over the matrix the brief requires:
 * EVERY hub x panel widths 480 / 640 / 920dp x font scales 1.0 / 1.3.
 * The hub list is HubScreen.HUB_ROWS (look, home-dock,
 * drawer-search, gestures-motion, privacy, device-data, apis,
 * mascot); the invariants are hub-independent because all hubs share
 * the same row components, so the test asserts the same invariants
 * per hub rather than rendering eight identical layouts.
 *
 * What this does NOT cover (no JVM renderer here): actual Text
 * measurement, the Gestures device diagram, tooltip rendering, and
 * the 360dp detail assertion on a real device — those are Pad checks
 * in DEVICE_CHECKLIST.md. Pixel inputs below treat 1dp as 1px and
 * scale label widths by the font scale, which is exactly how the
 * composables feed these functions (density * fontScale).
 */
class SettingsLayoutMatrixTest {

    private val hubs = listOf(
        "look", "home-dock", "drawer-search", "gestures-motion",
        "privacy", "device-data", "apis", "mascot",
    )
    private val panelWidths = listOf(480f, 640f, 920f)
    private val fontScales = listOf(1.0f, 1.3f)

    // Panel / rail / detail ------------------------------------------

    @Test
    fun tabletPanelWidth_clampsTo640_56pct_920() {
        // Narrow window: 56% would be 448dp -> clamped to 640dp.
        assertEquals(640f, tabletPanelWidthDp(800f), 0f)
        // Mid window: 56% rules.
        assertEquals(716.8f, tabletPanelWidthDp(1280f), 0.01f)
        // Wide window: 56% would be 1120dp -> clamped to 920dp.
        assertEquals(920f, tabletPanelWidthDp(2000f), 0f)
    }

    @Test
    fun railWidth_iconOnlyBelow900_expandedAt900() {
        assertEquals(72f, settingsRailWidthDp(899f), 0f)
        assertEquals(200f, settingsRailWidthDp(900f), 0f)
        assertEquals(72f, settingsRailWidthDp(640f), 0f)
    }

    @Test
    fun detailWidth_alwaysAtLeast360InMasterDetail() {
        // Narrow end of the new clamp: 640 - 72 - 41 = 527dp.
        assertEquals(527f, settingsDetailWidthDp(640f), 0f)
        // Wide end: 920 - 200 - 41 = 679dp.
        assertEquals(679f, settingsDetailWidthDp(920f), 0f)
    }

    // Adaptive row ----------------------------------------------------

    @Test
    fun adaptiveRow_wide_singleRowWithMinLabel() {
        val plan = adaptiveRowArrangement(
            rowWidthPx = 1000, valuePx = 100, controlPx = 48,
            gapPx = 8, minLabelPx = 500,
        )
        // 1000 - (100+8) - (48+8) = 836 >= 500.
        assertEquals(AdaptiveRowPlan.SingleRow(836), plan)
    }

    @Test
    fun adaptiveRow_narrow_stacks() {
        val plan = adaptiveRowArrangement(
            rowWidthPx = 400, valuePx = 100, controlPx = 48,
            gapPx = 8, minLabelPx = 200,
        )
        // 400 - (100+8) - (48+8) = 236 >= 200 -> still SingleRow.
        assertEquals(AdaptiveRowPlan.SingleRow(236), plan)
        val stacked = adaptiveRowArrangement(
            rowWidthPx = 300, valuePx = 100, controlPx = 48,
            gapPx = 8, minLabelPx = 200,
        )
        // 300 - 108 - 56 = 136 < 200 -> Stacked.
        assertEquals(AdaptiveRowPlan.Stacked, stacked)
    }

    @Test
    fun adaptiveRow_boundary_singleRowAtExactlyMinLabel() {
        val plan = adaptiveRowArrangement(
            rowWidthPx = 300, valuePx = 92, controlPx = 0,
            gapPx = 8, minLabelPx = 200,
        )
        // 300 - (92+8) = 200 == min -> SingleRow.
        assertEquals(AdaptiveRowPlan.SingleRow(200), plan)
    }

    @Test
    fun adaptiveRow_zeroWidths() {
        assertEquals(
            AdaptiveRowPlan.SingleRow(300),
            adaptiveRowArrangement(300, 0, 0, 8, 140),
        )
        assertEquals(
            AdaptiveRowPlan.Stacked,
            adaptiveRowArrangement(0, 0, 0, 8, 140),
        )
    }

    // Segmented ---------------------------------------------------------

    @Test
    fun segmented_fittingLabels_equalSegmentsAtLeast64() {
        val mode = segmentedLayoutMode(
            availableWidthPx = 400,
            labelWidthsPx = listOf(60, 70, 50, 40),
            minSegmentPx = 64,
            labelPaddingPx = 8,
        )
        // 400 / 4 = 100 >= 64 and every label + 8 <= 100.
        assertEquals(SegmentedMode.Equal(100), mode)
    }

    @Test
    fun segmented_longLabel_switchesToChips() {
        // "Most used" at a narrow width: 200 / 4 = 50 < 64.
        val mode = segmentedLayoutMode(
            availableWidthPx = 200,
            labelWidthsPx = listOf(70, 50, 60, 40),
            minSegmentPx = 64,
            labelPaddingPx = 8,
        )
        assertEquals(SegmentedMode.Chips, mode)
    }

    @Test
    fun segmented_labelWiderThanSegment_switchesToChips() {
        // Segments would be 100px, but one label needs 110px + 8px.
        val mode = segmentedLayoutMode(
            availableWidthPx = 400,
            labelWidthsPx = listOf(110, 50, 50, 50),
            minSegmentPx = 64,
            labelPaddingPx = 8,
        )
        assertEquals(SegmentedMode.Chips, mode)
    }

    @Test
    fun segmented_emptyOptions_equalZero() {
        assertEquals(
            SegmentedMode.Equal(0),
            segmentedLayoutMode(400, emptyList(), 64, 8),
        )
    }

    // Full matrix -------------------------------------------------------

    @Test
    fun matrix_everyHub_everyWidth_everyFontScale() {
        // Representative label/value/control widths at 14sp, in px
        // at font scale 1.0; scaled by the font scale per column.
        val labelPx = 200
        val valuePx = 80
        val controlPx = 48
        val gapPx = 8
        for (hub in hubs) {
            for (panel in panelWidths) {
                val rail = settingsRailWidthDp(panel)
                val detail = settingsDetailWidthDp(panel)
                val singlePane = panel < 560f
                // Detail >= 360dp OR single-pane active.
                assertTrue(
                    "$hub panel=$panel detail=$detail singlePane=$singlePane",
                    detail >= 360f || singlePane,
                )
                for (scale in fontScales) {
                    val rowPx = (detail * scale).toInt()
                    val minLabel = maxOf(
                        (140f * scale).toInt(),
                        rowPx / 2,
                    )
                    val plan = adaptiveRowArrangement(
                        rowPx,
                        (valuePx * scale).toInt(),
                        (controlPx * scale).toInt(),
                        gapPx,
                        minLabel,
                    )
                    when (plan) {
                        is AdaptiveRowPlan.SingleRow -> {
                            // Label keeps its minimum: never narrower
                            // than max(140dp, 50%) and never narrower
                            // than 3 characters of 14sp text (~24px
                            // at scale 1.0, ~31px at 1.3).
                            assertTrue(
                                "$hub panel=$panel scale=$scale label=${plan.labelWidthPx}",
                                plan.labelWidthPx >= minLabel &&
                                    plan.labelWidthPx >= (24f * scale).toInt(),
                            )
                        }
                        AdaptiveRowPlan.Stacked -> {
                            // Stacked: label gets the full row width,
                            // so it cannot wrap per character either.
                            assertTrue(
                                "$hub panel=$panel scale=$scale stacked",
                                rowPx >= (24f * scale).toInt(),
                            )
                        }
                    }
                    // Segmented decision matches the fit computation
                    // independently recomputed here.
                    val segLabels = listOf(70, 50, 60, 40)
                        .map { (it * scale).toInt() }
                    val segAvail = rowPx
                    val seg = segAvail / segLabels.size
                    val fits = seg >= 64 &&
                        segLabels.all { it + 8 <= seg }
                    val mode = segmentedLayoutMode(
                        segAvail, segLabels, 64, 8,
                    )
                    if (fits) {
                        assertEquals(
                            "$hub panel=$panel scale=$scale",
                            SegmentedMode.Equal(seg),
                            mode,
                        )
                    } else {
                        assertEquals(
                            "$hub panel=$panel scale=$scale",
                            SegmentedMode.Chips,
                            mode,
                        )
                    }
                }
            }
        }
    }
}
