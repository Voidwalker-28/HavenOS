package app.havenos.launcher.ui.glass

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb

// ---------------------------------------------------------------------------
// HavenOS 3.1 Phase B7 — runtime accent seeds. Best-effort, never throws:
// every failure yields "no seed", and the caller falls back to the Minimal
// default accent. Wallpaper seeds are suggestions only, never auto-applied.
// ---------------------------------------------------------------------------

/**
 * System wallpaper colours (WallpaperManager.getWallpaperColors, API 27+),
 * primary first, then secondary and tertiary when present. Empty on older
 * platforms, on failure, or when the device reports no colours. The
 * tablet hypothesis (works for the system wallpaper on Android 13+) is
 * recorded in UNVERIFIED.md — it cannot be proven without the device.
 */
fun wallpaperSeedColors(context: Context): List<Int> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return emptyList()
    return try {
        val wm = context.getSystemService(WallpaperManager::class.java) ?: return emptyList()
        val colors = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return emptyList()
        listOfNotNull(
            colors.primaryColor?.toArgb(),
            colors.secondaryColor?.toArgb(),
            colors.tertiaryColor?.toArgb(),
        )
    } catch (t: Throwable) {
        emptyList()
    }
}

/**
 * Material You dynamic-colour seed (Android 12+), or null on older
 * platforms or on failure. The UI hides the System source below API 31,
 * so null here only ever means "fall back to default".
 */
fun systemSeedColor(context: Context, dark: Boolean): Int? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return try {
        val scheme = if (dark) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
        scheme.primary.toArgb()
    } catch (t: Throwable) {
        null
    }
}
