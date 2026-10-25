package app.havenos.launcher.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

data class HourPoint(val label: String, val temp: Double, val code: Int)
data class DayPoint(val label: String, val max: Double, val min: Double, val code: Int)

data class WeatherData(
    val cityName: String,
    val temp: Double,
    val code: Int,
    val hourly: List<HourPoint>,
    val daily: List<DayPoint>,
    val fetchedAtMs: Long,
)

/** WMO weather code -> (label, symbol). Symbols are plain text glyphs. */
fun weatherInfo(code: Int): Pair<String, String> = when (code) {
    0 -> "Clear" to "☀"
    1 -> "Mostly clear" to "☀"
    2 -> "Partly cloudy" to "⛅"
    3 -> "Overcast" to "☁"
    45, 48 -> "Fog" to "≋"
    51, 53, 55 -> "Drizzle" to "☂"
    56, 57 -> "Freezing drizzle" to "☂"
    61, 63, 65 -> "Rain" to "☂"
    66, 67 -> "Freezing rain" to "☂"
    71, 73, 75 -> "Snow" to "❄"
    77 -> "Snow grains" to "❄"
    80, 81, 82 -> "Showers" to "☂"
    85, 86 -> "Snow showers" to "❄"
    95 -> "Thunderstorm" to "⚡"
    96, 99 -> "Thunderstorm, hail" to "⚡"
    else -> "Unknown" to "—"
}

/** The ONLY network surface in the app: Open-Meteo hosts, hard-allowlisted. */
object WeatherNet {
    private val ALLOWED_HOSTS = setOf("api.open-meteo.com", "geocoding-api.open-meteo.com")

    fun fetch(url: URL): String {
        require(url.host in ALLOWED_HOSTS) { "Weather host not allowed: ${url.host}" }
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.requestMethod = "GET"
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    /** City search -> up to [count] (display label, lat, lon); empty on any failure. */
    fun geocode(name: String, count: Int = 1): List<Triple<String, Double, Double>> = try {
        val q = URLEncoder.encode(name, "UTF-8")
        val text = fetch(URL("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=$count"))
        val results = JSONObject(text).optJSONArray("results") ?: JSONArray()
        val out = mutableListOf<Triple<String, Double, Double>>()
        for (i in 0 until results.length()) {
            val r = results.getJSONObject(i)
            val label = listOf(r.optString("name"), r.optString("admin1"), r.optString("country"))
                .filter { it.isNotBlank() }.joinToString(", ")
            out += Triple(label.ifBlank { name }, r.getDouble("latitude"), r.getDouble("longitude"))
        }
        out
    } catch (t: Throwable) {
        emptyList()
    }
}

class WeatherStore(ctx: Context) {
    private val appCtx = ctx.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cacheFile = File(appCtx.filesDir, "weather_cache.json")

    val data = MutableStateFlow<WeatherData?>(null)
    val updating = MutableStateFlow(false)
    /** Last fetch failed (cleared on the next success); drives Error state. */
    val error = MutableStateFlow(false)

    private var dataUnits = -1
    @Volatile private var lastAttemptMs = 0L

    init {
        // Off the main thread: this store is constructed in Application.onCreate.
        scope.launch { data.value = loadCache() }
    }

    /**
     * Fetch only when weather is enabled, a city is set, and the cache is
     * stale (> 30 min, wrong city, or wrong units). All IO off the main thread.
     */
    fun refresh(settings: SettingsState, force: Boolean = false) {
        if (!settings.weatherNetwork) return
        val lat = settings.weatherLat.toDoubleOrNull() ?: return
        val lon = settings.weatherLon.toDoubleOrNull() ?: return
        val now = System.currentTimeMillis()
        val cached = data.value
        val fresh = cached != null && now - cached.fetchedAtMs < 30 * 60_000L &&
            cached.cityName == settings.weatherCity && dataUnits == settings.weatherUnits
        if (fresh && !force) return
        if (now - lastAttemptMs < 60_000L && !force) return // recomposition guard
        lastAttemptMs = now
        scope.launch {
            updating.value = true
            try {
                val unit = if (settings.weatherUnits == 1) "&temperature_unit=fahrenheit" else ""
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,weather_code" +
                        "&hourly=temperature_2m,weather_code" +
                        "&daily=temperature_2m_max,temperature_2m_min,weather_code" +
                        "&forecast_days=7&timezone=auto$unit",
                )
                val parsed = parseForecast(JSONObject(WeatherNet.fetch(url)), settings.weatherCity, now)
                if (parsed != null) {
                    data.value = parsed
                    dataUnits = settings.weatherUnits
                    error.value = false
                    saveCache(parsed, settings.weatherUnits)
                } else {
                    error.value = true
                }
            } catch (t: Throwable) {
                // keep showing the cache; the widget shows Error/Offline
                error.value = true
            } finally {
                updating.value = false
            }
        }
    }

