package app.havenos.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONObject
import java.io.IOException

/** Default gesture map (trigger -> action), edited in Settings → Gestures. */
const val DEFAULT_GESTURES =
    """{"swipeUp":"drawer","swipeDown":"quickPanel","doubleTap":"none","pinchIn":"none","twoFingerDown":"search"}"""

data class SettingsState(
    // Look & Feel (§ SETTINGS-SPEC 1)
    // Minimal-only since 2026-10-23: `look` is kept as stored history
    // (old backups/profiles still carry it) but the theme ignores it —
    // resolveTokens() always returns Minimal tokens.
    val look: String = "Minimal",
    val darkMode: Int = 0, // 0 system, 1 light, 2 dark, 3 auto (by time of day)
    val density: Int = 40, // 0..100, drives cols/rows/icon size unless overridden
    val colsLand: Int = -1, val rowsLand: Int = -1, // -1 = from density
    val colsPort: Int = -1, val rowsPort: Int = -1,
    val iconShape: Int = 1, // 0 square, 1 rounded, 2 squircle, 3 circle
    val iconSizeOverride: Int = -1, // dp; -1 = from density
    val labelsOn: Boolean = true,
    val labelSize: Int = 11, // sp
    val fontChoice: Int = 0, // 0 system, 1 serif, 2 monospace
    val reduceEffects: Boolean = false,
    val reduceMotion: Boolean = false,
    // Legacy, kept for backup round-trip only since 2026-10-23: the Look Editor / .havenlook
    // machinery that wrote this was deleted; resolveTokens() never reads it.
    val customLooksJson: String = "{}",
    // Wallpaper (spec 2)
    val wallpaperMode: Int = 0, // 0 system, 1 HavenOS
    val wpKind: Int = 0, // 0..3 gradient presets, 4 solid, 5 photo
    val wpUri: String = "",
    val wpDim: Int = 15, // percent 0..60
    val wpZoom: Int = 100, // percent 100..200 (crop)
    val wpOffX: Float = 0f, val wpOffY: Float = 0f, // -1..1 (crop offset)
    val wpTint: Int = 0, // argb tint overlay (alpha-baked); 0 = no tint
    val wpSolid: Int = 0, // argb solid-wallpaper colour; 0 = WP_SOLID_DEFAULT
    val wpParallax: Boolean = false,
    // Home Screen (spec 3)
    val placementMode: Int = 0, // 0 grid, 1 free canvas, 2 scroll list
    val swipeUpDrawer: Boolean = true, // swipe-up opens the drawer; off = swipe-up does nothing
    val gapDp: Int = 4, // per-cell padding (margins & gaps), 0..12
    val lockLayout: Boolean = false,
    val clockPage: Boolean = true,
    val atAGlance: Int = 1, // 0 off, 1 date line, 2 date + weather line
    val autoAddApps: Boolean = true,
    val dock: List<String> = emptyList(),
    // Dock (spec 4)
    val dockMax: Int = 6,
    val dockRecent: Boolean = true,
    val dockPos: Int = 0, // 0 bottom, 1 left, 2 right
    val dockAutoHide: Boolean = false,
    val dockSearch: Boolean = true,
    val dockDrawerBtn: Boolean = true,
    // Drawer (spec 5)
    val drawerMode: Int = 1, // 0 grid, 1 list, 2 categories, 3 search-first
    val drawerSort: Int = 0, // 0 alphabetical, 1 most used, 2 recently installed
    val scrollbarStyle: Int = 1, // 0 none, 1 letter rail, 2 thin bar
    val hiddenJson: String = "[]", // app keys
    val hiddenLock: Boolean = false, // device-credential gate on hidden-apps list
    val catHiddenJson: String = "[]", // category ids
    val catNamesJson: String = "{}", // category id -> custom name
    // Widgets (spec 6) + weather
    val weatherCity: String = "",
    val weatherLat: String = "",
    val weatherLon: String = "",
    val weatherUnits: Int = 0, // 0 °C, 1 °F
    val weatherNetwork: Boolean = true,
    val calendarsJson: String = "", // "" = all calendars; else JSON list of ids
    val stacksOn: Boolean = true,
    // Icons & Badges (spec 7)
    val iconPack: String = "", // package name; "" = system icons
    val monoIcons: Boolean = false,
    val badgeMode: Int = 1, // 0 off, 1 dot, 2 count
    // Gestures (spec 8)
    val gesturesJson: String = DEFAULT_GESTURES,
    val navGesturesOn: Boolean = false,
    val navMapJson: String = DEFAULT_NAV_MAP,
    val navEdgeDp: Int = 24,
    val navSensitivity: Int = 50,
    val navHaptics: Boolean = true,
    val navHideBar: Boolean = false,
    val navSavedPolicy: String = "",
    val navSavedPolicySet: Boolean = false,
    // Search (spec 9)
    val searchContacts: Boolean = false,
    val searchCalendar: Boolean = false,
    val searchSettings: Boolean = true,
    val searchCalc: Boolean = true,
    val searchHistoryJson: String = "[]",
    // Profiles (spec 10)
    val profilesJson: String = "[]",
    val activeProfile: String = "Default",
    val profileSchedule: Boolean = false,
    val schedStart: Int = 540, // minutes of day
    val schedEnd: Int = 1020,
    val schedProfile: String = "",
    // Backup (spec 11)
    val backupIncludeWp: Boolean = true,
    val lastBackupDay: Long = 0, // epoch day of last automatic backup
    val undoJson: String = "", // last layout snapshot (backup/profile-apply undo)
    // Durable misc
    val launchStatsJson: String = "{}", // app key -> {"c":count,"t":lastLaunchMs}
    val seeded: Boolean = false,
    // M5 Settings categories 1-7 (SETTINGS-DESIGN-v1 §4.1-4.7). Fields added
    // only where no existing field covered the spec setting; each is stored
    // by the new Cat screens. Fields whose home/drawer consumer is not yet
    // wired are marked "kept for backup round-trip only" in the M5 worker report — they change
    // no live behaviour until that consumer reads them.
    // Cat 1 Style & Theme
    val accentMode: Int = 0, // 0 Minimal default, 1 palette, 2 custom hex, 3 wallpaper-derived (kept for backup round-trip only)
    val accentArgb: Int = 0, // argb; 0 = Minimal default
    val cornerDp: Int = -1, // surface corner radius dp; -1 = Minimal default (12dp)
    val surfaceMaterial: Int = 2, // 0 solid, 1 translucent, 2 frosted
    val surfaceBlurPct: Int = 60, // 0..100 (kept for backup round-trip only since 2026-10-23: surface material drives blur)
    val surfaceNoisePct: Int = 0, // 0..100 (kept for backup round-trip only: GlassSurface has no noise pass)
    val shadowDp: Int = -1, // dp; -1 = Minimal default (kept for backup round-trip only)
    val outlineOn: Boolean = true, // (kept for backup round-trip only: GlassSurface always draws its border)
    val fontWeight: Int = 0, // 0 normal, 1 medium, 2 bold (kept for backup round-trip only)
    val fontScalePct: Int = 100, // 80..130 (kept for backup round-trip only)
    // Cat 2 Home screen
    val colsTablet: Int = -1, val rowsTablet: Int = -1, // -1 = from density (kept for backup round-trip only)
    val marginDp: Int = 16, // home outer margin (kept for backup round-trip only; gapDp is the live cell padding)
    val cellSpacingDp: Int = 4, // (kept for backup round-trip only; gapDp is the live cell padding)
    val labelLines: Int = 1, // 1..3 (kept for backup round-trip only)
    val labelColorArgb: Int = 0, // argb; 0 = theme default (kept for backup round-trip only)
    val searchBarPos: Int = 0, // 0 top, 1 bottom, 2 hidden (kept for backup round-trip only)
    val searchBarStyle: Int = 0, // 0 pill, 1 rounded, 2 flat (kept for backup round-trip only)
    val searchBarHeightDp: Int = 56, // 40..72 (kept for backup round-trip only)
    val todayPageOn: Boolean = false, // left "Today" page (kept for backup round-trip only; distinct from clockPage)
    val todayPageContent: Int = 0, // 0 date+weather, 1 agenda, 2 all (kept for backup round-trip only)
    val scrollEffect: Int = 0, // 0 none, 1 bounce, 2 fade, 3 zoom (kept for backup round-trip only)
    val statusBarMode: Int = 0, // 0 system, 1 dark icons, 2 light icons, 3 hidden (kept for backup round-trip only)
    // Cat 3 Wallpaper
    val wpBlurPct: Int = 0, // 0..100 (kept for backup round-trip only: glass backdrop blur is fixed)
    val wpAutoDim: Boolean = false, // extra dim in dark mode (kept for backup round-trip only)
    val wpPerPage: Boolean = false, // per-page wallpaper (kept for backup round-trip only)
    // Cat 4 Dock
    val dockRows: Int = 1, // 1..2 (kept for backup round-trip only)
    val dockFloating: Boolean = true, // floating pill vs attached bar (kept for backup round-trip only)
    val dockMaterial: Int = 0, // 0 glass, 1 solid, 2 transparent (kept for backup round-trip only)
    val dockOpacityPct: Int = 100, // 0..100 (kept for backup round-trip only)
    val dockMagnify: Boolean = false, // magnify on touch (kept for backup round-trip only)
    val dockLabels: Boolean = false, // show labels under dock icons (kept for backup round-trip only)
    // Cat 5 App drawer
    val drawerStyle: Int = 2, // 0 vertical grid, 1 horizontal pages, 2 list, 3 A-Z scrubber, 4 auto-categories
    val drawerGesture: Int = 0, // 0 swipe up, 1 swipe up + hold, 2 double-tap (kept for backup round-trip only; 0 = swipeUpDrawer)
    val drawerCols: Int = 0, // 0 = auto, else 3..8 (kept for backup round-trip only)
    val drawerIconDp: Int = 0, // 0 = auto, else dp (kept for backup round-trip only)
    val drawerSearchOn: Boolean = true, // (kept for backup round-trip only)
    val drawerRecentsOn: Boolean = true, // (kept for backup round-trip only)
    val drawerSuggestionsOn: Boolean = true, // (kept for backup round-trip only)
    // Cat 6 Icons
    val iconShapeExt: Int = 1, // 0 square, 1 rounded, 2 squircle, 3 circle, 4 teardrop, 5 system; 0..3 sync iconShape
    val perAppIconJson: String = "{}", // app key -> pack pkg or "system" (kept for backup round-trip only: drawer long-press entry pending)
    // Cat 7 Folders
    val folderPreview: Int = 0, // 0 grid, 1 stack, 2 fan (kept for backup round-trip only)
    val folderAnim: Int = 0, // 0 fade, 1 scale, 2 slide (kept for backup round-trip only)
    val folderSizePct: Int = 100, // 50..150 (kept for backup round-trip only)
    val folderBg: Int = 0, // 0 glass, 1 solid, 2 transparent (kept for backup round-trip only)
    val folderTitleOn: Boolean = true, // (kept for backup round-trip only)
    // M5 categories 8–14 (SETTINGS-DESIGN-v1 §4.8–§4.14). Each field below is
    // read by its own Cat screen's live sample/test pad, so no control here
    // is stored-only; home/drawer adoption of widget frames, gesture
    // sensitivity and animation speed is coordinator follow-up.
    val widgetRadius: Int = 12, // dp 0..32, widget frame corner radius
    val widgetBg: Int = 0, // 0 follow Look, 1 solid, 2 translucent
    val widgetOpacity: Int = 100, // percent 0..100; solid ignores it; 100 = unchanged today
    val gestureSensitivity: Int = 50, // 0..100; drives the gesture test pad thresholds
    val animSpeed: Int = 2, // 0 off, 1 fast, 2 normal, 3 slow
    val pressFeedback: Boolean = true,
    val hapticStrength: Int = 50, // 0..100; 0 = no vibration
    // M5 shell state (ui/settings): mini-home header collapsed, and
    // per-app label overrides (app key -> custom label, "" = none).
    val miniHomeCollapsed: Boolean = false,
    val labelOverridesJson: String = "{}",
    // vc5 feature-gap audit additions (appended at the end; see worker report).
    val iconSwipesJson: String = "{}", // package -> swipe-up action id (IconSwipes.kt); UI in Cat9, gesture wiring pending coordinator
    val badgeHiddenJson: String = "[]", // packages whose dots are suppressed (read by Home BadgeOverlay)
    val catMergeJson: String = "{}", // category id -> target category id (Drawer + Cat5 editor read this live)
    // M7 Wave 2 additions (coordinator-owned keys from the Wave 1 UI
    // contracts; each UI file references its key by name only and takes
    // the value as a parameter — see Legibility.kt, LibrarySearch.kt,
    // TodayStack.kt, Dock.kt, CalmMode.kt, CategoriesView.kt).
    val labelMode: String = "auto", // "auto" / "pill" / "off" (Legibility.kt labelModeFromKey)
    val snapToGrid: Boolean = true, // edit-mode snap; see parse() derivation note below
    val dockRecentsOn: Boolean = true, // OneDock recents section (Dock.kt); synced with legacy dockRecent
    val libraryDefaultView: String = "all", // "all" / "categories" / "list" (LibraryView.fromKey)
    val todayOn: Boolean = true, // Today section on/off (TodayStack.kt)
    val smartStackAutoRotate: Boolean = false, // time-based Smart Stack rotation (TodayStack.kt)
    val categoryOverridesJson: String = "", // moves + renames (CategoriesView.kt); "" = none
    val calmFavoritesJson: String = "", // JSON array of app keys (CalmMode.kt); "" = none chosen
    val calmScheduleJson: String = "", // JSON CalmSchedule (CalmMode.kt); "" = disabled
    // M9 Calm editor entry options (CalmEditor.kt): {"autoEnter":bool,"dimPct":0..100}; "" = defaults.
    val calmEntryJson: String = "",
    // M8 pages (horizontal paged home). Hidden pages + default page
    // live here, not in a table; page order is HomeItem.page order.
    // pagesMigrated is the one-time vc5-flow -> pages gate: the host
    // runs migrateFlowToPages() only while it is false, then sets it
    // true. A section reset must NOT clear it (see resetSection(3)).
    val pageIndicatorOn: Boolean = true,
    val parallaxOn: Boolean = true,
    val defaultPage: Int = 0, // page index; host clamps to existing pages
    val hiddenPagesJson: String = "", // JSON int array; "" = none (parse via PageMigration.parseHiddenPages)
    val pagesMigrated: Boolean = false,
    // M8 integration: floor on the grid-page count, so pages added while
    // empty survive a restart (empty pages leave no HomeItem rows).
    // 0 = derive from items. Reset to 0 by section reset (empty added
    // pages then drop; item pages are unaffected).
    val pageCount: Int = 0,
    // M9 edit mode: how many times edit mode has opened. The hint chip
    // shows only while this is < 3 (Doc 1 §3.10); old installs read 0.
    val editHintOpens: Int = 0,
)

