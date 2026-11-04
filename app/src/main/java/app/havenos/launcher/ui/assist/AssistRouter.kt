package app.havenos.launcher.ui.assist

import app.havenos.launcher.data.Settings
import app.havenos.launcher.ui.LibraryApp
import app.havenos.launcher.ui.glass.AccentSelection
import app.havenos.launcher.ui.glass.AccentSource
import app.havenos.launcher.ui.glass.AccentStrength
import app.havenos.launcher.ui.glass.parseRecents
import app.havenos.launcher.ui.mascot.FindBand
import app.havenos.launcher.ui.mascot.FindTarget
import app.havenos.launcher.ui.mascot.matchApps
import app.havenos.launcher.ui.mascot.matchSettings
import app.havenos.launcher.ui.mascot.normalizeFindText

// 3.1 Phase F (F1–F3): the on-device assist foundation. LocalAssist is the
// ONE AssistProvider implementation: rules, synonyms and ranking over data
// already on the device — no model, no network, no new permission. Every
// action it can emit is a typed object validated against the whitelist
// below; anything outside it (uninstall, hide-all, permission grants) has
// no representation here at all, so it cannot be emitted, only refused
// by falling back to normal search.

/** The whitelist (prompt §F3). Sealed: no other action type can exist. */
sealed interface AssistAction {
    /** Launch an installed app, resolved against the real app list. */
    data class OpenApp(val appKey: String, val label: String, val pkg: String) : AssistAction

    /** Open a settings hub by its real route. */
    data class OpenHub(val route: String, val title: String) : AssistAction

    /** Dark mode: 0 system, 1 light, 2 dark, 3 auto (SettingsState range). */
    data class SetDarkMode(val mode: Int) : AssistAction

    /** Icon-size step in dp; the executor clamps to the real UI range. */
    data class StepIconSize(val deltaDp: Int) : AssistAction

    /** Curated accent by its real name (never an invented colour). */
    data class SetAccentCurated(val name: String) : AssistAction

    /** Open the transient Calm overlay. No persistent setting changes. */
    data object ToggleCalm : AssistAction
}

/**
 * One parsed command. [hubRoute] is the settings hub the command lands
 * the user in (null for app launches and Calm, which need no hub); the
 * host uses it to open the panel so the change lands in Before/Changes
 * with a readable name and a Revert.
 */
data class AssistCommand(
    val action: AssistAction,
    val label: String,
    val hubRoute: String?,
)

/** Everything the parser needs from the host. All local, all real. */
data class AssistContext(
    val apps: List<LibraryApp>,
    val stats: Map<String, Pair<Int, Long>>,
    val darkMode: Int,
    val effectiveIconDp: Int,
    val curatedNames: List<String>,
    val commandsEnabled: Boolean = true,
)

/** Effective icon size, mirroring Settings.kt's formula exactly. */
fun effectiveIconDp(iconSizeOverride: Int, density: Int): Int =
    if (iconSizeOverride > 0) iconSizeOverride else 42 + density.coerceIn(0, 100) * 25 / 100

/** Icon-size UI range (HubScreen slider): the executor clamps to this. */
const val ASSIST_ICON_MIN_DP = 40
const val ASSIST_ICON_MAX_DP = 68
const val ASSIST_ICON_STEP_DP = 8

private fun darkModeName(mode: Int): String = when (mode) {
    1 -> "Light"
    2 -> "Dark"
    3 -> "Auto"
    else -> "System"
}

/**
 * Hub aliases, resolved against the REAL routes (the panel registry).
 * "settings" alone means the hub root; "home" alone means Home & Dock —
 * "Settings" the app is reached via the root alias.
 */
