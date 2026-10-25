package app.havenos.launcher.ui.settings.shell

import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.Settings
import app.havenos.launcher.data.SettingsState

// ---------------------------------------------------------------------------
// M6 integration: session helpers moved out of the deleted M5 shell
// (ui/settings/SettingsHub.kt + PreviewMini.kt). sessionChanged and
// restoreSettingsSnapshot keep their M5 semantics; the restore below
// additionally covers the three keys commitDraft never wrote
// (catMergeJson, badgeHiddenJson, iconSwipesJson), so a session revert
// after a category merge, a badge silence, or an icon-swipe edit is
// exact instead of leaving the edit behind.
// ---------------------------------------------------------------------------

/** True when [current] differs from the entry [snapshot] in any field. */
fun sessionChanged(snapshot: SettingsState, current: SettingsState): Boolean =
    snapshot != current

/** Write every field of [draft] back into the store (full-state restore). */
private suspend fun commitSnapshot(settings: Settings, draft: SettingsState) {
    settings.setLook(draft.look)
    settings.setDarkMode(draft.darkMode)
    settings.setDensity(draft.density)
    if (draft.colsLand < 0 && draft.rowsLand < 0) settings.clearGridOverride(true)
    else settings.setGridOverride(true, draft.colsLand, draft.rowsLand)
    if (draft.colsPort < 0 && draft.rowsPort < 0) settings.clearGridOverride(false)
    else settings.setGridOverride(false, draft.colsPort, draft.rowsPort)
    settings.setIconShape(draft.iconShape)
    settings.setIconSizeOverride(draft.iconSizeOverride)
    settings.setLabels(draft.labelsOn, draft.labelSize)
    settings.setFontChoice(draft.fontChoice)
    settings.setReduceEffects(draft.reduceEffects)
    settings.setReduceMotion(draft.reduceMotion)
    settings.setCustomLooksJson(draft.customLooksJson)
    settings.setWallpaperMode(draft.wallpaperMode)
    settings.setWpKind(draft.wpKind)
    settings.setWpUri(draft.wpUri)
    settings.setWpDim(draft.wpDim)
    settings.setWpZoom(draft.wpZoom)
    settings.setWpOffsets(draft.wpOffX, draft.wpOffY)
    settings.setWpTint(draft.wpTint)
    settings.setWpSolid(draft.wpSolid)
    settings.setWpParallax(draft.wpParallax)
    settings.setPlacementMode(draft.placementMode)
    settings.setSnapToGrid(draft.snapToGrid)
    settings.setLabelMode(draft.labelMode)
    settings.setDockRecentsOn(draft.dockRecentsOn)
    settings.setLibraryDefaultView(draft.libraryDefaultView)
    settings.setTodayOn(draft.todayOn)
    settings.setSmartStackAutoRotate(draft.smartStackAutoRotate)
    settings.setCategoryOverridesJson(draft.categoryOverridesJson)
    settings.setCalmFavoritesJson(draft.calmFavoritesJson)
    settings.setCalmScheduleJson(draft.calmScheduleJson)
    settings.setSwipeUpDrawer(draft.swipeUpDrawer)
    settings.setGapDp(draft.gapDp)
    settings.setLockLayout(draft.lockLayout)
    settings.setClockPage(draft.clockPage)
    settings.setAtAGlance(draft.atAGlance)
    settings.setAutoAddApps(draft.autoAddApps)
    settings.setDock(draft.dock)
    settings.setDockMax(draft.dockMax)
    settings.setDockRecent(draft.dockRecent)
    settings.setDockPos(draft.dockPos)
    settings.setDockAutoHide(draft.dockAutoHide)
    settings.setDockSearch(draft.dockSearch)
    settings.setDockDrawerBtn(draft.dockDrawerBtn)
    settings.setDrawerMode(draft.drawerMode)
    settings.setDrawerSort(draft.drawerSort)
    settings.setScrollbarStyle(draft.scrollbarStyle)
    settings.setHiddenJson(draft.hiddenJson)
    settings.setHiddenLock(draft.hiddenLock)
    settings.setCatHiddenJson(draft.catHiddenJson)
    settings.setCatNamesJson(draft.catNamesJson)
    settings.setCatMergeJson(draft.catMergeJson)
    settings.setWeather(draft.weatherCity, draft.weatherLat, draft.weatherLon)
    settings.setWeatherUnits(draft.weatherUnits)
    settings.setWeatherNetwork(draft.weatherNetwork)
    settings.setCalendarsJson(draft.calendarsJson)
    settings.setStacksOn(draft.stacksOn)
    settings.setIconPack(draft.iconPack)
    settings.setMonoIcons(draft.monoIcons)
    settings.setBadgeMode(draft.badgeMode)
    settings.setBadgeHiddenJson(draft.badgeHiddenJson)
    settings.setGesturesJson(draft.gesturesJson)
    settings.setIconSwipesJson(draft.iconSwipesJson)
    settings.setNavGesturesOn(draft.navGesturesOn)
    settings.setNavMapJson(draft.navMapJson)
    settings.setNavEdgeDp(draft.navEdgeDp)
    settings.setNavSensitivity(draft.navSensitivity)
    settings.setNavHaptics(draft.navHaptics)
    settings.setNavHideBar(draft.navHideBar)
    settings.setNavSavedPolicy(draft.navSavedPolicy, draft.navSavedPolicySet)
    settings.setSearchSources(
        draft.searchContacts, draft.searchCalendar, draft.searchSettings, draft.searchCalc,
    )
    settings.setSearchHistoryJson(draft.searchHistoryJson)
    settings.setProfilesJson(draft.profilesJson)
    settings.setActiveProfile(draft.activeProfile)
    settings.setProfileSchedule(
        draft.profileSchedule, draft.schedStart, draft.schedEnd, draft.schedProfile,
    )
    settings.setBackupIncludeWp(draft.backupIncludeWp)
    settings.setLastBackupDay(draft.lastBackupDay)
    settings.setUndoJson(draft.undoJson)
    settings.setLaunchStatsJson(draft.launchStatsJson)
    settings.setSeeded(draft.seeded)
    settings.setAccent(draft.accentMode, draft.accentArgb)
    settings.setCornerDp(draft.cornerDp)
    settings.setSurfaceMaterial(draft.surfaceMaterial)
    settings.setSurfaceBlurPct(draft.surfaceBlurPct)
    settings.setSurfaceNoisePct(draft.surfaceNoisePct)
    settings.setShadowDp(draft.shadowDp)
    settings.setOutlineOn(draft.outlineOn)
    settings.setFontWeight(draft.fontWeight)
    settings.setFontScalePct(draft.fontScalePct)
    settings.setGridTablet(draft.colsTablet, draft.rowsTablet)
    settings.setMarginDp(draft.marginDp)
    settings.setCellSpacingDp(draft.cellSpacingDp)
    settings.setLabelLines(draft.labelLines)
    settings.setLabelColorArgb(draft.labelColorArgb)
    settings.setSearchBar(draft.searchBarPos, draft.searchBarStyle, draft.searchBarHeightDp)
    settings.setTodayPage(draft.todayPageOn, draft.todayPageContent)
    settings.setScrollEffect(draft.scrollEffect)
    settings.setStatusBarMode(draft.statusBarMode)
    settings.setWpBlurPct(draft.wpBlurPct)
    settings.setWpAutoDim(draft.wpAutoDim)
    settings.setWpPerPage(draft.wpPerPage)
    settings.setDockRows(draft.dockRows)
    settings.setDockFloating(draft.dockFloating)
    settings.setDockMaterial(draft.dockMaterial, draft.dockOpacityPct)
    settings.setDockMagnify(draft.dockMagnify)
    settings.setDockLabels(draft.dockLabels)
    settings.setDrawerStyle(draft.drawerStyle)
    settings.setDrawerGesture(draft.drawerGesture)
    settings.setDrawerCols(draft.drawerCols)
    settings.setDrawerIconDp(draft.drawerIconDp)
    settings.setDrawerRows(draft.drawerSearchOn, draft.drawerRecentsOn, draft.drawerSuggestionsOn)
    settings.setIconShapeExt(draft.iconShapeExt)
    settings.setPerAppIconJson(draft.perAppIconJson)
    settings.setFolderPreview(draft.folderPreview)
    settings.setFolderAnim(draft.folderAnim)
    settings.setFolderSizePct(draft.folderSizePct)
    settings.setFolderBg(draft.folderBg)
    settings.setFolderTitleOn(draft.folderTitleOn)
    settings.setWidgetFrame(draft.widgetRadius, draft.widgetBg, draft.widgetOpacity)
    settings.setGestureSensitivity(draft.gestureSensitivity)
    settings.setAnimSpeed(draft.animSpeed)
    settings.setPressFeedback(draft.pressFeedback)
    settings.setHapticStrength(draft.hapticStrength)
    settings.setMiniHomeCollapsed(draft.miniHomeCollapsed)
    settings.setLabelOverridesJson(draft.labelOverridesJson)
    // Drawer/icon-shape setters sync side effects; re-apply the plain
    // values last so a revert is exact.
    settings.setDrawerMode(draft.drawerMode)
    settings.setIconShape(draft.iconShape)
}

