package app.havenos.launcher.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.SettingsState
import app.havenos.launcher.ui.glass.HavenIcon
import app.havenos.launcher.ui.glass.HavenIconImage
import app.havenos.launcher.ui.glass.HavenSegmented
import app.havenos.launcher.ui.glass.HavenSkeleton
import app.havenos.launcher.ui.glass.vector
import app.havenos.launcher.ui.mascot.MascotCharacter
import app.havenos.launcher.ui.mascot.MascotExpression
import app.havenos.launcher.ui.mascot.MascotFigure
import app.havenos.launcher.ui.mascot.StaticSpriteFrame
import app.havenos.launcher.ui.mascot.USE_PIXEL_MASCOT
import app.havenos.launcher.ui.mascot.rememberSpriteSet
import app.havenos.launcher.ui.mascot.spriteScaleFor
import app.havenos.launcher.ui.settings.shell.PanelDeepLink
import app.havenos.launcher.data.network.ApiCacheKeys
import app.havenos.launcher.data.network.ApiClient
import app.havenos.launcher.data.network.ApiFailureClass
import app.havenos.launcher.data.network.ApiEndpoints
import app.havenos.launcher.data.network.ApiId
import app.havenos.launcher.data.network.ApiStore
import app.havenos.launcher.data.network.ApiToggles
import app.havenos.launcher.data.network.CachedState
import app.havenos.launcher.data.network.CardStatus
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

/**
 * 3.1 A1: "HH:mm" in the DEVICE's local timezone from an ISO-8601
 * timestamp. Unparseable input falls back to the raw HH:mm slice
 * (previous behavior), never a crash and never the word "null".
 */
fun scoreTimeLabel(iso: String): String {
    if (iso.isBlank()) return ""
    val parsed = try {
        val inFmt = when {
            iso.endsWith("Z") ->
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            iso.contains("+") || iso.lastIndexOf("-") > 10 ->
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
            else -> null
        }?.apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        val date = inFmt?.parse(iso.trim())
        date?.let {
            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(it)
        }
    } catch (t: Throwable) {
        null
    }
    if (parsed != null) return parsed
    if (iso.length < 16) return iso
    return iso.substring(11, 16)
}

/**
 * 3.1 A1: plain labels for provider status codes, in ONE table.
 * A code the table does not know shows no status text — in
 * particular "Abnormal" is NOT mapped, because its meaning is not
 * documented in the repo's API notes or the provider docs the code
 * can see; guessing it would invent a meaning. The raw
 * status_text field is never rendered anymore.
 */
val SCORE_STATUS_LABELS: Map<String, String> = mapOf(
    "upcoming" to "Not started",
    "live" to "Live",
    "finished" to "Finished",
    "delayed" to "Delayed",
)

/**
 * A score cell counts as missing when blank, a lone dash (live
 * cricket reports "-" before any numeric score exists), or the
 * literal string "null" (defensive: a JSON null must never reach
 * the screen as text, whatever path produced it).
 */
fun isMissingScore(value: String): Boolean =
    value.isBlank() || value == "-" || value.equals("null", ignoreCase = true)

/**
 * 3.1 A1: one display model per scores row — teams on the left,
 * result or time on the right, competition as a muted second line.
 * Null when both team names are missing: the row is dropped, never
 * rendered. Rules: not started shows "vs" (in the teams string)
 * plus the local start time when known; live/finished show the
 * provider's score format; missing scores on a live/finished match
 * show the mapped status label only; an unknown status shows no
 * status text at all.
 */
data class ScoreRowDisplay(val teams: String, val right: String, val competition: String)

