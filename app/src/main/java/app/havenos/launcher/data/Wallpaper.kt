package app.havenos.launcher.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.FileInputStream
import kotlin.math.max
import kotlin.math.min

/** Original bundled gradient presets (top color, bottom color). */
val WP_GRADIENTS: List<Pair<Int, Int>> = listOf(
    0xFF1D3557.toInt() to 0xFF0B0F14.toInt(), // deep ocean
    0xFF7A4A6E.toInt() to 0xFF1E1626.toInt(), // dusk plum
    0xFF1F4D3A.toInt() to 0xFF0C1710.toInt(), // forest
    0xFF4A4A52.toInt() to 0xFF101013.toInt(), // graphite
)

const val WP_SOLID_DEFAULT: Int = 0xFF1B2531.toInt()

/**
 * Builds and caches the HavenOS-mode wallpaper bitmap plus the downscaled,
 * pre-blurred copy the glass pipeline samples (§8), and probes whether the
 * system wallpaper bitmap is readable (it throws SecurityException on the
 * target tablet — probed, never assumed). All work happens off the main thread.
 */
class WallpaperStore(ctx: Context) {
    private val appCtx = ctx.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** HavenOS-mode wallpaper at render size (<= 1440 px wide). */
    val source = MutableStateFlow<Bitmap?>(null)

    /** Blurred backdrop copy (either mode) for the glass pipeline. */
    val blurred: StateFlow<Bitmap?> = MutableStateFlow<Bitmap?>(null)
    private val _blurred = blurred as MutableStateFlow<Bitmap?>

    /** True once a probe actually read the system wallpaper bitmap. */
    val systemReadable = MutableStateFlow(false)

    private var lastKey: String = ""

    private var lastProbeMs: Long = 0L

    fun refresh(state: SettingsState, widthPx: Int, heightPx: Int) {
        val key = listOf(
            state.wallpaperMode, state.wpKind, state.wpUri, state.wpTint, state.wpSolid,
            widthPx, heightPx,
        ).joinToString("|")
        scope.launch {
            if (state.wallpaperMode == 1) {
                if (key != lastKey) {
                    lastKey = key
                    val bmp = buildSource(state, widthPx, heightPx)
                    source.value = bmp
                    _blurred.value = bmp?.let { blurCopy(it) }
                }
            } else if (System.currentTimeMillis() - lastProbeMs > 10_000L) {
                // System mode: re-probing decodes the full wallpaper bitmap;
                // at most once per 10s (the system wallpaper rarely changes).
                lastProbeMs = System.currentTimeMillis()
                probeSystem()
            }
        }
    }

    private fun probeSystem() {
        try {
            val wm = appCtx.getSystemService(WallpaperManager::class.java) ?: return
            var pfd: android.os.ParcelFileDescriptor? = null
            try {
                pfd = wm.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)
            } catch (t: Throwable) {
                systemReadable.value = false
                return
            }
            if (pfd == null) {
                systemReadable.value = false
                return
            }
            FileInputStream(pfd.fileDescriptor).use { input ->
                val bmp = BitmapFactory.decodeStream(input)
                if (bmp != null) {
                    systemReadable.value = true
                    _blurred.value = blurCopy(bmp)
                } else {
                    systemReadable.value = false
                }
            }
            pfd.close()
        } catch (t: Throwable) {
            systemReadable.value = false
        }
    }

    private fun buildSource(state: SettingsState, w: Int, h: Int): Bitmap? {
        val width = if (w > 0) min(w, 1440) else 1080
        val height = if (h > 0) (h.toLong() * width / max(1, w)).toInt().coerceIn(200, 1920) else 1350
        return try {
            when (state.wpKind) {
                5 -> decodePhoto(state.wpUri, width)
                4 -> {
                    val color = if (state.wpSolid != 0) state.wpSolid else WP_SOLID_DEFAULT
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                        Canvas(it).drawColor(color)
                    }
                }
                else -> {
                    val (top, bottom) = WP_GRADIENTS[state.wpKind.coerceIn(0, WP_GRADIENTS.lastIndex)]
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    val paint = Paint().apply {
                        shader = LinearGradient(
                            0f, 0f, width * 0.3f, height.toFloat(),
                            top, bottom, Shader.TileMode.CLAMP,
                        )
                    }
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                    bmp
                }
            }
        } catch (t: Throwable) {
            null
        }
    }

    private fun decodePhoto(uriStr: String, targetW: Int): Bitmap? {
        if (uriStr.isBlank()) return null
        val uri = Uri.parse(uriStr)
        val resolver = appCtx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > targetW * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    companion object {
        /** Downscale to ~180px wide + two-pass box blur: the glass backdrop copy. */
        fun blurCopy(src: Bitmap): Bitmap {
            val targetW = 180
            val targetH = max(1, (src.height.toLong() * targetW / max(1, src.width)).toInt())
            val small = Bitmap.createScaledBitmap(src, targetW, targetH, true)
            boxBlur(small, 12)
            boxBlur(small, 12)
            return small
        }

        /** In-place separable box blur on a small bitmap (glass backdrops only). */
        fun boxBlur(bmp: Bitmap, radius: Int) {
            val w = bmp.width
            val h = bmp.height
            val px = IntArray(w * h)
            bmp.getPixels(px, 0, w, 0, 0, w, h)
            val tmp = IntArray(w * h)
            // horizontal
            for (y in 0 until h) {
                var r = 0; var g = 0; var b = 0; var a = 0
                val row = y * w
                for (x in -radius..radius) {
                    val c = px[row + x.coerceIn(0, w - 1)]
                    r += android.graphics.Color.red(c); g += android.graphics.Color.green(c)
                    b += android.graphics.Color.blue(c); a += android.graphics.Color.alpha(c)
                }
                val n = 2 * radius + 1
                for (x in 0 until w) {
                    tmp[row + x] = android.graphics.Color.argb(a / n, r / n, g / n, b / n)
                    val outC = px[row + (x - radius).coerceIn(0, w - 1)]
                    val inC = px[row + (x + radius + 1).coerceIn(0, w - 1)]
                    r += android.graphics.Color.red(inC) - android.graphics.Color.red(outC)
                    g += android.graphics.Color.green(inC) - android.graphics.Color.green(outC)
                    b += android.graphics.Color.blue(inC) - android.graphics.Color.blue(outC)
                    a += android.graphics.Color.alpha(inC) - android.graphics.Color.alpha(outC)
                }
            }
            // vertical
            for (x in 0 until w) {
                var r = 0; var g = 0; var b = 0; var a = 0
                for (y in -radius..radius) {
                    val c = tmp[y.coerceIn(0, h - 1) * w + x]
                    r += android.graphics.Color.red(c); g += android.graphics.Color.green(c)
                    b += android.graphics.Color.blue(c); a += android.graphics.Color.alpha(c)
                }
                val n = 2 * radius + 1
                for (y in 0 until h) {
                    px[y * w + x] = android.graphics.Color.argb(a / n, r / n, g / n, b / n)
                    val outC = tmp[(y - radius).coerceIn(0, h - 1) * w + x]
                    val inC = tmp[(y + radius + 1).coerceIn(0, h - 1) * w + x]
                    r += android.graphics.Color.red(inC) - android.graphics.Color.red(outC)
                    g += android.graphics.Color.green(inC) - android.graphics.Color.green(outC)
                    b += android.graphics.Color.blue(inC) - android.graphics.Color.blue(outC)
                    a += android.graphics.Color.alpha(inC) - android.graphics.Color.alpha(outC)
                }
            }
            bmp.setPixels(px, 0, w, 0, 0, w, h)
        }
    }
}
