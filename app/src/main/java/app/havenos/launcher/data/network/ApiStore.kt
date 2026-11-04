package app.havenos.launcher.data.network

import app.havenos.launcher.data.SettingsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Cache-first UI contract for all v2 API features (M1). Downstream
 * card/search workers use [cacheFirstFlow] (or [dictionaryFlow]) and
 * NEVER fetch directly: the flow emits the cached row immediately, then
 * refreshes only when stale, and on failure keeps the stale row with
 * [CachedState.isOffline] = true. Result: never a blank card, never an
 * endless spinner — stale data + an offline label beats both.
 */

/** What a card/search row renders. Never null data + spinner at once. */
data class CachedState<T>(
    /** Parsed cached (or just-fetched) data, or null when nothing is cached yet. */
    val data: T?,
    /** A refresh is in flight. Render the cached data (if any) meanwhile. */
    val isRefreshing: Boolean,
    /** Epoch ms of the cached row, or 0 when nothing is cached yet. */
    val lastUpdatedMs: Long,
    /** Last refresh failed (offline / rate-limited / other). Stale data still renders. */
    val isOffline: Boolean,
    /** Last refresh returned 404 (dictionary: word not found). Not an error state. */
    val notFound: Boolean = false,
    /**
     * Coordination fix §7: age-derived status (Fresh/Stale/Expired/
     * EmptyOffline), or null until the first row is read (initial
     * states render the old fallback lines). The worker failure
     * counter never drives this — age ratio alone does.
     */
    val cardStatus: CardStatus? = null,
    /**
     * 3.1 A2: the class of the last refresh outcome in THIS flow
     * lifetime, or null when the last refresh succeeded (or none has
     * run yet). The card renders A2 states from this first, and falls
     * back to [cardStatus] when it is null. A fresh cache row implies
     * a recent success, so a null initial value is correct in every
     * path: a stale row always triggers a refresh on collect, and a
     * parse failure on a fresh body forces one too.
     */
    val failureClass: ApiFailureClass? = null,
)

/** Toggle gating: the single place that maps an API to its Settings flag. */
object ApiToggles {
    fun isEnabled(state: SettingsState, api: ApiId): Boolean = when (api) {
        ApiId.QUOTE -> state.apiQuote
        ApiId.SCORES -> state.apiScores
        ApiId.CRYPTO -> state.apiCrypto
        ApiId.APOD -> state.apiApod
        ApiId.GITHUB -> state.apiGithub
        ApiId.CURRENCY -> state.apiCurrency
        ApiId.DICTIONARY -> state.apiDictionary
    }
}

/** Honest status lines for the "last updated / offline" row on a card. */
fun lastUpdatedLabel(lastUpdatedMs: Long): String {
    if (lastUpdatedMs <= 0L) return "Never updated"
    val now = System.currentTimeMillis()
    val sameDay = now / 86_400_000L == lastUpdatedMs / 86_400_000L
    val fmt = if (sameDay) SimpleDateFormat("HH:mm", Locale.getDefault())
        else SimpleDateFormat("d MMM HH:mm", Locale.getDefault())
    return "Updated ${fmt.format(Date(lastUpdatedMs))}"
}

/** Offline line: callers show this only when [CachedState.isOffline] is true. */
fun offlineLabel(hasCachedData: Boolean): String =
    if (hasCachedData) "Offline — showing saved data" else "Offline — no saved data yet"