fun ScoreMatch.toDisplay(): ScoreRowDisplay? {
    val teams = listOf(home, away).filter { it.isNotBlank() }
    if (teams.isEmpty()) return null
    val statusKey = status.lowercase()
    val label = SCORE_STATUS_LABELS[statusKey].orEmpty()
    val bothScores = !isMissingScore(homeScore) && !isMissingScore(awayScore)
    val right = when {
        bothScores -> "$homeScore – $awayScore"
        statusKey == "upcoming" -> scoreTimeLabel(time).ifBlank { label }
        else -> label
    }
    return ScoreRowDisplay(
        teams = teams.joinToString(" vs "),
        right = right,
        competition = competition,
    )
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
 * 3.1 A2: the last-result class wins over the age-derived status.
 * Offline with cache shows the cached content plus Updated HH:MM;
 * Offline without cache shows "Offline. Nothing saved yet.";
 * RateLimited shows "Busy right now. Try again later."; every other
 * failure class shows "Couldn't load." with a working Retry (only
 * while the toggle is on, i.e. when [onRetry] is non-null). The
 * phrase "check your connection" appears ONLY for the Offline
 * class. When the last refresh succeeded, the coordination-fix §7
 * age status (Fresh/Stale/Expired) drives the line as before.
 * A null status (initial states) falls back to the old lines.
 * 3.1 Phase C: the footer anatomy is Updated HH:MM · Source on
 * success paths; error/offline lines keep their A2 text.
 */
@Composable
private fun ApiStatusLine(
    state: CachedState<*>,
    onRetry: (() -> Unit)? = null,
    isRetrying: Boolean = false,
    source: String = "",
) {
    val palette = LocalPalette.current
    if (isRetrying) {
        Text("Loading…", color = palette.muted, fontSize = 12.sp, maxLines = 1)
        return
    }
    // Success footer: Updated HH:MM · Source.
    fun updatedLine(lastUpdatedMs: Long): String {
        val label = lastUpdatedLabel(lastUpdatedMs)
        return if (source.isBlank()) label else "$label · $source"
    }
    when (val line = failureLineFor(state.failureClass, state.data != null, state.lastUpdatedMs)) {
        is FailureLine.Updated -> {
            Text(
                updatedLine(line.lastUpdatedMs),
                color = palette.muted, fontSize = 12.sp, maxLines = 1,
            )
            return
        }
        FailureLine.OfflineEmpty -> {
            Text(
                "Offline. Nothing saved yet.",
                color = palette.muted, fontSize = 12.sp, maxLines = 1,
            )
            return
        }
        FailureLine.Busy -> {
            Text(
                "Busy right now. Try again later.",
                color = palette.muted, fontSize = 12.sp, maxLines = 1,
            )
            return
        }
        FailureLine.CouldNotLoad -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Couldn't load.",
                    color = palette.muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (onRetry != null) {
                    TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) {
                        Text("Retry", fontSize = 12.sp)
                    }
                }
            }
            return
        }
        FailureLine.None -> Unit // fall through to the age-derived status below
    }
    when (val status = state.cardStatus) {
        is CardStatus.Expired -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Couldn't refresh — showing ${lastUpdatedLabel(status.lastUpdatedMs)}",
                    color = palette.muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (onRetry != null) {
                    TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) {
                        Text("Retry", fontSize = 12.sp)
                    }
                }
            }
        }
        CardStatus.Stale -> Text(
            offlineLabel(state.data != null),
            color = palette.muted, fontSize = 12.sp, maxLines = 1,
        )
        CardStatus.Fresh -> Text(
            updatedLine(state.lastUpdatedMs),
            color = palette.muted, fontSize = 12.sp, maxLines = 1,
        )
        else -> {
            val line = when {
                state.isOffline -> offlineLabel(state.data != null)
                state.lastUpdatedMs > 0L -> updatedLine(state.lastUpdatedMs)
                state.isRefreshing -> "Loading…"
                else -> "Never updated"
            }
            Text(line, color = palette.muted, fontSize = 12.sp, maxLines = 1)
        }
    }
}

/**
 * 3.1 A2: pure half of the status line's failure branch — which
 * line a failure class renders. The composable only turns the
 * result into Text; the decision is unit-testable here.
 */
sealed interface FailureLine {
    data class Updated(val lastUpdatedMs: Long) : FailureLine
    data object OfflineEmpty : FailureLine
    data object Busy : FailureLine
    data object CouldNotLoad : FailureLine
    data object None : FailureLine
}

