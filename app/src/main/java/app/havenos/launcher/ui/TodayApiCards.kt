package app.havenos.launcher.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.SettingsState
import app.havenos.launcher.data.network.ApiCacheKeys
import app.havenos.launcher.data.network.ApiClient
import app.havenos.launcher.data.network.ApiEndpoints
import app.havenos.launcher.data.network.ApiId
import app.havenos.launcher.data.network.ApiStore
import app.havenos.launcher.data.network.ApiToggles
import app.havenos.launcher.data.network.CachedState
import app.havenos.launcher.data.network.lastUpdatedLabel
import app.havenos.launcher.data.network.offlineLabel
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.LocalPalette
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * HavenOS v2 M1 — Today feed cards for the five batch-1 APIs with a
 * card UI (quote, scores, crypto, APOD, GitHub). Currency and
 * dictionary are NOT cards here; they belong to the search worker.
 *
 * Every card is cache-first through [ApiStore.cacheFirstFlow]: the
 * cached row renders immediately, a refresh only runs when the row is
 * stale (per-API max age in ApiCachePolicy), and a failed refresh keeps
 * the stale row with an offline label. No card ever shows a spinner
 * (first load is a "Loading…" text line) and no card ever shows fake
 * or placeholder data — no cache + no network is an honest offline
 * state, not an invented value.
 *
 * Toggle gating: a card renders ONLY when its [ApiId] toggle is on
 * ([ApiToggles.isEnabled]). Toggle off = the card does not render at
 * all. [TodayApiCards] takes the whole [SettingsState] so each card
 * can read its toggle (and, for crypto/APOD/GitHub, its key/username)
 * without new plumbing at every call site. Call sites that pass null
 * render nothing — the cards never guess at settings.
 *
 * Wiring: [TodayPage] (pager page, all size classes) and
 * [TodayRailContent] (Expanded rail + Medium strip) both render this
 * block AFTER the built-in cards; the Open-Meteo weather card is
 * untouched. The periodic WorkManager workers (ApiWorkers.kt) own
 * refresh cadence; these cards only read the cache flow.
 */

// ------------------------------------------------------------------ models

/** Quotable quote: content + author, from the first array element. */
data class QuoteData(val content: String, val author: String)

/** One SportScore match, parsed defensively (see [parseScores]). */
data class ScoreMatch(
    val home: String,
    val away: String,
    val homeScore: String,
    val awayScore: String,
    val status: String,
    val statusText: String,
    val time: String,
    val competition: String,
)

/** CoinGecko simple/price: USD price + 24h change per coin. NaN = absent. */
data class CryptoData(
    val btcUsd: Double,
    val btcChange: Double,
    val ethUsd: Double,
    val ethChange: Double,
)

/** NASA APOD entry. [displayUrl] is the image to show (see [apodDisplayUrl]). */
data class ApodData(
    val title: String,
    val explanation: String,
    val mediaType: String,
    val url: String,
    val thumbnailUrl: String,
    val date: String,
)

/** Public GitHub repo (public data only — no token is ever sent). */
data class GithubRepo(
    val name: String,
    val description: String,
    val stars: Int,
    val language: String,
)

/** Public GitHub event. Never presented as live (docs latency 30s–6h). */
data class GithubEvent(
    val type: String,
    val repoName: String,
    val createdAt: String,
)

// ------------------------------------------------------------------ parsers
// All parsers are total: malformed or unexpected JSON returns null (or
// an empty list) — never a throw, so a changed API shape degrades to
// an honest empty/offline state instead of a crash.

/**
 * Quotable /quotes/random returns an ARRAY; the quote is its first
 * element's content + author. A lone object is tolerated too (defensive
 * only — the verified contract is the array form).
 */
fun parseQuote(json: String): QuoteData? {
    return try {
        val trimmed = json.trim()
        val obj = when {
            trimmed.startsWith("[") -> {
                val arr = JSONArray(trimmed)
                if (arr.length() == 0) return null
                arr.optJSONObject(0) ?: return null
            }
            trimmed.startsWith("{") -> JSONObject(trimmed)
            else -> return null
        }
        val content = obj.optString("content", "").trim()
        if (content.isBlank()) return null
        QuoteData(content = content, author = obj.optString("author", "").trim())
    } catch (t: Throwable) {
        null
    }
}

