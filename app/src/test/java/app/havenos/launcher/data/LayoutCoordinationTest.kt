package app.havenos.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Worker D (M9) tests: coordination, Settings behaviors, permissions, fresh defaults, migration SQL. */
class LayoutCoordinationTest {

    @Test
    fun iconSize_autoDerivesFromCellAndLabel() {
        val withLabels = resolveIconSize(true, -1, 100, 120, true)
        assertTrue(withLabels.isAuto)
        assertEquals(58, withLabels.iconDp) // 100 * 0.58
        assertEquals(null, withLabels.clampNote)
        assertEquals(58, withLabels.touchTargetDp)
        val noLabels = resolveIconSize(true, -1, 100, 120, false)
        assertEquals(68, noLabels.iconDp)
    }

    @Test
    fun iconSize_manualClampsWithNoteAndNeverBelow48Target() {
        val clamped = resolveIconSize(false, 90, 80, 100, true)
        assertFalse(clamped.isAuto)
        assertEquals("Max for this grid", clamped.clampNote)
        assertEquals(60, clamped.iconDp) // 80 - 20 label reserve
        assertTrue(clamped.touchTargetDp >= 48)
        val small = resolveIconSize(false, 40, 200, 200, false)
        assertEquals(48, small.touchTargetDp) // visual 40, target floored at 48
    }

    @Test
    fun modeResolve_systemScheduleAndForced() {
        assertTrue(resolveDark(0, true))
        assertFalse(resolveDark(0, false))
        assertFalse(resolveDark(1, true))
        assertTrue(resolveDark(2, false))
        assertTrue(resolveDark(3, false, scheduleDark = true))
        assertFalse(resolveDark(3, true, scheduleDark = false))
        assertEquals(200, MODE_CROSSFADE_MS)
    }

    @Test
    fun lookTokens_chosenAccentBeatsSystemUnlessWallpaperDerived() {
        val settings = SettingsState(accentMode = 2, accentArgb = 0xFF112233.toInt())
        val tokens = deriveLookTokens(settings, systemDark = false, systemDynamicArgb = 0xFF445566.toInt())
        assertEquals(0xFF112233.toInt(), tokens.accentArgb)
        assertTrue(tokens.modified)
        val derived = deriveLookTokens(
            settings.copy(accentMode = 3, accentArgb = 0),
            systemDark = false, systemDynamicArgb = 0xFF445566.toInt(),
        )
        assertEquals(0xFF445566.toInt(), derived.accentArgb)
        val minimal = deriveLookTokens(SettingsState(), systemDark = false)
        assertFalse(minimal.modified)
        assertEquals(0, minimal.accentArgb)
    }

    @Test
    fun dockInsets_andIndicatorPlacement() {
        assertEquals(DockInsets(84, 0, 0, 0), dockInsets(0))
        assertEquals(DockInsets(0, 84, 0, 0), dockInsets(1))
        assertEquals(DockInsets(0, 0, 84, 0), dockInsets(2))
        assertEquals("above-bottom-dock", indicatorEdge(0))
        assertEquals("opposite-edge-bottom", indicatorEdge(1))
        assertEquals(240, DOCK_MOVE_ANIM_MS)
    }

    @Test
    fun wallpaperPipeline_suggestsButNeverAutoApplies() {
        val result = wallpaperPipelineResult(paletteArgb = 0xFF123456.toInt(), null, null)
        assertTrue(result.suggestAccent)
        assertEquals(0xFF123456.toInt(), result.suggestedAccentArgb)
        assertTrue(result.legibilityRecompute)
        assertTrue(result.scrimRecompute)
        assertTrue(result.calmDimRecompute)
        // Per-page falls back to global when missing.
        val fallback = wallpaperPipelineResult(null, null, 0xFF999999.toInt())
        assertEquals(0xFF999999.toInt(), fallback.suggestedAccentArgb)
        val none = wallpaperPipelineResult(null, null, null)
        assertFalse(none.suggestAccent)
    }

    @Test
    fun lookChanges_listsDifferencesWithRevertValues() {
        val before = SettingsState()
        val current = before.copy(darkMode = 2, dockPos = 1)
        val changes = lookChanges(before, current)
        assertEquals(setOf("Mode", "Dock position"), changes.map { it.name }.toSet())
        val mode = changes.first { it.name == "Mode" }
        assertEquals("0", mode.before)
        assertEquals("2", mode.after)
        assertTrue(lookChanges(before, before).isEmpty())
    }

    @Test
    fun permissionMap_everyRowHasReasonAndDeniedState() {
        val map = permissionMap()
        assertTrue(map.size >= 6)
        for (row in map) {
            assertTrue(row.reason.isNotBlank())
            assertTrue(row.deniedState.isNotBlank())
        }
        val (enabled, _) = permissionStateUpdate(true, "disabled")
        assertTrue(enabled)
        val (disabled, state) = permissionStateUpdate(false, "Needs-permission state")
        assertFalse(disabled)
        assertEquals("Needs-permission state", state)
    }