fun failureLineFor(
    failureClass: ApiFailureClass?,
    hasData: Boolean,
    lastUpdatedMs: Long,
): FailureLine = when (failureClass) {
    null -> FailureLine.None
    ApiFailureClass.OFFLINE ->
        if (hasData) FailureLine.Updated(lastUpdatedMs) else FailureLine.OfflineEmpty
    ApiFailureClass.RATE_LIMITED -> FailureLine.Busy
    else -> FailureLine.CouldNotLoad
}

/**
 * Coordination fix §7: manual retry for an Expired card. Refreshes
 * via the existing [ApiStore.refreshApi], then runs [onRefreshed] so
 * the card's flow re-reads the row. Returns (retry action or null,
 * retrying flag). The action is null while the API's toggle is off,
 * so the Retry button only renders while the toggle is on. No retry
 * storm: one user-initiated refresh; worker cadence unchanged.
 */
@Composable
private fun rememberApiRetry(
    apiStore: ApiStore,
    api: ApiId,
    onRefreshed: () -> Unit,
): Pair<(() -> Unit)?, Boolean> {
    val context = LocalContext.current
    val app = HavenApp.of(context)
    val settingsState by app.settings.state.collectAsState()
    val scope = rememberCoroutineScope()
    var retrying by remember { mutableStateOf(false) }
    val onRetry: (() -> Unit)? = if (!ApiToggles.isEnabled(settingsState, api)) {
        null
    } else {
        {
            scope.launch {
                retrying = true
                try {
                    apiStore.refreshApi(api, app.settings.state.value)
                } catch (t: Throwable) {
                    // 3.1.1 (item 2): unexpected refresh failure only.
                    // Classified failures (offline, rate-limited) never
                    // throw — they render as card status lines.
                    if (!FeatureSessionState.isDisabled(FeatureId.ONLINE_CARDS)) {
                        logFeatureError(context, FeatureId.ONLINE_CARDS, t)
                        FeatureSessionState.disable(FeatureId.ONLINE_CARDS)
                        try {
                            Toast.makeText(
                                context,
                                FeatureId.ONLINE_CARDS.toastText,
                                Toast.LENGTH_LONG,
                            ).show()
                        } catch (_: Exception) {
                        }
                    }
                } finally {
                    retrying = false
                    onRefreshed()
                }
            }
        }
    }
    return onRetry to retrying
}

/**
 * Honest empty state for a card with no cached data and no live data.
 * 3.1 A2: the phrase "check your connection" appears ONLY when the
 * last result class is Offline; every other failure says "try again
 * later" without it, so a device that is online never blames the
 * connection.
 */
@Composable
private fun ApiEmptyLine(state: CachedState<*>, what: String) {
    val palette = LocalPalette.current
    Text(
        when {
            state.failureClass == ApiFailureClass.OFFLINE ->
                "No saved $what yet — check your connection and try again later."
            state.isRefreshing -> "Loading $what…"
            state.failureClass != null || state.isOffline ->
                "No saved $what yet — try again later."
            else -> "No $what yet."
        },
        color = palette.muted,
        fontSize = 13.sp,
    )
}

/** 3.1 Phase C: initial-load skeleton (no data, refreshing, no failure recorded yet). */
private fun isCardLoading(state: CachedState<*>): Boolean =
    state.data == null && state.isRefreshing && state.failureClass == null

/** 3.1 Phase C: skeleton bars while a card's first load is in flight. */
@Composable
private fun ApiCardSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HavenSkeleton(Modifier.fillMaxWidth().height(16.dp))
        HavenSkeleton(Modifier.fillMaxWidth(0.7f).height(16.dp))
        HavenSkeleton(Modifier.fillMaxWidth(0.5f).height(16.dp))
    }
}

/**
 * 3.1 Phase C: offline/error/empty states use a static mascot pose
 * (SAD, no blink, no animation) beside the honest A2 text.
 */
