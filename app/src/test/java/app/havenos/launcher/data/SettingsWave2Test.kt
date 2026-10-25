package app.havenos.launcher.data

import app.havenos.launcher.ui.LibraryView
import app.havenos.launcher.ui.categoryOverridesWithMove
import app.havenos.launcher.ui.encodeCalmSchedule
import app.havenos.launcher.ui.CalmSchedule
import app.havenos.launcher.ui.labelModeFromKey
import app.havenos.launcher.ui.migrateToFlow
import app.havenos.launcher.ui.parseCalmFavorites
import app.havenos.launcher.ui.parseCalmSchedule
import app.havenos.launcher.ui.parseCategoryOverrides
import app.havenos.launcher.ui.parseSnapshot
import app.havenos.launcher.ui.snapshotJson
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M7 Wave 2 — settings keys, migration safety, and JSON codecs.
 *
 * Pure-JVM scope, deliberately: DataStore writes (Settings setters) and
 * Room / applyBackupJson need an Android runtime (HavenApp), so the
 * device-side half — write -> read -> backup -> restore through the
 * real store — is coordinator integration work, not faked here. What
 * this file does cover is everything checkable without that runtime:
 * the contract defaults, the normalization parse() mirrors, the JSON
 * codecs for the string keys, and the position-preserving layout
 * migration. Deliberately NOT duplicated here (already covered by
 * Wave 1 tests): category move/rename round-trip and malformed-input
 * handling (TodayCategoriesCalmTest), size-class boundaries and
 * gridColumns (HomeTokensTest), default-density gridCells
 * (GridTest.gridCells_defaultDensityMatchesM1Defaults).
 */
class SettingsWave2Test {

    // ------------------------------------------------------------ defaults

    @Test
    fun newKeys_carryContractDefaults() {
        val s = SettingsState()
        assertEquals("auto", s.labelMode)
        assertTrue(s.snapToGrid)
        assertTrue(s.dockRecentsOn)
        assertEquals("all", s.libraryDefaultView)
        assertTrue(s.todayOn)
        assertFalse(s.smartStackAutoRotate)
        assertEquals("", s.categoryOverridesJson)
        assertEquals("", s.calmFavoritesJson)
        assertEquals("", s.calmScheduleJson)
    }

    @Test
    fun oldData_newKeysDefaultWithoutTouchingLegacyFields() {
        // A pre-Wave-2 state (legacy fields set, new fields untouched)
        // keeps its legacy values and picks up the new defaults — the
        // state-level shape of "old data keeps positions, new defaults
        // apply". Nothing here re-flows or resets anything.
        val old = SettingsState(
            density = 55,
            dock = listOf("dialer", "messages"),
            drawerMode = 2,
        )
        assertEquals(55, old.density)
        assertEquals(listOf("dialer", "messages"), old.dock)
        assertEquals(2, old.drawerMode)
        assertEquals("auto", old.labelMode)
        assertTrue(old.snapToGrid)
        assertTrue(old.dockRecentsOn)
        assertEquals("all", old.libraryDefaultView)
        assertTrue(old.todayOn)
        assertFalse(old.smartStackAutoRotate)
        assertEquals("", old.categoryOverridesJson)
        assertEquals("", old.calmFavoritesJson)
        assertEquals("", old.calmScheduleJson)
    }

