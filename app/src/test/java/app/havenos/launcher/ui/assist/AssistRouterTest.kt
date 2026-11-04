package app.havenos.launcher.ui.assist

import app.havenos.launcher.ui.LibraryApp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun ctx(
    darkMode: Int = 0,
    iconDp: Int = 56,
    commandsEnabled: Boolean = true,
): AssistContext = AssistContext(
    apps = listOf(
        LibraryApp(key = "camera", label = "Camera", pkg = "com.example.camera"),
        LibraryApp(key = "camera2", label = "Camera Pro", pkg = "com.example.camera2"),
        LibraryApp(key = "clock", label = "Clock", pkg = "com.example.clock"),
    ),
    stats = mapOf("camera" to (10 to 1000L)),
    darkMode = darkMode,
    effectiveIconDp = iconDp,
    curatedNames = listOf("Ember", "Iris", "Lagoon"),
    commandsEnabled = commandsEnabled,
)

class AssistRouterTest {

    @Test fun darkModeCommand() {
        val cmds = parseAssistCommands("dark mode", ctx(darkMode = 0))
        assertEquals(1, cmds?.size)
        val cmd = cmds!![0]
        assertTrue(cmd.action is AssistAction.SetDarkMode)
        assertEquals(2, (cmd.action as AssistAction.SetDarkMode).mode)
        assertEquals("look", cmd.hubRoute)
        assertTrue(cmd.label.contains("Dark"))
    }

    @Test fun lightAndSystemModes() {
        val light = parseAssistCommands("light mode", ctx())!![0].action as AssistAction.SetDarkMode
        assertEquals(1, light.mode)
        val system = parseAssistCommands("system theme", ctx())!![0].action as AssistAction.SetDarkMode
        assertEquals(0, system.mode)
    }

    @Test fun biggerAndSmallerIconsClamp() {
        val bigger = parseAssistCommands("bigger icons", ctx(iconDp = 56))!![0]
        assertEquals(AssistAction.StepIconSize(8), bigger.action)
        assertTrue(bigger.label.contains("56"))
        assertTrue(bigger.label.contains("64"))
        val atMax = parseAssistCommands("bigger icons", ctx(iconDp = 68))!![0]
        assertTrue(atMax.label.contains("maximum"))
        val smaller = parseAssistCommands("smaller icons", ctx(iconDp = 56))!![0]
        assertEquals(AssistAction.StepIconSize(-8), smaller.action)
    }

    @Test fun accentByNameAndUnknownNameFallsBack() {
        val cmd = parseAssistCommands("accent iris", ctx())!![0]
        assertEquals(AssistAction.SetAccentCurated("Iris"), cmd.action)
        assertEquals("look", cmd.hubRoute)
        assertNull(parseAssistCommands("accent chartreuse", ctx()))
    }

    @Test fun compoundAllOrNothing() {
        val cmds = parseAssistCommands("dark mode and bigger icons", ctx())
        assertEquals(2, cmds?.size)
        assertTrue(cmds!![0].action is AssistAction.SetDarkMode)
        assertTrue(cmds[1].action is AssistAction.StepIconSize)
        // One bad clause voids the whole compound: falls back to search.
        assertNull(parseAssistCommands("dark mode and frobnicate", ctx()))
    }

    @Test fun openAppTopRankedWinsTiedFallsBack() {
        // "camera" matches Camera and Camera Pro in the same band, but
        // Camera's 10 local launches break the tie: the row shows the
        // exact label, so the tap itself is the confirmation.
        val camera = parseAssistCommands("open camera", ctx())!![0]
        assertEquals(AssistAction.OpenApp("camera", "Camera", "com.example.camera"), camera.action)
        val clock = parseAssistCommands("open clock", ctx())!![0]
        assertEquals(AssistAction.OpenApp("clock", "Clock", "com.example.clock"), clock.action)
        // Both cameras at zero launches: indistinguishable -> no guess.
        val tied = ctx().copy(stats = mapOf("camera" to (0 to 0L), "camera2" to (0 to 0L)))
        assertNull(parseAssistCommands("open camera", tied))
    }

    @Test fun goToHubs() {
        val privacy = parseAssistCommands("go to Privacy", ctx())!![0]
        assertEquals(AssistAction.OpenHub("privacy", "Privacy"), privacy.action)
        val look = parseAssistCommands("go to Look", ctx())!![0]
        assertEquals(AssistAction.OpenHub("look", "Look"), look.action)
        assertNull(parseAssistCommands("go to Narnia", ctx()))
    }

    @Test fun silentHasNoMapping() {
        // No silent / do-not-disturb setting exists in SettingsState,
        // so the word must fall back to normal search, never invent one.
        assertNull(parseAssistCommands("silent", ctx()))
    }

    @Test fun unknownAndDisabledFallBack() {
        assertNull(parseAssistCommands("purple monkey dishwasher", ctx()))
        assertNull(parseAssistCommands("dark mode", ctx(commandsEnabled = false)))
        assertNull(parseAssistCommands("", ctx()))
        assertNull(parseAssistCommands("   ", ctx()))
    }

    @Test fun calmCommand() {
        val cmd = parseAssistCommands("toggle calm", ctx())!![0]
        assertEquals(AssistAction.ToggleCalm, cmd.action)
        assertNull(cmd.hubRoute)
    }

    @Test fun whitelistIsExhaustive() {
        // Every action the parser can emit is one of these six types;
        // there is no uninstall / hide / permission variant to emit.
        val cmds = listOfNotNull(
            parseAssistCommands("dark mode", ctx()),
            parseAssistCommands("bigger icons", ctx()),
            parseAssistCommands("accent ember", ctx()),
            parseAssistCommands("open clock", ctx()),
            parseAssistCommands("go to Privacy", ctx()),
            parseAssistCommands("toggle calm", ctx()),
        ).flatten()
        assertEquals(6, cmds.size)
        for (cmd in cmds) {
            when (cmd.action) {
                is AssistAction.OpenApp,
                is AssistAction.OpenHub,
                is AssistAction.SetDarkMode,
                is AssistAction.StepIconSize,
                is AssistAction.SetAccentCurated,
                AssistAction.ToggleCalm,
                -> Unit
            }
        }
    }

    @Test fun effectiveIconDpFormula() {
        assertEquals(56, effectiveIconDp(-1, 56))
        assertEquals(42, effectiveIconDp(-1, 0))
        assertEquals(67, effectiveIconDp(-1, 100))
        assertEquals(48, effectiveIconDp(48, 90))
    }
}
