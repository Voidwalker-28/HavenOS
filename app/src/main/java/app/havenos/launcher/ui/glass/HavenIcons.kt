package app.havenos.launcher.ui.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// HavenOS 3.1 Phase B4 — one outline icon family. Every hub, row,
// permission, dock, and menu entry maps through [HavenIcon]; no screen
// draws its own glyph.
//
// Honesty note: these are ORIGINAL outline vectors authored for HavenOS
// (24dp viewport, ~1.7 stroke, round caps). They are NOT Material Symbols:
// the extended icon set is not a dependency of this project and the build
// is offline, so no symbol font could be fetched. The style matches the
// prompt's outline requirement; the attribution does not claim otherwise.
// ---------------------------------------------------------------------------

/** Every icon a HavenOS surface may show. Entries share vectors where the meaning matches. */
enum class HavenIcon {
    // Hubs
    HubSettings, HubLook, HubHomeDock, HubDrawerSearch, HubGesturesMotion,
    HubPrivacy, HubDeviceData, HubApis, HubMascot,
    // Rows and menu
    RowWallpaper, RowGrid, RowWidgets, RowSearch, MenuEditHome,
    // Permissions
    PermCalendar, PermContacts, PermNotifications, PermUsage,
    PermAccessibility, PermOverlay, PermDeviceAdmin,
    // Dock
    DockSearch, DockLibrary,
    // Weather
    WeatherSun, WeatherPartly, WeatherCloud, WeatherFog, WeatherDrizzle,
    WeatherRain, WeatherSnow, WeatherShowers, WeatherStorm, WeatherUnknown,
    // Media and misc
    MediaPlay, MediaPause, MediaPrevious, MediaNext, MediaCharging,
    Check, Close, ChevronRight, Back, Help, Info, Star, Music, Moon, Gear,
    CheckBoxOn, CheckBoxOff, Add, Eye, Search, Home, Calendar, People, Bell,
    Chart, Accessibility, Layers, Lock, Image, Swipe, Menu, Apps,
    // 3.1 Phase C: toolbar + picker glyphs (undo/redo/grid/radio).
    Undo, Redo, Grid, RadioOn, RadioOff, Minus, Plus, Sort, ArrowUp, ArrowDown,
}

private fun outline(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

private fun ImageVector.Builder.strokePath(block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
    path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero,
        pathBuilder = block,
    )
}

private fun androidx.compose.ui.graphics.vector.PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, false, true, cx + r, cy)
    arcTo(r, r, 0f, false, true, cx - r, cy)
}

private fun androidx.compose.ui.graphics.vector.PathBuilder.rect(x0: Float, y0: Float, x1: Float, y1: Float) {
    moveTo(x0, y0)
    lineTo(x1, y0)
    lineTo(x1, y1)
    lineTo(x0, y1)
    close()
}

/** Cloud outline used by every cloud-bearing weather icon (one shape, not six). */
private fun androidx.compose.ui.graphics.vector.PathBuilder.cloud() {
    moveTo(6f, 16f)
    curveTo(4f, 16f, 3.5f, 13.5f, 5f, 11.5f)
    curveTo(6f, 9.5f, 8.5f, 9f, 10f, 10.5f)
    curveTo(11f, 7.5f, 14.5f, 7f, 16f, 9f)
    curveTo(18.5f, 9f, 20f, 11f, 19f, 13.5f)
    curveTo(18f, 15.5f, 16f, 16f, 14f, 16f)
    lineTo(6f, 16f)
    close()
}