/** Effective grid (in CELLS) for the current orientation, honoring overrides. */
fun SettingsState.gridCells(landscape: Boolean): Pair<Int, Int> {
    val d = density.coerceIn(0, 100)
    val cols = if (landscape) {
        if (colsLand > 0) colsLand else 4 + (d * 6 + 50) / 100
    } else {
        if (colsPort > 0) colsPort else 3 + (d * 3 + 50) / 100
    }
    val rows = if (landscape) {
        if (rowsLand > 0) rowsLand else 3 + (d * 5 + 50) / 100
    } else {
        if (rowsPort > 0) rowsPort else 5 + (d * 4 + 50) / 100
    }
    return cols.coerceIn(3, 12) to rows.coerceIn(3, 12)
}

fun SettingsState.iconDp(): Int =
    if (iconSizeOverride > 0) iconSizeOverride else 42 + density.coerceIn(0, 100) * 25 / 100

fun SettingsState.hiddenApps(): Set<String> = parseMembers(hiddenJson).toSet()

/** Parse launch stats JSON: key -> (launchCount, lastLaunchMs). Never throws. */
fun parseLaunchStats(json: String): Map<String, Pair<Int, Long>> {
    return try {
        val obj = JSONObject(json)
        val out = mutableMapOf<String, Pair<Int, Long>>()
        for (k in obj.keys()) {
            val v = obj.optJSONObject(k) ?: continue
            out[k] = v.optInt("c", 0) to v.optLong("t", 0L)
        }
        out
    } catch (e: Exception) {
        emptyMap()
    }
}