/**
 * SportScore /api/widget/matches/ — LIVE shape verified 2026-10-26
 * (curl, cricket + football): top-level object {sport, count,
 * matches: [...]}; each match carries home, away, home_logo,
 * away_logo, home_score (string or null; live cricket matches report
 * "-" while no numeric score exists yet), away_score, status
 * ("upcoming" | "live" | "finished"), status_text, time (ISO-8601),
 * competition, competition_logo, url (relative path). A bare array
 * root is tolerated. Every field is read with optString/optJSONArray
 * and blank/missing values are kept blank — a renamed field shows as
 * an empty row field, never a crash. No betting fields exist in this
 * shape and none are rendered.
 */
fun parseScores(json: String): List<ScoreMatch> {
    return try {
        val trimmed = json.trim()
        val arr = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> JSONObject(trimmed).optJSONArray("matches") ?: return emptyList()
            else -> return emptyList()
        }
        val out = mutableListOf<ScoreMatch>()
        for (i in 0 until arr.length()) {
            val m = arr.optJSONObject(i) ?: continue
            val home = m.optString("home", "").trim()
            val away = m.optString("away", "").trim()
            if (home.isBlank() && away.isBlank()) continue
            out.add(
                ScoreMatch(
                    home = home,
                    away = away,
                    homeScore = m.optString("home_score", "").trim(),
                    awayScore = m.optString("away_score", "").trim(),
                    status = m.optString("status", "").trim(),
                    statusText = m.optString("status_text", "").trim(),
                    time = m.optString("time", "").trim(),
                    competition = m.optString("competition", "").trim(),
                ),
            )
        }
        out
    } catch (t: Throwable) {
        emptyList()
    }
}

/** CoinGecko simple/price: {bitcoin: {usd, usd_24h_change}, ethereum: {...}}. */
fun parseCrypto(json: String): CryptoData? {
    return try {
        val root = JSONObject(json)
        val btc = root.optJSONObject("bitcoin")
        val eth = root.optJSONObject("ethereum")
        if (btc == null && eth == null) return null
        CryptoData(
            btcUsd = btc?.optDouble("usd", Double.NaN) ?: Double.NaN,
            btcChange = btc?.optDouble("usd_24h_change", Double.NaN) ?: Double.NaN,
            ethUsd = eth?.optDouble("usd", Double.NaN) ?: Double.NaN,
            ethChange = eth?.optDouble("usd_24h_change", Double.NaN) ?: Double.NaN,
        )
    } catch (t: Throwable) {
        null
    }
}

/** NASA APOD: title, explanation, media_type, url, thumbnail_url, date. */
fun parseApod(json: String): ApodData? {
    return try {
        val obj = JSONObject(json)
        val title = obj.optString("title", "").trim()
        if (title.isBlank() && obj.optString("url", "").isBlank()) return null
        ApodData(
            title = title,
            explanation = obj.optString("explanation", "").trim(),
            mediaType = obj.optString("media_type", "").trim(),
            url = obj.optString("url", "").trim(),
            thumbnailUrl = obj.optString("thumbnail_url", "").trim(),
            date = obj.optString("date", "").trim(),
        )
    } catch (t: Throwable) {
        null
    }
}

/** GitHub repos: sorted by stars descending (UI takes the top 5). */
fun parseGithubRepos(json: String): List<GithubRepo> {
    return try {
        val arr = JSONArray(json)
        val out = mutableListOf<GithubRepo>()
        for (i in 0 until arr.length()) {
            val r = arr.optJSONObject(i) ?: continue
            val name = r.optString("name", "").trim()
            if (name.isBlank()) continue
            out.add(
                GithubRepo(
                    name = name,
                    description = r.optString("description", "").trim(),
                    stars = r.optInt("stargazers_count", 0),
                    language = r.optString("language", "").trim(),
                ),
            )
        }
        out.sortedByDescending { it.stars }
    } catch (t: Throwable) {
        emptyList()
    }
}