/** Restore [snapshot] into the store. Settings-only: home layout untouched. */
suspend fun restoreSettingsSnapshot(app: HavenApp, snapshot: SettingsState) {
    commitSnapshot(app.settings, snapshot)
}

/**
 * Per-hub Reset (M5 category resets, moved from SettingsHub.kt).
 * Category 14 (System) has no resettable settings. Kept because the
 * Look hub's panel Reset uses id 1; other hubs supply no Reset rather
 * than a second reset row next to their own reset actions.
 */
suspend fun resetNewCategory(app: HavenApp, id: Int) {
    val s = app.settings
    when (id) {
        1 -> {
            s.resetSection(1)
            s.setAccent(0, 0)
            s.setSurfaceMaterial(2); s.setSurfaceBlurPct(60); s.setSurfaceNoisePct(0)
            s.setShadowDp(-1); s.setOutlineOn(true)
            s.setFontWeight(0); s.setFontScalePct(100)
        }
        2 -> {
            s.resetSection(3)
            s.setGridTablet(-1, -1)
            s.setMarginDp(16); s.setCellSpacingDp(4)
            s.setLabelLines(1); s.setLabelColorArgb(0)
            s.setSearchBar(0, 0, 56)
            s.setTodayPage(false, 0)
            s.setScrollEffect(0); s.setStatusBarMode(0)
        }
        3 -> {
            s.resetSection(2)
            s.setWpBlurPct(0); s.setWpAutoDim(false); s.setWpPerPage(false)
        }
        4 -> {
            s.resetSection(4)
            s.setDockRows(1); s.setDockFloating(true); s.setDockMaterial(0, 100)
            s.setDockMagnify(false); s.setDockLabels(false)
        }
        5 -> {
            s.resetSection(5)
            s.setDrawerStyle(2); s.setDrawerGesture(0)
            s.setDrawerCols(0); s.setDrawerIconDp(0)
            s.setDrawerRows(true, true, true)
        }
        6 -> {
            s.resetSection(7)
            s.setIconShapeExt(1); s.setPerAppIconJson("{}")
        }
        7 -> {
            s.setFolderPreview(0); s.setFolderAnim(0); s.setFolderSizePct(100)
            s.setFolderBg(0); s.setFolderTitleOn(true)
        }
        8 -> {
            s.resetSection(6)
            s.setWidgetFrame(12, 0, 100)
        }
        9 -> {
            s.resetSection(8)
            s.setGestureSensitivity(50)
        }
        10 -> s.resetSection(9)
        11 -> {
            s.setAnimSpeed(2); s.setPressFeedback(true); s.setHapticStrength(50)
        }
        12 -> s.setHiddenLock(false)
        13 -> {
            s.setProfileSchedule(false, 540, 1020, "")
            s.setBackupIncludeWp(true)
        }
        else -> Unit
    }
}