fun encodeLaunchStats(stats: Map<String, Pair<Int, Long>>): String {
    val obj = JSONObject()
    for ((k, v) in stats) {
        obj.put(k, JSONObject().put("c", v.first).put("t", v.second))
    }
    return obj.toString()
}

private val Context.dataStore by preferencesDataStore(name = "havenos_settings")

class Settings(ctx: Context) {
    private val appCtx = ctx.applicationContext

    private object K {
        val look = stringPreferencesKey("look")
        val darkMode = intPreferencesKey("dark_mode")
        val density = intPreferencesKey("density")
        val colsLand = intPreferencesKey("cols_land")
        val rowsLand = intPreferencesKey("rows_land")
        val colsPort = intPreferencesKey("cols_port")
        val rowsPort = intPreferencesKey("rows_port")
        val iconShape = intPreferencesKey("icon_shape")
        val iconSizeOverride = intPreferencesKey("icon_size_override")
        val labelsOn = booleanPreferencesKey("labels_on")
        val labelSize = intPreferencesKey("label_size")
        val fontChoice = intPreferencesKey("font_choice")
        val reduceEffects = booleanPreferencesKey("reduce_effects")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val customLooksJson = stringPreferencesKey("custom_looks_json")
        val cornerDp = intPreferencesKey("corner_dp")
        val wallpaperMode = intPreferencesKey("wallpaper_mode")
        val wpKind = intPreferencesKey("wp_kind")
        val wpUri = stringPreferencesKey("wp_uri")
        val wpDim = intPreferencesKey("wp_dim")
        val wpZoom = intPreferencesKey("wp_zoom")
        val wpOffX = floatPreferencesKey("wp_off_x")
        val wpOffY = floatPreferencesKey("wp_off_y")
        val wpTint = intPreferencesKey("wp_tint")
        val wpSolid = intPreferencesKey("wp_solid")
        val wpParallax = booleanPreferencesKey("wp_parallax")
        val placementMode = intPreferencesKey("placement_mode")
        val swipeUpDrawer = booleanPreferencesKey("swipe_up_drawer")
        val gapDp = intPreferencesKey("gap_dp")
        val lockLayout = booleanPreferencesKey("lock_layout")
        val clockPage = booleanPreferencesKey("clock_page")
        val atAGlance = intPreferencesKey("at_a_glance")
        val autoAddApps = booleanPreferencesKey("auto_add_apps")
        val dockJson = stringPreferencesKey("dock_json")
        val dockMax = intPreferencesKey("dock_max")
        val dockRecent = booleanPreferencesKey("dock_recent")
        val dockPos = intPreferencesKey("dock_pos")
        val dockAutoHide = booleanPreferencesKey("dock_autohide")
        val dockSearch = booleanPreferencesKey("dock_search")
        val dockDrawerBtn = booleanPreferencesKey("dock_drawer_btn")
        val drawerMode = intPreferencesKey("drawer_mode")
        val drawerSort = intPreferencesKey("drawer_sort")
        val scrollbarStyle = intPreferencesKey("scrollbar_style")
        val hiddenJson = stringPreferencesKey("hidden_json")
        val hiddenLock = booleanPreferencesKey("hidden_lock")
        val catHiddenJson = stringPreferencesKey("cat_hidden_json")
        val catNamesJson = stringPreferencesKey("cat_names_json")
        val weatherCity = stringPreferencesKey("weather_city")
        val weatherLat = stringPreferencesKey("weather_lat")
        val weatherLon = stringPreferencesKey("weather_lon")
        val weatherUnits = intPreferencesKey("weather_units")
        val weatherNetwork = booleanPreferencesKey("weather_network")
        val calendarsJson = stringPreferencesKey("calendars_json")
        val stacksOn = booleanPreferencesKey("stacks_on")
        val iconPack = stringPreferencesKey("icon_pack")
        val monoIcons = booleanPreferencesKey("mono_icons")
        val badgeMode = intPreferencesKey("badge_mode")
        val gesturesJson = stringPreferencesKey("gestures_json")
        val navGesturesOn = booleanPreferencesKey("nav_gestures_on")
        val navMapJson = stringPreferencesKey("nav_map_json")
        val navEdgeDp = intPreferencesKey("nav_edge_dp")
        val navSensitivity = intPreferencesKey("nav_sensitivity")
        val navHaptics = booleanPreferencesKey("nav_haptics")
        val navHideBar = booleanPreferencesKey("nav_hide_bar")
        val navSavedPolicy = stringPreferencesKey("nav_saved_policy")
        val navSavedPolicySet = booleanPreferencesKey("nav_saved_policy_set")
        val searchContacts = booleanPreferencesKey("search_contacts")
        val searchCalendar = booleanPreferencesKey("search_calendar")
        val searchSettings = booleanPreferencesKey("search_settings")
        val searchCalc = booleanPreferencesKey("search_calc")
        val searchHistoryJson = stringPreferencesKey("search_history_json")
        val profilesJson = stringPreferencesKey("profiles_json")
        val activeProfile = stringPreferencesKey("active_profile")
        val profileSchedule = booleanPreferencesKey("profile_schedule")
        val schedStart = intPreferencesKey("sched_start")
        val schedEnd = intPreferencesKey("sched_end")
        val schedProfile = stringPreferencesKey("sched_profile")
        val backupIncludeWp = booleanPreferencesKey("backup_include_wp")
        val lastBackupDay = longPreferencesKey("last_backup_day")
        val undoJson = stringPreferencesKey("undo_json")
        val launchStatsJson = stringPreferencesKey("launch_stats_json")
        val seeded = booleanPreferencesKey("seeded")
        val miniHomeCollapsed = booleanPreferencesKey("mini_home_collapsed")
        val labelOverridesJson = stringPreferencesKey("label_overrides_json")
        val widgetRadius = intPreferencesKey("widget_radius")
        val widgetBg = intPreferencesKey("widget_bg")
        val widgetOpacity = intPreferencesKey("widget_opacity")
        val gestureSensitivity = intPreferencesKey("gesture_sensitivity")
        val animSpeed = intPreferencesKey("anim_speed")
        val pressFeedback = booleanPreferencesKey("press_feedback")
        val hapticStrength = intPreferencesKey("haptic_strength")
        val accentMode = intPreferencesKey("accent_mode")
        val accentArgb = intPreferencesKey("accent_argb")
        val surfaceMaterial = intPreferencesKey("surface_material")
        val surfaceBlurPct = intPreferencesKey("surface_blur_pct")
        val surfaceNoisePct = intPreferencesKey("surface_noise_pct")
        val shadowDp = intPreferencesKey("shadow_dp")
        val outlineOn = booleanPreferencesKey("outline_on")
        val fontWeight = intPreferencesKey("font_weight")
        val fontScalePct = intPreferencesKey("font_scale_pct")
        val colsTablet = intPreferencesKey("cols_tablet")
        val rowsTablet = intPreferencesKey("rows_tablet")
        val marginDp = intPreferencesKey("margin_dp")
        val cellSpacingDp = intPreferencesKey("cell_spacing_dp")
        val labelLines = intPreferencesKey("label_lines")
        val labelColorArgb = intPreferencesKey("label_color_argb")
        val searchBarPos = intPreferencesKey("search_bar_pos")
        val searchBarStyle = intPreferencesKey("search_bar_style")
        val searchBarHeightDp = intPreferencesKey("search_bar_height_dp")
        val todayPageOn = booleanPreferencesKey("today_page_on")
        val todayPageContent = intPreferencesKey("today_page_content")
        val scrollEffect = intPreferencesKey("scroll_effect")
        val statusBarMode = intPreferencesKey("status_bar_mode")
        val wpBlurPct = intPreferencesKey("wp_blur_pct")
        val wpAutoDim = booleanPreferencesKey("wp_auto_dim")
        val wpPerPage = booleanPreferencesKey("wp_per_page")
        val dockRows = intPreferencesKey("dock_rows")
        val dockFloating = booleanPreferencesKey("dock_floating")
        val dockMaterial = intPreferencesKey("dock_material")
        val dockOpacityPct = intPreferencesKey("dock_opacity_pct")
        val dockMagnify = booleanPreferencesKey("dock_magnify")
        val dockLabels = booleanPreferencesKey("dock_labels")
        val drawerStyle = intPreferencesKey("drawer_style")
        val drawerGesture = intPreferencesKey("drawer_gesture")
        val drawerCols = intPreferencesKey("drawer_cols")
        val drawerIconDp = intPreferencesKey("drawer_icon_dp")
        val drawerSearchOn = booleanPreferencesKey("drawer_search_on")
        val drawerRecentsOn = booleanPreferencesKey("drawer_recents_on")
        val drawerSuggestionsOn = booleanPreferencesKey("drawer_suggestions_on")
        val iconShapeExt = intPreferencesKey("icon_shape_ext")
        val perAppIconJson = stringPreferencesKey("per_app_icon_json")
        val folderPreview = intPreferencesKey("folder_preview")
        val folderAnim = intPreferencesKey("folder_anim")
        val folderSizePct = intPreferencesKey("folder_size_pct")
        val folderBg = intPreferencesKey("folder_bg")
        val folderTitleOn = booleanPreferencesKey("folder_title_on")
        val iconSwipesJson = stringPreferencesKey("icon_swipes_json")
        val badgeHiddenJson = stringPreferencesKey("badge_hidden_json")
        val catMergeJson = stringPreferencesKey("cat_merge_json")
        val labelMode = stringPreferencesKey("labelMode")
        val snapToGrid = booleanPreferencesKey("snapToGrid")
        val dockRecentsOn = booleanPreferencesKey("dockRecentsOn")
        val libraryDefaultView = stringPreferencesKey("libraryDefaultView")
        val todayOn = booleanPreferencesKey("todayOn")
        val smartStackAutoRotate = booleanPreferencesKey("smartStackAutoRotate")
        val categoryOverridesJson = stringPreferencesKey("categoryOverridesJson")
        val calmFavoritesJson = stringPreferencesKey("calmFavoritesJson")
        val calmScheduleJson = stringPreferencesKey("calmScheduleJson")
        val calmEntryJson = stringPreferencesKey("calmEntryJson")
        // M8 pages.
        val pageIndicatorOn = booleanPreferencesKey("pageIndicatorOn")
        val parallaxOn = booleanPreferencesKey("parallaxOn")
        val defaultPage = intPreferencesKey("defaultPage")
        val hiddenPagesJson = stringPreferencesKey("hiddenPagesJson")
        val pagesMigrated = booleanPreferencesKey("pagesMigrated")
        val pageCount = intPreferencesKey("pageCount")
        // M9 edit mode.
        val editHintOpens = intPreferencesKey("editHintOpens")
    }