@Composable
private fun ApiEmptyState(state: CachedState<*>, what: String, character: MascotCharacter) {
    val palette = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        // 3.1 Phase E: pixel sad frame when available, else the
        // vector pose. Static image only, never the roaming overlay.
        val sadSet = rememberSpriteSet(character, palette.accent.toArgb())
        if (USE_PIXEL_MASCOT && sadSet != null) {
            StaticSpriteFrame(
                sadSet, 30,
                spriteScaleFor(48f, LocalDensity.current.density),
            )
        } else {
            MascotFigure(
                character = character,
                expression = MascotExpression.SAD,
                size = 48.dp,
                body = palette.onSurface,
                eye = palette.surface,
                accent = palette.accent,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            ApiEmptyLine(state, what)
        }
    }
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

/**
 * 3.1 A12: re-collection key for Today card flows. Bumps on every
 * home resume (MainActivity.onResume → HavenApp.noteHomeResumed),
 * so a long-lived card composition re-runs cacheFirstFlow's stale
 * check when the user returns to home — the Xiaomi-deferred-worker
 * case — and refreshes only when the cache is actually stale.
 * Fresh caches emit without any network call.
 */
@Composable
private fun homeResumeKey(): Int {
    val app = HavenApp.of(LocalContext.current)
    val tick by app.homeResumedTick.collectAsState()
    return tick
}

@Composable
private fun QuoteCard(apiStore: ApiStore, character: MascotCharacter) {
    val palette = LocalPalette.current
    var refreshKey by remember { mutableStateOf(0) }
    val (onRetry, retrying) = rememberApiRetry(apiStore, ApiId.QUOTE) { refreshKey++ }
    val resumeKey = homeResumeKey()
    val flow = remember(apiStore, refreshKey, resumeKey) {
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
                // 3.1 Phase C: 18sp regular + author caption.
                Text(
                    quote.content,
                    color = palette.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                )
                if (quote.author.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("— ${quote.author}", color = palette.muted, fontSize = 12.sp, maxLines = 1)
                }
            } else if (isCardLoading(state)) {
                ApiCardSkeleton()
            } else {
                // No bundled or invented quotes: first run with no
                // cache and no network is an honest offline state.
                ApiEmptyState(state, "quote", character)
            }
            Spacer(Modifier.height(8.dp))
            ApiStatusLine(state, onRetry = onRetry, isRetrying = retrying, source = "Quotable")
        }
    }
}