private fun short(value: Any?): String = value.toString().take(48)

private fun darkModeName(v: Int): String = when (v) {
    1 -> "Light"
    2 -> "Dark"
    3 -> "Auto"
    else -> "System"
}

private fun dockPosName(v: Int): String = when (v) {
    1 -> "Left"
    2 -> "Right"
    else -> "Bottom"
}

private fun drawerSortName(v: Int): String = when (v) {
    1 -> "Most used"
    2 -> "Recently installed"
    else -> "A–Z"
}

/**
 * Session diff, split in two so the pure part is unit-testable: the
 * Settings store needs an Android Context, so no JVM test can build
 * one, but the field-by-field diff over two SettingsStates is pure.
 * [sessionDiffFields] is that pure diff; [sessionDiffRecords] wraps
 * each field in a [ChangeRecord] whose revert writes that single field
 * back to its snapshot value through the typed setters.
 */
data class SessionFieldChange(val key: String, val oldText: String, val newText: String)

fun sessionDiffFields(
    snapshot: SettingsState,
    current: SettingsState,
): List<SessionFieldChange> {
    val out = mutableListOf<SessionFieldChange>()
    fun <T> diff(key: String, old: T, new: T, oldText: String, newText: String) {
        if (old != new) out += SessionFieldChange(key, oldText, newText)
    }
    diff("Dark mode", snapshot.darkMode, current.darkMode, darkModeName(snapshot.darkMode), darkModeName(current.darkMode))
    diff("Accent", snapshot.accentMode to snapshot.accentArgb, current.accentMode to current.accentArgb, "${snapshot.accentMode}/${short(snapshot.accentArgb)}", "${current.accentMode}/${short(current.accentArgb)}")
    diff("Material", snapshot.surfaceMaterial, current.surfaceMaterial, short(snapshot.surfaceMaterial), short(current.surfaceMaterial))
    diff("Corner radius", snapshot.cornerDp, current.cornerDp, short(snapshot.cornerDp), short(current.cornerDp))
    diff("Font", snapshot.fontChoice, current.fontChoice, short(snapshot.fontChoice), short(current.fontChoice))
    diff("Density", snapshot.density, current.density, short(snapshot.density), short(current.density))
    diff("Icon size", snapshot.iconSizeOverride, current.iconSizeOverride, short(snapshot.iconSizeOverride), short(current.iconSizeOverride))
    diff("Labels", snapshot.labelsOn to snapshot.labelSize, current.labelsOn to current.labelSize, "${snapshot.labelsOn}/${snapshot.labelSize}", "${current.labelsOn}/${current.labelSize}")
    diff("Lock layout", snapshot.lockLayout, current.lockLayout, short(snapshot.lockLayout), short(current.lockLayout))
    diff("Snap", snapshot.placementMode, current.placementMode, short(snapshot.placementMode), short(current.placementMode))
    diff("Swipe up drawer", snapshot.swipeUpDrawer, current.swipeUpDrawer, short(snapshot.swipeUpDrawer), short(current.swipeUpDrawer))
    fun gridDiff(landscape: Boolean, oldCols: Int, oldRows: Int, newCols: Int, newRows: Int) {
        val key = if (landscape) "Grid landscape" else "Grid portrait"
        if (oldCols != newCols || oldRows != newRows) {
            out += SessionFieldChange(key, "$oldCols × $oldRows", "$newCols × $newRows")
        }
    }
    gridDiff(false, snapshot.colsPort, snapshot.rowsPort, current.colsPort, current.rowsPort)
    gridDiff(true, snapshot.colsLand, snapshot.rowsLand, current.colsLand, current.rowsLand)
    diff("Dock", snapshot.dock, current.dock, short(snapshot.dock), short(current.dock))
    diff("Dock slots", snapshot.dockMax, current.dockMax, short(snapshot.dockMax), short(current.dockMax))
    diff("Dock position", snapshot.dockPos, current.dockPos, dockPosName(snapshot.dockPos), dockPosName(current.dockPos))
    diff("Recent apps", snapshot.dockRecent, current.dockRecent, short(snapshot.dockRecent), short(current.dockRecent))
    diff("Auto-hide", snapshot.dockAutoHide, current.dockAutoHide, short(snapshot.dockAutoHide), short(current.dockAutoHide))
    diff("Search pill", snapshot.dockSearch, current.dockSearch, short(snapshot.dockSearch), short(current.dockSearch))
    diff("Drawer button", snapshot.dockDrawerBtn, current.dockDrawerBtn, short(snapshot.dockDrawerBtn), short(current.dockDrawerBtn))
    diff("Drawer sort", snapshot.drawerSort, current.drawerSort, drawerSortName(snapshot.drawerSort), drawerSortName(current.drawerSort))
    diff("Drawer mode", snapshot.drawerMode, current.drawerMode, short(snapshot.drawerMode), short(current.drawerMode))
    diff("Scroll aid", snapshot.scrollbarStyle, current.scrollbarStyle, short(snapshot.scrollbarStyle), short(current.scrollbarStyle))
    diff("Wallpaper", snapshot.wpKind, current.wpKind, short(snapshot.wpKind), short(current.wpKind))
    diff("Wallpaper dim", snapshot.wpDim, current.wpDim, short(snapshot.wpDim), short(current.wpDim))
    diff("Wallpaper zoom", snapshot.wpZoom, current.wpZoom, short(snapshot.wpZoom), short(current.wpZoom))
    diff("Wallpaper position", snapshot.wpOffX to snapshot.wpOffY, current.wpOffX to current.wpOffY, "${snapshot.wpOffX}, ${snapshot.wpOffY}", "${current.wpOffX}, ${current.wpOffY}")
    diff("Wallpaper tint", snapshot.wpTint, current.wpTint, short(snapshot.wpTint), short(current.wpTint))
    diff("Wallpaper colour", snapshot.wpSolid, current.wpSolid, short(snapshot.wpSolid), short(current.wpSolid))
    diff("At a glance", snapshot.atAGlance, current.atAGlance, short(snapshot.atAGlance), short(current.atAGlance))
    diff("Weather city", snapshot.weatherCity, current.weatherCity, short(snapshot.weatherCity), short(current.weatherCity))
    diff("Weather units", snapshot.weatherUnits, current.weatherUnits, short(snapshot.weatherUnits), short(current.weatherUnits))
    diff("Weather network", snapshot.weatherNetwork, current.weatherNetwork, short(snapshot.weatherNetwork), short(current.weatherNetwork))
    diff("Stacks", snapshot.stacksOn, current.stacksOn, short(snapshot.stacksOn), short(current.stacksOn))
    diff("Icon pack", snapshot.iconPack, current.iconPack, short(snapshot.iconPack), short(current.iconPack))
    diff("Themed icons", snapshot.monoIcons, current.monoIcons, short(snapshot.monoIcons), short(current.monoIcons))
    diff("Badges", snapshot.badgeMode, current.badgeMode, short(snapshot.badgeMode), short(current.badgeMode))
    diff("Gestures", snapshot.gesturesJson, current.gesturesJson, short(snapshot.gesturesJson), short(current.gesturesJson))
    diff("Navigation gestures", snapshot.navGesturesOn, current.navGesturesOn, short(snapshot.navGesturesOn), short(current.navGesturesOn))
    diff("Navigation map", snapshot.navMapJson, current.navMapJson, short(snapshot.navMapJson), short(current.navMapJson))
    diff("Edge thickness", snapshot.navEdgeDp, current.navEdgeDp, short(snapshot.navEdgeDp), short(current.navEdgeDp))
    diff("Navigation sensitivity", snapshot.navSensitivity, current.navSensitivity, short(snapshot.navSensitivity), short(current.navSensitivity))
    diff("Navigation haptics", snapshot.navHaptics, current.navHaptics, short(snapshot.navHaptics), short(current.navHaptics))
    diff("Search sources", listOf(snapshot.searchContacts, snapshot.searchCalendar, snapshot.searchSettings, snapshot.searchCalc), listOf(current.searchContacts, current.searchCalendar, current.searchSettings, current.searchCalc), short(listOf(snapshot.searchContacts, snapshot.searchCalendar, snapshot.searchSettings, snapshot.searchCalc)), short(listOf(current.searchContacts, current.searchCalendar, current.searchSettings, current.searchCalc)))
    diff("Hidden apps", snapshot.hiddenJson, current.hiddenJson, short(snapshot.hiddenJson), short(current.hiddenJson))
    diff("Hidden lock", snapshot.hiddenLock, current.hiddenLock, short(snapshot.hiddenLock), short(current.hiddenLock))
    diff("Reduce motion", snapshot.reduceMotion, current.reduceMotion, short(snapshot.reduceMotion), short(current.reduceMotion))
    diff("Reduce effects", snapshot.reduceEffects, current.reduceEffects, short(snapshot.reduceEffects), short(current.reduceEffects))
    diff("Motion speed", snapshot.animSpeed, current.animSpeed, short(snapshot.animSpeed), short(current.animSpeed))
    diff("Profiles", snapshot.profilesJson, current.profilesJson, short(snapshot.profilesJson), short(current.profilesJson))
    diff("Active profile", snapshot.activeProfile, current.activeProfile, short(snapshot.activeProfile), short(current.activeProfile))
    diff("Profile schedule", listOf(snapshot.profileSchedule, snapshot.schedStart, snapshot.schedEnd, snapshot.schedProfile), listOf(current.profileSchedule, current.schedStart, current.schedEnd, current.schedProfile), short(snapshot.profileSchedule), short(current.profileSchedule))
    diff("Backup wallpaper", snapshot.backupIncludeWp, current.backupIncludeWp, short(snapshot.backupIncludeWp), short(current.backupIncludeWp))
    return out
}

