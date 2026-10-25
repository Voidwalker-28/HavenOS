package app.havenos.launcher.data

/**
 * Worker D (M9) — coordination + Settings behaviors, Doc 1 §4–§6 and
 * Doc 3 §3.3/§4/§5. Pure functions over [SettingsState]/[LayoutState].
 *
 * ADAPTATION (standing user direction, overrides the docs where they
 * conflict): there is NO preset picker/carousel/switching. "Preset"
 * everywhere below means Minimal's tokens; [LookTokens] is always
 * Minimal + user overrides. Where existing UI copy says "preset", the
 * copy is left unchanged (frozen UI) but its behavior is Minimal
 * defaults — see [lookChanges] and [applyLookCommit].
 *
 * Precedence (Doc 1 §6), highest wins, implemented here:
 * user > Minimal defaults (mark Modified) · manual > Auto ·
 * per-page wallpaper > global (fallback global if missing) ·
 * dock > grid (re-fit + overflow preview) · lock > edit ·
 * reduced motion > motion preset · a11y > icon size (label wraps or
 * hides, icon touch target never < 48dp) · permission > feature ·
 * Calm active vs edit -> edit exits Calm first, state restored on
 * exit · chosen accent > system dynamic colour unless
 * "Wallpaper-derived" is selected.
 */

// ------------------------------------------------------------------ look

/**
 * Derived look tokens (Doc 3 §3.1): Minimal + overrides + mode +
 * wallpaper values. DERIVED, never stored — the coordinator recomputes
 * them whenever SettingsState, the system mode, or the wallpaper
 * palette changes. [modified] is true when any token differs from
 * Minimal defaults (Doc 1 §5.5 "Modified" flag).
 */
data class LookTokens(
    val dark: Boolean,
    /** 0 = Minimal default accent; otherwise the chosen ARGB. */
    val accentArgb: Int,
    val cornerDp: Int,
    val material: Int, // 0 solid, 1 translucent, 2 frosted
    val modified: Boolean,
)

/** Minimal defaults, the only "preset" in the Minimal-only build. */
fun minimalLookTokens(dark: Boolean): LookTokens =
    LookTokens(dark = dark, accentArgb = 0, cornerDp = 12, material = 2, modified = false)

/**
 * Resolve the effective mode (Doc 1 §5.4): darkMode 0 follows the
 * system, 1 forces light, 2 forces dark, 3 follows a time schedule
 * ([scheduleDark] is true when the schedule says dark now).
 */
fun resolveDark(darkMode: Int, systemDark: Boolean, scheduleDark: Boolean = false): Boolean =
    when (darkMode) {
        1 -> false
        2 -> true
        3 -> scheduleDark
        else -> systemDark
    }

/** Mode cross-fade duration (Doc 1 §4): always 200ms. */
const val MODE_CROSSFADE_MS: Int = 200

/**
 * Derive LookTokens from settings. Accent precedence: a chosen accent
 * (palette/custom, accentMode 1/2) beats the system dynamic colour;
 * the system/wallpaper colour is used ONLY when accentMode is 3
 * ("Wallpaper-derived"). A wallpaper suggestion (see
 * [wallpaperPipelineResult]) is NEVER auto-applied — it only feeds the
 * suggestion chip.
 */
fun deriveLookTokens(
    settings: SettingsState,
    systemDark: Boolean,
    scheduleDark: Boolean = false,
    systemDynamicArgb: Int = 0,
): LookTokens {
    val dark = resolveDark(settings.darkMode, systemDark, scheduleDark)
    val base = minimalLookTokens(dark)
    val accent = when (settings.accentMode) {
        3 -> systemDynamicArgb // Wallpaper-derived: system colour wins.
        1, 2 -> settings.accentArgb // Palette / custom: chosen wins.
        else -> 0 // Minimal default.
    }
    val corner = if (settings.cornerDp >= 0) settings.cornerDp else base.cornerDp
    val material = settings.surfaceMaterial.coerceIn(0, 2)
    val modified = accent != 0 || corner != base.cornerDp || material != base.material ||
        settings.iconShape != 1 || settings.fontChoice != 0
    return base.copy(accentArgb = accent, cornerDp = corner, material = material, modified = modified)
}

