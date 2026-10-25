package app.havenos.launcher.data

import app.havenos.launcher.HavenApp
import app.havenos.launcher.ui.parseSnapshot
import app.havenos.launcher.ui.snapshotJson
import org.json.JSONObject

/**
 * Full backup (Settings → Backup & Restore, section-reset undo, daily
 * auto-backup): a settings subset + home items + folders, tagged
 * {"type":"havenos-backup","version":2}. Version 2 (M9 Worker D) adds
 * layout profiles + Calm editor entry options; version 1 backups still
 * import (absent keys keep current values — see applyBackupJson).
 * All parsing never throws.
 */
suspend fun buildBackupJson(app: HavenApp, state: SettingsState, includeWallpapers: Boolean): String {
    val root = JSONObject()
    root.put("type", "havenos-backup")
    root.put("version", BACKUP_VERSION)
    val s = JSONObject()
    s.put("look", state.look)
    s.put("darkMode", state.darkMode)
    s.put("density", state.density)
    s.put("colsLand", state.colsLand); s.put("rowsLand", state.rowsLand)
    s.put("colsPort", state.colsPort); s.put("rowsPort", state.rowsPort)
    s.put("iconShape", state.iconShape)
    s.put("iconSizeOverride", state.iconSizeOverride)
    s.put("labelsOn", state.labelsOn); s.put("labelSize", state.labelSize)
    s.put("fontChoice", state.fontChoice)
    s.put("reduceEffects", state.reduceEffects); s.put("reduceMotion", state.reduceMotion)
    s.put("customLooksJson", state.customLooksJson)
    s.put("placementMode", state.placementMode); s.put("gapDp", state.gapDp)
    s.put("swipeUpDrawer", state.swipeUpDrawer)
    s.put("lockLayout", state.lockLayout); s.put("clockPage", state.clockPage)
    s.put("atAGlance", state.atAGlance); s.put("autoAddApps", state.autoAddApps)
    s.put("dockMax", state.dockMax); s.put("dockRecent", state.dockRecent)
    s.put("dockPos", state.dockPos); s.put("dockAutoHide", state.dockAutoHide)
    s.put("dockSearch", state.dockSearch); s.put("dockDrawerBtn", state.dockDrawerBtn)
    s.put("drawerMode", state.drawerMode); s.put("drawerSort", state.drawerSort)
    s.put("scrollbarStyle", state.scrollbarStyle)
    s.put("hiddenJson", state.hiddenJson); s.put("hiddenLock", state.hiddenLock)
    s.put("catHiddenJson", state.catHiddenJson); s.put("catNamesJson", state.catNamesJson)
    s.put("weatherCity", state.weatherCity); s.put("weatherLat", state.weatherLat)
    s.put("weatherLon", state.weatherLon); s.put("weatherUnits", state.weatherUnits)
    s.put("weatherNetwork", state.weatherNetwork)
    s.put("calendarsJson", state.calendarsJson); s.put("stacksOn", state.stacksOn)
    s.put("widgetRadius", state.widgetRadius); s.put("widgetBg", state.widgetBg)
    s.put("widgetOpacity", state.widgetOpacity)
    s.put("gestureSensitivity", state.gestureSensitivity)
    s.put("animSpeed", state.animSpeed); s.put("pressFeedback", state.pressFeedback)
    s.put("hapticStrength", state.hapticStrength)
    s.put("iconPack", state.iconPack); s.put("monoIcons", state.monoIcons)
    s.put("badgeMode", state.badgeMode)
    s.put("gesturesJson", state.gesturesJson)
    // M7 Wave 2 keys. Every key is written unconditionally (not only
    // when non-default) so a restore is exact; old backups simply lack
    // these entries and the has() guards in applyBackupJson leave the
    // current values untouched — old data never wipes a new key.
    s.put("labelMode", state.labelMode); s.put("snapToGrid", state.snapToGrid)
    s.put("dockRecentsOn", state.dockRecentsOn)
    s.put("libraryDefaultView", state.libraryDefaultView)
    s.put("todayOn", state.todayOn); s.put("smartStackAutoRotate", state.smartStackAutoRotate)
    s.put("categoryOverridesJson", state.categoryOverridesJson)
    s.put("calmFavoritesJson", state.calmFavoritesJson)
    s.put("calmScheduleJson", state.calmScheduleJson)
    s.put("calmEntryJson", state.calmEntryJson)
    // M8 pages. Written unconditionally like the M7 keys above.
    s.put("pageIndicatorOn", state.pageIndicatorOn)
    s.put("parallaxOn", state.parallaxOn)
    s.put("defaultPage", state.defaultPage)
    s.put("hiddenPagesJson", state.hiddenPagesJson)
    s.put("pagesMigrated", state.pagesMigrated)
    s.put("pageCount", state.pageCount)
    s.put("navGesturesOn", state.navGesturesOn); s.put("navMapJson", state.navMapJson)
    s.put("navEdgeDp", state.navEdgeDp); s.put("navSensitivity", state.navSensitivity)
    s.put("navHaptics", state.navHaptics); s.put("navHideBar", state.navHideBar)
    s.put("searchContacts", state.searchContacts); s.put("searchCalendar", state.searchCalendar)
    s.put("searchSettings", state.searchSettings); s.put("searchCalc", state.searchCalc)
    s.put("profilesJson", state.profilesJson); s.put("activeProfile", state.activeProfile)
    if (includeWallpapers) {
        s.put("wallpaperMode", state.wallpaperMode); s.put("wpKind", state.wpKind)
        s.put("wpUri", state.wpUri); s.put("wpDim", state.wpDim); s.put("wpZoom", state.wpZoom)
        s.put("wpOffX", state.wpOffX.toDouble()); s.put("wpOffY", state.wpOffY.toDouble())
        s.put("wpTint", state.wpTint); s.put("wpSolid", state.wpSolid); s.put("wpParallax", state.wpParallax)
    }
    root.put("settings", s)
    val dockArr = org.json.JSONArray()
    for (k in state.dock) dockArr.put(k)
    root.put("dock", dockArr)
    val snap = try {
        JSONObject(snapshotJson(app.db.homeItems().all(), app.db.folders().all()))
    } catch (t: Throwable) {
        JSONObject()
    }
    root.put("items", snap.optJSONArray("items") ?: org.json.JSONArray())
    root.put("folders", snap.optJSONArray("folders") ?: org.json.JSONArray())
    // M9: layout profiles (canonical order + per-class placements).
    // Best-effort: a failed read exports no profiles rather than failing
    // the whole backup; profiles re-derive on first use anyway.
    try {
        val rows = app.db.layoutProfiles().all()
        val byClass = mutableMapOf<WindowClass, MutableMap<String, ProfilePlacement>>()
        for (row in rows) {
            val windowClass = try {
                WindowClass.valueOf(row.windowClass)
            } catch (t: Throwable) {
                continue
            }
            byClass.getOrPut(windowClass) { mutableMapOf() }[row.itemId] =
                ProfilePlacement(row.page, row.col, row.row, row.colSpan, row.rowSpan)
        }
        val itemsForCanon = try {
            app.db.homeItems().all()
        } catch (t: Throwable) {
            emptyList()
        }
        root.put(
            "layoutProfiles",
            JSONObject(LayoutProfiles(canonicalOrderOf(itemsForCanon), byClass).toJson()),
        )
    } catch (t: Throwable) {
        // No profiles in this backup; restore still works.
    }
    return root.toString()
}