private val HUB_ALIASES: Map<String, Pair<String, String>> = mapOf(
    "settings" to ("home" to "Settings"),
    "look" to ("look" to "Look"),
    "home" to ("home-dock" to "Home & Dock"),
    "home and dock" to ("home-dock" to "Home & Dock"),
    "dock" to ("home-dock" to "Home & Dock"),
    "drawer" to ("drawer-search" to "Drawer & Search"),
    "app drawer" to ("drawer-search" to "Drawer & Search"),
    "library" to ("drawer-search" to "Drawer & Search"),
    "search" to ("drawer-search" to "Drawer & Search"),
    "gestures" to ("gestures-motion" to "Gestures & Motion"),
    "gesture" to ("gestures-motion" to "Gestures & Motion"),
    "animations" to ("gestures-motion" to "Gestures & Motion"),
    "motion" to ("gestures-motion" to "Gestures & Motion"),
    "privacy" to ("privacy" to "Privacy"),
    "device" to ("device-data" to "Device & Data"),
    "data" to ("device-data" to "Device & Data"),
    "backup" to ("device-data" to "Device & Data"),
    "device and data" to ("device-data" to "Device & Data"),
    "apis" to ("apis" to "APIs"),
    "api" to ("apis" to "APIs"),
    "mascot" to ("mascot" to "Mascot"),
)

/**
 * The command grammar (prompt §F2). Returns null when the query is not
 * a command — the caller then falls back to normal search, unchanged.
 * Compounds split on " and " / "," and are all-or-nothing: one bad
 * clause voids the whole query, so a command never half-applies.
 * "silent" deliberately has NO mapping: no silent / do-not-disturb
 * setting exists in SettingsState, and inventing one would need new
 * behaviour and a permission beyond §F — so it falls back to search.
 */
fun parseAssistCommands(query: String, ctx: AssistContext): List<AssistCommand>? {
    if (!ctx.commandsEnabled) return null
    if (query.isBlank()) return null
    val clauses = query.split(Regex("(?i)\\s+and\\s+|," )).map { it.trim() }.filter { it.isNotEmpty() }
    if (clauses.isEmpty()) return null
    val commands = ArrayList<AssistCommand>()
    for (clause in clauses) {
        commands += parseAssistClause(clause, ctx) ?: return null
    }
    return commands
}

private fun parseAssistClause(rawClause: String, ctx: AssistContext): AssistCommand? {
    val clause = normalizeFindText(rawClause)
    if (clause.isBlank()) return null
    // Dark / light / system theme.
    when (clause) {
        "dark mode", "night mode", "dark theme" -> return AssistCommand(
            AssistAction.SetDarkMode(2),
            "Dark mode: ${darkModeName(ctx.darkMode)} → Dark",
            "look",
        )
        "light mode", "light theme" -> return AssistCommand(
            AssistAction.SetDarkMode(1),
            "Dark mode: ${darkModeName(ctx.darkMode)} → Light",
            "look",
        )
        "system theme", "system mode" -> return AssistCommand(
            AssistAction.SetDarkMode(0),
            "Dark mode: ${darkModeName(ctx.darkMode)} → System",
            "look",
        )
        "bigger icons", "larger icons", "increase icon size" ->
            return iconStepCommand(ctx, ASSIST_ICON_STEP_DP)
        "smaller icons", "decrease icon size" ->
            return iconStepCommand(ctx, -ASSIST_ICON_STEP_DP)
        "calm", "toggle calm", "open calm" -> return AssistCommand(
            AssistAction.ToggleCalm,
            "Open Calm",
            null,
        )
    }
    // Accent: "accent X" / "set accent X" (with optional "color").
    val accentName = when {
        clause.startsWith("set accent color ") -> clause.removePrefix("set accent color ")
        clause.startsWith("accent color ") -> clause.removePrefix("accent color ")
        clause.startsWith("set accent ") -> clause.removePrefix("set accent ")
        clause.startsWith("accent ") -> clause.removePrefix("accent ")
        else -> null
    }
    if (accentName != null) {
        val canonical = ctx.curatedNames.firstOrNull { normalizeFindText(it) == accentName }
            ?: return null
        return AssistCommand(
            AssistAction.SetAccentCurated(canonical),
            "Accent: $canonical",
            "look",
        )
    }
    // "go to X": hubs only — a place, not an app.
    if (clause.startsWith("go to ")) {
        val target = clause.removePrefix("go to ")
        return resolveHub(target, ctx)
    }
    // "open X" / "launch X": hub first, then the real app list.
    if (clause.startsWith("open ") || clause.startsWith("launch ")) {
        val target = clause.removePrefix("open ").removePrefix("launch ")
        return resolveHub(target, ctx) ?: resolveApp(target, ctx)
    }
    return null
}

