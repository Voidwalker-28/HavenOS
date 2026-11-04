package app.havenos.launcher.ui.mascot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class MascotSpritesTest {

    @Test fun sizeTargets() {
        assertEquals(56f, targetDpForMascotSize(0))
        assertEquals(72f, targetDpForMascotSize(1))
        assertEquals(96f, targetDpForMascotSize(2))
        assertEquals(72f, targetDpForMascotSize(99))
    }

    @Test fun integerScaleNeverFractionalAndMinTwo() {
        // 56dp at density 2 -> 112px -> scale 5 (120px, close to target).
        assertEquals(5, spriteScaleFor(56f, 2f))
        // 72dp at density 1 -> 72px -> scale 3.
        assertEquals(3, spriteScaleFor(72f, 1f))
        // Tiny density clamps to the minimum of 2, never 1.
        assertEquals(2, spriteScaleFor(56f, 0.5f))
        // 96dp at density 2.5 -> 240px -> scale 10 exactly.
        assertEquals(10, spriteScaleFor(96f, 2.5f))
    }

    @Test fun frameSteppingIsTimeBased() {
        val anim = SpriteAnimation(listOf(4, 5, 6, 7, 8, 9), 10)
        assertEquals(4, frameIndexFor(anim, 0L))
        assertEquals(5, frameIndexFor(anim, 100L))
        assertEquals(4, frameIndexFor(anim, 600L)) // wraps after 6 frames
        assertEquals(7, frameIndexFor(anim, 350L))
        assertEquals(0, frameIndexFor(SpriteAnimation(emptyList(), 10), 500L))
    }

    @Test fun atlasParsing() {
        val json = """{"version":1,"cell":24,"animations":{
            "idle":{"frames":[0,1,2,3],"fps":4},
            "walk":{"frames":[4,5],"fps":10},
            "empty":{"frames":[],"fps":4}
        }}"""
        val atlas = parseSpriteAtlas(json)
        assertNotNull(atlas)
        assertEquals(listOf(0, 1, 2, 3), atlas.animations["idle"]!!.frames)
        assertEquals(10, atlas.animations["walk"]!!.fps)
        // Empty frame lists are skipped, never crash the stepper.
        assertNull(atlas.animations["empty"])
        assertNull(parseSpriteAtlas("not json"))
        assertNull(parseSpriteAtlas("""{"animations":{}}"""))
    }

    @Test fun defaultAtlasMatchesSheetLayout() {
        // gen_sprites.py layout: idle 0-3, walk 4-9, poof 37-40.
        assertEquals(listOf(0, 1, 2, 3), DEFAULT_ATLAS.animations["idle"]!!.frames)
        assertEquals(listOf(4, 5, 6, 7, 8, 9), DEFAULT_ATLAS.animations["walk"]!!.frames)
        assertEquals(listOf(37, 38, 39, 40), DEFAULT_ATLAS.animations["poof"]!!.frames)
        assertEquals(41, DEFAULT_ATLAS.animations.values.flatMap { it.frames }.distinct().size)
    }

    @Test fun expressionMapping() {
        assertEquals("idle", animationForExpression(MascotExpression.IDLE))
        assertEquals("happy", animationForExpression(MascotExpression.HAPPY))
        assertEquals("happy", animationForExpression(MascotExpression.FOUND))
        assertEquals("think", animationForExpression(MascotExpression.THINKING))
        assertEquals("think", animationForExpression(MascotExpression.SEARCHING))
        assertEquals("surprised", animationForExpression(MascotExpression.CONFUSED))
        assertEquals("wave", animationForExpression(MascotExpression.CELEBRATE))
        assertEquals("sleep", animationForExpression(MascotExpression.SLEEPY))
        assertEquals("sad", animationForExpression(MascotExpression.SAD))
    }
}
