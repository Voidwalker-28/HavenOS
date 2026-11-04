package app.havenos.launcher.data.network

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * HavenOS v2 network foundation (M1). The single HTTP surface for all v2
 * API features. Raw HttpURLConnection, no new network dependencies —
 * the same pattern as WeatherNet in data/Weather.kt.
 *
 * Rules every caller inherits:
 * - HTTPS only, and the URL's host must be on that API's allowlist
 *   ([ApiHosts]); anything else is refused BEFORE any request is sent.
 * - No exception ever escapes to the UI: every outcome is an [ApiResult].
 * - No API key is ever hardcoded here. Keys come from DataStore via the
 *   caller (see [ApiEndpoints]); the only built-in key value is NASA's
 *   published DEMO_KEY identifier, which is not a secret.
 */

/** The seven v2 batch-1 APIs. One enum value = one allowlist + one cache policy. */
enum class ApiId {
    QUOTE, SCORES, CRYPTO, APOD, GITHUB, CURRENCY, DICTIONARY,
}

/** Typed failure kinds, so UI can say "offline" instead of spinning forever. */
enum class ApiErrorKind {
    /** No route to the host (DNS, timeout, connection failure). */
    OFFLINE,
    /** HTTP 429 — back off; the cached value stays on screen. */
    RATE_LIMITED,
    /** HTTP 404 — for the dictionary this is "word not found", not a crash. */
    NOT_FOUND,
    /** HTTP 5xx — the server failed; retrying immediately will not help. */
    SERVER_ERROR,
    /** Any other HTTP status, or a malformed URL. */
    OTHER,
}

/**
 * 3.1 Phase A2: the single classification of an API outcome, used by
 * every card and by the Diagnostics row. [OFFLINE] is produced ONLY
 * when the device has no validated internet network — a transport
 * failure while validated means [NETWORK_ERROR] (host unreachable),
 * never offline. Parse failures are recorded by the caller that
 * actually parsed (cache-first flow), because the transport layer
 * never sees the shape.
 */
enum class ApiFailureClass {
    OFFLINE,
    NETWORK_ERROR,
    RATE_LIMITED,
    NOT_FOUND,
    PARSE_ERROR,
    SERVER_ERROR,
    OTHER,
}

/**
 * Pure classifier: transport kind + HTTP code + validated-network
 * flag → the class the card renders. No Android calls, so it is
 * unit-testable without a device. In particular, OFFLINE is never
 * produced when [hasValidatedInternet] is true.
 */
fun classifyFailure(
    kind: ApiErrorKind,
    httpCode: Int,
    hasValidatedInternet: Boolean,
): ApiFailureClass = when (kind) {
    ApiErrorKind.RATE_LIMITED -> ApiFailureClass.RATE_LIMITED
    ApiErrorKind.NOT_FOUND -> ApiFailureClass.NOT_FOUND
    ApiErrorKind.SERVER_ERROR -> ApiFailureClass.SERVER_ERROR
    ApiErrorKind.OFFLINE ->
        if (hasValidatedInternet) ApiFailureClass.NETWORK_ERROR else ApiFailureClass.OFFLINE
    ApiErrorKind.OTHER ->
        if (httpCode in 500..599) ApiFailureClass.SERVER_ERROR else ApiFailureClass.OTHER
}

/**
 * True when the device currently has a validated internet network
 * (ConnectivityManager network capabilities). Never throws; false on
 * any error, so a failed check degrades to the offline path rather
 * than crashing the card. Requires ACCESS_NETWORK_STATE (normal
 * permission, no runtime prompt).
 */
fun android.content.Context.hasValidatedInternet(): Boolean = try {
    val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
        as? android.net.ConnectivityManager ?: return false
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
} catch (t: Throwable) {
    false
}

sealed interface ApiResult {
    data class Success(val body: String) : ApiResult
    data class Failure(val kind: ApiErrorKind, val httpCode: Int = -1) : ApiResult
}

/** Per-API host allowlist, enforced in [ApiClient.get] before connecting. */
object ApiHosts {
    private val hosts: Map<ApiId, Set<String>> = mapOf(
        ApiId.QUOTE to setOf("api.quotable.io"),
        ApiId.SCORES to setOf("sportscore.com"),
        ApiId.CRYPTO to setOf("api.coingecko.com"),
        ApiId.APOD to setOf("api.nasa.gov"),
        ApiId.GITHUB to setOf("api.github.com"),
        // currency-api: primary CDN host + official fallback host.
        ApiId.CURRENCY to setOf("cdn.jsdelivr.net", "latest.currency-api.pages.dev"),
        ApiId.DICTIONARY to setOf("api.dictionaryapi.dev"),
    )

    /** True when [host] may serve [api]. Case-insensitive, exact match only. */
    fun allows(api: ApiId, host: String): Boolean =
        hosts[api]?.contains(host.lowercase()) == true
}

/**
 * Endpoint builders for the verified v2 table. These build URL strings
 * only; they never fetch, so a wrong host here is still caught by
 * [ApiClient.get]'s allowlist check at request time.
 *
 * Verified table (do not invent other hosts/endpoints):
 * - Quotable: GET api.quotable.io/quotes/random (returns an ARRAY;
 *   /random is deprecated). No daily endpoint — daily behaviour is
 *   client-side fetch-once + 1-day cache.
 * - SportScore: GET sportscore.com/api/widget/matches/?sport=…&limit=50.
 *   Free tier requires visible "Powered by SportScore" attribution —
 *   the cards worker renders it; this layer just fetches.
 * - CoinGecko: GET api.coingecko.com/api/v3/simple/price?… Demo key via
 *   the x-cg-demo-api-key header, user-supplied only, never hardcoded.
 * - NASA APOD: GET api.nasa.gov/planetary/apod?api_key=…. media_type
 *   "video" has no displayable url — use thumbnail_url (thumbs=true).
 * - GitHub: GET api.github.com/users/{username}/repos and
 *   /users/{username}/events/public. Events are NOT real-time
 *   (30s–6h latency) — never present them as live.
 * - currency-api: daily rates only; response is {date, <base>: {…}},
 *   NOT a "rates" object. Primary + fallback hosts both listed.
 * - Free Dictionary: GET api.dictionaryapi.dev/api/v2/entries/en/{word}.
 *   404 = word not found (normal).
 */