    /** Stale-type-safe preference read: any ClassCastException from a stale value falls back to default. */
    private fun <T> Preferences.safe(key: Preferences.Key<T>, default: T): T {
        return try {
            this[key] ?: default
        } catch (e: ClassCastException) {
            default
        }
    }

    /** Parse one persisted Preferences snapshot into a full state. The only parse path. */
    private fun parse(p: Preferences): SettingsState {
        val d = SettingsState()
        // M7 Wave 2 normalizations (mirror the UI-side fromKey helpers;
        // data must not import ui, so the small checks live here):
        // unknown/blank labelMode or libraryDefaultView fall back to
        // their defaults, exactly like labelModeFromKey / LibraryView.fromKey.
        val placement = p.safe(K.placementMode, 0).coerceIn(0, 2)
        // snapToGrid is new in M7 Wave 2. Old installs have no stored
        // value: derive it from the placementMode the old Snap row wrote
        // (0 grid / 2 scroll-list = snap on, 1 free canvas = snap off),
        // i.e. the old snapOn() rule — so an upgrade never flips a
        // user's snap behaviour. A stored value (or a stale-type value,
        // which safe() would mask) always wins over the derivation: a
        // present-but-unreadable value falls back to the derivation
        // rather than to a blind true.
        val snapStored: Boolean? = try {
            p[K.snapToGrid]
        } catch (e: ClassCastException) {
            null
        }
        val snap = snapStored ?: (placement != 1)
        val rawLabelMode = p.safe(K.labelMode, "auto")
        val labelMode = if (rawLabelMode == "pill" || rawLabelMode == "off") rawLabelMode else "auto"
        val rawLibraryView = p.safe(K.libraryDefaultView, "all")
        val libraryView =
            if (rawLibraryView == "categories" || rawLibraryView == "list") rawLibraryView else "all"
        return SettingsState(
                // Minimal-only: any stored look name (old built-in, custom
                // id) normalizes to Minimal on read, so no stale Look name
                // can resurface in diagnostics or summaries.
                look = "Minimal",
                darkMode = p.safe(K.darkMode, d.darkMode).coerceIn(0, 3),
                density = p.safe(K.density, d.density).coerceIn(0, 100),
                colsLand = p.safe(K.colsLand, -1),
                rowsLand = p.safe(K.rowsLand, -1),
                colsPort = p.safe(K.colsPort, -1),
                rowsPort = p.safe(K.rowsPort, -1),
                iconShape = p.safe(K.iconShape, d.iconShape).coerceIn(0, 3),
                iconSizeOverride = p.safe(K.iconSizeOverride, -1),
                labelsOn = p.safe(K.labelsOn, true),
                labelSize = p.safe(K.labelSize, d.labelSize).coerceIn(9, 16),
                fontChoice = p.safe(K.fontChoice, 0).coerceIn(0, 2),
                reduceEffects = p.safe(K.reduceEffects, false),
                reduceMotion = p.safe(K.reduceMotion, false),
                customLooksJson = p.safe(K.customLooksJson, "{}"),
                wallpaperMode = p.safe(K.wallpaperMode, 0).coerceIn(0, 1),
                wpKind = p.safe(K.wpKind, 0).coerceIn(0, 5),
                wpUri = p.safe(K.wpUri, ""),
                wpDim = p.safe(K.wpDim, d.wpDim).coerceIn(0, 60),
                wpZoom = p.safe(K.wpZoom, 100).coerceIn(100, 200),
                wpOffX = p.safe(K.wpOffX, 0f).coerceIn(-1f, 1f),
                wpOffY = p.safe(K.wpOffY, 0f).coerceIn(-1f, 1f),
                wpTint = p.safe(K.wpTint, 0),
                wpSolid = p.safe(K.wpSolid, 0),
                wpParallax = p.safe(K.wpParallax, false),
                placementMode = placement,
                swipeUpDrawer = p.safe(K.swipeUpDrawer, true),
                gapDp = p.safe(K.gapDp, 4).coerceIn(0, 12),
                lockLayout = p.safe(K.lockLayout, false),
                clockPage = p.safe(K.clockPage, true),
                atAGlance = p.safe(K.atAGlance, 1).coerceIn(0, 2),
                autoAddApps = p.safe(K.autoAddApps, true),
                dock = parseMembers(p.safe(K.dockJson, "[]")),
                dockMax = p.safe(K.dockMax, d.dockMax).coerceIn(0, 12),
                dockRecent = p.safe(K.dockRecent, true),
                dockPos = p.safe(K.dockPos, 0).coerceIn(0, 2),
                dockAutoHide = p.safe(K.dockAutoHide, false),
                dockSearch = p.safe(K.dockSearch, true),
                dockDrawerBtn = p.safe(K.dockDrawerBtn, true),
                drawerMode = p.safe(K.drawerMode, 1).coerceIn(0, 3),
                drawerSort = p.safe(K.drawerSort, 0).coerceIn(0, 2),
                scrollbarStyle = p.safe(K.scrollbarStyle, 1).coerceIn(0, 2),
                hiddenJson = p.safe(K.hiddenJson, "[]"),
                hiddenLock = p.safe(K.hiddenLock, false),
                catHiddenJson = p.safe(K.catHiddenJson, "[]"),
                catNamesJson = p.safe(K.catNamesJson, "{}"),
                weatherCity = p.safe(K.weatherCity, ""),
                weatherLat = p.safe(K.weatherLat, ""),
                weatherLon = p.safe(K.weatherLon, ""),
                weatherUnits = p.safe(K.weatherUnits, 0).coerceIn(0, 1),
                weatherNetwork = p.safe(K.weatherNetwork, true),
                calendarsJson = p.safe(K.calendarsJson, ""),
                stacksOn = p.safe(K.stacksOn, true),
                iconPack = p.safe(K.iconPack, ""),
                monoIcons = p.safe(K.monoIcons, false),
                badgeMode = p.safe(K.badgeMode, 1).coerceIn(0, 2),
                gesturesJson = p.safe(K.gesturesJson, DEFAULT_GESTURES),
                navGesturesOn = p.safe(K.navGesturesOn, false),
                navMapJson = p.safe(K.navMapJson, DEFAULT_NAV_MAP),
                navEdgeDp = p.safe(K.navEdgeDp, 24).coerceIn(12, 48),
                navSensitivity = p.safe(K.navSensitivity, 50).coerceIn(0, 100),
                navHaptics = p.safe(K.navHaptics, true),
                navHideBar = p.safe(K.navHideBar, false),
                navSavedPolicy = p.safe(K.navSavedPolicy, ""),
                navSavedPolicySet = p.safe(K.navSavedPolicySet, false),
                searchContacts = p.safe(K.searchContacts, false),
                searchCalendar = p.safe(K.searchCalendar, false),
                searchSettings = p.safe(K.searchSettings, true),
                searchCalc = p.safe(K.searchCalc, true),
                searchHistoryJson = p.safe(K.searchHistoryJson, "[]"),
                profilesJson = p.safe(K.profilesJson, "[]"),
                activeProfile = p.safe(K.activeProfile, "Default"),
                profileSchedule = p.safe(K.profileSchedule, false),
                schedStart = p.safe(K.schedStart, 540).coerceIn(0, 1439),
                schedEnd = p.safe(K.schedEnd, 1020).coerceIn(0, 1439),
                schedProfile = p.safe(K.schedProfile, ""),
                backupIncludeWp = p.safe(K.backupIncludeWp, true),
                lastBackupDay = p.safe(K.lastBackupDay, 0L),
                undoJson = p.safe(K.undoJson, ""),
                launchStatsJson = p.safe(K.launchStatsJson, "{}"),
                seeded = p.safe(K.seeded, false),
                miniHomeCollapsed = p.safe(K.miniHomeCollapsed, false),
                labelOverridesJson = p.safe(K.labelOverridesJson, "{}"),
                accentMode = p.safe(K.accentMode, 0).coerceIn(0, 3),
                accentArgb = p.safe(K.accentArgb, 0),
                cornerDp = p.safe(K.cornerDp, -1).coerceIn(-1, 40),
                surfaceMaterial = p.safe(K.surfaceMaterial, 2).coerceIn(0, 2),
                surfaceBlurPct = p.safe(K.surfaceBlurPct, 60).coerceIn(0, 100),
                surfaceNoisePct = p.safe(K.surfaceNoisePct, 0).coerceIn(0, 100),
                shadowDp = p.safe(K.shadowDp, -1).coerceIn(-1, 24),
                outlineOn = p.safe(K.outlineOn, true),
                fontWeight = p.safe(K.fontWeight, 0).coerceIn(0, 2),
                fontScalePct = p.safe(K.fontScalePct, 100).coerceIn(80, 130),
                colsTablet = p.safe(K.colsTablet, -1),
                rowsTablet = p.safe(K.rowsTablet, -1),
                marginDp = p.safe(K.marginDp, 16).coerceIn(0, 32),
                cellSpacingDp = p.safe(K.cellSpacingDp, 4).coerceIn(0, 24),
                labelLines = p.safe(K.labelLines, 1).coerceIn(1, 3),
                labelColorArgb = p.safe(K.labelColorArgb, 0),
                searchBarPos = p.safe(K.searchBarPos, 0).coerceIn(0, 2),
                searchBarStyle = p.safe(K.searchBarStyle, 0).coerceIn(0, 2),
                searchBarHeightDp = p.safe(K.searchBarHeightDp, 56).coerceIn(40, 72),
                todayPageOn = p.safe(K.todayPageOn, false),
                todayPageContent = p.safe(K.todayPageContent, 0).coerceIn(0, 2),
                scrollEffect = p.safe(K.scrollEffect, 0).coerceIn(0, 3),
                statusBarMode = p.safe(K.statusBarMode, 0).coerceIn(0, 3),
                wpBlurPct = p.safe(K.wpBlurPct, 0).coerceIn(0, 100),
                wpAutoDim = p.safe(K.wpAutoDim, false),
                wpPerPage = p.safe(K.wpPerPage, false),
                dockRows = p.safe(K.dockRows, 1).coerceIn(1, 2),
                dockFloating = p.safe(K.dockFloating, true),
                dockMaterial = p.safe(K.dockMaterial, 0).coerceIn(0, 2),
                dockOpacityPct = p.safe(K.dockOpacityPct, 100).coerceIn(0, 100),
                dockMagnify = p.safe(K.dockMagnify, false),
                dockLabels = p.safe(K.dockLabels, false),
                drawerStyle = p.safe(K.drawerStyle, 2).coerceIn(0, 4),
                drawerGesture = p.safe(K.drawerGesture, 0).coerceIn(0, 2),
                drawerCols = p.safe(K.drawerCols, 0).coerceIn(0, 8),
                drawerIconDp = p.safe(K.drawerIconDp, 0).coerceIn(0, 96),
                drawerSearchOn = p.safe(K.drawerSearchOn, true),
                drawerRecentsOn = p.safe(K.drawerRecentsOn, true),
                drawerSuggestionsOn = p.safe(K.drawerSuggestionsOn, true),
                iconShapeExt = p.safe(K.iconShapeExt, 1).coerceIn(0, 5),
                perAppIconJson = p.safe(K.perAppIconJson, "{}"),
                folderPreview = p.safe(K.folderPreview, 0).coerceIn(0, 2),
                folderAnim = p.safe(K.folderAnim, 0).coerceIn(0, 2),
                folderSizePct = p.safe(K.folderSizePct, 100).coerceIn(50, 150),
                folderBg = p.safe(K.folderBg, 0).coerceIn(0, 2),
                folderTitleOn = p.safe(K.folderTitleOn, true),
                widgetRadius = p.safe(K.widgetRadius, 12).coerceIn(0, 32),
                widgetBg = p.safe(K.widgetBg, 0).coerceIn(0, 2),
                widgetOpacity = p.safe(K.widgetOpacity, 100).coerceIn(0, 100),
                gestureSensitivity = p.safe(K.gestureSensitivity, 50).coerceIn(0, 100),
                animSpeed = p.safe(K.animSpeed, 2).coerceIn(0, 3),
                pressFeedback = p.safe(K.pressFeedback, true),
                hapticStrength = p.safe(K.hapticStrength, 50).coerceIn(0, 100),
                iconSwipesJson = p.safe(K.iconSwipesJson, "{}"),
                badgeHiddenJson = p.safe(K.badgeHiddenJson, "[]"),
                catMergeJson = p.safe(K.catMergeJson, "{}"),
                labelMode = labelMode,
                snapToGrid = snap,
                dockRecentsOn = p.safe(K.dockRecentsOn, true),
                libraryDefaultView = libraryView,
                todayOn = p.safe(K.todayOn, true),
                smartStackAutoRotate = p.safe(K.smartStackAutoRotate, false),
                categoryOverridesJson = p.safe(K.categoryOverridesJson, ""),
                calmFavoritesJson = p.safe(K.calmFavoritesJson, ""),
                calmScheduleJson = p.safe(K.calmScheduleJson, ""),
                calmEntryJson = p.safe(K.calmEntryJson, ""),
                // M8 pages. Stale-type-safe via safe(): a wrong-typed
                // stored value falls back to the default. hiddenPagesJson
                // is kept raw here (blank = none); consumers must parse
                // it with PageMigration.parseHiddenPages, which tolerates
                // malformed JSON. defaultPage is clamped to existing
                // pages by the host (parse() has no page list).
                pageIndicatorOn = p.safe(K.pageIndicatorOn, true),
                parallaxOn = p.safe(K.parallaxOn, true),
                defaultPage = p.safe(K.defaultPage, 0).coerceAtLeast(0),
                hiddenPagesJson = p.safe(K.hiddenPagesJson, ""),
                pagesMigrated = p.safe(K.pagesMigrated, false),
                pageCount = p.safe(K.pageCount, 0).coerceAtLeast(0),
                editHintOpens = p.safe(K.editHintOpens, 0).coerceIn(0, 100),
        )
    }