// ------------------------------------------------------------- icon size

/** Icon-size resolution (Doc 1 §5.1). */
data class IconSizeResult(
    /** Visual icon size in dp. */
    val iconDp: Int,
    /** True when the value came from Auto derivation (grid/rotation move it). */
    val isAuto: Boolean,
    /** "Max for this grid" when a manual value was clamped to fit, else null. */
    val clampNote: String?,
    /** Touch target actually used: never below 48dp (Doc 1 §6 a11y rule). */
    val touchTargetDp: Int,
)

/**
 * Auto: f(cell, label, density) — a fraction of the smaller cell
 * dimension, shrunk when labels are on (label space is reserved, so the
 * icon must leave room for it), clamped to the 40..72dp usable band.
 * Manual: the fixed dp per profile; grid changes do not alter it
 * unless it no longer fits the cell, in which case it is clamped and
 * the note "Max for this grid" is returned.
 */
fun resolveIconSize(
    auto: Boolean,
    manualDp: Int,
    cellWidthDp: Int,
    cellHeightDp: Int,
    labelsOn: Boolean,
): IconSizeResult {
    val cellMin = minOf(cellWidthDp, cellHeightDp).coerceAtLeast(1)
    if (auto) {
        val fraction = if (labelsOn) 0.58f else 0.68f
        val dp = (cellMin * fraction).toInt().coerceIn(40, 72)
        return IconSizeResult(dp, true, null, maxOf(dp, 48))
    }
    val labelReserve = if (labelsOn) 20 else 0
    val fitMax = (cellMin - labelReserve).coerceAtLeast(40)
    return if (manualDp > fitMax) {
        IconSizeResult(fitMax, false, "Max for this grid", maxOf(fitMax, 48))
    } else {
        IconSizeResult(manualDp.coerceIn(40, 96), false, null, maxOf(manualDp, 48))
    }
}

/** Label space rule (Doc 1 §4): Labels Off removes label space and re-centres icons. */
fun labelSpaceDp(labelsOn: Boolean, labelSizeSp: Int): Int =
    if (labelsOn) labelSizeSp + 8 else 0

// ------------------------------------------------------------ dock/grid

/** Grid inset reserved for the dock (Doc 1 §5.3): the grid re-fits around it, never under it. */
data class DockInsets(val bottomDp: Int, val leftDp: Int, val rightDp: Int, val topDp: Int)

/** Dock position 0 bottom / 1 left / 2 right; side docks reserve a side inset, bottom a bottom inset. */
fun dockInsets(dockPos: Int, dockThicknessDp: Int = 84): DockInsets = when (dockPos) {
    1 -> DockInsets(0, dockThicknessDp, 0, 0)
    2 -> DockInsets(0, 0, dockThicknessDp, 0)
    else -> DockInsets(dockThicknessDp, 0, 0, 0)
}

/** Page-indicator placement (Doc 1 §5.3): above a bottom dock, else at the opposite edge's bottom. */
fun indicatorEdge(dockPos: Int): String = if (dockPos == 0) "above-bottom-dock" else "opposite-edge-bottom"

/** Dock-position change (Doc 3 §3.3 flow 2): 240ms animation while the grid re-fits. */
const val DOCK_MOVE_ANIM_MS: Int = 240

/**
 * Dock-position change plan: new insets + a grid-change plan for the
 * re-fit (dock > grid precedence: the grid yields, overflow is
 * previewed, never silently committed). The host applies the plan on
 * the same Apply/Cancel pattern as a grid change.
 */
fun planDockPositionChange(
    state: LayoutState,
    newDockPos: Int,
    colsCells: Int,
    rowsCells: Int,
): GridChangePlan = planGridChange(state.items, colsCells, rowsCells)

// -------------------------------------------------------------- wallpaper

/** Wallpaper pipeline result (Doc 1 §5.8, Doc 3 §3.3 flow 5). */
data class WallpaperPipelineResult(
    /** Suggested accent colour for the SUGGESTION CHIP. Never auto-applied. */
    val suggestedAccentArgb: Int?,
    val suggestAccent: Boolean,
    val legibilityRecompute: Boolean = true,
    val scrimRecompute: Boolean = true,
    val calmDimRecompute: Boolean = true,
)