    @Test
    fun defaultLauncherTransition_bannerAndHomeButton() {
        val asDefault = defaultLauncherTransition(true)
        assertFalse(asDefault.bannerVisible)
        assertEquals(0, asDefault.homeButtonPage)
        assertTrue(asDefault.closeOverlays)
        val notDefault = defaultLauncherTransition(false)
        assertTrue(notDefault.bannerVisible)
        assertEquals(-1, notDefault.homeButtonPage)
    }

    @Test
    fun motion_reducedMotionOverridesSpeed() {
        assertEquals(0 to true, effectiveMotion(120, 2, true))
        assertEquals(0 to true, effectiveMotion(120, 0, false))
        assertEquals(60 to false, effectiveMotion(120, 1, false))
        assertEquals(120 to false, effectiveMotion(120, 2, false))
        assertEquals(180 to false, effectiveMotion(120, 3, false))
        assertEquals(120, ICON_RESIZE_CROSSFADE_MS)
        assertTrue(hapticEnabled(50, true))
        assertFalse(hapticEnabled(0, true))
        assertFalse(hapticEnabled(50, false))
    }

    @Test
    fun sliderDebounce_200msGate() {
        assertFalse(sliderWriteDue(1000L, 1199L))
        assertTrue(sliderWriteDue(1000L, 1200L))
        assertEquals(200L, SLIDER_DEBOUNCE_MS)
    }

    @Test
    fun backupValidation_v1OkV3RejectedWrongTypeRejected() {
        assertNull(validateBackupImport("havenos-backup", 1, true))
        assertNull(validateBackupImport("havenos-backup", 2, true))
        assertTrue(validateBackupImport("havenos-backup", 3, true)!!.isNotBlank())
        assertTrue(validateBackupImport("other", 1, true)!!.isNotBlank())
        assertTrue(validateBackupImport("havenos-backup", 1, false)!!.isNotBlank())
        assertEquals(2, BACKUP_VERSION)
    }

    @Test
    fun dockIntentResolution_picksFirstInstalledPerRoleInOrderDeduped() {
        val resolved = mapOf(
            DockIntentRole.PHONE to listOf("com.example.dialer", "com.example.dialer2"),
            DockIntentRole.MESSAGES to listOf("com.example.dialer"), // same pkg, deduped
            DockIntentRole.BROWSER to listOf("com.example.browser"),
            DockIntentRole.CAMERA to listOf("com.example.camera"),
        )
        val installed = setOf("com.example.dialer", "com.example.browser", "com.example.camera")
        assertEquals(
            listOf("com.example.dialer", "com.example.browser", "com.example.camera"),
            resolveDockFromIntents(resolved, installed),
        )
        // Uninstalled packages are skipped, never pinned.
        assertTrue(resolveDockFromIntents(resolved, emptySet()).isEmpty())
    }

    @Test
    fun freshInstall_oneAgendaCardNoDuplicates() {
        val cards = freshInstallTodayCards()
        assertEquals(1, cards.count { it.kind == ItemKind.AGENDA })
        assertTrue(cards.any { it.kind == ItemKind.CLOCK })
        val withAgenda = listOf(HomeItem("ag", ItemKind.AGENDA, 0, 0, 0, 4, 4, "x"))
        assertTrue(agendaSeedWouldDuplicate(withAgenda))
        assertFalse(agendaSeedWouldDuplicate(emptyList()))
    }

    @Test
    fun a11yActions_widgetsGetResizeAppsDoNot() {
        assertTrue("Resize" in a11yActionsForItem(true))
        assertFalse("Resize" in a11yActionsForItem(false))
        assertTrue("Add to dock" in a11yActionsForItem(false))
    }

    @Test
    fun calmSnapshot_restoresPageAndEditState() {
        val snap = calmEnterSnapshot(pageIndex = 2, editMode = false, overlaysOpen = true)
        assertEquals(2, snap.pageIndex)
        assertTrue(snap.dockVisible)
        assertTrue(calmDimAlpha(15, false) in 0f..0.9f)
        assertTrue(calmDimAlpha(60, true) > calmDimAlpha(0, false))
    }

    @Test
    fun migration34Sql_createsBothNewTables() {
        val sql = migration34Sql()
        assertEquals(2, sql.size)
        val joined = sql.joinToString(" ")
        assertTrue(joined.contains("layout_profiles"))
        assertTrue(joined.contains("layout_snapshots"))
        assertTrue(joined.contains("windowClass"))
        assertTrue(joined.contains("layoutJson"))
        assertEquals(3, MIGRATION_3_4.startVersion)
        assertEquals(4, MIGRATION_3_4.endVersion)
    }
}