private val vectors: Map<HavenIcon, ImageVector> by lazy {
    fun v(icon: HavenIcon, block: ImageVector.Builder.() -> Unit) = icon to outline(icon.name, block)
    mapOf(
        v(HavenIcon.HubSettings) {
            strokePath {
                circle(12f, 12f, 3.2f)
                moveTo(12f, 4f); lineTo(12f, 6.5f)
                moveTo(12f, 17.5f); lineTo(12f, 20f)
                moveTo(4f, 12f); lineTo(6.5f, 12f)
                moveTo(17.5f, 12f); lineTo(20f, 12f)
                moveTo(6.7f, 6.7f); lineTo(8.5f, 8.5f)
                moveTo(15.5f, 15.5f); lineTo(17.3f, 17.3f)
                moveTo(17.3f, 6.7f); lineTo(15.5f, 8.5f)
                moveTo(8.5f, 15.5f); lineTo(6.7f, 17.3f)
            }
        },
        v(HavenIcon.Search) {
            strokePath {
                circle(11f, 11f, 6.5f)
                moveTo(16.5f, 16.5f); lineTo(21f, 21f)
            }
        },
        v(HavenIcon.Check) {
            strokePath {
                moveTo(5f, 12f); lineTo(10f, 17f); lineTo(19f, 7f)
            }
        },
        v(HavenIcon.Close) {
            strokePath {
                moveTo(6f, 6f); lineTo(18f, 18f)
                moveTo(18f, 6f); lineTo(6f, 18f)
            }
        },
        v(HavenIcon.ChevronRight) {
            strokePath {
                moveTo(9f, 5.5f); lineTo(15f, 12f); lineTo(9f, 18.5f)
            }
        },
        v(HavenIcon.Back) {
            strokePath {
                moveTo(15f, 5.5f); lineTo(9f, 12f); lineTo(15f, 18.5f)
            }
        },
        v(HavenIcon.Info) {
            strokePath {
                circle(12f, 12f, 8.5f)
                moveTo(12f, 11f); lineTo(12f, 16f)
                moveTo(12f, 7.8f); lineTo(12f, 7.9f)
            }
        },
        v(HavenIcon.Help) {
            strokePath {
                circle(12f, 12f, 8.5f)
                moveTo(9.5f, 9.5f)
                curveTo(9.5f, 7.5f, 11f, 6.5f, 12.2f, 6.5f)
                curveTo(13.8f, 6.5f, 14.8f, 7.5f, 14.8f, 9f)
                curveTo(14.8f, 10.5f, 13.2f, 11f, 12.2f, 11.8f)
                lineTo(12.2f, 13f)
                moveTo(12.2f, 16f); lineTo(12.2f, 16.1f)
            }
        },
        v(HavenIcon.Home) {
            strokePath {
                moveTo(4f, 11f); lineTo(12f, 4f); lineTo(20f, 11f)
                moveTo(6f, 10f); lineTo(6f, 20f); lineTo(18f, 20f); lineTo(18f, 10f)
                moveTo(10f, 20f); lineTo(10f, 14f); lineTo(14f, 14f); lineTo(14f, 20f)
            }
        },
        v(HavenIcon.Eye) {
            strokePath {
                moveTo(3f, 12f)
                curveTo(7f, 6.5f, 17f, 6.5f, 21f, 12f)
                curveTo(17f, 17.5f, 7f, 17.5f, 3f, 12f)
                close()
                circle(12f, 12f, 3f)
            }
        },
        v(HavenIcon.Calendar) {
            strokePath {
                rect(4f, 5.5f, 20f, 19f)
                moveTo(4f, 9.5f); lineTo(20f, 9.5f)
                moveTo(8f, 3.5f); lineTo(8f, 6.5f)
                moveTo(16f, 3.5f); lineTo(16f, 6.5f)
            }
        },
        v(HavenIcon.People) {
            strokePath {
                circle(9f, 8f, 3f)
                moveTo(4f, 20f)
                curveTo(4f, 15f, 6f, 13.5f, 9f, 13.5f)
                curveTo(12f, 13.5f, 14f, 15f, 14f, 20f)
                circle(16.5f, 9f, 2.3f)
                moveTo(15.5f, 20f)
                curveTo(15.5f, 16.5f, 17f, 14.8f, 20f, 14.8f)
            }
        },
        v(HavenIcon.Bell) {
            strokePath {
                moveTo(6f, 11f)
                curveTo(6f, 6.5f, 8.5f, 4.5f, 12f, 4.5f)
                curveTo(15.5f, 4.5f, 18f, 6.5f, 18f, 11f)
                lineTo(18f, 14.5f); lineTo(20f, 17f); lineTo(4f, 17f); lineTo(6f, 14.5f)
                close()
                moveTo(12f, 17.5f); lineTo(12f, 20f)
            }
        },
        v(HavenIcon.Chart) {
            strokePath {
                moveTo(5f, 20f); lineTo(5f, 5f)
                moveTo(5f, 20f); lineTo(20f, 20f)
                moveTo(8.5f, 20f); lineTo(8.5f, 13f)
                moveTo(12.5f, 20f); lineTo(12.5f, 9f)
                moveTo(16.5f, 20f); lineTo(16.5f, 15f)
            }
        },
        v(HavenIcon.Accessibility) {
            strokePath {
                circle(12f, 5.5f, 2f)
                moveTo(12f, 8f); lineTo(12f, 14f)
                moveTo(5.5f, 10f); lineTo(18.5f, 10f)
                moveTo(12f, 14f); lineTo(8f, 20f)
                moveTo(12f, 14f); lineTo(16f, 20f)
            }
        },
        v(HavenIcon.Layers) {
            strokePath {
                moveTo(12f, 4f); lineTo(20f, 8.5f); lineTo(12f, 13f); lineTo(4f, 8.5f)
                close()
                moveTo(6f, 12.5f); lineTo(12f, 15.5f); lineTo(18f, 12.5f)
            }
        },
        v(HavenIcon.Lock) {
            strokePath {
                rect(5f, 11f, 19f, 20f)
                moveTo(8f, 11f); lineTo(8f, 7.5f)
                curveTo(8f, 5f, 16f, 5f, 16f, 7.5f)
                lineTo(16f, 11f)
            }
        },
        v(HavenIcon.Image) {
            strokePath {
                rect(3.5f, 6f, 20.5f, 18f)
                circle(9f, 10.5f, 1.6f)
                moveTo(4f, 17f); lineTo(10f, 11.5f); lineTo(14f, 15f); lineTo(20f, 9.5f)
            }
        },
        v(HavenIcon.Swipe) {
            strokePath {
                moveTo(4f, 12f); lineTo(17f, 12f)
                moveTo(13f, 7.5f); lineTo(17.5f, 12f); lineTo(13f, 16.5f)
            }
        },
        v(HavenIcon.Menu) {
            strokePath {
                moveTo(5f, 7f); lineTo(19f, 7f)
                moveTo(5f, 12f); lineTo(19f, 12f)
                moveTo(5f, 17f); lineTo(19f, 17f)
            }
        },
        v(HavenIcon.Apps) {
            strokePath {
                rect(5f, 5f, 10f, 10f)
                rect(14f, 5f, 19f, 10f)
                rect(5f, 14f, 10f, 19f)
                rect(14f, 14f, 19f, 19f)
            }
        },
        v(HavenIcon.Add) {
            strokePath {
                moveTo(12f, 5f); lineTo(12f, 19f)
                moveTo(5f, 12f); lineTo(19f, 12f)
            }
        },
        v(HavenIcon.Star) {
            strokePath {
                moveTo(12f, 3.2f); lineTo(14.9f, 8.9f); lineTo(20.9f, 9.7f)
                lineTo(16.6f, 14f); lineTo(17.6f, 20f); lineTo(12f, 17f)
                lineTo(6.4f, 20f); lineTo(7.4f, 14f); lineTo(3.1f, 9.7f)
                lineTo(9.1f, 8.9f)
                close()
            }
        },
        v(HavenIcon.Music) {
            strokePath {
                circle(8.5f, 17.5f, 2.5f)
                moveTo(11f, 17.5f); lineTo(11f, 6f)
                curveTo(14f, 7f, 16f, 7.5f, 18f, 8f)
            }
        },
        v(HavenIcon.Moon) {
            strokePath {
                moveTo(20f, 3.5f)
                curveTo(14f, 4f, 9f, 9f, 10f, 15f)
                curveTo(11f, 20f, 16f, 21.5f, 20f, 19.5f)
                curveTo(16.5f, 18f, 14.5f, 14.5f, 15.5f, 10.5f)
                curveTo(16.5f, 6.5f, 18f, 4.5f, 20f, 3.5f)
                close()
            }
        },
        v(HavenIcon.Gear) {
            strokePath {
                circle(12f, 12f, 3.2f)
                moveTo(12f, 4f); lineTo(12f, 6.5f)
                moveTo(12f, 17.5f); lineTo(12f, 20f)
                moveTo(4f, 12f); lineTo(6.5f, 12f)
                moveTo(17.5f, 12f); lineTo(20f, 12f)
                moveTo(6.7f, 6.7f); lineTo(8.5f, 8.5f)
                moveTo(15.5f, 15.5f); lineTo(17.3f, 17.3f)
                moveTo(17.3f, 6.7f); lineTo(15.5f, 8.5f)
                moveTo(8.5f, 15.5f); lineTo(6.7f, 17.3f)
            }
        },
        v(HavenIcon.CheckBoxOn) {
            strokePath {
                rect(4f, 4f, 20f, 20f)
                moveTo(8f, 12f); lineTo(11f, 15f); lineTo(17f, 9f)
            }
        },
        v(HavenIcon.CheckBoxOff) {
            strokePath {
                rect(4f, 4f, 20f, 20f)
            }
        },
        v(HavenIcon.Undo) {
            strokePath {
                moveTo(19f, 12f); arcTo(7f, 7f, 0f, false, false, 12f, 5f)
                moveTo(8.5f, 6.8f); lineTo(12f, 5f); lineTo(13.8f, 8.5f)
            }
        },
        v(HavenIcon.Redo) {
            strokePath {
                moveTo(5f, 12f); arcTo(7f, 7f, 0f, false, true, 12f, 5f)
                moveTo(15.5f, 6.8f); lineTo(12f, 5f); lineTo(10.2f, 8.5f)
            }
        },
        v(HavenIcon.Grid) {
            strokePath {
                rect(5f, 5f, 10f, 10f)
                rect(14f, 5f, 19f, 10f)
                rect(5f, 14f, 10f, 19f)
                rect(14f, 14f, 19f, 19f)
            }
        },
        v(HavenIcon.RadioOff) {
            strokePath {
                circle(12f, 12f, 8.5f)
            }
        },
        v(HavenIcon.RadioOn) {
            strokePath {
                circle(12f, 12f, 8.5f)
            }
            path(fill = SolidColor(Color.Black), pathBuilder = { circle(12f, 12f, 3.5f) })
        },
        v(HavenIcon.ArrowUp) {
            strokePath {
                moveTo(6f, 17f); lineTo(12f, 7f); lineTo(18f, 17f)
            }
        },
        v(HavenIcon.ArrowDown) {
            strokePath {
                moveTo(6f, 7f); lineTo(12f, 17f); lineTo(18f, 7f)
            }
        },
        v(HavenIcon.Sort) {
            strokePath {
                moveTo(4f, 6f); lineTo(20f, 6f)
                moveTo(4f, 12f); lineTo(16f, 12f)
                moveTo(4f, 18f); lineTo(12f, 18f)
            }
        },
        v(HavenIcon.Minus) {
            strokePath {
                moveTo(5f, 12f); lineTo(19f, 12f)
            }
        },
        v(HavenIcon.Plus) {
            strokePath {
                moveTo(12f, 5f); lineTo(12f, 19f)
                moveTo(5f, 12f); lineTo(19f, 12f)
            }
        },
        v(HavenIcon.MediaPlay) {
            strokePath {
                moveTo(8f, 5f); lineTo(8f, 19f); lineTo(19f, 12f)
                close()
            }
        },
        v(HavenIcon.MediaPause) {
            strokePath {
                moveTo(8f, 5f); lineTo(8f, 19f)
                moveTo(16f, 5f); lineTo(16f, 19f)
            }
        },
        v(HavenIcon.MediaPrevious) {
            strokePath {
                moveTo(6f, 5f); lineTo(6f, 19f)
                moveTo(19f, 5f); lineTo(9f, 12f); lineTo(19f, 19f)
                close()
            }
        },
        v(HavenIcon.MediaNext) {
            strokePath {
                moveTo(18f, 5f); lineTo(18f, 19f)
                moveTo(5f, 5f); lineTo(15f, 12f); lineTo(5f, 19f)
                close()
            }
        },
        v(HavenIcon.MediaCharging) {
            strokePath {
                moveTo(13f, 3f); lineTo(6.5f, 13.5f); lineTo(11f, 13.5f)
                lineTo(10f, 21f); lineTo(18f, 10f); lineTo(12.5f, 10f)
                close()
            }
        },
        v(HavenIcon.WeatherSun) {
            strokePath {
                circle(12f, 12f, 4f)
                moveTo(12f, 3.5f); lineTo(12f, 5.5f)
                moveTo(12f, 18.5f); lineTo(12f, 20.5f)
                moveTo(3.5f, 12f); lineTo(5.5f, 12f)
                moveTo(18.5f, 12f); lineTo(20.5f, 12f)
                moveTo(6f, 6f); lineTo(7.4f, 7.4f)
                moveTo(16.6f, 16.6f); lineTo(18f, 18f)
                moveTo(18f, 6f); lineTo(16.6f, 7.4f)
                moveTo(7.4f, 16.6f); lineTo(6f, 18f)
            }
        },
        v(HavenIcon.WeatherPartly) {
            strokePath {
                circle(7.5f, 7.5f, 2.2f)
                moveTo(7.5f, 3.8f); lineTo(7.5f, 5f)
                moveTo(3.8f, 7.5f); lineTo(5f, 7.5f)
                moveTo(4.9f, 4.9f); lineTo(5.7f, 5.7f)
                moveTo(9f, 20f)
                curveTo(7f, 20f, 6.5f, 17.5f, 8f, 15.5f)
                curveTo(9f, 13.5f, 11.5f, 13f, 13f, 14.5f)
                curveTo(14f, 11.5f, 17.5f, 11f, 19f, 13f)
                curveTo(21.5f, 13f, 23f, 15f, 22f, 17.5f)
                curveTo(21f, 19.5f, 19f, 20f, 17f, 20f)
                lineTo(9f, 20f)
                close()
            }
        },
        v(HavenIcon.WeatherCloud) {
            strokePath { cloud() }
        },
        v(HavenIcon.WeatherFog) {
            strokePath {
                moveTo(6f, 9f); lineTo(18f, 9f)
                moveTo(5f, 13.5f); lineTo(19f, 13.5f)
                moveTo(7f, 18f); lineTo(17f, 18f)
            }
        },
        v(HavenIcon.WeatherDrizzle) {
            strokePath {
                cloud()
                moveTo(8f, 18f); lineTo(7f, 20f)
                moveTo(12f, 18f); lineTo(11f, 20f)
                moveTo(16f, 18f); lineTo(15f, 20f)
            }
        },
        v(HavenIcon.WeatherRain) {
            strokePath {
                cloud()
                moveTo(8f, 18f); lineTo(6.5f, 21f)
                moveTo(12f, 18f); lineTo(10.5f, 21f)
                moveTo(16f, 18f); lineTo(14.5f, 21f)
            }
        },
        v(HavenIcon.WeatherSnow) {
            strokePath {
                cloud()
                moveTo(8f, 19.5f); lineTo(8f, 19.6f)
                moveTo(12f, 19.5f); lineTo(12f, 19.6f)
                moveTo(16f, 19.5f); lineTo(16f, 19.6f)
            }
        },
        v(HavenIcon.WeatherShowers) {
            strokePath {
                cloud()
                moveTo(8f, 18f); lineTo(6f, 22f)
                moveTo(12f, 18f); lineTo(10f, 22f)
                moveTo(16f, 18f); lineTo(14f, 22f)
            }
        },
        v(HavenIcon.WeatherStorm) {
            strokePath {
                cloud()
                moveTo(13.5f, 14f); lineTo(11f, 18f); lineTo(13f, 18f); lineTo(12f, 21f)
            }
        },
    )
}