    @Test
    fun newKeys_surviveDataClassCopyRoundTrip() {
        val edited = SettingsState().copy(
            labelMode = "pill",
            snapToGrid = false,
            dockRecentsOn = false,
            libraryDefaultView = "categories",
            todayOn = false,
            smartStackAutoRotate = true,
            categoryOverridesJson = categoryOverridesWithMove("", "app1", "finance"),
            calmFavoritesJson = "[\"app1\"]",
            calmScheduleJson = encodeCalmSchedule(
                CalmSchedule(enabled = true, days = setOf(2, 3), startMinutes = 1320, endMinutes = 1380),
            ),
        )
        // Copy is the transport SettingsState takes through drafts,
        // previews and session snapshots; every new field must survive it.
        val restored = edited.copy()
        assertEquals(edited, restored)
        assertEquals("pill", restored.labelMode)
        assertFalse(restored.snapToGrid)
        assertEquals("categories", restored.libraryDefaultView)
        assertEquals("finance", parseCategoryOverrides(restored.categoryOverridesJson).moves["app1"])
        assertEquals(listOf("app1"), parseCalmFavorites(restored.calmFavoritesJson))
        assertTrue(parseCalmSchedule(restored.calmScheduleJson).enabled)
    }

    // ------------------------------------------- normalization (parse mirrors)

    @Test
    fun labelMode_unknownAndBlankFallBackToAuto() {
        assertEquals(app.havenos.launcher.ui.LabelMode.Auto, labelModeFromKey("auto"))
        assertEquals(app.havenos.launcher.ui.LabelMode.Pill, labelModeFromKey("pill"))
        assertEquals(app.havenos.launcher.ui.LabelMode.Off, labelModeFromKey("off"))
        assertEquals(app.havenos.launcher.ui.LabelMode.Auto, labelModeFromKey("bogus"))
        assertEquals(app.havenos.launcher.ui.LabelMode.Auto, labelModeFromKey(""))
        assertEquals(app.havenos.launcher.ui.LabelMode.Auto, labelModeFromKey(null))
    }

    @Test
    fun libraryView_unknownAndBlankFallBackToAll() {
        assertEquals(LibraryView.ALL, LibraryView.fromKey("all"))
        assertEquals(LibraryView.CATEGORIES, LibraryView.fromKey("categories"))
        assertEquals(LibraryView.LIST, LibraryView.fromKey("list"))
        assertEquals(LibraryView.ALL, LibraryView.fromKey("grid"))
        assertEquals(LibraryView.ALL, LibraryView.fromKey(""))
        assertEquals(LibraryView.ALL, LibraryView.fromKey(null))
    }

    // ------------------------------------------------------- JSON key codecs

    @Test
    fun categoryOverrides_defaultEmptyStringParsesToEmpty() {
        // The new default ("") must mean "no overrides", not an error.
        assertEquals(emptyMap<String, String>(), parseCategoryOverrides("").moves)
        assertEquals(emptyMap<String, String>(), parseCategoryOverrides("").names)
    }

    @Test
    fun calmDefaults_blankMeansNoFavoritesAndDisabledSchedule() {
        assertEquals(emptyList<String>(), parseCalmFavorites(""))
        val schedule = parseCalmSchedule("")
        assertFalse(schedule.enabled)
        assertTrue(schedule.days.isEmpty())
        assertFalse(parseCalmSchedule("{bad").enabled)
    }

    @Test
    fun calmSchedule_encodeParseRoundTrip() {
        val original = CalmSchedule(
            enabled = true,
            days = setOf(1, 2, 3, 4, 5, 6, 7),
            startMinutes = 22 * 60,
            endMinutes = 23 * 60 + 30,
        )
        assertEquals(original, parseCalmSchedule(encodeCalmSchedule(original)))
    }

    // ------------------------------------------- backup shape (old vs new)

    @Test
    fun oldBackupSettingsObject_lacksEveryNewKey() {
        // applyBackupJson guards every new key with has(): an old backup
        // (built before Wave 2, so without these entries) must skip them
        // and leave the current values untouched, never reset them.
        // This test pins the old shape; the guard code is the mechanism.
        val oldSettings = JSONObject()
            .put("darkMode", 2)
            .put("density", 40)
            .put("dockRecent", true)
        for (key in listOf(
            "labelMode", "snapToGrid", "dockRecentsOn", "libraryDefaultView",
            "todayOn", "smartStackAutoRotate", "categoryOverridesJson",
            "calmFavoritesJson", "calmScheduleJson",
        )) {
            assertFalse("old backup must not carry $key", oldSettings.has(key))
        }
    }

