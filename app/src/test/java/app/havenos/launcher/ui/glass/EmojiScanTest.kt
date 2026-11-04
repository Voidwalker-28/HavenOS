package app.havenos.launcher.ui.glass

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * 3.1 Phase B4: no emoji or pictograph code points in main sources.
 * Scans Kotlin string literals (whole files — a literal cannot hide from
 * a file scan) and string resources. Fails the build when one is found.
 * Ranges are the prompt's: U+1F300–U+1FAFF, U+2600–U+27BF, U+2B50, U+FE0F.
 * Written, not run (sandbox test executor broken, pre-existing).
 */
class EmojiScanTest {

    @Test
    fun noEmojiInMainSources() {
        val main = findMainSrc()
        val offenders = mutableListOf<String>()
        val ktDir = File(main, "java")
        val resDir = File(main, "res")
        val files = mutableListOf<File>()
        if (ktDir.isDirectory) ktDir.walkTopDown().filter { it.extension == "kt" }.forEach { files += it }
        if (resDir.isDirectory) {
            resDir.walkTopDown().filter {
                it.extension == "xml" && (it.parentFile?.name == "values" || it.parentFile?.name == "drawable")
            }.forEach { files += it }
        }
        for (file in files) {
            val text = file.readText()
            val points = text.codePoints().toArray()
            for (cp in points) {
                if (isEmoji(cp)) {
                    offenders += "${file.relativeTo(main)}: U+%04X".format(cp)
                    break
                }
            }
        }
        assertTrue("Emoji/pictograph code points in main sources: ${offenders.take(20)}", offenders.isEmpty())
    }

    private fun isEmoji(cp: Int): Boolean =
        (cp in 0x1F300..0x1FAFF) || (cp in 0x2600..0x27BF) || cp == 0x2B50 || cp == 0xFE0F

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