/** GitHub public events: type + repo name + created_at, in API order. */
fun parseGithubEvents(json: String): List<GithubEvent> {
    return try {
        val arr = JSONArray(json)
        val out = mutableListOf<GithubEvent>()
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: continue
            val repoName = e.optJSONObject("repo")?.optString("name", "")?.trim().orEmpty()
            val type = e.optString("type", "").trim()
            if (type.isBlank() && repoName.isBlank()) continue
            out.add(
                GithubEvent(
                    type = type,
                    repoName = repoName,
                    createdAt = e.optString("created_at", "").trim(),
                ),
            )
        }
        out
    } catch (t: Throwable) {
        emptyList()
    }
}

// ------------------------------------------------------------------ pure bits

/**
 * APOD display URL: media_type "video" has no displayable url — the
 * API's thumbnail_url (thumbs=true) is the image. Anything else uses
 * url. Blank when neither exists (caller shows a text state, never a
 * broken embed).
 */
fun apodDisplayUrl(apod: ApodData): String =
    if (apod.mediaType.equals("video", ignoreCase = true)) apod.thumbnailUrl else apod.url

/** "HH:mm" from an ISO-8601 timestamp, with a UTC marker when the offset is UTC. */
fun scoreTimeLabel(iso: String): String {
    if (iso.length < 16) return iso
    val hm = iso.substring(11, 16)
    val utc = iso.endsWith("Z") || iso.contains("+00:00")
    return if (utc) "$hm UTC" else hm
}

/** Live matches first, then upcoming, then finished; API order kept within each group. */
fun sortScoresForDisplay(matches: List<ScoreMatch>): List<ScoreMatch> {
    fun rank(m: ScoreMatch): Int = when (m.status.lowercase()) {
        "live" -> 0
        "upcoming" -> 1
        else -> 2
    }
    return matches.sortedBy { rank(it) }
}

fun formatUsd(value: Double): String =
    "$" + String.format(Locale.US, "%,.2f", value)

fun formatChangePct(value: Double): String =
    String.format(Locale.US, "%+.2f%%", value)

/** "PushEvent" -> "Push"; unknown types pass through unchanged. */
fun prettyEventType(type: String): String =
    type.removeSuffix("Event").ifBlank { type }

// ------------------------------------------------------------------ shared UI

/** One Today card frame: matches TodayPage's private frame (28dp glass, 16dp padding). */
@Composable
private fun ApiCardFrame(content: @Composable () -> Unit) {
    GlassSurface(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth().padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun ApiCardHeader(title: String) {
    val palette = LocalPalette.current
    Text(
        title,
        color = palette.accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
    Spacer(Modifier.height(8.dp))
}

/**
 * The "last updated / offline" status line every API card carries.
 * Stale data + this label beats a blank card and beats a spinner:
 * offline with cached data says so, offline with nothing says so,
 * otherwise the last-updated time is shown.
 */
@Composable
private fun ApiStatusLine(state: CachedState<*>) {
    val palette = LocalPalette.current
    val line = when {
        state.isOffline -> offlineLabel(state.data != null)
        state.lastUpdatedMs > 0L -> lastUpdatedLabel(state.lastUpdatedMs)
        state.isRefreshing -> "Loading…"
        else -> "Never updated"
    }
    Text(line, color = palette.muted, fontSize = 12.sp, maxLines = 1)
}

/** Honest empty state for a card with no cached data and no live data. */
@Composable
private fun ApiEmptyLine(state: CachedState<*>, what: String) {
    val palette = LocalPalette.current
    Text(
        if (state.isOffline) "No saved $what yet — check your connection and try again later."
        else if (state.isRefreshing) "Loading $what…"
        else "No $what yet.",
        color = palette.muted,
        fontSize = 13.sp,
    )
}

/**
 * NASA image hosts allowed for APOD image bytes. The APOD JSON itself
 * goes through ApiClient (api.nasa.gov allowlist); the image bytes in
 * the JSON's url/thumbnail_url live on separate NASA hosts, so they
 * get their own allowlist here — never an arbitrary URL. Hosts: the
 * classic APOD host (apod.nasa.gov), the NASA image library
 * (images-assets.nasa.gov), and the current NASA science site
 * (science.nasa.gov, seen in a live APOD response 2026-10-26 when the
 * API served a placeholder instead of a real picture). No genuine
 * apod.nasa.gov picture URL was observable in that live check (the
 * undated call returned 500, the dated call a placeholder logo), so
 * all three hosts are kept rather than narrowing on one observation.
 */
private val APOD_IMAGE_HOSTS = setOf(
    "apod.nasa.gov",
    "images-assets.nasa.gov",
    "science.nasa.gov",
)

/** Refuse APOD image downloads larger than this (card + wallpaper share this path). */
private const val MAX_APOD_IMAGE_BYTES = 20 * 1024 * 1024

/** Download image bytes for a URL. HTTPS only, host on [APOD_IMAGE_HOSTS],
 * redirects never followed — an off-allowlist redirect target is refused
 * rather than chased. Bounded: a declared or actual size over
 * [MAX_APOD_IMAGE_BYTES] aborts the download. */
private suspend fun downloadImageBytes(urlString: String): ByteArray? {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            if (url.protocol != "https") return@withContext null
            if (url.host.lowercase() !in APOD_IMAGE_HOSTS) return@withContext null
            val conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.requestMethod = "GET"
            if (conn.responseCode !in 200..299) return@withContext null
            if (conn.contentLengthLong > MAX_APOD_IMAGE_BYTES) return@withContext null
            val out = java.io.ByteArrayOutputStream()
            conn.inputStream.use { input ->
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    if (out.size() > MAX_APOD_IMAGE_BYTES) return@withContext null
                }
            }
            out.toByteArray()
        } catch (t: Throwable) {
            null
        }
    }
}