    val state: StateFlow<SettingsState> = appCtx.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { p -> parse(p) }
        .stateIn(
            CoroutineScope(SupervisorJob() + Dispatchers.IO),
            SharingStarted.Eagerly,
            SettingsState(),
        )

    /** One-shot read of the persisted state — never the pre-load default value. */
    suspend fun persistedState(): SettingsState = parse(appCtx.dataStore.data.first())

    /** Generic write: `settings.update { it[K.look] = "Minimal" }`. Keys stay private to this file. */
    suspend fun update(block: (MutablePreferences) -> Unit) {
        appCtx.dataStore.edit { block(it) }
    }

    // Typed setters (the only write surface used by the UI).
    suspend fun setLook(v: String) = update { it[K.look] = v }
    suspend fun setDarkMode(v: Int) = update { it[K.darkMode] = v.coerceIn(0, 3) }
    suspend fun setDensity(v: Int) = update { it[K.density] = v.coerceIn(0, 100) }
    suspend fun setGridOverride(landscape: Boolean, cols: Int, rows: Int) = update {
        if (landscape) {
            it[K.colsLand] = cols; it[K.rowsLand] = rows
        } else {
            it[K.colsPort] = cols; it[K.rowsPort] = rows
        }
    }
    suspend fun clearGridOverride(landscape: Boolean) = update {
        if (landscape) {
            it[K.colsLand] = -1; it[K.rowsLand] = -1
        } else {
            it[K.colsPort] = -1; it[K.rowsPort] = -1
        }
    }
    suspend fun setIconShape(v: Int) = update { it[K.iconShape] = v.coerceIn(0, 3) }
    suspend fun setIconSizeOverride(v: Int) = update { it[K.iconSizeOverride] = v }
    suspend fun setLabels(on: Boolean, size: Int) = update {
        it[K.labelsOn] = on; it[K.labelSize] = size.coerceIn(9, 16)
    }
    suspend fun setFontChoice(v: Int) = update { it[K.fontChoice] = v.coerceIn(0, 2) }
    suspend fun setReduceEffects(v: Boolean) = update { it[K.reduceEffects] = v }
    suspend fun setReduceMotion(v: Boolean) = update { it[K.reduceMotion] = v }
    suspend fun setCustomLooksJson(v: String) = update { it[K.customLooksJson] = v }
    suspend fun setWallpaperMode(v: Int) = update { it[K.wallpaperMode] = v.coerceIn(0, 1) }
    suspend fun setWpKind(v: Int) = update { it[K.wpKind] = v.coerceIn(0, 5) }
    suspend fun setWpUri(v: String) = update { it[K.wpUri] = v }
    suspend fun setWpDim(v: Int) = update { it[K.wpDim] = v.coerceIn(0, 60) }
    suspend fun setWpZoom(v: Int) = update { it[K.wpZoom] = v.coerceIn(100, 200) }
    suspend fun setWpOffsets(x: Float, y: Float) = update {
        it[K.wpOffX] = x.coerceIn(-1f, 1f); it[K.wpOffY] = y.coerceIn(-1f, 1f)
    }
    suspend fun setWpTint(v: Int) = update { it[K.wpTint] = v }
    suspend fun setWpSolid(v: Int) = update { it[K.wpSolid] = v }
    suspend fun setWpParallax(v: Boolean) = update { it[K.wpParallax] = v }
    // Syncs snapToGrid with the old snapOn() rule (v != 1) so the two
    // can never diverge through the existing writers (Hub Snap row,
    // section reset, backup import). setSnapToGrid below is the
    // independent write for the future host that reads snapToGrid.
    suspend fun setPlacementMode(v: Int) = update {
        val coerced = v.coerceIn(0, 2)
        it[K.placementMode] = coerced
        it[K.snapToGrid] = coerced != 1
    }
    suspend fun setSwipeUpDrawer(v: Boolean) = update { it[K.swipeUpDrawer] = v }
    suspend fun setGapDp(v: Int) = update { it[K.gapDp] = v.coerceIn(0, 12) }
    suspend fun setLockLayout(v: Boolean) = update { it[K.lockLayout] = v }
    suspend fun setClockPage(v: Boolean) = update { it[K.clockPage] = v }
    suspend fun setAtAGlance(v: Int) = update { it[K.atAGlance] = v.coerceIn(0, 2) }
    suspend fun setAutoAddApps(v: Boolean) = update { it[K.autoAddApps] = v }
    suspend fun setDock(v: List<String>) = update { it[K.dockJson] = encodeMembers(v) }
    suspend fun setDockMax(v: Int) = update { it[K.dockMax] = v.coerceIn(0, 12) }
    suspend fun setDockRecent(v: Boolean) = update { it[K.dockRecent] = v }
    suspend fun setDockPos(v: Int) = update { it[K.dockPos] = v.coerceIn(0, 2) }
    suspend fun setDockAutoHide(v: Boolean) = update { it[K.dockAutoHide] = v }
    suspend fun setDockSearch(v: Boolean) = update { it[K.dockSearch] = v }
    suspend fun setDockDrawerBtn(v: Boolean) = update { it[K.dockDrawerBtn] = v }
    suspend fun setDrawerMode(v: Int) = update { it[K.drawerMode] = v.coerceIn(0, 3) }
    suspend fun setDrawerSort(v: Int) = update { it[K.drawerSort] = v.coerceIn(0, 2) }
    suspend fun setScrollbarStyle(v: Int) = update { it[K.scrollbarStyle] = v.coerceIn(0, 2) }
    suspend fun setHiddenJson(v: String) = update { it[K.hiddenJson] = v }
    suspend fun setHiddenLock(v: Boolean) = update { it[K.hiddenLock] = v }
    suspend fun setCatHiddenJson(v: String) = update { it[K.catHiddenJson] = v }
    suspend fun setCatNamesJson(v: String) = update { it[K.catNamesJson] = v }
    suspend fun setWeather(city: String, lat: String, lon: String) = update {
        it[K.weatherCity] = city; it[K.weatherLat] = lat; it[K.weatherLon] = lon
    }
    suspend fun setWeatherUnits(v: Int) = update { it[K.weatherUnits] = v.coerceIn(0, 1) }
    suspend fun setWeatherNetwork(v: Boolean) = update { it[K.weatherNetwork] = v }
    suspend fun setCalendarsJson(v: String) = update { it[K.calendarsJson] = v }
    suspend fun setStacksOn(v: Boolean) = update { it[K.stacksOn] = v }
    suspend fun setIconPack(v: String) = update { it[K.iconPack] = v }
    suspend fun setMonoIcons(v: Boolean) = update { it[K.monoIcons] = v }
    suspend fun setBadgeMode(v: Int) = update { it[K.badgeMode] = v.coerceIn(0, 2) }
    suspend fun setGesturesJson(v: String) = update { it[K.gesturesJson] = v }
    suspend fun setNavGesturesOn(v: Boolean) = update { it[K.navGesturesOn] = v }
    suspend fun setNavMapJson(v: String) = update { it[K.navMapJson] = v }
    suspend fun setNavEdgeDp(v: Int) = update { it[K.navEdgeDp] = v.coerceIn(12, 48) }
    suspend fun setNavSensitivity(v: Int) = update { it[K.navSensitivity] = v.coerceIn(0, 100) }
    suspend fun setNavHaptics(v: Boolean) = update { it[K.navHaptics] = v }
    suspend fun setNavHideBar(v: Boolean) = update { it[K.navHideBar] = v }
    suspend fun setNavSavedPolicy(v: String, set: Boolean) = update {
        it[K.navSavedPolicy] = v; it[K.navSavedPolicySet] = set
    }
    suspend fun setSearchSources(contacts: Boolean, calendar: Boolean, settings: Boolean, calc: Boolean) =
        update {
            it[K.searchContacts] = contacts; it[K.searchCalendar] = calendar
            it[K.searchSettings] = settings; it[K.searchCalc] = calc
        }
    suspend fun setSearchHistoryJson(v: String) = update { it[K.searchHistoryJson] = v }
    suspend fun setProfilesJson(v: String) = update { it[K.profilesJson] = v }
    suspend fun setActiveProfile(v: String) = update { it[K.activeProfile] = v }
    suspend fun setProfileSchedule(on: Boolean, start: Int, end: Int, profile: String) = update {
        it[K.profileSchedule] = on
        it[K.schedStart] = start.coerceIn(0, 1439)
        it[K.schedEnd] = end.coerceIn(0, 1439)
        it[K.schedProfile] = profile
    }
    suspend fun setBackupIncludeWp(v: Boolean) = update { it[K.backupIncludeWp] = v }
    suspend fun setLastBackupDay(v: Long) = update { it[K.lastBackupDay] = v }
    suspend fun setUndoJson(v: String) = update { it[K.undoJson] = v }
    suspend fun setLaunchStatsJson(v: String) = update { it[K.launchStatsJson] = v }
    suspend fun setSeeded(v: Boolean) = update { it[K.seeded] = v }
    suspend fun setMiniHomeCollapsed(v: Boolean) = update { it[K.miniHomeCollapsed] = v }
    suspend fun setLabelOverridesJson(v: String) = update { it[K.labelOverridesJson] = v }
    suspend fun setWidgetFrame(radius: Int, bg: Int, opacity: Int) = update {
        it[K.widgetRadius] = radius.coerceIn(0, 32)
        it[K.widgetBg] = bg.coerceIn(0, 2)
        it[K.widgetOpacity] = opacity.coerceIn(0, 100)
    }
    suspend fun setGestureSensitivity(v: Int) = update { it[K.gestureSensitivity] = v.coerceIn(0, 100) }
    suspend fun setAnimSpeed(v: Int) = update { it[K.animSpeed] = v.coerceIn(0, 3) }
    suspend fun setPressFeedback(v: Boolean) = update { it[K.pressFeedback] = v }
    suspend fun setHapticStrength(v: Int) = update { it[K.hapticStrength] = v.coerceIn(0, 100) }

