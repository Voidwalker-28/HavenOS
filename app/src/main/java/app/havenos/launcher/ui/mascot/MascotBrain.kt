package app.havenos.launcher.ui.mascot

import app.havenos.launcher.ui.LibraryApp

/**
 * HavenOS v3 Track A — MascotBrain seam + ScriptedBrain (prompt §6).
 *
 * `MascotBrain` is the seam: `suspend fun handle(...)` returning text +
 * expression + zero or more proposed actions. The model NEVER executes
 * anything directly — the host layer performs actions, and state-changing
 * actions require an explicit confirm chip in the bubble (prompt §6.2).
 *
 * Phase 3 decision (see docs/MASCOT-LLM-PLAN.md in this track dir):
 * LocalLlmBrain ships as a NOT-CONFIGURED implementation that answers
 * honestly instead of pretending. No model is bundled, no download URL
 * or checksum is invented — both are null placeholders listed as setup
 * gaps. Candidates considered and why none is wired: MediaPipe LLM
 * Inference API (maintained, Apache-2.0, but needs a Play-services /
 * native artifact fetch this offline sandbox cannot verify, and its
 * latency/RAM on the Helio G99 is unmeasured); llama.cpp via JNI
 * (maintained, MIT, but a native build + JNI bridge is a whole project,
 * not a clean integration); ExecuTorch (maintained, but its Android
 * story for chat LLMs is thin and equally unmeasured here). Wiring any
 * of them blind would violate the "integrates cleanly" gate.
 */

/** Where an installed app lives, from the host's real layout data. */
data class HomePlacement(
    val itemId: String? = null, // home flow item id, when placed on home
    val page: Int? = null, // grid page of the home item, when known
    val folderId: String? = null, // folder id, when inside a folder
    val folderName: String? = null,
    val inDock: Boolean = false,
)

sealed class BrainInput {
    data class TextQuery(val text: String) : BrainInput()
    object HelpMenu : BrainInput()
    object SuggestMenu : BrainInput()
    data class TutorialRequest(val tutorialId: String) : BrainInput()
}

sealed class MascotAction {
    data class GuideHomeApp(val appKey: String, val label: String, val itemId: String, val page: Int?) : MascotAction()
    data class GuideFolder(val appKey: String, val label: String, val folderId: String, val folderName: String) : MascotAction()
    data class GuideDockApp(val appKey: String, val label: String) : MascotAction()
    data class GuideDrawerApp(val appKey: String, val label: String) : MascotAction()
    data class GuideSetting(val route: String, val title: String, val controlKey: String? = null) : MascotAction()
    /** 3.1 Phase E: point at a placed widget's frame. */
    data class GuideWidget(val itemId: String, val label: String) : MascotAction()
    data class DoAction(val actionId: String) : MascotAction()
    data class LaunchApp(val appKey: String, val label: String) : MascotAction()
    data class ShowChoices(val candidates: List<FindCandidate>) : MascotAction()
    data class StartTutorial(val tutorialId: String) : MascotAction()
    object ShowTutorialOffer : MascotAction()
    object ShowHelpList : MascotAction()
    object None : MascotAction()
}

data class MascotResponse(
    val text: String,
    val expression: MascotExpression,
    val action: MascotAction = MascotAction.None,
    /** 3.1 Phase E: the Find decision behind this response, when the
     * response answers a Find query; null for chat/help answers. The
     * layer tracks flow 14 from it instead of guessing from the
     * action type. */
    val findDecision: FindDecision? = null,
)

data class BrainContext(
    val apps: List<LibraryApp>,
    val stats: Map<String, Pair<Int, Long>>,
    val placementOf: (String) -> HomePlacement,
    val findEnabled: Boolean = true,
    /** 3.1 Phase E: placed widgets, for Find widget targets. */
    val widgets: List<WidgetFindEntry> = emptyList(),
    // 3.1 Phase F (F4): ledger switch; off gates chat, never Find.
    // Last on purpose: existing positional constructions keep working.
    val chatEnabled: Boolean = true,
)

interface MascotBrain {
    suspend fun handle(input: BrainInput, ctx: BrainContext): MascotResponse
}