/** Validate + summarise a backup file; null when it is not a HavenOS backup. */
fun backupSummary(json: String): String? = try {
    val root = JSONObject(json)
    if (root.optString("type") != "havenos-backup") {
        null
    } else {
        val items = root.optJSONArray("items")?.length() ?: 0
        val folders = root.optJSONArray("folders")?.length() ?: 0
        val look = root.optJSONObject("settings")?.optString("look", "?") ?: "?"
        "Backup: $items home items, $folders folders, look “$look”, version ${root.optInt("version", 0)}."
    }
} catch (t: Throwable) {
    null
}

/** Apply a backup built by [buildBackupJson]. False when the file is invalid. */
suspend fun applyBackupJson(app: HavenApp, json: String): Boolean {
    val root = try {
        JSONObject(json)
    } catch (t: Throwable) {
        return false
    }
    if (root.optString("type") != "havenos-backup") return false
    // M9: unsupported versions change nothing (missing version = v1, the
    // historical default). See validateBackupImport for the rule.
    if (root.has("version") && root.optInt("version", 1) !in 1..BACKUP_VERSION) return false
    // M6 import-undo: snapshot the pre-import layout into undoJson BEFORE
    // any write, so a later Undo can restore it. Invalid files return
    // above and never reach this write, so they still change nothing.
    // This snapshot covers layout/dock and the snapshot's few settings
    // (see applyLayoutSnapshot) — it is not a full settings rollback.
    try {
        val before = app.settings.persistedState()
        val items = app.db.homeItems().all()
        val folders = app.db.folders().all()
        app.settings.setUndoJson(layoutSnapshotJson(items, folders, before))
    } catch (t: Throwable) {
        // Snapshot failure must not block the import itself.
    }
    val s = root.optJSONObject("settings")
    if (s != null) {
        val st = app.settings
        if (s.has("look")) st.setLook(s.optString("look", "Minimal"))
        if (s.has("darkMode")) st.setDarkMode(s.optInt("darkMode", 0))
        if (s.has("density")) st.setDensity(s.optInt("density", 40))
        if (s.has("colsLand")) {
            st.setGridOverride(true, s.optInt("colsLand", -1), s.optInt("rowsLand", -1))
        }
        if (s.has("colsPort")) {
            st.setGridOverride(false, s.optInt("colsPort", -1), s.optInt("rowsPort", -1))
        }
        if (s.has("iconShape")) st.setIconShape(s.optInt("iconShape", 1))
        if (s.has("iconSizeOverride")) st.setIconSizeOverride(s.optInt("iconSizeOverride", -1))
        if (s.has("labelsOn")) st.setLabels(s.optBoolean("labelsOn", true), s.optInt("labelSize", 11))
        if (s.has("fontChoice")) st.setFontChoice(s.optInt("fontChoice", 0))
        if (s.has("reduceEffects")) st.setReduceEffects(s.optBoolean("reduceEffects", false))
        if (s.has("reduceMotion")) st.setReduceMotion(s.optBoolean("reduceMotion", false))
        if (s.has("customLooksJson")) st.setCustomLooksJson(s.optString("customLooksJson", "{}"))
        if (s.has("placementMode")) st.setPlacementMode(s.optInt("placementMode", 0))
        if (s.has("swipeUpDrawer")) st.setSwipeUpDrawer(s.optBoolean("swipeUpDrawer", true))
        if (s.has("gapDp")) st.setGapDp(s.optInt("gapDp", 4))
        if (s.has("lockLayout")) st.setLockLayout(s.optBoolean("lockLayout", false))
        if (s.has("clockPage")) st.setClockPage(s.optBoolean("clockPage", true))
        if (s.has("atAGlance")) st.setAtAGlance(s.optInt("atAGlance", 1))
        if (s.has("autoAddApps")) st.setAutoAddApps(s.optBoolean("autoAddApps", true))
        if (s.has("dockMax")) st.setDockMax(s.optInt("dockMax", 6))
        if (s.has("dockRecent")) st.setDockRecent(s.optBoolean("dockRecent", true))
        if (s.has("dockPos")) st.setDockPos(s.optInt("dockPos", 0))
        if (s.has("dockAutoHide")) st.setDockAutoHide(s.optBoolean("dockAutoHide", false))
        if (s.has("dockSearch")) st.setDockSearch(s.optBoolean("dockSearch", true))
        if (s.has("dockDrawerBtn")) st.setDockDrawerBtn(s.optBoolean("dockDrawerBtn", true))
        if (s.has("drawerMode")) st.setDrawerMode(s.optInt("drawerMode", 1))
        if (s.has("drawerSort")) st.setDrawerSort(s.optInt("drawerSort", 0))
        if (s.has("scrollbarStyle")) st.setScrollbarStyle(s.optInt("scrollbarStyle", 1))
        if (s.has("hiddenJson")) st.setHiddenJson(s.optString("hiddenJson", "[]"))
        if (s.has("hiddenLock")) st.setHiddenLock(s.optBoolean("hiddenLock", false))
        if (s.has("catHiddenJson")) st.setCatHiddenJson(s.optString("catHiddenJson", "[]"))
        if (s.has("catNamesJson")) st.setCatNamesJson(s.optString("catNamesJson", "{}"))
        if (s.has("weatherCity")) {
            st.setWeather(
                s.optString("weatherCity", ""),
                s.optString("weatherLat", ""),
                s.optString("weatherLon", ""),
            )
        }
        if (s.has("weatherUnits")) st.setWeatherUnits(s.optInt("weatherUnits", 0))
        if (s.has("weatherNetwork")) st.setWeatherNetwork(s.optBoolean("weatherNetwork", true))
        if (s.has("calendarsJson")) st.setCalendarsJson(s.optString("calendarsJson", ""))
        if (s.has("stacksOn")) st.setStacksOn(s.optBoolean("stacksOn", true))
        if (s.has("widgetRadius")) {
            st.setWidgetFrame(
                s.optInt("widgetRadius", 12),
                s.optInt("widgetBg", 0),
                s.optInt("widgetOpacity", 100),
            )
        }
        if (s.has("gestureSensitivity")) st.setGestureSensitivity(s.optInt("gestureSensitivity", 50))
        if (s.has("animSpeed")) st.setAnimSpeed(s.optInt("animSpeed", 2))
        if (s.has("pressFeedback")) st.setPressFeedback(s.optBoolean("pressFeedback", true))
        if (s.has("hapticStrength")) st.setHapticStrength(s.optInt("hapticStrength", 50))
        if (s.has("iconPack")) st.setIconPack(s.optString("iconPack", ""))
        if (s.has("monoIcons")) st.setMonoIcons(s.optBoolean("monoIcons", false))
        if (s.has("badgeMode")) st.setBadgeMode(s.optInt("badgeMode", 1))
        if (s.has("gesturesJson")) st.setGesturesJson(s.optString("gesturesJson", DEFAULT_GESTURES))
        // M7 Wave 2 keys. Setters normalize labelMode / libraryDefaultView,
        // so a hand-edited backup with an unknown value lands on the default.
        // Absent keys (any pre-Wave-2 backup) are skipped, never reset.
        if (s.has("labelMode")) st.setLabelMode(s.optString("labelMode", "auto"))
        if (s.has("snapToGrid")) st.setSnapToGrid(s.optBoolean("snapToGrid", true))
        if (s.has("dockRecentsOn")) st.setDockRecentsOn(s.optBoolean("dockRecentsOn", true))
        if (s.has("libraryDefaultView")) st.setLibraryDefaultView(s.optString("libraryDefaultView", "all"))
        if (s.has("todayOn")) st.setTodayOn(s.optBoolean("todayOn", true))
        if (s.has("smartStackAutoRotate")) st.setSmartStackAutoRotate(s.optBoolean("smartStackAutoRotate", false))
        if (s.has("categoryOverridesJson")) st.setCategoryOverridesJson(s.optString("categoryOverridesJson", ""))
        if (s.has("calmFavoritesJson")) st.setCalmFavoritesJson(s.optString("calmFavoritesJson", ""))
        if (s.has("calmScheduleJson")) st.setCalmScheduleJson(s.optString("calmScheduleJson", ""))
        if (s.has("calmEntryJson")) st.setCalmEntryJson(s.optString("calmEntryJson", ""))
        // M8 pages. Absent keys (any pre-M8 backup) are skipped here;
        // the items block below then clears pagesMigrated, so the host
        // migration runs over the imported scroll-flow items instead
        // of leaving them unpaged on a migrated device.
        if (s.has("pageIndicatorOn")) st.setPageIndicatorOn(s.optBoolean("pageIndicatorOn", true))
        if (s.has("parallaxOn")) st.setParallaxOn(s.optBoolean("parallaxOn", true))
        if (s.has("defaultPage")) st.setDefaultPage(s.optInt("defaultPage", 0))
        if (s.has("hiddenPagesJson")) st.setHiddenPagesJson(s.optString("hiddenPagesJson", ""))
        if (s.has("pagesMigrated")) st.setPagesMigrated(s.optBoolean("pagesMigrated", false))
        if (s.has("pageCount")) st.setPageCount(s.optInt("pageCount", 0))
        if (s.has("navGesturesOn")) st.setNavGesturesOn(s.optBoolean("navGesturesOn", false))
        if (s.has("navMapJson")) st.setNavMapJson(s.optString("navMapJson", DEFAULT_NAV_MAP))
        if (s.has("navEdgeDp")) st.setNavEdgeDp(s.optInt("navEdgeDp", 24))
        if (s.has("navSensitivity")) st.setNavSensitivity(s.optInt("navSensitivity", 50))
        if (s.has("navHaptics")) st.setNavHaptics(s.optBoolean("navHaptics", true))
        if (s.has("navHideBar")) st.setNavHideBar(s.optBoolean("navHideBar", false))
        if (s.has("searchContacts")) {
            st.setSearchSources(
                s.optBoolean("searchContacts", false),
                s.optBoolean("searchCalendar", false),
                s.optBoolean("searchSettings", true),
                s.optBoolean("searchCalc", true),
            )
        }
        if (s.has("profilesJson")) st.setProfilesJson(s.optString("profilesJson", "[]"))
        if (s.has("activeProfile")) st.setActiveProfile(s.optString("activeProfile", "Default"))
        if (s.has("wallpaperMode")) st.setWallpaperMode(s.optInt("wallpaperMode", 0))
        if (s.has("wpKind")) st.setWpKind(s.optInt("wpKind", 0))
        if (s.has("wpUri")) st.setWpUri(s.optString("wpUri", ""))
        if (s.has("wpDim")) st.setWpDim(s.optInt("wpDim", 15))
        if (s.has("wpZoom")) st.setWpZoom(s.optInt("wpZoom", 100))
        if (s.has("wpOffX")) {
            st.setWpOffsets(
                s.optDouble("wpOffX", 0.0).toFloat(),
                s.optDouble("wpOffY", 0.0).toFloat(),
            )
        }
        if (s.has("wpTint")) st.setWpTint(s.optInt("wpTint", 0))
        if (s.has("wpSolid")) st.setWpSolid(s.optInt("wpSolid", 0))
        if (s.has("wpParallax")) st.setWpParallax(s.optBoolean("wpParallax", false))
    }
    if (root.has("dock")) {
        val arr = root.optJSONArray("dock")
        val dock = mutableListOf<String>()
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val v = arr.optString(i, "")
                if (v.isNotBlank()) dock += v
            }
        }
        app.settings.setDock(dock)
    }
    // M9: restore layout profiles when present (v2). A present-but-
    // unparseable profiles block is skipped, never a reason to fail the
    // whole import; v1 backups have no block and keep current profiles.
    if (root.has("layoutProfiles")) {
        try {
            val profiles = parseLayoutProfiles(root.optJSONObject("layoutProfiles")?.toString() ?: "")
            val rows = profiles.profiles.flatMap { (windowClass, map) ->
                map.map { (id, p) ->
                    LayoutProfileRow(
                        key = "${windowClass.name}|$id",
                        windowClass = windowClass.name,
                        itemId = id,
                        page = p.page, col = p.col, row = p.row,
                        colSpan = p.colSpan, rowSpan = p.rowSpan,
                    )
                }
            }
            if (rows.isNotEmpty()) app.db.layoutProfiles().replaceAll(rows)
        } catch (t: Throwable) {
            // Skip profiles only; items/settings below still restore.
        }
    }
    if (root.has("items") || root.has("folders")) {
        val rawItems = root.optJSONArray("items")
        val (items, folders) = parseSnapshot(json)
        // Fail loud: a backup whose items array is non-empty must never
        // restore to an empty home while claiming success.
        if (rawItems != null && rawItems.length() > 0 && items.isEmpty()) return false
        app.db.homeItems().replaceAll(items)
        app.db.folders().replaceAll(folders)
        // Pre-M8 backups carry no pagesMigrated key: their items are
        // scroll-flow items, so clear the gate and let the host migrate
        // them to pages. M8 backups always carry the key (applied above).
        if (s?.has("pagesMigrated") != true) {
            app.settings.setPagesMigrated(false)
            // Pre-M8 items carry no page floor either: derive from items.
            app.settings.setPageCount(0)
        }
    }
    return true
}
