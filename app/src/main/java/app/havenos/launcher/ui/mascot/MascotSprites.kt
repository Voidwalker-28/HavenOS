package app.havenos.launcher.ui.mascot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import app.havenos.launcher.R
import kotlinx.coroutines.delay
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * HavenOS 3.1 Phase E (E1) — pixel sprite loading + rendering.
 *
 * Sheets live in drawable-nodpi (no density scaling: the loader picks
 * the integer scale itself). The JSON atlas in raw/ is the frame
 * layout's source of truth; [DEFAULT_ATLAS] is the fallback when the
 * atlas cannot be parsed, so a corrupt atlas degrades to the same
 * layout instead of crashing.
 *
 * Accent swap: accent-detail pixels are painted sentinel magenta
 * (255, 0, 255) in the PNGs and replaced with the current accent
 * once, at load — never per frame.
 */

/** Debug flag: false restores the old vector figure (MascotFigure).
 * The vector figure is kept until the pixel art is accepted. */
const val USE_PIXEL_MASCOT = true

/** Sentinel magenta in the PNGs = accent-detail pixels. */
private const val ACCENT_SENTINEL_ARGB: Int = 0xFFFF00FF.toInt()

data class SpriteAnimation(val frames: List<Int>, val fps: Int)

data class SpriteAtlas(val animations: Map<String, SpriteAnimation>)

/** Hardcoded copy of the atlas layout (gen_sprites.py ANIMS). */
val DEFAULT_ATLAS = SpriteAtlas(
    mapOf(
        "idle" to SpriteAnimation(listOf(0, 1, 2, 3), 4),
        "walk" to SpriteAnimation(listOf(4, 5, 6, 7, 8, 9), 10),
        "jump" to SpriteAnimation(listOf(10, 11, 12, 13, 14), 8),
        "sit" to SpriteAnimation(listOf(15, 16), 2),
        "sleep" to SpriteAnimation(listOf(17, 18, 19), 1),
        "wave" to SpriteAnimation(listOf(20, 21, 22, 23), 6),
        "happy" to SpriteAnimation(listOf(24, 25, 26, 27), 4),
        "surprised" to SpriteAnimation(listOf(28, 29), 2),
        "sad" to SpriteAnimation(listOf(30, 31), 2),
        "think" to SpriteAnimation(listOf(32, 33, 34), 2),
        "point" to SpriteAnimation(listOf(35, 36), 2),
        "poof" to SpriteAnimation(listOf(37, 38, 39, 40), 6),
    ),
)

/** Parse the raw atlas JSON; null on any error (caller falls back). */
fun parseSpriteAtlas(json: String): SpriteAtlas? {
    return try {
        val root = JSONObject(json)
        val anims = root.getJSONObject("animations")
        val map = mutableMapOf<String, SpriteAnimation>()
        val keys = anims.keys()
        while (keys.hasNext()) {
            val name = keys.next()
            val obj = anims.getJSONObject(name)
            val arr = obj.getJSONArray("frames")
            val frames = (0 until arr.length()).map { arr.getInt(it) }
            if (frames.isNotEmpty()) {
                map[name] = SpriteAnimation(frames, obj.getInt("fps").coerceIn(1, 30))
            }
        }
        if (map.isEmpty()) null else SpriteAtlas(map)
    } catch (t: Throwable) {
        null
    }
}

/** One loaded character: per-frame images, pre-converted so drawing
 * allocates nothing per frame. Null when the sheet cannot load. */
data class SpriteSet(
    val character: MascotCharacter,
    val frames: List<androidx.compose.ui.graphics.ImageBitmap>,
    val atlas: SpriteAtlas,
)

/** Split the sheet into per-frame bitmaps + swap the accent sentinel. */
fun loadSpriteSet(context: Context, character: MascotCharacter, accentArgb: Int): SpriteSet? {
    return try {
        val resId = if (character == MascotCharacter.MILA) R.drawable.mascot_mila
        else R.drawable.mascot_milo
        val opts = BitmapFactory.Options().apply { inScaled = false }
        val sheet = BitmapFactory.decodeResource(context.resources, resId, opts)
            ?: return null
        val atlas = try {
            val json = context.resources.openRawResource(R.raw.mascot_frames)
                .bufferedReader().use { it.readText() }
            parseSpriteAtlas(json) ?: DEFAULT_ATLAS
        } catch (t: Throwable) {
            DEFAULT_ATLAS
        }
        val pixels = IntArray(sheet.width * sheet.height)
        sheet.getPixels(pixels, 0, sheet.width, 0, 0, sheet.width, sheet.height)
        for (i in pixels.indices) {
            if (pixels[i] == ACCENT_SENTINEL_ARGB) pixels[i] = accentArgb
        }
        val swapped = Bitmap.createBitmap(pixels, sheet.width, sheet.height, Bitmap.Config.ARGB_8888)
        val frames = (0 until 41).map { idx ->
            Bitmap.createBitmap(swapped, idx * 24, 0, 24, 24).asImageBitmap()
        }
        SpriteSet(character, frames, atlas)
    } catch (t: Throwable) {
        null
    }
}