// ---------------------------------------------------------------------------
// Help Q&A map (prompt §7.2): every question maps to the real Settings
// screen that answers it. "What can you do?" lists capabilities honestly.
// ---------------------------------------------------------------------------

data class HelpEntry(
    val id: String,
    val question: String,
    val keywords: List<String>,
    val answer: String,
    val settingRoute: String? = null,
    /** 3.1 Phase E: deep-link field for the row this answer talks
     * about; null when the answer names a hub, not a row. */
    val controlKey: String? = null,
)

val HELP_ENTRIES: List<HelpEntry> = listOf(
    HelpEntry(
        "hide-app", "How do I hide an app?",
        listOf("hide", "hidden"),
        "Open Settings, then Drawer & Search, and add the app to the hidden list. Hidden apps stay installed but leave the drawer and home.",
        "drawer-search",
    ),
    HelpEntry(
        "accent", "How do I change the accent color?",
        listOf("accent", "color", "colour"),
        "Open Settings, then Look, and pick Default, Palette, or a custom color. The accent only recolors buttons, toggles, and highlights — Minimal stays Minimal.",
        "look", "accent",
    ),
    HelpEntry(
        "theme", "How do I change the theme or style?",
        listOf("theme", "style", "look"),
        "HavenOS is Minimal-only: there are no theme presets to switch. You can change the accent color and the wallpaper under Settings, then Look.",
        "look",
    ),
    HelpEntry(
        "dock", "How do I add an app to the dock?",
        listOf("dock", "favorites", "favourite"),
        "Long-press an app and choose Add to dock, or open the app drawer and drag it to the dock. The dock holds favorites on every page.",
        "home-dock", "dock",
    ),
    HelpEntry(
        "folder-resize", "How do I resize a folder?",
        listOf("folder", "resize"),
        "Open a folder, then use its size control if shown; otherwise long-press the folder on home and look for resize in its menu. Folder size follows the home grid.",
        "home-dock",
    ),
    HelpEntry(
        "widget", "How do I add a widget?",
        listOf("widget"),
        "Long-press empty home space and choose Widgets, or ask me to add a widget and I will open the widget picker for you.",
        "home-dock", "widgets",
    ),
    HelpEntry(
        "wallpaper", "How do I change the wallpaper?",
        listOf("wallpaper", "background"),
        "Open Settings, then Look, then Wallpaper. You can use the system wallpaper or a HavenOS wallpaper — the two are separate choices.",
        "look", "wallpaper",
    ),
    HelpEntry(
        "grid", "How do I change icon size or the grid?",
        listOf("grid", "icon size", "icons"),
        "Open Settings, then Home and Dock, and adjust the grid — the grid and icon size follow it. Per-icon size is not a separate control.",
        "home-dock", "home",
    ),
    HelpEntry(
        "mascot-off", "How do I turn the mascot off?",
        listOf("mascot off", "turn off", "disable mascot"),
        "Tap me, choose Dismiss, or open Settings, then Mascot, and switch Mascot off. Dismiss sticks until you bring me back from Settings.",
        "mascot", "mascot",
    ),
    HelpEntry(
        "mascot-back", "How do I bring the mascot back?",
        listOf("bring back", "mascot back", "restore mascot"),
        "Open Settings, then Mascot, and choose Bring back mascot. It appears only after I have been dismissed.",
        "mascot", "mascot",
    ),
    HelpEntry(
        "quiet-hours", "How do I change quiet hours?",
        listOf("quiet", "quiet hours", "do not disturb"),
        "Open Settings, then Mascot, and set the quiet-hours start and end. I sleep during quiet hours and never nudge then.",
        "mascot", "quiet",
    ),
    HelpEntry(
        "reset-tips", "How do I reset the mascot's tips?",
        listOf("reset", "tips"),
        "Open Settings, then Mascot, and choose Reset mascot. That clears dismissed tours and muted suggestions so I can offer them again.",
        "mascot", "reset",
    ),
    // 3.1 Phase E: new surfaces (island, Find, roaming, Today, offline).
    HelpEntry(
        "island", "How does the dynamic island work?",
        listOf("island", "dynamic island", "pill"),
        "The island is a small pill at the top of home that shows what is playing, a charging state, or a recent notification. It lives on home only and never floats over other apps. You can switch it in Settings, then Home and Dock, under Dynamic Island.",
        "home-dock", "island",
    ),
    HelpEntry(
        "mascot-find", "How do I find an app with the mascot?",
        listOf("find", "locate", "where is"),
        "Tap me, choose Find, and type the app's name. I walk over and point at it — or tell you plainly when I can't find it. Find never guesses and never opens folders unasked.",
        "mascot", "find",
    ),
    HelpEntry(
        "mascot-move", "Can I move the mascot?",
        listOf("move mascot", "drag mascot", "carry"),
        "Yes — long-press me for a moment, then drag me to any ledge: a dock top, a widget top, or an empty row. If you drop me somewhere I can't stand, I hop back to where I was.",
        "mascot",
    ),
    HelpEntry(
        "today-cards", "What are the Today cards?",
        listOf("today", "cards", "agenda"),
        "Today is the card column beside home: quote, scores, crypto, astronomy picture, and your GitHub activity. Each card has its own on/off switch in Settings, then APIs, and every card works from a saved copy when you are offline.",
        "apis",
    ),
    HelpEntry(
        "offline-cards", "Why do my cards show old data?",
        listOf("old data", "stale", "outdated", "not updating"),
        "Cards keep their last saved copy and refresh on a schedule. If a card shows an Expired line, its refresh has failed twice in a row — tap Retry on the card. If it keeps failing, check the connection, then the API switch in Settings, then APIs.",
        "apis",
    ),
    HelpEntry(
        "capabilities", "What can you do?",
        listOf("what can you do", "help", "abilities", "capabilities"),
        "I can find apps, widgets, folders, and settings and point to them; teach quick tours of edit mode, folders, and widgets; answer help questions; and occasionally suggest a useful pin or widget. I walk on ledges and never stand on an icon, and you can drag me to a new perch. Everything runs on-device, and I never leave the home screen.",
        null,
    ),
)