    // M5 Categories 1-7 typed setters (stale-type-safe via parse() coercions).
    suspend fun setAccent(mode: Int, argb: Int) = update {
        it[K.accentMode] = mode.coerceIn(0, 3); it[K.accentArgb] = argb
    }
    suspend fun setCornerDp(v: Int) = update { it[K.cornerDp] = v.coerceIn(-1, 40) }
    suspend fun setSurfaceMaterial(v: Int) = update { it[K.surfaceMaterial] = v.coerceIn(0, 2) }
    suspend fun setSurfaceBlurPct(v: Int) = update { it[K.surfaceBlurPct] = v.coerceIn(0, 100) }
    suspend fun setSurfaceNoisePct(v: Int) = update { it[K.surfaceNoisePct] = v.coerceIn(0, 100) }
    suspend fun setShadowDp(v: Int) = update { it[K.shadowDp] = v.coerceIn(-1, 24) }
    suspend fun setOutlineOn(v: Boolean) = update { it[K.outlineOn] = v }
    suspend fun setFontWeight(v: Int) = update { it[K.fontWeight] = v.coerceIn(0, 2) }
    suspend fun setFontScalePct(v: Int) = update { it[K.fontScalePct] = v.coerceIn(80, 130) }
    suspend fun setGridTablet(cols: Int, rows: Int) = update {
        it[K.colsTablet] = cols; it[K.rowsTablet] = rows
    }
    suspend fun clearGridTablet() = update { it[K.colsTablet] = -1; it[K.rowsTablet] = -1 }
    suspend fun setMarginDp(v: Int) = update { it[K.marginDp] = v.coerceIn(0, 32) }
    suspend fun setCellSpacingDp(v: Int) = update { it[K.cellSpacingDp] = v.coerceIn(0, 24) }
    suspend fun setLabelLines(v: Int) = update { it[K.labelLines] = v.coerceIn(1, 3) }
    suspend fun setLabelColorArgb(v: Int) = update { it[K.labelColorArgb] = v }
    suspend fun setSearchBar(pos: Int, style: Int, heightDp: Int) = update {
        it[K.searchBarPos] = pos.coerceIn(0, 2)
        it[K.searchBarStyle] = style.coerceIn(0, 2)
        it[K.searchBarHeightDp] = heightDp.coerceIn(40, 72)
    }
    suspend fun setTodayPage(on: Boolean, content: Int) = update {
        it[K.todayPageOn] = on; it[K.todayPageContent] = content.coerceIn(0, 2)
    }
    suspend fun setScrollEffect(v: Int) = update { it[K.scrollEffect] = v.coerceIn(0, 3) }
    suspend fun setStatusBarMode(v: Int) = update { it[K.statusBarMode] = v.coerceIn(0, 3) }
    suspend fun setWpBlurPct(v: Int) = update { it[K.wpBlurPct] = v.coerceIn(0, 100) }
    suspend fun setWpAutoDim(v: Boolean) = update { it[K.wpAutoDim] = v }
    suspend fun setWpPerPage(v: Boolean) = update { it[K.wpPerPage] = v }
    suspend fun setDockRows(v: Int) = update { it[K.dockRows] = v.coerceIn(1, 2) }
    suspend fun setDockFloating(v: Boolean) = update { it[K.dockFloating] = v }
    suspend fun setDockMaterial(v: Int, opacityPct: Int) = update {
        it[K.dockMaterial] = v.coerceIn(0, 2); it[K.dockOpacityPct] = opacityPct.coerceIn(0, 100)
    }
    suspend fun setDockMagnify(v: Boolean) = update { it[K.dockMagnify] = v }
    suspend fun setDockLabels(v: Boolean) = update { it[K.dockLabels] = v }
    suspend fun setDrawerStyle(v: Int) = update {
        it[K.drawerStyle] = v.coerceIn(0, 4)
        // Keep the live Drawer in sync for the styles it already renders.
        when (v) {
            0 -> it[K.drawerMode] = 0
            2 -> it[K.drawerMode] = 1
            4 -> it[K.drawerMode] = 2
        }
    }
    suspend fun setDrawerGesture(v: Int) = update { it[K.drawerGesture] = v.coerceIn(0, 2) }
    suspend fun setDrawerCols(v: Int) = update { it[K.drawerCols] = v.coerceIn(0, 8) }
    suspend fun setDrawerIconDp(v: Int) = update { it[K.drawerIconDp] = v.coerceIn(0, 96) }
    suspend fun setDrawerRows(onSearch: Boolean, onRecents: Boolean, onSuggestions: Boolean) = update {
        it[K.drawerSearchOn] = onSearch; it[K.drawerRecentsOn] = onRecents
        it[K.drawerSuggestionsOn] = onSuggestions
    }
    suspend fun setIconShapeExt(v: Int) = update {
        it[K.iconShapeExt] = v.coerceIn(0, 5)
        if (v in 0..3) it[K.iconShape] = v
    }
    suspend fun setPerAppIconJson(v: String) = update { it[K.perAppIconJson] = v }
    suspend fun setFolderPreview(v: Int) = update { it[K.folderPreview] = v.coerceIn(0, 2) }
    suspend fun setFolderAnim(v: Int) = update { it[K.folderAnim] = v.coerceIn(0, 2) }
    suspend fun setFolderSizePct(v: Int) = update { it[K.folderSizePct] = v.coerceIn(50, 150) }
    suspend fun setFolderBg(v: Int) = update { it[K.folderBg] = v.coerceIn(0, 2) }
    suspend fun setFolderTitleOn(v: Boolean) = update { it[K.folderTitleOn] = v }
    suspend fun setIconSwipesJson(v: String) = update { it[K.iconSwipesJson] = v }
    suspend fun setBadgeHiddenJson(v: String) = update { it[K.badgeHiddenJson] = v }
    suspend fun setCatMergeJson(v: String) = update { it[K.catMergeJson] = v }