/** Size setting index -> target dp (E1: Small 56 / Medium 72 / Large 96). */
fun targetDpForMascotSize(sizeIndex: Int): Float = when (sizeIndex) {
    0 -> 56f
    2 -> 96f
    else -> 72f
}

/** Integer scale for a target size; never fractional, minimum 2. */
fun spriteScaleFor(targetDp: Float, density: Float): Int {
    val targetPx = targetDp * density
    return (targetPx / 24f).roundToInt().coerceAtLeast(2)
}

/** Time-based frame stepping: speed independent of frame rate. Pure. */
fun frameIndexFor(anim: SpriteAnimation, elapsedMs: Long): Int {
    if (anim.frames.isEmpty()) return 0
    val step = (elapsedMs * anim.fps / 1000L).toInt()
    return anim.frames[step % anim.frames.size]
}

/** Expression -> pixel animation, so existing layer code that sets
 * expressions keeps working with the pixel figure. */
fun animationForExpression(expression: MascotExpression): String = when (expression) {
    MascotExpression.IDLE, MascotExpression.BLINK -> "idle"
    MascotExpression.HAPPY, MascotExpression.FOUND -> "happy"
    MascotExpression.CURIOUS, MascotExpression.THINKING, MascotExpression.SEARCHING -> "think"
    MascotExpression.CONFUSED, MascotExpression.SURPRISED -> "surprised"
    MascotExpression.CELEBRATE -> "wave"
    MascotExpression.SLEEPY -> "sleep"
    MascotExpression.SAD -> "sad"
}

/**
 * Animated pixel sprite. The frame loop is a timer (delay per atlas
 * fps), never a per-frame callback; drawing uses FilterQuality.None
 * at integer scale, so pixels stay crisp. [facingRight] mirrors via
 * scaleX = -1 (an exact flip, no resampling).
 */
@Composable
fun PixelSprite(
    spriteSet: SpriteSet,
    animation: String,
    scale: Int,
    facingRight: Boolean = true,
    modifier: Modifier = Modifier,
) {
    // 3.1.1: no !! — unknown name falls back to idle, then to any
    // animation, then to rendering nothing (never throws).
    val anim = spriteSet.atlas.animations[animation]
        ?: spriteSet.atlas.animations["idle"]
        ?: spriteSet.atlas.animations.values.firstOrNull()
        ?: return
    var step by remember(animation) { mutableIntStateOf(0) }
    LaunchedEffect(animation) {
        val delayMs = 1000L / anim.fps
        while (true) {
            delay(delayMs)
            step = (step + 1) % anim.frames.size
        }
    }
    val frame = spriteSet.frames[anim.frames[step % anim.frames.size]]
    val sizeDp: Dp = with(LocalDensity.current) { (24 * scale).toDp() }
    Image(
        bitmap = frame,
        contentDescription = null,
        filterQuality = FilterQuality.None,
        modifier = modifier
            .size(sizeDp)
            .graphicsLayer { scaleX = if (facingRight) 1f else -1f },
    )
}

/** One static frame (empty states, hub preview thumbnails). */
@Composable
fun StaticSpriteFrame(
    spriteSet: SpriteSet,
    frameIndex: Int,
    scale: Int,
    modifier: Modifier = Modifier,
) {
    val frame = spriteSet.frames[frameIndex.coerceIn(0, spriteSet.frames.size - 1)]
    val sizeDp: Dp = with(LocalDensity.current) { (24 * scale).toDp() }
    Image(
        bitmap = frame,
        contentDescription = null,
        filterQuality = FilterQuality.None,
        modifier = modifier.size(sizeDp),
    )
}

/** Remember a loaded sprite set for this character + accent; null when
 * the sheet cannot load (caller falls back to the vector figure). */
@Composable
fun rememberSpriteSet(character: MascotCharacter, accentArgb: Int): SpriteSet? {
    val context = LocalContext.current
    return remember(character, accentArgb) {
        loadSpriteSet(context, character, accentArgb)
    }
}