/**
 * Wallpaper change -> palette -> accent SUGGESTION (not auto-applied)
 * -> luminance rebuild -> label styles -> Calm dim recompute.
 * [paletteArgb] is the extracted palette colour (null when extraction
 * produced nothing usable); per-page wallpaper uses the same pipeline,
 * falling back to the global palette when its file is missing.
 */
fun wallpaperPipelineResult(
    paletteArgb: Int?,
    perPagePaletteArgb: Int?,
    globalFallbackArgb: Int?,
): WallpaperPipelineResult {
    val effective = perPagePaletteArgb ?: paletteArgb ?: globalFallbackArgb
    return WallpaperPipelineResult(
        suggestedAccentArgb = effective,
        suggestAccent = effective != null,
    )
}

// ------------------------------------------------------- look draft/apply

/** One row of the Changes list (Doc 1 §5.6): a difference with a revert. */
data class ChangeEntry(val name: String, val before: String, val after: String)

/**
 * Changes list: differences from Minimal defaults AND from the
 * snapshot taken at panel/edit open (the Before snapshot). Each entry
 * carries its before value, which is its revert. "Preset" here means
 * Minimal defaults — see the file header.
 */
fun lookChanges(
    before: SettingsState,
    current: SettingsState,
    minimal: SettingsState = SettingsState(),
): List<ChangeEntry> {
    val out = mutableListOf<ChangeEntry>()
    fun add(name: String, beforeV: Any, currentV: Any, minimalV: Any) {
        if (currentV != minimalV || currentV != beforeV) {
            out += ChangeEntry(name, beforeV.toString(), currentV.toString())
        }
    }
    add("Mode", before.darkMode, current.darkMode, minimal.darkMode)
    add("Accent", before.accentArgb, current.accentArgb, minimal.accentArgb)
    add("Icon size", before.iconSizeOverride, current.iconSizeOverride, minimal.iconSizeOverride)
    add("Labels", before.labelsOn, current.labelsOn, minimal.labelsOn)
    add("Dock position", before.dockPos, current.dockPos, minimal.dockPos)
    add("Grid density", before.density, current.density, minimal.density)
    add("Today", before.todayOn, current.todayOn, minimal.todayOn)
    add("Lock layout", before.lockLayout, current.lockLayout, minimal.lockLayout)
    return out
}

/**
 * Apply look (Doc 3 §3.3 flow 6, Minimal-only): the draft (a
 * SettingsState carrying the user's overrides) commits as the new
 * state; "Modified" clears only in the sense that the committed state
 * BECOMES the baseline for the next Changes diff — the host snapshots
 * the committed state as the new Before snapshot and resets the
 * Changes list. Applying a look never moves items (Doc 1 §5.5).
 */
fun applyLookCommit(draft: SettingsState): SettingsState = draft

// ------------------------------------------------------------------- calm

/**
 * Calm enter/exit snapshot (Doc 3 §3.3 flow 10): Calm never changes
 * the layout — entering snapshots the UiState (page index, overlays,
 * edit-mode flag, dock visibility) and hides dock+home; exiting
 * restores the snapshot EXACTLY. Edit mode exits Calm first (Doc 1
 * §6): [enterEditFromCalm] returns the snapshot to restore on exit.
 */
data class CalmUiSnapshot(
    val pageIndex: Int,
    val editMode: Boolean,
    val overlaysOpen: Boolean,
    val dockVisible: Boolean,
)

fun calmEnterSnapshot(pageIndex: Int, editMode: Boolean, overlaysOpen: Boolean): CalmUiSnapshot =
    CalmUiSnapshot(pageIndex, editMode, overlaysOpen, dockVisible = true)

/** Calm dim recompute input: wallpaper dim + mode, recomputed on wallpaper/mode change. */
fun calmDimAlpha(wallpaperDimPct: Int, dark: Boolean): Float {
    val base = 0.62f
    val dim = wallpaperDimPct.coerceIn(0, 60) / 100f * 0.2f
    return (base + dim + if (dark) 0.05f else 0f).coerceIn(0f, 0.9f)
}