@Composable
private fun ScoresCard(apiStore: ApiStore, character: MascotCharacter) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    var sport by remember { mutableStateOf("cricket") }
    var showAllScores by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    val (onRetry, retrying) = rememberApiRetry(apiStore, ApiId.SCORES) { refreshKey++ }
    val resumeKey = homeResumeKey()
    val cricketFlow = remember(apiStore, refreshKey, resumeKey) {
        apiStore.cacheFirstFlow(
            ApiId.SCORES,
            ApiCacheKeys.scores("cricket"),
            ::parseScores,
        ) { ApiClient.get(ApiId.SCORES, ApiEndpoints.scores("cricket")) }
            .flowOn(Dispatchers.IO)
    }
    val footballFlow = remember(apiStore, refreshKey, resumeKey) {
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
            // 3.1 Phase C: sport selector is HavenSegmented.
            HavenSegmented(
                options = listOf("Cricket", "Football"),
                selected = if (sport == "football") 1 else 0,
                onSelect = {
                    sport = if (it == 1) "football" else "cricket"
                    showAllScores = false
                },
            )
            Spacer(Modifier.height(10.dp))
            val matches = current.data
            if (matches.isNullOrEmpty()) {
                if (isCardLoading(current)) ApiCardSkeleton()
                else ApiEmptyState(current, "scores", character)
            } else {
                // Compact: live first, then upcoming, then finished;
                // 3.1 Phase C: at most 4 rows + Show more.
                // 3.1 A1: one display model per row (teams left, result
                // or time right, competition muted second line); raw
                // status text and "null"/dash-pair artifacts are gone.
                val sorted = sortScoresForDisplay(matches)
                val shown = if (showAllScores) sorted else sorted.take(4)
                for (m in shown) {
                    val row = m.toDisplay() ?: continue
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                row.teams,
                                color = palette.onSurface,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (row.competition.isNotBlank()) {
                                Text(
                                    row.competition,
                                    color = palette.muted,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (row.right.isNotBlank()) {
                            Text(
                                row.right,
                                color = palette.muted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
                if (sorted.size > 4) {
                    TextButton(onClick = { showAllScores = !showAllScores }) {
                        Text(if (showAllScores) "Show less" else "Show more", fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            ApiStatusLine(current, onRetry = onRetry, isRetrying = retrying, source = "SportScore")
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

/**
 * 3.1 A3: crypto card states. HIDDEN = toggle off (no card, no
 * fetch); KEY_NEEDED = toggle on without a key (compact state +
 * Add-key button); PRICES = toggle on with a key. Crypto stays
 * default Off for new installs (SettingsState.apiCrypto = false).
 */
enum class CryptoCardState { HIDDEN, KEY_NEEDED, PRICES }

fun cryptoCardState(apiCryptoOn: Boolean, keyBlank: Boolean): CryptoCardState = when {
    !apiCryptoOn -> CryptoCardState.HIDDEN
    keyBlank -> CryptoCardState.KEY_NEEDED
    else -> CryptoCardState.PRICES
}

/** 3.1 A3: deep-link destination for the crypto Add-key button. */
fun cryptoAddKeyTarget(): PanelDeepLink =
    PanelDeepLink(route = "apis", field = "coingecko-key", nonce = 0)

@Composable
private fun CryptoCard(apiStore: ApiStore, settings: SettingsState, character: MascotCharacter) {
    val palette = LocalPalette.current
    // Key-gated: with NO user Demo key set, do NOT fetch at all —
    // never burn the shared unauthenticated quota, never show a fake
    // price, never a blank card. The gated state is compact with one
    // button that deep-links to the key field in the APIs hub.
    if (settings.coingeckoKey.isBlank()) {
        val openSettings = LocalOpenSettingsPanel.current
        ApiCardFrame {
            Column(Modifier.fillMaxWidth()) {
                ApiCardHeader("Crypto")
                Text(
                    "A free CoinGecko Demo key is needed to see prices.",
                    color = palette.onSurface,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "No prices are fetched without a key.",
                    color = palette.muted,
                    fontSize = 12.sp,
                )
                // The button exists under the home host (the only real
                // composition site); elsewhere the state still explains
                // itself instead of showing a dead button.
                if (openSettings != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { openSettings("apis", cryptoAddKeyTarget().field) }) {
                        Text("Add key", fontSize = 13.sp)
                    }
                }
            }
        }
        return
    }
    var refreshKey by remember { mutableStateOf(0) }
    val (onRetry, retrying) = rememberApiRetry(apiStore, ApiId.CRYPTO) { refreshKey++ }
    val resumeKey = homeResumeKey()
    val flow = remember(apiStore, settings.coingeckoKey, refreshKey, resumeKey) {
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
                if (isCardLoading(state)) ApiCardSkeleton()
                else ApiEmptyState(state, "prices", character)
            } else {
                // 3.1 Phase C: symbol, price, 24h change with an
                // arrow icon + sign — never colour alone.
                for ((symbol, usd, change) in listOf(
                    Triple("BTC", data.btcUsd, data.btcChange),
                    Triple("ETH", data.ethUsd, data.ethChange),
                )) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(symbol, color = palette.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (!usd.isNaN()) {
                            Text(formatUsd(usd), color = palette.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (!change.isNaN()) {
                            HavenIconImage(
                                if (change >= 0) HavenIcon.ArrowUp else HavenIcon.ArrowDown,
                                if (change >= 0) "Up" else "Down",
                                size = 16.dp,
                                tint = palette.muted,
                            )
                            Text(
                                formatChangePct(change),
                                color = palette.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
                if (data.btcUsd.isNaN() && data.ethUsd.isNaN()) {
                    Text("Prices unavailable right now.", color = palette.muted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            ApiStatusLine(state, onRetry = onRetry, isRetrying = retrying, source = "CoinGecko")
        }
    }
}

@Composable
private fun ApodCard(apiStore: ApiStore, character: MascotCharacter) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var wallpaperStatus by remember { mutableStateOf("") }
    var refreshKey by remember { mutableStateOf(0) }
    val (onRetry, retrying) = rememberApiRetry(apiStore, ApiId.APOD) { refreshKey++ }
    val resumeKey = homeResumeKey()
    val flow = remember(apiStore, refreshKey, resumeKey) {
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
                if (isCardLoading(state)) ApiCardSkeleton()
                else ApiEmptyState(state, "picture", character)
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
                                        "Set as HavenOS wallpaper"
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
            ApiStatusLine(state, onRetry = onRetry, isRetrying = retrying, source = "NASA")
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
private fun GithubCard(apiStore: ApiStore, settings: SettingsState, character: MascotCharacter) {
    val palette = LocalPalette.current
    val username = settings.githubUsername.ifBlank { "Voidwalker-28" }
    var refreshKey by remember { mutableStateOf(0) }
    val (onRetry, retrying) = rememberApiRetry(apiStore, ApiId.GITHUB) { refreshKey++ }
    val resumeKey = homeResumeKey()
    val reposFlow = remember(apiStore, username, refreshKey, resumeKey) {
        apiStore.cacheFirstFlow(
            ApiId.GITHUB,
            ApiCacheKeys.githubRepos(username),
            ::parseGithubRepos,
        ) {
            ApiClient.get(ApiId.GITHUB, ApiEndpoints.githubRepos(username), ApiEndpoints.githubHeaders())
        }.flowOn(Dispatchers.IO)
    }
    val eventsFlow = remember(apiStore, username, refreshKey, resumeKey) {
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
                if (isCardLoading(repos)) ApiCardSkeleton()
                else ApiEmptyState(repos, "repos", character)
            } else {
                for (r in repoList.take(5)) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(r.name, color = palette.onSurface, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (r.description.isNotBlank()) {
                                Text(r.description, color = palette.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Icon(
                                HavenIcon.Star.vector(),
                                contentDescription = null,
                                tint = palette.muted,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(2.dp))
                            Text("${r.stars}", color = palette.muted, fontSize = 12.sp)
                        }
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
                if (isCardLoading(events)) ApiCardSkeleton()
                else ApiEmptyState(events, "activity", character)
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
            // Combined status: Expired wins (oldest expired row's time),
            // then Stale, then Fresh; null falls back to the old lines.
            val expiredMs = listOfNotNull(
                (repos.cardStatus as? CardStatus.Expired)?.lastUpdatedMs,
                (events.cardStatus as? CardStatus.Expired)?.lastUpdatedMs,
            ).minOrNull()
            val combinedStatus = when {
                expiredMs != null -> CardStatus.Expired(expiredMs)
                repos.cardStatus == CardStatus.Stale || events.cardStatus == CardStatus.Stale -> CardStatus.Stale
                repos.cardStatus == CardStatus.Fresh || events.cardStatus == CardStatus.Fresh -> CardStatus.Fresh
                else -> null
            }
            val combined = CachedState(
                data = if (repos.data != null || events.data != null) Unit else null,
                isRefreshing = repos.isRefreshing || events.isRefreshing,
                lastUpdatedMs = maxOf(repos.lastUpdatedMs, events.lastUpdatedMs),
                isOffline = repos.isOffline || events.isOffline,
                cardStatus = combinedStatus,
            )
            ApiStatusLine(combined, onRetry = onRetry, isRetrying = retrying, source = "GitHub")
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
    // 3.1.1 (item 2): an unexpectedly failed card refresh disables
    // the online cards for the session (see rememberApiRetry).
    if (FeatureSessionState.isDisabled(FeatureId.ONLINE_CARDS)) return
    val context = LocalContext.current
    val apiStore = remember(context) { HavenApp.of(context).apiStore }
    val character = if (settings.mascotCharacter == 1) MascotCharacter.MILA else MascotCharacter.MILO
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (ApiToggles.isEnabled(settings, ApiId.QUOTE)) QuoteCard(apiStore, character)
        if (ApiToggles.isEnabled(settings, ApiId.SCORES)) ScoresCard(apiStore, character)
        if (ApiToggles.isEnabled(settings, ApiId.CRYPTO)) CryptoCard(apiStore, settings, character)
        if (ApiToggles.isEnabled(settings, ApiId.APOD)) ApodCard(apiStore, character)
        if (ApiToggles.isEnabled(settings, ApiId.GITHUB)) GithubCard(apiStore, settings, character)
    }
}