    private fun parseForecast(root: JSONObject, city: String, nowMs: Long): WeatherData? = try {
        val current = root.getJSONObject("current")
        val temp = current.getDouble("temperature_2m")
        val code = current.getInt("weather_code")
        val currentTime = current.optString("time", "")

        val hourly = mutableListOf<HourPoint>()
        val h = root.optJSONObject("hourly")
        if (h != null) {
            val times = h.getJSONArray("time")
            val temps = h.getJSONArray("temperature_2m")
            val codes = h.getJSONArray("weather_code")
            var start = 0
            for (i in 0 until times.length()) {
                if (times.optString(i) >= currentTime) { start = i; break }
            }
            val hourFmt = SimpleDateFormat("h a", Locale.getDefault())
            val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            for (i in start until minOf(times.length(), start + 8)) {
                val label = try {
                    isoFmt.parse(times.optString(i))?.let { hourFmt.format(it) } ?: ""
                } catch (t: Throwable) {
                    ""
                }
                hourly += HourPoint(label, temps.optDouble(i, 0.0), codes.optInt(i, -1))
            }
        }

        val daily = mutableListOf<DayPoint>()
        val d = root.optJSONObject("daily")
        if (d != null) {
            val times = d.getJSONArray("time")
            val maxs = d.getJSONArray("temperature_2m_max")
            val mins = d.getJSONArray("temperature_2m_min")
            val codes = d.getJSONArray("weather_code")
            val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
            val isoDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            for (i in 0 until times.length()) {
                val label = if (i == 0) {
                    "Today"
                } else {
                    try {
                        isoDay.parse(times.optString(i))?.let { dayFmt.format(it) } ?: ""
                    } catch (t: Throwable) {
                        ""
                    }
                }
                daily += DayPoint(label, maxs.optDouble(i, 0.0), mins.optDouble(i, 0.0), codes.optInt(i, -1))
            }
        }
        WeatherData(city, temp, code, hourly, daily, nowMs)
    } catch (t: Throwable) {
        null
    }

    private fun saveCache(w: WeatherData, units: Int) {
        try {
            val root = JSONObject()
                .put("city", w.cityName).put("temp", w.temp).put("code", w.code)
                .put("fetchedAt", w.fetchedAtMs).put("units", units)
                .put("hourly", JSONArray().apply {
                    for (p in w.hourly) put(JSONObject().put("l", p.label).put("t", p.temp).put("c", p.code))
                })
                .put("daily", JSONArray().apply {
                    for (p in w.daily) {
                        put(JSONObject().put("l", p.label).put("x", p.max).put("n", p.min).put("c", p.code))
                    }
                })
            cacheFile.writeText(root.toString())
        } catch (t: Throwable) {
            // cache is best-effort
        }
    }

    private fun loadCache(): WeatherData? {
        return try {
            if (!cacheFile.exists()) return null
            val root = JSONObject(cacheFile.readText())
        dataUnits = root.optInt("units", -1)
        val hourly = mutableListOf<HourPoint>()
        val ha = root.optJSONArray("hourly") ?: JSONArray()
        for (i in 0 until ha.length()) {
            val o = ha.getJSONObject(i)
            hourly += HourPoint(o.optString("l"), o.optDouble("t", 0.0), o.optInt("c", -1))
        }
        val daily = mutableListOf<DayPoint>()
        val da = root.optJSONArray("daily") ?: JSONArray()
        for (i in 0 until da.length()) {
            val o = da.getJSONObject(i)
            daily += DayPoint(o.optString("l"), o.optDouble("x", 0.0), o.optDouble("n", 0.0), o.optInt("c", -1))
        }
        WeatherData(
            cityName = root.optString("city"),
            temp = root.optDouble("temp", 0.0),
            code = root.optInt("code", -1),
            hourly = hourly,
            daily = daily,
            fetchedAtMs = root.optLong("fetchedAt", 0L),
            )
        } catch (t: Throwable) {
            null
        }
    }
}