/**
 * Decode [bytes] downsampled to ~[targetW] px wide, mirroring
 * Wallpaper.decodePhoto: bounds pass first, then a power-of-two
 * inSampleSize (loop stops once the width is within 2x the target),
 * so a multi-megapixel APOD image never lands at full resolution.
 */
private fun decodeDownsampled(bytes: ByteArray, targetW: Int): android.graphics.Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > targetW * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    } catch (t: Throwable) {
        null
    }
}

// ------------------------------------------------------------------ cards

@Composable
private fun QuoteCard(apiStore: ApiStore) {
    val palette = LocalPalette.current
    val flow = remember(apiStore) {
        apiStore.cacheFirstFlow(
            ApiId.QUOTE,
            ApiCacheKeys.quote(),
            ::parseQuote,
        ) { ApiClient.get(ApiId.QUOTE, ApiEndpoints.quoteRandom()) }
            .flowOn(Dispatchers.IO)
    }
    val state by flow.collectAsState(
        initial = CachedState<QuoteData>(data = null, isRefreshing = true, lastUpdatedMs = 0L, isOffline = false),
    )
    ApiCardFrame {
        Column(Modifier.fillMaxWidth()) {
            ApiCardHeader("Quote of the day")
            val quote = state.data
            if (quote != null) {
                Text(quote.content, color = palette.onSurface, fontSize = 15.sp)
                if (quote.author.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("— ${quote.author}", color = palette.muted, fontSize = 13.sp, maxLines = 1)
                }
            } else {
                // No bundled or invented quotes: first run with no
                // cache and no network is an honest offline state.
                ApiEmptyLine(state, "quote")
            }
            Spacer(Modifier.height(8.dp))
            ApiStatusLine(state)
        }
    }
}