// ------------------------------------------------------------- permissions

/** One permission-map row (Doc 3 §4): ask only at point of use, one-sentence reason first. */
data class PermissionRow(
    val feature: String,
    val permission: String,
    val reason: String,
    /** What the UI shows when denied (never a pre-feature request). */
    val deniedState: String,
)

/** The permission map (Doc 3 §4). No row here is requested before its feature is used. */
fun permissionMap(): List<PermissionRow> = listOf(
    PermissionRow(
        "Weather location", "location (optional)",
        "Weather needs your location to show local conditions.",
        "Source toggle disabled with reason; choose a city manually instead.",
    ),
    PermissionRow(
        "Agenda / Glance event", "calendar",
        "Agenda needs calendar access to show your events.",
        "Needs-permission state with an \"Allow calendar\" chip.",
    ),
    PermissionRow(
        "Contact search", "contacts",
        "Contact search needs contacts access to find people.",
        "Source toggle disabled with reason.",
    ),
    PermissionRow(
        "Notification badges", "notification access",
        "Badges need notification access to count notifications.",
        "No badges and no reserved space.",
    ),
    PermissionRow(
        "Hidden apps lock", "biometric / device credential",
        "Locking hidden apps needs your device credential.",
        "Falls back to device credential.",
    ),
    PermissionRow(
        "Default launcher", "system role",
        "HavenOS needs the default-launcher role to handle the home button.",
        "Banner persists with a \"Make default\" action.",
    ),
)

/**
 * Permission granted/denied (Doc 3 §3.3 flow 11): dependent rows and
 * widgets update WITHOUT leaving the screen — this is a pure flag
 * computation the host applies to its already-open panel/widgets.
 * Returns (rowEnabled, widgetState) for one feature.
 */
fun permissionStateUpdate(granted: Boolean, deniedState: String): Pair<Boolean, String> =
    if (granted) true to "Ready" else false to deniedState

// ---------------------------------------------------------- default role

/**
 * Default-launcher transition (Doc 3 §3.3 flow 12, Doc 1 §5.7): while
 * not default the banner stays; once default, the banner disappears
 * AT ONCE and the home button returns to page 1 and closes overlays.
 */
data class DefaultLauncherTransition(
    val bannerVisible: Boolean,
    /** Page the home button returns to (page 1 = index 0) + close overlays. */
    val homeButtonPage: Int,
    val closeOverlays: Boolean,
)

fun defaultLauncherTransition(isDefault: Boolean): DefaultLauncherTransition =
    if (isDefault) {
        DefaultLauncherTransition(bannerVisible = false, homeButtonPage = 0, closeOverlays = true)
    } else {
        DefaultLauncherTransition(bannerVisible = true, homeButtonPage = -1, closeOverlays = false)
    }

// --------------------------------------------------------- motion/haptics

/**
 * Motion + haptics wiring where it touches state (Doc 1 §4/§6):
 * reduced motion OVERRIDES the animation-speed preset (fades only);
 * animation speed 0 (off) also yields no motion. Returns (durationMs,
 * fadeOnly). The 120ms icon/label cross-fade on icon-size change and
 * the 240ms dock move are base durations scaled the same way.
 */
fun effectiveMotion(baseMs: Int, animSpeed: Int, reducedMotion: Boolean): Pair<Int, Boolean> {
    if (reducedMotion) return 0 to true
    return when (animSpeed) {
        0 -> 0 to true
        1 -> (baseMs / 2) to false
        3 -> (baseMs * 3 / 2) to false
        else -> baseMs to false
    }
}

/** Icon-size change cross-fade (Doc 3 §3.3 flow 1): 120ms, scaled by [effectiveMotion]. */
const val ICON_RESIZE_CROSSFADE_MS: Int = 120

/** Haptic strength 0..100; 0 means no vibration. */
fun hapticEnabled(hapticStrength: Int, pressFeedback: Boolean): Boolean =
    pressFeedback && hapticStrength > 0

// ------------------------------------------------------------------- a11y

/**
 * TalkBack/keyboard actions the state layer exposes (Doc 1 §3.11):
 * Worker A's UI calls these names; each maps to a pure function here
 * or in LayoutState.kt. The state layer guarantees the actions exist
 * even though their UI is Worker A's.
 */