object ApiEndpoints {
    /** NASA's published demo identifier — not a secret, shared limits apply. */
    const val NASA_DEMO_KEY = "DEMO_KEY"

    fun quoteRandom(): String = "https://api.quotable.io/quotes/random"

    /** [sport] must be "cricket" or "football" (anything else is refused by the API, not here). */
    fun scores(sport: String): String =
        "https://sportscore.com/api/widget/matches/?sport=$sport&limit=50"

    fun cryptoPrice(): String =
        "https://api.coingecko.com/api/v3/simple/price" +
            "?ids=bitcoin,ethereum&vs_currencies=usd" +
            "&include_24hr_change=true&include_last_updated_at=true"

    /** Header for a user-supplied CoinGecko Demo key; empty map when no key is set. */
    fun cryptoHeaders(demoKey: String): Map<String, String> =
        if (demoKey.isBlank()) emptyMap() else mapOf("x-cg-demo-api-key" to demoKey)

    /** [apiKey] is the user's personal key when set, else [NASA_DEMO_KEY]. */
    fun apod(apiKey: String): String =
        "https://api.nasa.gov/planetary/apod?api_key=$apiKey&thumbs=true"

    fun githubRepos(username: String): String =
        "https://api.github.com/users/$username/repos"

    fun githubEvents(username: String): String =
        "https://api.github.com/users/$username/events/public"

    /** Headers GitHub asks unauthenticated callers to send. */
    fun githubHeaders(): Map<String, String> = mapOf(
        "Accept" to "application/vnd.github+json",
        "X-GitHub-Api-Version" to "2022-11-28",
    )

    /** Primary currency-api URL for [base] (lowercase, e.g. "usd"). */
    fun currencyPrimary(base: String): String =
        "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/${base.lowercase()}.json"

    /** Fallback currency-api URL for [base]. */
    fun currencyFallback(base: String): String =
        "https://latest.currency-api.pages.dev/v1/currencies/${base.lowercase()}.json"

    fun dictionary(word: String): String =
        "https://api.dictionaryapi.dev/api/v2/entries/en/${word.lowercase()}"
}

/** Single HTTP client for all v2 APIs. Blocking — call off the main thread. */
object ApiClient {
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000

    /**
     * GET [urlString] for [api]. Refuses (returns [ApiResult.Failure]
     * OTHER) when the URL is malformed, not HTTPS, or its host is not
     * on [api]'s allowlist — before any connection is opened.
     * Redirects are NOT followed: the allowlist is checked on the
     * original URL only, so a 3xx to an off-allowlist host must never
     * be chased automatically. A 3xx is returned as OTHER, and callers
     * with a fallback (currency) retry on their fallback host.
     */
    fun get(
        api: ApiId,
        urlString: String,
        headers: Map<String, String> = emptyMap(),
    ): ApiResult {
        val url = try {
            URL(urlString)
        } catch (t: Throwable) {
            return ApiResult.Failure(ApiErrorKind.OTHER)
        }
        if (url.protocol != "https") return ApiResult.Failure(ApiErrorKind.OTHER)
        if (!ApiHosts.allows(api, url.host)) {
            return ApiResult.Failure(ApiErrorKind.OTHER)
        }
        var conn: HttpURLConnection? = null
        return try {
            conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.requestMethod = "GET"
            for ((k, v) in headers) conn.setRequestProperty(k, v)
            val code = conn.responseCode
            when {
                code in 200..299 -> {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    ApiResult.Success(body)
                }
                code == 429 -> ApiResult.Failure(ApiErrorKind.RATE_LIMITED, code)
                code == 404 -> ApiResult.Failure(ApiErrorKind.NOT_FOUND, code)
                code in 300..399 -> ApiResult.Failure(ApiErrorKind.OTHER, code)
                code in 500..599 -> ApiResult.Failure(ApiErrorKind.SERVER_ERROR, code)
                else -> ApiResult.Failure(ApiErrorKind.OTHER, code)
            }
        } catch (e: UnknownHostException) {
            ApiResult.Failure(ApiErrorKind.OFFLINE)
        } catch (e: SocketTimeoutException) {
            ApiResult.Failure(ApiErrorKind.OFFLINE)
        } catch (e: IOException) {
            ApiResult.Failure(ApiErrorKind.OFFLINE)
        } catch (t: Throwable) {
            ApiResult.Failure(ApiErrorKind.OTHER)
        } finally {
            try {
                conn?.disconnect()
            } catch (t: Throwable) {
                // disconnect is best-effort
            }
        }
    }

    /**
     * Currency fetch with fallback: primary CDN host first, official
     * fallback host on any failure. Returns the first success, else the
     * primary's failure (so the kind still reflects the primary cause).
     */
    fun getCurrency(base: String): ApiResult {
        val primary = get(ApiId.CURRENCY, ApiEndpoints.currencyPrimary(base))
        if (primary is ApiResult.Success) return primary
        val fallback = get(ApiId.CURRENCY, ApiEndpoints.currencyFallback(base))
        return if (fallback is ApiResult.Success) fallback else primary
    }
}