fun findHelpEntry(queryNorm: String): HelpEntry? {
    if (queryNorm.isBlank()) return null
    return HELP_ENTRIES.firstOrNull { entry ->
        entry.keywords.any { kw -> queryNorm.contains(kw) }
    }
}

// ---------------------------------------------------------------------------
// Tutorials (prompt §7.1): edit mode, folder, widget — each offered once.
// Steps are honest text; steps that act use a real host action, and the
// final step celebrates. "Don't show again" is persisted by the layer.
// ---------------------------------------------------------------------------

data class Tutorial(
    val id: String,
    val title: String,
    val steps: List<String>,
    val actionId: String? = null, // host action for the "Try it" step, if any
)

val TUTORIALS: List<Tutorial> = listOf(
    Tutorial(
        "edit-mode", "Edit mode tour",
        listOf(
            "Long-press empty home space, then choose Edit home.",
            "In edit mode you can drag apps to move them and use the toolbar to add pages or undo.",
            "Tap Done, or press Back, to leave edit mode. Your layout is kept.",
        ),
        actionId = "edit-mode",
    ),
    Tutorial(
        "folder", "Folder tour",
        listOf(
            "Drag one app onto another on home to make a folder.",
            "Tap a folder to open it; drag apps in or out to change it.",
            "Long-press a folder for its menu, including rename and remove.",
        ),
    ),
    Tutorial(
        "widget", "Widget tour",
        listOf(
            "Long-press empty home space and choose Widgets to open the picker.",
            "Pick a widget, then place it where there is room on home.",
            "Long-press a placed widget to move or remove it.",
        ),
        actionId = "add-widget",
    ),
)

// ---------------------------------------------------------------------------
// ScriptedBrain: deterministic, instant, offline, zero model cost.
// Intent classes (prompt §6.1): Find, Help, OpenSetting, StartTutorial,
// Greeting, Unknown. Find runs FIRST for bare queries (prompt §2:
// Find and teach first, suggest second, free chat last).
// ---------------------------------------------------------------------------

