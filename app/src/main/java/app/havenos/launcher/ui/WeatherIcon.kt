package app.havenos.launcher.ui

import app.havenos.launcher.data.WeatherKind
import app.havenos.launcher.ui.glass.HavenIcon

/**
 * 3.1 Phase B4: weather kind -> HavenIcon. Kept in ui (not ui.glass)
 * so the glass module never imports data; call sites use
 * `Icon(weatherIcon(kind).vector(), ...)`.
 */
fun weatherIcon(kind: WeatherKind): HavenIcon = when (kind) {
    WeatherKind.CLEAR -> HavenIcon.WeatherSun
    WeatherKind.MOSTLY_CLEAR -> HavenIcon.WeatherSun
    WeatherKind.PARTLY -> HavenIcon.WeatherPartly
    WeatherKind.OVERCAST -> HavenIcon.WeatherCloud
    WeatherKind.FOG -> HavenIcon.WeatherFog
    WeatherKind.DRIZZLE -> HavenIcon.WeatherDrizzle
    WeatherKind.RAIN -> HavenIcon.WeatherRain
    WeatherKind.SNOW -> HavenIcon.WeatherSnow
    WeatherKind.SHOWERS -> HavenIcon.WeatherShowers
    WeatherKind.STORM -> HavenIcon.WeatherStorm
    WeatherKind.UNKNOWN -> HavenIcon.WeatherUnknown
}