/** Resolve an entry to its vector. Shared mappings (aliases) are resolved here, not at call sites. */
fun HavenIcon.vector(): ImageVector = when (this) {
    HavenIcon.HubSettings -> vectors.getValue(HavenIcon.Gear)
    HavenIcon.HubLook -> vectors.getValue(HavenIcon.Eye)
    HavenIcon.HubHomeDock -> vectors.getValue(HavenIcon.Home)
    HavenIcon.HubDrawerSearch -> vectors.getValue(HavenIcon.Search)
    HavenIcon.HubGesturesMotion -> vectors.getValue(HavenIcon.Swipe)
    HavenIcon.HubPrivacy -> vectors.getValue(HavenIcon.Lock)
    HavenIcon.HubDeviceData -> vectors.getValue(HavenIcon.Apps)
    HavenIcon.HubApis -> vectors.getValue(HavenIcon.Chart)
    HavenIcon.HubMascot -> vectors.getValue(HavenIcon.Star)
    HavenIcon.RowWallpaper -> vectors.getValue(HavenIcon.Image)
    HavenIcon.RowGrid -> vectors.getValue(HavenIcon.Apps)
    HavenIcon.RowWidgets -> vectors.getValue(HavenIcon.Apps)
    HavenIcon.RowSearch -> vectors.getValue(HavenIcon.Search)
    HavenIcon.MenuEditHome -> vectors.getValue(HavenIcon.Gear)
    HavenIcon.PermCalendar -> vectors.getValue(HavenIcon.Calendar)
    HavenIcon.PermContacts -> vectors.getValue(HavenIcon.People)
    HavenIcon.PermNotifications -> vectors.getValue(HavenIcon.Bell)
    HavenIcon.PermUsage -> vectors.getValue(HavenIcon.Chart)
    HavenIcon.PermAccessibility -> vectors.getValue(HavenIcon.Accessibility)
    HavenIcon.PermOverlay -> vectors.getValue(HavenIcon.Layers)
    HavenIcon.PermDeviceAdmin -> vectors.getValue(HavenIcon.Lock)
    HavenIcon.DockSearch -> vectors.getValue(HavenIcon.Search)
    HavenIcon.DockLibrary -> vectors.getValue(HavenIcon.Apps)
    HavenIcon.WeatherUnknown -> vectors.getValue(HavenIcon.Help)
    else -> vectors.getValue(this)
}
