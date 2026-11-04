package app.havenos.launcher.ui.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.havenos.launcher.data.SettingsState
import app.havenos.launcher.theme.Look
import app.havenos.launcher.theme.mixOpaque
import app.havenos.launcher.theme.resolveTokens
import app.havenos.launcher.theme.tokensFor
import app.havenos.launcher.ui.Legibility
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * 3.1 Phase B7: no hex colour literals in composables outside the token
 * modules. Token modules are theme/, ui/glass/, and *Tokens.kt files;
 * Legibility.kt holds one named colour constant, not a composable
 * literal, and is allowlisted on that basis. Data tables (wallpaper
 * gradients in data/Wallpaper.kt) are not composable literals and are
 * out of this test's scope. Fails the build when a new literal appears.
 * Written, not run (sandbox test executor broken, pre-existing).
 */
class HexLiteralTest {

    private val hexLiteral = Regex("""Color\s*\(\s*0x[0-9A-Fa-f]+""")

    @Test
    fun noHexLiteralsOutsideTokenModules() {
        val main = findMainSrc()
        val ktDir = File(main, "java")
        val offenders = mutableListOf<String>()
        ktDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val rel = file.relativeTo(main).path.replace(File.separatorChar, '/')
            if (isTokenFile(rel)) return@forEach
            file.readLines().forEachIndexed { i, line ->
                if (hexLiteral.containsMatchIn(line)) {
                    offenders += "$rel:${i + 1}: ${line.trim().take(100)}"
                }
            }
        }
        assertTrue("Hex colour literals outside token modules: ${offenders.take(20)}", offenders.isEmpty())
    }

    private fun isTokenFile(rel: String): Boolean =
        "/theme/" in rel || "/ui/glass/" in rel || rel.endsWith("Tokens.kt") ||
            rel == "java/app/havenos/launcher/ui/Legibility.kt"

    // 3.1.1 (item 6): drawer solidity, same scan/test approach as
    // the hex-literal rule above — no second mechanism. The drawer
    // surface and field are opaque in both modes, text on them keeps
    // >= 4.5:1 contrast, and the drawer source uses no translucent
    // glass call anywhere (a translucent call fails this test, which
    // fails the build). Written, not run (same pre-existing status
    // as the test above).
    @Test
    fun drawerSurfaceAndField_areOpaque() {
        val base = tokensFor(Look.Minimal)
        assertEquals(1f, base.light.drawerSurface.alpha, 0f)
        assertEquals(1f, base.dark.drawerSurface.alpha, 0f)
        assertEquals(1f, base.light.drawerField.alpha, 0f)
        assertEquals(1f, base.dark.drawerField.alpha, 0f)
        val resolved = resolveTokens(SettingsState())
        assertEquals(1f, resolved.light.drawerSurface.alpha, 0f)
        assertEquals(1f, resolved.dark.drawerSurface.alpha, 0f)
        assertEquals(1f, resolved.light.drawerField.alpha, 0f)
        assertEquals(1f, resolved.dark.drawerField.alpha, 0f)
        assertEquals(
            1f,
            mixOpaque(Color.Black, Color.White, 0.04f).alpha,
            0f,
        )
    }

    @Test
    fun drawerTextContrast_atLeast45() {
        for (tokens in listOf(tokensFor(Look.Minimal), resolveTokens(SettingsState()))) {
            val lightRatio = Legibility.contrastRatio(
                tokens.light.onSurface.luminance(),
                tokens.light.drawerSurface.luminance(),
            )
            assertTrue("light drawer contrast $lightRatio", lightRatio >= 4.5)
            val darkRatio = Legibility.contrastRatio(
                tokens.dark.onSurface.luminance(),
                tokens.dark.drawerSurface.luminance(),
            )
            assertTrue("dark drawer contrast $darkRatio", darkRatio >= 4.5)
        }
    }

    @Test
    fun drawerSource_usesNoTranslucentGlass() {
        val main = findMainSrc()
        val drawer = File(main, "java/app/havenos/launcher/ui/Drawer.kt")
        assertTrue("Drawer.kt not found", drawer.isFile)
        val text = drawer.readText()
        // The solid surface is wired up (28dp top corners, 40% scrim).
        assertTrue(text.contains("drawerSurface"))
        assertTrue(text.contains("28.dp"))
        assertTrue(text.contains("copy(alpha = 0.4f)"))
        // No translucent glass call may remain in the drawer source:
        // .glass(...) modifiers, palette.glass washes, and the
        // Glass2/Glass3 tint helpers are all gone from this file.
        assertTrue(".glass( call remains", !text.contains(".glass("))
        assertTrue("palette.glass remains", !text.contains("palette.glass"))
        assertTrue("glassTintBase remains", !text.contains("glassTintBase("))
        assertTrue("glassScrimColor remains", !text.contains("glassScrimColor("))
        assertTrue("glassTintAlpha remains", !text.contains("glassTintAlpha("))
    }

    private fun findMainSrc(): File {
        val candidates = listOf(
            File("src/main"),
            File("app/src/main"),
            File(System.getProperty("user.dir") ?: ".", "src/main"),
        )
        return candidates.firstOrNull { File(it, "java/app/havenos/launcher").isDirectory }
            ?: fail("Phase B lint test: main sources not found; searched: $candidates")
    }
}