class ScriptedBrain : MascotBrain {
    override suspend fun handle(input: BrainInput, ctx: BrainContext): MascotResponse {
        return when (input) {
            // 3.1 Phase F (F4): chat off gates the conversational inputs;
            // Find (TextQuery) keeps working, and SuggestMenu belongs to
            // the separate Suggestions switch, so neither is gated here.
            is BrainInput.HelpMenu -> if (!ctx.chatEnabled) {
                MascotResponse(
                    "Mascot chat is switched off in Settings, then Privacy.",
                    MascotExpression.SAD,
                )
            } else {
                MascotResponse(
                    "Pick a question and I will open the right Settings screen for you.",
                    MascotExpression.CURIOUS,
                    MascotAction.ShowHelpList,
                )
            }
            is BrainInput.SuggestMenu -> MascotResponse(
                "I will check for one useful suggestion. If there is nothing genuinely useful, I will say so rather than invent one.",
                MascotExpression.THINKING,
                MascotAction.None,
            )
            is BrainInput.TutorialRequest -> if (!ctx.chatEnabled) {
                MascotResponse(
                    "Mascot chat is switched off in Settings, then Privacy.",
                    MascotExpression.SAD,
                )
            } else {
                val tutorial = TUTORIALS.firstOrNull { it.id == input.tutorialId }
                if (tutorial == null) {
                    MascotResponse("I don't have that tour.", MascotExpression.CONFUSED)
                } else {
                    MascotResponse(
                        "Starting: ${tutorial.title}.",
                        MascotExpression.HAPPY,
                        MascotAction.StartTutorial(tutorial.id),
                    )
                }
            }
            is BrainInput.TextQuery -> handleText(input.text, ctx)
        }
    }

    private fun handleText(raw: String, ctx: BrainContext): MascotResponse {
        val query = raw.trim()
        if (query.isBlank()) {
            return MascotResponse(
                "Type what you're looking for — an app, a setting, or a question.",
                MascotExpression.CONFUSED,
            )
        }
        val norm = normalizeFindText(query)
        val tokens = norm.split(' ').filter { it.isNotEmpty() }
        // Greeting: short greeting-only message. 3.1 Phase F: gated by
        // chatEnabled — when chat is off, queries fall through to Find.
        if (ctx.chatEnabled && tokens.size <= 3 && tokens.any { it in setOf("hi", "hello", "hey", "yo") }) {
            return MascotResponse(
                "Hi! Tap Find and tell me what you're looking for, or ask for help.",
                MascotExpression.HAPPY,
            )
        }
        // Tutorial intent. 3.1 Phase F: gated by chatEnabled, as above.
        if (ctx.chatEnabled && tokens.any { it in setOf("tutorial", "tour", "teach", "learn") }) {
            return MascotResponse(
                "I have three quick tours: edit mode, folders, and widgets. Each is offered once — pick one to start.",
                MascotExpression.CURIOUS,
                MascotAction.ShowTutorialOffer,
            )
        }
        // Help intent: "how do I…" or a help-map keyword hit with no app match.
        val helpHit = findHelpEntry(norm)
        val startsHow = tokens.firstOrNull() in setOf("how", "where", "can")
        if (ctx.chatEnabled && helpHit != null && (startsHow || tokens.size > 1)) {
            val action = if (helpHit.settingRoute != null) {
                MascotAction.GuideSetting(
                    helpHit.settingRoute, helpHit.question, helpHit.controlKey,
                )
            } else {
                MascotAction.None
            }
            return MascotResponse(helpHit.answer, MascotExpression.HAPPY, action)
        }
        // Find (default path for everything else).
        if (!ctx.findEnabled) {
            return MascotResponse(
                "Find is switched off in Settings, then Mascot.",
                MascotExpression.SAD,
            )
        }
        val candidates = rankFind(ctx.apps, query, ctx.stats, ctx.widgets)
        return when (val decision = decideFind(candidates)) {
            is FindDecision.Act -> responseForCandidate(decision.candidate, ctx)
                .copy(findDecision = decision)
            is FindDecision.Choices -> MascotResponse(
                "I found a few matches — pick one and I will point to it.",
                MascotExpression.CURIOUS,
                MascotAction.ShowChoices(decision.candidates),
                findDecision = decision,
            )
            is FindDecision.NoMatch -> MascotResponse(
                "I couldn't find that here. Try another word — I only search your apps, folders, and HavenOS settings, never the web.",
                MascotExpression.CONFUSED,
                findDecision = decision,
            )
        }
    }