private fun iconStepCommand(ctx: AssistContext, delta: Int): AssistCommand {
    val current = ctx.effectiveIconDp
    val next = (current + delta).coerceIn(ASSIST_ICON_MIN_DP, ASSIST_ICON_MAX_DP)
    val label = if (next == current) {
        val bound = if (delta > 0) "maximum" else "minimum"
        "Icon size: already at $bound (${current}dp)"
    } else {
        "Icon size: ${current}dp → ${next}dp"
    }
    return AssistCommand(AssistAction.StepIconSize(delta), label, "home-dock")
}

private fun resolveHub(target: String, @Suppress("UNUSED_PARAMETER") ctx: AssistContext): AssistCommand? {
    HUB_ALIASES[target]?.let { (route, title) ->
        return AssistCommand(AssistAction.OpenHub(route, title), "Open $title", route)
    }
    // Fallback: the real settings index (titles + keywords, real routes).
    // Only exact/prefix/keyword matches count — a contains match is too
    // loose to open a hub on the user's behalf.
    val hit = matchSettings(target).firstOrNull()
    if (hit != null && hit.band <= FindBand.WORD) {
        val setting = hit.target as? FindTarget.Setting ?: return null
        return AssistCommand(
            AssistAction.OpenHub(setting.route, setting.title),
            "Open ${setting.title}",
            setting.route,
        )
    }
    return null
}

private fun resolveApp(target: String, ctx: AssistContext): AssistCommand? {
    val candidates = matchApps(ctx.apps, target, ctx.stats)
    if (candidates.isEmpty()) return null
    val top = candidates[0].target as? FindTarget.App ?: return null
    // Ambiguity guard: two candidates tied on band AND confidence (band
    // + launch count) are indistinguishable — refuse rather than guess.
    if (candidates.size > 1) {
        val second = candidates[1]
        if (second.band == candidates[0].band && second.confidence == candidates[0].confidence) {
            return null
        }
    }
    return AssistCommand(AssistAction.OpenApp(top.key, top.label, top.pkg), "Open ${top.label}", null)
}

/**
 * Executes a setting-changing command through the typed SettingsState
 * accessors, with range checks. Called while the settings panel session
 * is active (panel path) or inside the panel-open effect after
 * panelSession.begin() (drawer path), so the change lands in
 * Before/Changes/Undo with its readable name and a Revert — the preview
 * plus confirmation §F3 requires. Non-setting actions are no-ops here;
 * the host executes those (launch, hub navigation, Calm) itself.
 */
suspend fun applyAssistSettingAction(settings: Settings, action: AssistAction) {
    val live = settings.state.value
    when (action) {
        is AssistAction.SetDarkMode -> settings.setDarkMode(action.mode)
        is AssistAction.StepIconSize -> {
            val current = effectiveIconDp(live.iconSizeOverride, live.density)
            val next = (current + action.deltaDp).coerceIn(ASSIST_ICON_MIN_DP, ASSIST_ICON_MAX_DP)
            settings.setIconSizeOverride(next)
        }
        is AssistAction.SetAccentCurated -> {
            // Preserve strength/recents: only source + curated change.
            settings.setAccentSelection(
                AccentSelection(
                    source = AccentSource.CURATED,
                    curated = action.name,
                    customArgb = live.accentCustomArgb,
                    strength = AccentStrength.entries.getOrNull(live.accentStrength)
                        ?: AccentStrength.SUBTLE,
                    recents = parseRecents(live.accentRecents),
                ),
            )
        }
        else -> Unit
    }
}
