# API INTEGRATION — HavenOS v2

Short companion to COVERAGE.md (v2 M1 section). COVERAGE is the
per-API record; this file is the architecture + rules + checklist.

## Architecture (v2 M1, as built)

- **Single HTTP surface — `ApiClient`** (`data/network/ApiClient.kt`).
  Raw `HttpURLConnection`, no new network dependencies (same pattern as
  `WeatherNet`). Every outcome is a typed `ApiResult` (Success / Failure
  with OFFLINE / RATE_LIMITED / NOT_FOUND / OTHER) — no exception ever
  escapes to the UI.
- **Allowlist — `ApiHosts`** (same file). One exact host set per API,
  HTTPS only, checked BEFORE any connection is opened. A wrong host is
  refused, not fetched. Currency is the only API with two hosts
  (primary CDN + official fallback); `ApiClient.getCurrency` tries the
  primary first and the fallback on any failure.
- **Endpoints — `ApiEndpoints`** (same file). URL builders only — they
  never fetch, so a wrong URL is still caught by the allowlist check at
  request time. See COVERAGE.md for the verified per-API table.
- **Cache — Room `api_cache`** (`data/network/ApiCache.kt`). One row per
  (API, parameter) fetch: key format `"<api>"` or
  `"<api>:<lowercase-param>"` (e.g. `scores:cricket`,
  `dictionary:serendipity`). `ApiCachePolicy.maxAgeMs` sets the policy:
  quote 24h, scores 15min, crypto 30min, APOD 24h, GitHub 1h, currency
  24h, dictionary `Long.MAX_VALUE` (cached by word, never stale).
- **Store — `ApiStore`** (`data/network/ApiStore.kt`). `cacheFirstFlow`
  is the one flow every card/row uses: emit cache immediately, fetch
  when missing or stale, store on success, keep stale cache on failure
  with an offline flag. `NOT_FOUND` is not offline (it is the
  dictionary's "no definition" case).
- **Workers — `ApiRefreshWorker` + `ApiRefreshScheduler`**
  (`data/network/ApiWorkers.kt`). One worker class parameterized by API,
  one unique periodic job per API (cadences = the cache policies above;
  dictionary has none — on-demand only). Each run reads persisted
  Settings FIRST and returns success-without-fetching when its toggle
  is off; failures also return success so WorkManager never
  retry-storms rate limits. Jobs are enqueued KEEP on launch, require
  network, and carry no charging/idle constraints.
- **UI — Today cards + search rows + APIs hub.** Cards live in
  `ui/TodayApiCards.kt` (Today page + Today rail), search rows in
  `ui/Drawer.kt` (`searchExtrasItems`: currency + dictionary), toggles
  and key fields in `ui/settings/hubs/ApiHub.kt`. Weather (Open-Meteo)
  is untouched by this layer and stays on its own pre-existing path.

## Toggle + cache rules

- Every API has its own toggle in Settings → APIs. A toggle gates BOTH
  its worker and its card/row — off means no fetch, period.
- Cache-first, always: stale cache stays on screen with an offline /
  last-updated label; no cache + no network is an honest empty/offline
  state, never a spinner and never invented data.
- 429 = back off, keep showing cache. 404 = dictionary "no definition"
  only (silent: the row does not render). Any other failure keeps the
  last good cache until the next scheduled run.
- The APIs hub's panel Reset restores the 7 toggles only (not the key
  fields).

## Key handling rules

- User-entered keys live in DataStore only (`coingeckoKey`, `nasaKey`),
  never in code, never committed, never shipped in a build.
- Crypto is NEVER fetched without a user-supplied CoinGecko Demo key:
  the store refuses the fetch and the card shows the key-needed state.
  Crypto defaults OFF.
- NASA's DEMO_KEY is the one built-in value, and it is a published demo
  identifier, not a secret. A user-entered personal key overrides it.
- GitHub uses no key at all: public data only, never private data,
  never any token. The username field is just a username.
- No analytics, no telemetry, no tracking of API use anywhere in the
  v2 network files or the APIs hub.

## How to add the next API — checklist

1. **Catalogue entry** — confirm the API in the public-apis catalogue
   (source of the v2 list) and record host, auth, endpoint, and any
   free-tier limits/attribution requirement in COVERAGE.md first.
2. **Verify auth/endpoint live** — confirm the endpoint and response
   shape with a real fetch before wiring UI; if the shape is
   unconfirmed, ship the honest offline/empty state, never placeholder
   data.
3. **Allowlist** — add the exact host(s) to `ApiHosts`; HTTPS only.
4. **Cache key + policy** — add the key to `ApiCacheKeys` and the max
   age to `ApiCachePolicy` (match the data's real freshness, not a
   guess: daily data gets a daily cache).
5. **Toggle** — add the `api*` boolean to SettingsState + DataStore,
   the `ApiToggles` mapping, and the row in the APIs hub. Default OFF
   if the API needs a user-supplied key, otherwise ON.
6. **Worker** — add the cadence to `ApiRefreshScheduler` (or document
   why the API is on-demand only, like dictionary). The worker must
   check the toggle before fetching and must never retry-storm.
7. **Card/row** — build the Today card and/or search row on
   `cacheFirstFlow`, with last-updated/offline states and an honest
   empty state. Render any required attribution (e.g. "Powered by …")
   in every state, and never present stale/daily data as live.
8. **Docs entry** — add the COVERAGE.md row (host, auth, endpoint,
   cache, limits, honesty notes) in the same change.
9. **Device checklist entry** — add the Pad checks (toggle on/off,
   offline-with-cache, offline-without-cache, key-needed state if any)
   to DEVICE_CHECKLIST.md; the API is not "done" until those pass.
