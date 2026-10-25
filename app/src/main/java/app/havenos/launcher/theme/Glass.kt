package app.havenos.launcher.theme

import android.app.WallpaperManager
import android.view.WindowManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.havenos.launcher.data.SettingsState
import kotlin.math.roundToInt

/** Glass quality ladder (builder prompt §8). */
enum class GlassTier { Full, Partial, Flat }

data class GlassState(
    val tier: GlassTier = GlassTier.Flat,
    val reason: String = "",
    /** Pre-blurred wallpaper copy for Full-tier region sampling (either mode). */
    val blurred: Bitmap? = null,
    /** Bitmap pixels per window pixel (the blurred copy is downscaled). */
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val reduceEffects: Boolean = false,
)

val LocalGlass = staticCompositionLocalOf { GlassState() }

/**
 * Runtime glass-quality detection (§8 ladder). [systemReadable] is the result
 * of actually attempting to read the system wallpaper bitmap (it throws
 * SecurityException on the target tablet — measured), never assumed.
 */
fun detectGlassTier(ctx: Context, state: SettingsState, systemReadable: Boolean): Pair<GlassTier, String> {
    if (state.reduceEffects) return GlassTier.Flat to "Reduce Effects is on"
    if (state.wallpaperMode == 1) {
        return GlassTier.Full to "HavenOS wallpaper — HavenOS keeps its own blurred copy"
    }
    val wm = try {
        ctx.getSystemService(WallpaperManager::class.java)
    } catch (t: Throwable) {
        null
    }
    val live = try {
        wm?.wallpaperInfo != null
    } catch (t: Throwable) {
        false
    }
    if (live) {
        return if (crossWindowBlur(ctx)) {
            GlassTier.Partial to "Live wallpaper (never sampled) + cross-window blur"
        } else {
            GlassTier.Flat to "Live wallpaper (never sampled); no cross-window blur"
        }
    }
    if (systemReadable) return GlassTier.Full to "System wallpaper bitmap is readable"
    return if (crossWindowBlur(ctx)) {
        GlassTier.Partial to "System wallpaper bitmap not readable; cross-window blur (window blur-behind)"
    } else {
        GlassTier.Flat to "System wallpaper bitmap not readable; cross-window blur unavailable"
    }
}

fun crossWindowBlur(ctx: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    return try {
        ctx.getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true
    } catch (t: Throwable) {
        false
    }
}

/**
 * The one glass surface every card, dock, folder and widget draws with:
 * backdrop sample (Full tier) or translucent tint, plus in ALL tiers the
 * tint + 1px specular border + soft inner highlight + drop shadow.
 * Reduce Effects (or a Flat tier) falls back to flat translucent surfaces.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LocalTokens.current.corner),
    tintAlpha: Float = LocalTokens.current.glassAlpha,
    tintCap: Float = 1f,
    flatAlpha: Float = 0.92f,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalTokens.current
    val palette = LocalPalette.current
    val glass = LocalGlass.current
    var pos by remember { mutableStateOf(IntOffset.Zero) }
    val specular = remember(palette, tokens) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = tokens.borderAlpha),
                Color.White.copy(alpha = tokens.borderAlpha * 0.35f),
                Color.White.copy(alpha = tokens.borderAlpha * 0.6f),
            ),
        )
    }
    val effTint = minOf(tintAlpha, tintCap)
    val sampleBackdrop = glass.tier == GlassTier.Full && tokens.blur && !glass.reduceEffects && glass.blurred != null
    Box(
        modifier
            .shadow(tokens.shadow, shape, clip = false)
            .clip(shape)
            .onGloballyPositioned { pos = it.positionInWindow().let { o -> IntOffset(o.x.roundToInt(), o.y.roundToInt()) } }
            .drawBehind {
                if (sampleBackdrop) {
                    val bmp = glass.blurred!!
                    val sx = (pos.x * glass.scaleX).toInt().coerceIn(0, bmp.width - 1)
                    val sy = (pos.y * glass.scaleY).toInt().coerceIn(0, bmp.height - 1)
                    val sw = (size.width * glass.scaleX).roundToInt().coerceIn(1, bmp.width - sx)
                    val sh = (size.height * glass.scaleY).roundToInt().coerceIn(1, bmp.height - sy)
                    drawImage(
                        bmp.asImageBitmap(),
                        srcOffset = IntOffset(sx, sy),
                        srcSize = IntSize(sw, sh),
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    )
                    drawRect(palette.glass.copy(alpha = effTint * 0.55f))
                } else if (glass.reduceEffects || glass.tier == GlassTier.Flat) {
                    drawRect(palette.surface.copy(alpha = flatAlpha))
                } else {
                    // Partial tier: window blur-behind supplies the blur; we tint.
                    drawRect(palette.glass.copy(alpha = effTint))
                }
            }
            .border(1.dp, specular, shape),
    ) {
        content()
        // Soft inner highlight along the top edge.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.72f)
                .height(1.dp)
                .drawBehind {
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = tokens.highlightAlpha),
                                Color.Transparent,
                            ),
                        ),
                    )
                },
        )
    }
}