class ApiStore(
    private val dao: ApiCacheDao,
    private val diagnosticsDao: ApiDiagnosticsDao? = null,
    /**
     * 3.1 A2: read at fetch time, so a network change between
     * opens is honored. Production passes the
     * ConnectivityManager validated-network check; tests pass a
     * fixed lambda. Defaults to true (validated) so existing
     * constructions without a network check keep the old
     * transport-failure behavior.
     */
    private val hasValidatedInternet: () -> Boolean = { true },
) {

    /**
     * 3.1 A2: record one Diagnostics row per API outcome. Success
     * clears the failure class (stored as "SUCCESS", HTTP 200).
     * Body snippets are recorded for parse errors only, capped at
     * 200 chars, and never contain request credentials (keys travel
     * in headers, never in bodies). Best-effort: a diagnostics
     * write failure must never fail the card refresh itself.
     */
    private suspend fun recordDiagnostic(
        api: ApiId,
        resultClass: String,
        httpCode: Int,
        bodySnippet: String = "",
    ) {
        val dao = diagnosticsDao ?: return
        try {
            dao.upsert(
                ApiDiagnosticsRow(
                    api = api.name,
                    resultClass = resultClass,
                    httpCode = httpCode,
                    timeMs = System.currentTimeMillis(),
                    bodySnippet = bodySnippet.take(200),
                ),
            )
        } catch (t: Throwable) {
            // Diagnostics must never break a refresh.
        }
    }

    /** 3.1 A2: all Diagnostics rows, for the Device & Data hub. Empty when no fetch has run. */
    suspend fun allDiagnostics(): List<ApiDiagnosticsRow> =
        try {
            diagnosticsDao?.all().orEmpty()
        } catch (t: Throwable) {
            emptyList()
        }

    /**
     * Cache-first flow for one cache [key]. [parse] converts the raw
     * cached JSON to the card's model (return null on unparseable JSON
     * — the row is then treated as absent, never a crash). [fetch]
     * performs the network request and returns its [ApiResult]; it is
     * called only when the row is missing or stale.
     */
    fun <T> cacheFirstFlow(
        api: ApiId,
        key: String,
        parse: (String) -> T?,
        fetch: suspend () -> ApiResult,
    ): Flow<CachedState<T>> = flow {
        var row = dao.get(key)
        var data = row?.json?.let { runCatching { parse(it) }.getOrNull() }
        var lastUpdated = row?.updatedAtMs ?: 0L
        emit(
            CachedState(
                data = data, isRefreshing = false, lastUpdatedMs = lastUpdated, isOffline = false,
                cardStatus = cardStatus(api, row, System.currentTimeMillis()),
            ),
        )
        // A row that no longer parses is stale even when fresh: a
        // worker may have stored an unparseable 200 body (shape
        // change), and that row must not block refetch until max age.
        val unparseableRow = row != null && data == null
        if (!unparseableRow && !ApiCachePolicy.isStale(api, row, System.currentTimeMillis())) return@flow
        emit(
            CachedState(
                data = data, isRefreshing = true, lastUpdatedMs = lastUpdated, isOffline = false,
                cardStatus = cardStatus(api, row, System.currentTimeMillis()),
            ),
        )
        when (val result = runCatching { fetch() }.getOrElse { ApiResult.Failure(ApiErrorKind.OFFLINE) }) {
            is ApiResult.Success -> {
                // Parse BEFORE upserting: an unparseable 200 body must
                // never overwrite (poison) the cache. On parse failure
                // the old row and its timestamp stand, so the card
                // keeps showing old data and retries on next view.
                val parsed = runCatching { parse(result.body) }.getOrNull()
                if (parsed != null) {
                    val now = System.currentTimeMillis()
                    dao.upsert(ApiCacheRow(key, result.body, now))
                    recordDiagnostic(api, "SUCCESS", 200)
                    emit(
                        CachedState(
                            data = parsed, isRefreshing = false, lastUpdatedMs = now, isOffline = false,
                            cardStatus = CardStatus.Fresh,
                            failureClass = null,
                        ),
                    )
                } else {
                    // 3.1 A2: a 200 body that does not match the
                    // expected shape is a ParseError, never Offline.
                    // The snippet lets the developer confirm the live
                    // shape on a real network.
                    recordDiagnostic(api, ApiFailureClass.PARSE_ERROR.name, 200, result.body)
                    emit(
                        CachedState(
                            data = data, isRefreshing = false, lastUpdatedMs = lastUpdated, isOffline = false,
                            cardStatus = cardStatus(api, row, System.currentTimeMillis()),
                            failureClass = ApiFailureClass.PARSE_ERROR,
                        ),
                    )
                }
            }
            is ApiResult.Failure -> {
                val failureClass =
                    classifyFailure(result.kind, result.httpCode, hasValidatedInternet())
                recordDiagnostic(api, failureClass.name, result.httpCode)
                emit(
                    CachedState(
                        data = data,
                        isRefreshing = false,
                        lastUpdatedMs = lastUpdated,
                        isOffline = result.kind != ApiErrorKind.NOT_FOUND,
                        notFound = result.kind == ApiErrorKind.NOT_FOUND,
                        cardStatus = cardStatus(api, row, System.currentTimeMillis()),
                        failureClass = failureClass,
                    ),
                )
            }
        }
    }

    /**
     * Dictionary lookup: on-demand only, cached by word, never
     * periodically refreshed. A cached word is returned without any
     * network call; a miss fetches once. 404 surfaces as
     * [CachedState.notFound] = true (word not found, not a crash).
     */
    fun dictionaryFlow(word: String): Flow<CachedState<String>> {
        val key = ApiCacheKeys.dictionary(word)
        return cacheFirstFlow(ApiId.DICTIONARY, key, { it }, {
            ApiClient.get(ApiId.DICTIONARY, ApiEndpoints.dictionary(word))
        })
    }

    /**
     * Worker path: refresh one API's cache row(s) from [state].
     * Returns false when the API is toggled off or has nothing it can
     * fetch (CoinGecko with no Demo key set; dictionary, which has no
     * periodic refresh). Never throws. Success upserts a fresh row
     * (failure counter reset to 0 by the default); a failed fetch
     * keeps the old payload and timestamp and bumps ONLY
     * consecutiveFailures (§7, diagnostics only — the card status
     * line is driven by cache age, not by this counter). No row →
     * nothing to bump (EmptyOffline stays row-less). Toggle-off and
     * no-key gates return before any row is touched, so they are
     * never counted as fetch failures.
     */
    suspend fun refreshApi(api: ApiId, state: SettingsState): Boolean {
        if (!ApiToggles.isEnabled(state, api)) return false
        val now = System.currentTimeMillis()
        suspend fun store(key: String, result: ApiResult): Boolean {
            if (result is ApiResult.Success) {
                dao.upsert(ApiCacheRow(key, result.body, now))
                recordDiagnostic(api, "SUCCESS", 200)
                return true
            }
            // Success returned above; the sealed result has no other
            // subtype, so a non-Failure here can only mean a future
            // subtype — fail closed (keep cache, no diagnostic write
            // for a shape we do not understand).
            val failure = result as? ApiResult.Failure ?: return false
            val failureClass =
                classifyFailure(failure.kind, failure.httpCode, hasValidatedInternet())
            recordDiagnostic(api, failureClass.name, failure.httpCode)
            val existing = dao.get(key)
            if (existing != null) {
                dao.upsert(existing.copy(consecutiveFailures = existing.consecutiveFailures + 1))
            }
            return false
        }
        return when (api) {
            ApiId.QUOTE -> store(
                ApiCacheKeys.quote(),
                ApiClient.get(ApiId.QUOTE, ApiEndpoints.quoteRandom()),
            )
            ApiId.SCORES -> {
                // Cricket and football are independent rows; one failing
                // must not wipe the other's fresh cache.
                val cricket = store(
                    ApiCacheKeys.scores("cricket"),
                    ApiClient.get(ApiId.SCORES, ApiEndpoints.scores("cricket")),
                )
                val football = store(
                    ApiCacheKeys.scores("football"),
                    ApiClient.get(ApiId.SCORES, ApiEndpoints.scores("football")),
                )
                cricket || football
            }
            ApiId.CRYPTO -> {
                // Key-gated: without a user-supplied Demo key, do not
                // burn the shared unauthenticated quota.
                if (state.coingeckoKey.isBlank()) return false
                store(
                    ApiCacheKeys.crypto(),
                    ApiClient.get(
                        ApiId.CRYPTO,
                        ApiEndpoints.cryptoPrice(),
                        ApiEndpoints.cryptoHeaders(state.coingeckoKey),
                    ),
                )
            }
            ApiId.APOD -> {
                val key = state.nasaKey.ifBlank { ApiEndpoints.NASA_DEMO_KEY }
                store(ApiCacheKeys.apod(), ApiClient.get(ApiId.APOD, ApiEndpoints.apod(key)))
            }
            ApiId.GITHUB -> {
                val user = state.githubUsername.ifBlank { "Voidwalker-28" }
                val headers = ApiEndpoints.githubHeaders()
                val repos = store(
                    ApiCacheKeys.githubRepos(user),
                    ApiClient.get(ApiId.GITHUB, ApiEndpoints.githubRepos(user), headers),
                )
                val events = store(
                    ApiCacheKeys.githubEvents(user),
                    ApiClient.get(ApiId.GITHUB, ApiEndpoints.githubEvents(user), headers),
                )
                repos || events
            }
            ApiId.CURRENCY -> store(
                ApiCacheKeys.currency("usd"),
                ApiClient.getCurrency("usd"),
            )
            ApiId.DICTIONARY -> false // on-demand only, no periodic refresh
        }
    }
}