    private fun responseForCandidate(candidate: FindCandidate, ctx: BrainContext): MascotResponse {
        return when (val target = candidate.target) {
            is FindTarget.App -> {
                val placement = ctx.placementOf(target.key)
                when {
                    placement.itemId != null -> MascotResponse(
                        "Found ${target.label} on your home — walking over to point at it.",
                        MascotExpression.FOUND,
                        MascotAction.GuideHomeApp(target.key, target.label, placement.itemId, placement.page),
                    )
                    placement.folderId != null -> MascotResponse(
                        "Found ${target.label} inside the folder “${placement.folderName ?: "Folder"}”. Open the folder to see it, or launch it from here.",
                        MascotExpression.FOUND,
                        MascotAction.GuideFolder(target.key, target.label, placement.folderId, placement.folderName ?: "Folder"),
                    )
                    placement.inDock -> MascotResponse(
                        "Found ${target.label} in your dock.",
                        MascotExpression.FOUND,
                        MascotAction.GuideDockApp(target.key, target.label),
                    )
                    else -> MascotResponse(
                        "Found ${target.label} in the app drawer — opening the drawer for you.",
                        MascotExpression.FOUND,
                        MascotAction.GuideDrawerApp(target.key, target.label),
                    )
                }
            }
            is FindTarget.Setting -> MascotResponse(
                "Found “${target.title}” in Settings — opening that screen.",
                MascotExpression.FOUND,
                MascotAction.GuideSetting(target.route, target.title, target.controlKey),
            )
            is FindTarget.Widget -> MascotResponse(
                "Found the ${target.label} widget on your home — walking over to point at it.",
                MascotExpression.FOUND,
                MascotAction.GuideWidget(target.itemId, target.label),
            )
            is FindTarget.Action -> MascotResponse(
                "“${target.title}” — confirm and I will do it.",
                MascotExpression.CURIOUS,
                MascotAction.DoAction(target.id),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// LocalLlmBrain — Phase 3 gate (prompt §6.2). NOT configured: no runtime
// is wired, no model is bundled, and the URL/checksum below are null
// placeholders by design. The layer reports this state honestly in
// Settings → Mascot → Chat model, and chat falls back to ScriptedBrain.
// ---------------------------------------------------------------------------

/**
 * Model setup placeholders ONLY. Never invent a URL, key, or checksum:
 * these stay null until the owner supplies a real model location, and
 * the Settings row lists them as setup gaps.
 */
data class MascotLlmConfig(
    val modelDownloadUrl: String? = null,
    val modelChecksumSha256: String? = null,
    val runtime: String? = null,
)

enum class MascotLlmStatus { NOT_CONFIGURED, DOWNLOADING, READY, FAILED }

class LocalLlmBrain(
    val config: MascotLlmConfig = MascotLlmConfig(),
    val status: MascotLlmStatus = MascotLlmStatus.NOT_CONFIGURED,
) : MascotBrain {
    override suspend fun handle(input: BrainInput, ctx: BrainContext): MascotResponse {
        // No runtime is wired, so every chat input gets the honest
        // fallback instead of a fabricated model reply. The caller
        // (MascotLayer) routes free-form chat to ScriptedBrain first
        // and only reaches here for the explicit model-status path.
        return MascotResponse(
            "The on-device chat model isn't set up yet, so I'm answering from my scripted brain instead. See Settings, then Mascot, then Chat model for what's missing.",
            MascotExpression.SAD,
        )
    }
}

/** Suggestion engine (prompt §7.3): on-device signals the launcher
 * already has — most-launched app not in the dock, or room for a widget.
 * Returns null when nothing is genuinely useful (never invent one). */
fun suggestPinCandidate(
    apps: List<LibraryApp>,
    stats: Map<String, Pair<Int, Long>>,
    dockKeys: List<String>,
    dockMax: Int,
): LibraryApp? {
    if (dockKeys.size >= dockMax) return null
    return stats.entries
        .filter { (key, _) -> key !in dockKeys }
        .sortedByDescending { it.value.first }
        .mapNotNull { (key, _) -> apps.firstOrNull { it.key == key } }
        .firstOrNull()
}

fun suggestWidgetCandidate(homeItemCount: Int): Boolean = homeItemCount < 8
