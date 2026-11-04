package app.havenos.launcher.ui.mascot

import app.havenos.launcher.ui.LibraryApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HavenOS v3 Track A — ScriptedBrain + suggestion engine (MascotBrain.kt).
 * NOTE: written but never run in this sandbox — the Gradle test
 * executor is broken here (known pre-existing baseline).
 */
class MascotBrainTest {

    private fun app(key: String, label: String) = LibraryApp(key, label, "com.example.$key")

    private val apps = listOf(
        app("camera", "Camera"),
        app("gallery", "Gallery"),
        app("messages", "Messages"),
        app("chrome", "Chrome"),
    )

    private fun ctx(): BrainContext = BrainContext(
        apps = apps,
        stats = emptyMap(),
        placementOf = { key ->
            when (key) {
                "camera" -> HomePlacement(itemId = "item-camera", page = 0)
                "gallery" -> HomePlacement(folderId = "folder-1", folderName = "Photos")
                "messages" -> HomePlacement(inDock = true)
                else -> HomePlacement()
            }
        },
    )

    private val brain = ScriptedBrain()

    @Test
    fun greeting_isHappyNotAi() {
        val response = runBlocking { brain.handle(BrainInput.TextQuery("hello"), ctx()) }
        assertEquals(MascotExpression.HAPPY, response.expression)
        assertTrue(response.action is MascotAction.None)
    }

    @Test
    fun find_homeApp_guidesToItem() {
        val response = runBlocking { brain.handle(BrainInput.TextQuery("Camera"), ctx()) }
        assertEquals(MascotExpression.FOUND, response.expression)
        val action = response.action as MascotAction.GuideHomeApp
        assertEquals("item-camera", action.itemId)
        assertEquals(0, action.page)
    }

    @Test
    fun find_folderApp_guidesToFolder() {
        val response = runBlocking { brain.handle(BrainInput.TextQuery("Gallery"), ctx()) }
        assertTrue(response.action is MascotAction.GuideFolder)
    }

    @Test
    fun find_dockApp_guidesToDock() {
        val response = runBlocking { brain.handle(BrainInput.TextQuery("Messages"), ctx()) }
        assertTrue(response.action is MascotAction.GuideDockApp)
    }

    @Test
    fun find_drawerOnlyApp_guidesToDrawer() {
        val response = runBlocking { brain.handle(BrainInput.TextQuery("Chrome"), ctx()) }
        assertTrue(response.action is MascotAction.GuideDrawerApp)
    }

    @Test
    fun find_noMatch_isConfusedWithNoWebOffer() {
        val response = runBlocking { brain.handle(BrainInput.TextQuery("zzz-no-such-thing"), ctx()) }
        assertEquals(MascotExpression.CONFUSED, response.expression)
        assertTrue(!response.text.contains("web", ignoreCase = true) || response.text.contains("never the web"))
    }

    @Test
    fun help_accent_opensLook() {
        val response = runBlocking {
            brain.handle(BrainInput.TextQuery("how do I change the accent color"), ctx())
        }
        val action = response.action as MascotAction.GuideSetting
        assertEquals("look", action.route)
    }

    @Test
    fun tutorial_request_startsTour() {
        val response = runBlocking { brain.handle(BrainInput.TutorialRequest("widget"), ctx()) }
        assertTrue(response.action is MascotAction.StartTutorial)
    }

    @Test
    fun llmBrain_notConfigured_answersHonestly() {
        val llm = LocalLlmBrain()
        val response = runBlocking { llm.handle(BrainInput.TextQuery("hello"), ctx()) }
        assertEquals(MascotExpression.SAD, response.expression)
        assertTrue(response.text.contains("isn't set up"))
        // Placeholders only: no invented URL, key, or checksum anywhere.
        assertEquals(null, llm.config.modelDownloadUrl)
        assertEquals(null, llm.config.modelChecksumSha256)
    }

    @Test
    fun suggestions_pinPicksMostLaunchedNotInDock() {
        val stats = mapOf(
            "camera" to (50 to 1000L),
            "messages" to (5 to 900L),
        )
        val pin = suggestPinCandidate(apps, stats, dockKeys = listOf("messages"), dockMax = 6)
        assertEquals("camera", pin?.key)
        assertEquals(null, suggestPinCandidate(apps, stats, dockKeys = listOf("camera", "messages"), dockMax = 2))
        assertTrue(suggestWidgetCandidate(3))
        assertTrue(!suggestWidgetCandidate(20))
    }
}