@Composable
private fun ScoresCard(apiStore: ApiStore) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    var sport by remember { mutableStateOf("cricket") }
    val cricketFlow = remember(apiStore) {
        apiStore.cacheFirstFlow(
            ApiId.SCORES,
            ApiCacheKeys.scores("cricket"),
            ::parseScores,
        ) { ApiClient.get(ApiId.SCORES, ApiEndpoints.scores("cricket")) }
            .flowOn(Dispatchers.IO)
    }
    val footballFlow = remember(apiStore) {
        apiStore.cacheFirstFlow(
            ApiId.SCORES,
            ApiCacheKeys.scores("football"),
            ::parseScores,
        ) { ApiClient.get(ApiId.SCORES, ApiEndpoints.scores("football")) }
            .flowOn(Dispatchers.IO)
    }
    val cricket by cricketFlow.collectAsState(
        initial = CachedState<List<ScoreMatch>>(data = null, isRefreshing = true, lastUpdatedMs = 0L, isOffline = false),
    )
    val football by footballFlow.collectAsState(
        initial = CachedState<List<ScoreMatch>>(data = null, isRefreshing = true, lastUpdatedMs = 0L, isOffline = false),
    )
    val current = if (sport == "football") football else cricket
    ApiCardFrame {
        Column(Modifier.fillMaxWidth()) {
            ApiCardHeader("Scores")
            // Sport selector: cricket + football, the two fetched sports.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((key, label) in listOf("cricket" to "Cricket", "football" to "Football")) {
                    val selected = sport == key
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (selected) palette.accent else palette.surface)
                            .clickable { sport = key }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = if (selected) palette.onSurface else palette.muted,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            val matches = current.data
            if (matches.isNullOrEmpty()) {
                ApiEmptyLine(current, "scores")
            } else {
                // Compact: live first, then upcoming, then finished; max 6 rows.
                for (m in sortScoresForDisplay(matches).take(6)) {
                    val scoreLine = if (m.homeScore.isNotBlank() || m.awayScore.isNotBlank()) {
                        "${m.home} ${m.homeScore} – ${m.awayScore} ${m.away}"
                    } else {
                        "${m.home} vs ${m.away}"
                    }
                    val detail = when {
                        m.statusText.isNotBlank() -> m.statusText
                        m.time.isNotBlank() -> scoreTimeLabel(m.time)
                        else -> ""
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                scoreLine.trim(),
                                color = palette.onSurface,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (m.competition.isNotBlank()) {
                                Text(
                                    m.competition,
                                    color = palette.muted,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (detail.isNotBlank()) {
                            Text(
                                detail,
                                color = palette.muted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            ApiStatusLine(current)
            Spacer(Modifier.height(6.dp))
            // Free-tier attribution: ALWAYS visible on the scores card,
            // in every state (data, empty, offline). Tap opens
            // sportscore.com — never a betting page, and no betting
            // content is rendered anywhere on this card.
            Text(
                "Powered by SportScore",
                color = palette.accent,
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier
                    .clickable {
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://sportscore.com"),
                            )
                            context.startActivity(intent)
                        } catch (t: Throwable) {
                            // No browser available: the label stays; nothing to do.
                        }
                    }
                    .padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun CryptoCard(apiStore: ApiStore, settings: SettingsState) {
    val palette = LocalPalette.current
    // Key-gated: with NO user Demo key set, do NOT fetch at all —
    // never burn the shared unauthenticated quota, never show a fake
    // price, never a blank card. The gated state names the fix.
    if (settings.coingeckoKey.isBlank()) {
        ApiCardFrame {
            Column(Modifier.fillMaxWidth()) {
                ApiCardHeader("Crypto")
                Text(
                    "Add your free CoinGecko Demo key in Settings → APIs to see prices.",
                    color = palette.onSurface,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "No prices are fetched without a key.",
                    color = palette.muted,
                    fontSize = 12.sp,
                )
            }
        }
        return
    }
    val flow = remember(apiStore, settings.coingeckoKey) {
        apiStore.cacheFirstFlow(
            ApiId.CRYPTO,
            ApiCacheKeys.crypto(),
            ::parseCrypto,
        ) {
            ApiClient.get(
                ApiId.CRYPTO,
                ApiEndpoints.cryptoPrice(),
                ApiEndpoints.cryptoHeaders(settings.coingeckoKey),
            )
        }.flowOn(Dispatchers.IO)
    }
    val state by flow.collectAsState(
        initial = CachedState<CryptoData>(data = null, isRefreshing = true, lastUpdatedMs = 0L, isOffline = false),
    )
    ApiCardFrame {
        Column(Modifier.fillMaxWidth()) {
            ApiCardHeader("Crypto")
            val data = state.data
            if (data == null) {
                ApiEmptyLine(state, "prices")
            } else {
                for ((label, usd, change) in listOf(
                    Triple("Bitcoin", data.btcUsd, data.btcChange),
                    Triple("Ethereum", data.ethUsd, data.ethChange),
                )) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, color = palette.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        if (!usd.isNaN()) {
                            Text(formatUsd(usd), color = palette.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (!change.isNaN()) {
                            Text(
                                formatChangePct(change),
                                color = palette.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
                if (data.btcUsd.isNaN() && data.ethUsd.isNaN()) {
                    Text("Prices unavailable right now.", color = palette.muted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            ApiStatusLine(state)
        }
    }
}

@Composable
private fun ApodCard(apiStore: ApiStore) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var wallpaperStatus by remember { mutableStateOf("") }
    val flow = remember(apiStore) {
        apiStore.cacheFirstFlow(
            ApiId.APOD,
            ApiCacheKeys.apod(),
            ::parseApod,
        ) {
            // DEMO_KEY by default; the user's personal key overrides it
            // when set. The key value itself is never rendered.
            val app = HavenApp.of(context)
            val key = app.settings.state.value.nasaKey.ifBlank { ApiEndpoints.NASA_DEMO_KEY }
            ApiClient.get(ApiId.APOD, ApiEndpoints.apod(key))
        }.flowOn(Dispatchers.IO)
    }
    val state by flow.collectAsState(
        initial = CachedState<ApodData>(data = null, isRefreshing = true, lastUpdatedMs = 0L, isOffline = false),
    )
    ApiCardFrame {
        Column(Modifier.fillMaxWidth()) {
            ApiCardHeader("Astronomy picture of the day")
            val apod = state.data
            if (apod == null) {
                ApiEmptyLine(state, "picture")
            } else {
                if (apod.title.isNotBlank()) {
                    Text(apod.title, color = palette.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                }
                val displayUrl = apodDisplayUrl(apod)
                if (displayUrl.isBlank()) {
                    Text(
                        if (apod.mediaType.equals("video", ignoreCase = true)) "Today's video has no preview image."
                        else "No image for today's picture.",
                        color = palette.muted,
                        fontSize = 13.sp,
                    )
                } else {
                    ApodImage(
                        url = displayUrl,
                        isVideo = apod.mediaType.equals("video", ignoreCase = true),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (apod.explanation.isNotBlank()) {
                    Text(apod.explanation, color = palette.onSurface, fontSize = 13.sp, maxLines = 8, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                }
                if (apod.date.isNotBlank()) {
                    Text(apod.date, color = palette.muted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                }
                // HavenOS-mode wallpaper ONLY: the image is saved to
                // the app's private files and applied through the
                // existing HavenOS-mode path (wallpaperMode=1,
                // wpKind=5 photo). The system wallpaper is never
                // touched — no WallpaperManager call exists here.
                if (displayUrl.isNotBlank()) {
                    Text(
                        "Set as HavenOS wallpaper",
                        color = palette.accent,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable {
                                wallpaperStatus = "Saving…"
                                scope.launch {
                                    val ok = runCatching {
                                        val bytes = downloadImageBytes(displayUrl) ?: return@runCatching false
                                        val app = HavenApp.of(context)
                                        val file = java.io.File(app.filesDir, "apod-wallpaper.jpg")
                                        file.writeBytes(bytes)
                                        app.settings.setWallpaperMode(1)
                                        app.settings.setWpKind(5)
                                        app.settings.setWpUri(Uri.fromFile(file).toString())
                                        true
                                    }.getOrDefault(false)
                                    wallpaperStatus = if (ok) {
                                        "Set as HavenOS wallpaper ✓"
                                    } else {
                                        "Couldn't save the image — try again later."
                                    }
                                }
                            }
                            .padding(vertical = 4.dp),
                    )
                    if (wallpaperStatus.isNotBlank()) {
                        Text(wallpaperStatus, color = palette.muted, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            ApiStatusLine(state)
        }
    }
}

/** APOD image loader (no image library in the app): fetch + decode off-main, bitmap remembered per URL. */
@Composable
private fun ApodImage(url: String, isVideo: Boolean) {
    val palette = LocalPalette.current
    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url) {
        val bytes = downloadImageBytes(url)
        val bmp = bytes?.let { decodeDownsampled(it, 1080) }
        if (bmp != null) bitmap = bmp else failed = true
    }
    val bmp = bitmap
    when {
        bmp != null -> {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = if (isVideo) "Video thumbnail" else "Astronomy picture of the day",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
        }
        failed -> {
            Text("Image couldn't be loaded.", color = palette.muted, fontSize = 13.sp)
        }
        else -> {
            Text("Loading image…", color = palette.muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun GithubCard(apiStore: ApiStore, settings: SettingsState) {
    val palette = LocalPalette.current
    val username = settings.githubUsername.ifBlank { "Voidwalker-28" }
    val reposFlow = remember(apiStore, username) {
        apiStore.cacheFirstFlow(
            ApiId.GITHUB,
            ApiCacheKeys.githubRepos(username),
            ::parseGithubRepos,
        ) {
            ApiClient.get(ApiId.GITHUB, ApiEndpoints.githubRepos(username), ApiEndpoints.githubHeaders())
        }.flowOn(Dispatchers.IO)
    }
    val eventsFlow = remember(apiStore, username) {
        apiStore.cacheFirstFlow(
            ApiId.GITHUB,
            ApiCacheKeys.githubEvents(username),
            ::parseGithubEvents,
        ) {
            ApiClient.get(ApiId.GITHUB, ApiEndpoints.githubEvents(username), ApiEndpoints.githubHeaders())
        }.flowOn(Dispatchers.IO)
    }
    val repos by reposFlow.collectAsState(
        initial = CachedState<List<GithubRepo>>(data = null, isRefreshing = true, lastUpdatedMs = 0L, isOffline = false),
    )
    val events by eventsFlow.collectAsState(
        initial = CachedState<List<GithubEvent>>(data = null, isRefreshing = true, lastUpdatedMs = 0L, isOffline = false),
    )
    ApiCardFrame {
        Column(Modifier.fillMaxWidth()) {
            ApiCardHeader("GitHub — @$username")
            // Public repos only; no token is ever sent, so private
            // data cannot appear here even in principle.
            Text("Public repos", color = palette.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            val repoList = repos.data
            if (repoList.isNullOrEmpty()) {
                ApiEmptyLine(repos, "repos")
            } else {
                for (r in repoList.take(5)) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(r.name, color = palette.onSurface, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (r.description.isNotBlank()) {
                                Text(r.description, color = palette.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Text("★ ${r.stars}", color = palette.muted, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // Recent activity — NEVER labelled live: GitHub's own
            // events latency is 30s–6h, so this is recent, not live.
            Text("Recent activity (not live)", color = palette.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            val eventList = events.data
            if (eventList.isNullOrEmpty()) {
                ApiEmptyLine(events, "activity")
            } else {
                for (e in eventList.take(5)) {
                    val day = if (e.createdAt.length >= 10) e.createdAt.substring(0, 10) else e.createdAt
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(
                            prettyEventType(e.type),
                            color = palette.onSurface,
                            fontSize = 13.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(
                            e.repoName,
                            color = palette.muted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (day.isNotBlank()) {
                            Text(day, color = palette.muted, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // Status: the newer of the two rows' updates, offline if either failed.
            val combined = CachedState(
                data = if (repos.data != null || events.data != null) Unit else null,
                isRefreshing = repos.isRefreshing || events.isRefreshing,
                lastUpdatedMs = maxOf(repos.lastUpdatedMs, events.lastUpdatedMs),
                isOffline = repos.isOffline || events.isOffline,
            )
            ApiStatusLine(combined)
        }
    }
}

// ------------------------------------------------------------------ block

/**
 * The five v2 M1 Today API cards, in card order: quote, scores,
 * crypto, APOD, GitHub. Each card self-gates on its toggle; with
 * [settings] null nothing renders. Callers place this AFTER the
 * built-in cards (weather card untouched).
 */
@Composable
fun TodayApiCards(settings: SettingsState?) {
    if (settings == null) return
    val context = LocalContext.current
    val apiStore = remember(context) { HavenApp.of(context).apiStore }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (ApiToggles.isEnabled(settings, ApiId.QUOTE)) QuoteCard(apiStore)
        if (ApiToggles.isEnabled(settings, ApiId.SCORES)) ScoresCard(apiStore)
        if (ApiToggles.isEnabled(settings, ApiId.CRYPTO)) CryptoCard(apiStore, settings)
        if (ApiToggles.isEnabled(settings, ApiId.APOD)) ApodCard(apiStore)
        if (ApiToggles.isEnabled(settings, ApiId.GITHUB)) GithubCard(apiStore, settings)
    }
}