fun a11yActionsForItem(isWidget: Boolean): List<String> = buildList {
    add("Move left")
    add("Move right")
    add("Move up")
    add("Move down")
    add("Move to page")
    add("Add to dock")
    if (isWidget) add("Resize")
    add("Remove")
    add("Edit label")
    add("Hide")
}

// ------------------------------------------------------------ debounce

/** Slider-write debounce (Doc 1 §7): writes fire 200ms after the last change. */
const val SLIDER_DEBOUNCE_MS: Long = 200L

/**
 * Pure debounce gate: given the last change time and now, true when a
 * write is due. The coordinator implements the actual delay with a
 * coroutine Job; this function is what the unit test pins down.
 */
fun sliderWriteDue(lastChangeMs: Long, nowMs: Long): Boolean =
    nowMs - lastChangeMs >= SLIDER_DEBOUNCE_MS

// ------------------------------------------------------------------ backup

/**
 * Backup import validation (Doc 3 §3.5): an invalid backup changes
 * NOTHING and reports the reason. Version 1 backups (pre-M9) still
 * import — new keys are simply absent and keep current values (the
 * has() guards in Backup.kt). Version 2 adds layout profiles +
 * Calm editor options. Unknown types and malformed JSON are refused.
 */
fun validateBackupImport(type: String?, version: Int, parses: Boolean): String? {
    if (!parses) return "Backup file is damaged and was not imported."
    if (type != "havenos-backup") return "Not a HavenOS backup."
    if (version !in 1..2) return "Backup version $version is not supported."
    return null // valid
}

/** Current backup schema version (bumped M9: layout profiles + Calm editor options). */
const val BACKUP_VERSION: Int = 2

// ---------------------------------------------------------- fresh install

/**
 * Fresh-install dock (Doc 3 §5): pinned apps resolved from SYSTEM
 * INTENTS, never hard-coded packages. The caller resolves each intent
 * role against the PackageManager and passes the resolved packages
 * (in preference order per role); this function picks the first
 * installed package per role, in role order, deduplicated. Roles:
 * Phone, Messages, Browser, Camera, Settings, Gallery.
 */
enum class DockIntentRole { PHONE, MESSAGES, BROWSER, CAMERA, SETTINGS, GALLERY }

fun resolveDockFromIntents(
    resolvedByRole: Map<DockIntentRole, List<String>>,
    installedPackages: Set<String>,
    limit: Int = 6,
): List<String> {
    val out = mutableListOf<String>()
    for (role in DockIntentRole.entries) {
        if (out.size >= limit) break
        val pkg = resolvedByRole[role]?.firstOrNull { it in installedPackages } ?: continue
        if (pkg !in out) out += pkg
    }
    return out
}

/**
 * Fresh-install defaults (Doc 3 §5): Labels On + Auto legibility,
 * Icon size Auto, Grid Density, Mode Auto (system), Today on with
 * Clock+weather, Agenda (ONE — never a duplicate seed), Suggested.
 * These match SettingsState() defaults except where noted; the host
 * seeds page 1 with Weather-or-Clock + most-used apps AFTER first run
 * (launch stats exist by then) and creates folders only when the user
 * creates them. No duplicate Agenda seed: the seeder must check for an
 * existing AGENDA item before adding one.
 */
fun freshInstallTodayCards(): List<TodayCard> = listOf(
    TodayCard("clock-weather", ItemKind.CLOCK),
    TodayCard("agenda", ItemKind.AGENDA),
    TodayCard("suggested", ItemKind.STACK),
)

/** True when an Agenda seed would duplicate an existing Agenda item/widget. */
fun agendaSeedWouldDuplicate(items: List<HomeItem>): Boolean =
    items.any { it.kind == ItemKind.AGENDA }

/** One-time toast texts owned by this layer (Worker A owns their display). */
object LayerToasts {
    const val DOCK_FULL = "Dock is full"
    const val LAYOUT_LOCKED = "Layout is locked"
    const val COULDNT_SAVE = "Couldn't save"
    const val LAYOUT_RESTORED = "Layout restored"
}