    @Test
    fun newBackupSettingsObject_readsBackWithApplyDefaults() {
        // The same opt* calls applyBackupJson uses, with the same
        // fallbacks, on a new backup carrying non-default values.
        val s = JSONObject()
            .put("labelMode", "pill")
            .put("snapToGrid", false)
            .put("dockRecentsOn", false)
            .put("libraryDefaultView", "list")
            .put("todayOn", false)
            .put("smartStackAutoRotate", true)
            .put("categoryOverridesJson", "{\"moves\":{},\"names\":{}}")
            .put("calmFavoritesJson", "[\"a\"]")
            .put("calmScheduleJson", "{\"enabled\":false,\"days\":[],\"start\":0,\"end\":0}")
        assertEquals("pill", s.optString("labelMode", "auto"))
        assertFalse(s.optBoolean("snapToGrid", true))
        assertFalse(s.optBoolean("dockRecentsOn", true))
        assertEquals("list", s.optString("libraryDefaultView", "all"))
        assertFalse(s.optBoolean("todayOn", true))
        assertTrue(s.optBoolean("smartStackAutoRotate", false))
        assertEquals("[\"a\"]", s.optString("calmFavoritesJson", ""))
    }

    // ------------------------------------------- layout migration safety

    @Test
    fun snapshotRoundTrip_preservesPositionsAndFlowOrder() {
        val items = listOf(
            HomeItem("clock", ItemKind.CLOCK, 0, 0, 0, 8, 4, "serif", flowOrder = 0),
            HomeItem("app:a", ItemKind.APP, 0, 0, 4, 2, 2, "a", flowOrder = 1),
            // Pre-vc5 item: no flowOrder yet (-1) — must survive, not drop.
            HomeItem("app:b", ItemKind.APP, 0, 2, 4, 2, 2, "b", flowOrder = -1),
        )
        val folders = listOf(Folder("f1", "Tools", "[\"app:a\"]"))
        val (back, backFolders) = parseSnapshot(snapshotJson(items, folders))
        assertEquals(items, back)
        assertEquals(folders, backFolders)
    }

    @Test
    fun migrateToFlow_numbersOnlyUnmigratedAndNeverRenumbers() {
        val migrated = HomeItem("app:a", ItemKind.APP, 0, 0, 4, 2, 2, "a", flowOrder = 5)
        val unmigrated = HomeItem("app:b", ItemKind.APP, 0, 2, 4, 2, 2, "b", flowOrder = -1)
        val out = migrateToFlow(listOf(migrated, unmigrated), emptyList())
        // The user's existing position (5) is untouched; the unmigrated
        // item takes the next slot (6). Re-running is a no-op.
        assertEquals(5, out.first { it.id == "app:a" }.flowOrder)
        assertEquals(6, out.first { it.id == "app:b" }.flowOrder)
        assertEquals(out, migrateToFlow(out, emptyList()))
    }

    // ------------------------------------------------------- grid computation

    @Test
    fun gridCells_explicitOverridesBeatDensity() {
        // Default-density behaviour is covered by GridTest; overrides
        // (the "Use new default" / Follow-density counterpart) were not.
        val overridden = SettingsState(colsPort = 5, rowsPort = 7, colsLand = 9, rowsLand = 4)
        assertEquals(5 to 7, overridden.gridCells(landscape = false))
        assertEquals(9 to 4, overridden.gridCells(landscape = true))
        // Clearing back to -1 returns to the density-derived grid.
        val cleared = overridden.copy(colsPort = -1, rowsPort = -1, colsLand = -1, rowsLand = -1)
        assertEquals(SettingsState().gridCells(landscape = false), cleared.gridCells(landscape = false))
        assertEquals(SettingsState().gridCells(landscape = true), cleared.gridCells(landscape = true))
    }
}