/** One field's revert: write that field back to its snapshot value. */
private fun revertFor(key: String, snapshot: SettingsState, settings: Settings): suspend () -> Unit = when (key) {
    "Dark mode" -> suspend { settings.setDarkMode(snapshot.darkMode) }
    "Accent" -> suspend { settings.setAccent(snapshot.accentMode, snapshot.accentArgb) }
    "Material" -> suspend { settings.setSurfaceMaterial(snapshot.surfaceMaterial) }
    "Corner radius" -> suspend { settings.setCornerDp(snapshot.cornerDp) }
    "Font" -> suspend { settings.setFontChoice(snapshot.fontChoice) }
    "Density" -> suspend { settings.setDensity(snapshot.density) }
    "Icon size" -> suspend { settings.setIconSizeOverride(snapshot.iconSizeOverride) }
    "Labels" -> suspend { settings.setLabels(snapshot.labelsOn, snapshot.labelSize) }
    "Lock layout" -> suspend { settings.setLockLayout(snapshot.lockLayout) }
    "Snap" -> suspend { settings.setPlacementMode(snapshot.placementMode) }
    "Swipe up drawer" -> suspend { settings.setSwipeUpDrawer(snapshot.swipeUpDrawer) }
    "Grid portrait" -> suspend {
        if (snapshot.colsPort < 0 && snapshot.rowsPort < 0) settings.clearGridOverride(false)
        else settings.setGridOverride(false, snapshot.colsPort, snapshot.rowsPort)
    }
    "Grid landscape" -> suspend {
        if (snapshot.colsLand < 0 && snapshot.rowsLand < 0) settings.clearGridOverride(true)
        else settings.setGridOverride(true, snapshot.colsLand, snapshot.rowsLand)
    }
    "Dock" -> suspend { settings.setDock(snapshot.dock) }
    "Dock slots" -> suspend { settings.setDockMax(snapshot.dockMax) }
    "Dock position" -> suspend { settings.setDockPos(snapshot.dockPos) }
    "Recent apps" -> suspend { settings.setDockRecent(snapshot.dockRecent) }
    "Auto-hide" -> suspend { settings.setDockAutoHide(snapshot.dockAutoHide) }
    "Search pill" -> suspend { settings.setDockSearch(snapshot.dockSearch) }
    "Drawer button" -> suspend { settings.setDockDrawerBtn(snapshot.dockDrawerBtn) }
    "Drawer sort" -> suspend { settings.setDrawerSort(snapshot.drawerSort) }
    "Drawer mode" -> suspend { settings.setDrawerMode(snapshot.drawerMode) }
    "Scroll aid" -> suspend { settings.setScrollbarStyle(snapshot.scrollbarStyle) }
    "Wallpaper" -> suspend { settings.setWpKind(snapshot.wpKind) }
    "Wallpaper dim" -> suspend { settings.setWpDim(snapshot.wpDim) }
    "Wallpaper zoom" -> suspend { settings.setWpZoom(snapshot.wpZoom) }
    "Wallpaper position" -> suspend { settings.setWpOffsets(snapshot.wpOffX, snapshot.wpOffY) }
    "Wallpaper tint" -> suspend { settings.setWpTint(snapshot.wpTint) }
    "Wallpaper colour" -> suspend { settings.setWpSolid(snapshot.wpSolid) }
    "At a glance" -> suspend { settings.setAtAGlance(snapshot.atAGlance) }
    "Weather city" -> suspend { settings.setWeather(snapshot.weatherCity, snapshot.weatherLat, snapshot.weatherLon) }
    "Weather units" -> suspend { settings.setWeatherUnits(snapshot.weatherUnits) }
    "Weather network" -> suspend { settings.setWeatherNetwork(snapshot.weatherNetwork) }
    "Stacks" -> suspend { settings.setStacksOn(snapshot.stacksOn) }
    "Icon pack" -> suspend { settings.setIconPack(snapshot.iconPack) }
    "Themed icons" -> suspend { settings.setMonoIcons(snapshot.monoIcons) }
    "Badges" -> suspend { settings.setBadgeMode(snapshot.badgeMode) }
    "Gestures" -> suspend { settings.setGesturesJson(snapshot.gesturesJson) }
    "Navigation gestures" -> suspend { settings.setNavGesturesOn(snapshot.navGesturesOn) }
    "Navigation map" -> suspend { settings.setNavMapJson(snapshot.navMapJson) }
    "Edge thickness" -> suspend { settings.setNavEdgeDp(snapshot.navEdgeDp) }
    "Navigation sensitivity" -> suspend { settings.setNavSensitivity(snapshot.navSensitivity) }
    "Navigation haptics" -> suspend { settings.setNavHaptics(snapshot.navHaptics) }
    "Search sources" -> suspend { settings.setSearchSources(snapshot.searchContacts, snapshot.searchCalendar, snapshot.searchSettings, snapshot.searchCalc) }
    "Hidden apps" -> suspend { settings.setHiddenJson(snapshot.hiddenJson) }
    "Hidden lock" -> suspend { settings.setHiddenLock(snapshot.hiddenLock) }
    "Reduce motion" -> suspend { settings.setReduceMotion(snapshot.reduceMotion) }
    "Reduce effects" -> suspend { settings.setReduceEffects(snapshot.reduceEffects) }
    "Motion speed" -> suspend { settings.setAnimSpeed(snapshot.animSpeed) }
    "Profiles" -> suspend { settings.setProfilesJson(snapshot.profilesJson) }
    "Active profile" -> suspend { settings.setActiveProfile(snapshot.activeProfile) }
    "Profile schedule" -> suspend { settings.setProfileSchedule(snapshot.profileSchedule, snapshot.schedStart, snapshot.schedEnd, snapshot.schedProfile) }
    "Backup wallpaper" -> suspend { settings.setBackupIncludeWp(snapshot.backupIncludeWp) }
    else -> suspend { }
}

/**
 * Session diff as [ChangeRecord]s: one per field that differs between
 * the panel-open [snapshot] and [current], each with a revert that
 * writes that single field back to its snapshot value. This is what
 * populates the panel's Changes list and its per-item Revert — hubs
 * write through typed setters and never record changes themselves, so
 * the host rebuilds this list whenever the live state changes.
 */
fun sessionDiffRecords(
    snapshot: SettingsState,
    current: SettingsState,
    settings: Settings,
): List<ChangeRecord> = sessionDiffFields(snapshot, current).map { change ->
    ChangeRecord(change.key, change.oldText, change.newText, revertFor(change.key, snapshot, settings))
}