    // M7 Wave 2 typed setters. String keys normalize on write exactly
    // as parse() normalizes on read, so no invalid value can persist.
    suspend fun setLabelMode(v: String) = update {
        it[K.labelMode] = if (v == "pill" || v == "off") v else "auto"
    }
    suspend fun setSnapToGrid(v: Boolean) = update { it[K.snapToGrid] = v }
    // Writes both keys: dockRecentsOn is the OneDock flag (Dock.kt),
    // dockRecent is the legacy flag Home.kt still reads live. One row,
    // one call, both readers agree.
    suspend fun setDockRecentsOn(v: Boolean) = update {
        it[K.dockRecentsOn] = v; it[K.dockRecent] = v
    }
    suspend fun setLibraryDefaultView(v: String) = update {
        it[K.libraryDefaultView] =
            if (v == "categories" || v == "list") v else "all"
    }
    suspend fun setTodayOn(v: Boolean) = update { it[K.todayOn] = v }
    suspend fun setSmartStackAutoRotate(v: Boolean) = update { it[K.smartStackAutoRotate] = v }
    suspend fun setCategoryOverridesJson(v: String) = update { it[K.categoryOverridesJson] = v }
    suspend fun setCalmFavoritesJson(v: String) = update { it[K.calmFavoritesJson] = v }
    suspend fun setCalmScheduleJson(v: String) = update { it[K.calmScheduleJson] = v }
    suspend fun setCalmEntryJson(v: String) = update { it[K.calmEntryJson] = v }

    // M8 pages typed setters.
    suspend fun setPageIndicatorOn(v: Boolean) = update { it[K.pageIndicatorOn] = v }
    suspend fun setParallaxOn(v: Boolean) = update { it[K.parallaxOn] = v }
    suspend fun setDefaultPage(v: Int) = update { it[K.defaultPage] = v.coerceAtLeast(0) }
    suspend fun setHiddenPagesJson(v: String) = update { it[K.hiddenPagesJson] = v }
    suspend fun setPagesMigrated(v: Boolean) = update { it[K.pagesMigrated] = v }
    suspend fun setPageCount(v: Int) = update { it[K.pageCount] = v.coerceAtLeast(0) }

    // M9 edit mode: hint-chip open count (Doc 1 §3.10).
    suspend fun setEditHintOpens(v: Int) = update { it[K.editHintOpens] = v.coerceIn(0, 100) }

    /** Section defaults for Settings → "Reset to default" per section. */
    suspend fun resetSection(section: Int) {
        when (section) {
            1 -> update {
                it[K.look] = "Minimal"; it[K.darkMode] = 0; it[K.density] = 40
                it[K.iconShape] = 1; it[K.iconSizeOverride] = -1; it[K.labelsOn] = true
                it[K.labelSize] = 11; it[K.fontChoice] = 0
                it[K.reduceEffects] = false; it[K.reduceMotion] = false
                it[K.accentMode] = 0; it[K.accentArgb] = 0; it[K.cornerDp] = -1
                it[K.labelMode] = "auto"
                it[K.surfaceMaterial] = 2
                it[K.surfaceBlurPct] = 60; it[K.surfaceNoisePct] = 0; it[K.shadowDp] = -1
                it[K.outlineOn] = true; it[K.fontWeight] = 0; it[K.fontScalePct] = 100
            }
            2 -> update {
                it[K.wallpaperMode] = 0; it[K.wpKind] = 0; it[K.wpUri] = ""
                it[K.wpDim] = 15; it[K.wpZoom] = 100; it[K.wpOffX] = 0f; it[K.wpOffY] = 0f
                it[K.wpTint] = 0; it[K.wpSolid] = 0; it[K.wpParallax] = false
                it[K.wpBlurPct] = 0; it[K.wpAutoDim] = false; it[K.wpPerPage] = false
            }
            3 -> update {
                it[K.placementMode] = 0; it[K.snapToGrid] = true
                it[K.gapDp] = 4; it[K.lockLayout] = false; it[K.clockPage] = true
                it[K.todayOn] = true; it[K.smartStackAutoRotate] = false
                it[K.calmFavoritesJson] = ""; it[K.calmScheduleJson] = ""
                it[K.calmEntryJson] = ""
                it[K.swipeUpDrawer] = true
                it[K.atAGlance] = 1; it[K.autoAddApps] = true
                // M8 pages: indicator/parallax/default/hidden reset;
                // pagesMigrated deliberately NOT reset — clearing it
                // would re-run the flow -> pages migration over a
                // layout the user has since edited.
                it[K.pageIndicatorOn] = true; it[K.parallaxOn] = true
                it[K.defaultPage] = 0; it[K.hiddenPagesJson] = ""
                it[K.pageCount] = 0
                it[K.colsLand] = -1; it[K.rowsLand] = -1; it[K.colsPort] = -1; it[K.rowsPort] = -1
                it[K.colsTablet] = -1; it[K.rowsTablet] = -1
                it[K.marginDp] = 16; it[K.cellSpacingDp] = 4; it[K.labelLines] = 1
                it[K.labelColorArgb] = 0; it[K.searchBarPos] = 0; it[K.searchBarStyle] = 0
                it[K.searchBarHeightDp] = 56; it[K.todayPageOn] = false; it[K.todayPageContent] = 0
                it[K.scrollEffect] = 0
                it[K.statusBarMode] = 0
            }
            4 -> update {
                it[K.dockMax] = 6; it[K.dockRecent] = true; it[K.dockRecentsOn] = true; it[K.dockPos] = 0
                it[K.dockAutoHide] = false; it[K.dockSearch] = true; it[K.dockDrawerBtn] = true
                it[K.dockRows] = 1; it[K.dockFloating] = true; it[K.dockMaterial] = 0
                it[K.dockOpacityPct] = 100; it[K.dockMagnify] = false; it[K.dockLabels] = false
            }
            5 -> update {
                it[K.drawerMode] = 1; it[K.drawerSort] = 0; it[K.scrollbarStyle] = 1
                it[K.libraryDefaultView] = "all"; it[K.categoryOverridesJson] = ""
                it[K.drawerStyle] = 2; it[K.drawerGesture] = 0; it[K.drawerCols] = 0
                it[K.drawerIconDp] = 0; it[K.drawerSearchOn] = true
                it[K.drawerRecentsOn] = true; it[K.drawerSuggestionsOn] = true
            }
            6 -> update {
                it[K.weatherUnits] = 0; it[K.weatherNetwork] = true
                it[K.calendarsJson] = ""; it[K.stacksOn] = true
            }
            7 -> update {
                it[K.iconPack] = ""; it[K.monoIcons] = false; it[K.badgeMode] = 1
                it[K.labelsOn] = true; it[K.labelSize] = 11
                it[K.iconShapeExt] = 1; it[K.iconShape] = 1; it[K.perAppIconJson] = "{}"
            }
            8 -> update {
                it[K.gesturesJson] = DEFAULT_GESTURES
                it[K.navMapJson] = DEFAULT_NAV_MAP; it[K.navEdgeDp] = 24
                it[K.navSensitivity] = 50; it[K.navHaptics] = true
            }
            9 -> update {
                it[K.searchContacts] = false; it[K.searchCalendar] = false
                it[K.searchSettings] = true; it[K.searchCalc] = true
            }
        }
    }
}
