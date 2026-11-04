package app.havenos.launcher.ui.settings.hubs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.network.ApiDiagnosticsRow
import app.havenos.launcher.data.network.ApiId
import app.havenos.launcher.data.network.ApiRefreshScheduler
import app.havenos.launcher.data.network.lastUpdatedLabel
import app.havenos.launcher.ui.glass.GroupedSection
import app.havenos.launcher.ui.glass.HavenHelp
import app.havenos.launcher.ui.settings.shell.DeepLinkAnchor
import app.havenos.launcher.ui.settings.shell.SearchEntry
import app.havenos.launcher.ui.settings.shell.ShellAdvancedFold
import app.havenos.launcher.ui.settings.shell.ShellRow
import app.havenos.launcher.ui.settings.shell.ShellSectionLabel
import app.havenos.launcher.ui.settings.shell.ShellSwitchRow
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// APIs hub (v2 M1). Honest rows only: one toggle per API, and the three
// input fields the APIs actually need. Every toggle gates BOTH its
// WorkManager worker (ApiRefreshWorker checks the toggle first) and its
// card/row (downstream workers check ApiToggles) — there are no dead
// toggles here. Keys are stored only on-device in DataStore, never
// committed, never uploaded; a blank NASA key means the public DEMO_KEY.
// Crypto stays off until a CoinGecko Demo key is entered, because the
// free tier is key-gated and an on-by-default toggle would fetch
// nothing useful.
// ---------------------------------------------------------------------------

/**
 * 3.1 Phase C: hub status line for one API row — Off, Needs key,
 * Updated HH:MM, or the A2 error text. Null when the API is on with
 * no recorded result yet (the row still shows its On value).
 */
fun apiHubStatusLine(enabled: Boolean, needsKey: Boolean, diag: ApiDiagnosticsRow?): String? {
    if (!enabled) return "Off"
    if (needsKey) return "Needs key"
    if (diag == null) return null
    return when (diag.resultClass) {
        "SUCCESS" -> lastUpdatedLabel(diag.timeMs)
        "OFFLINE" -> "Offline"
        "RATE_LIMITED" -> "Busy"
        else -> "Couldn't load"
    }
}

/**
 * GitHub username shape (public-username rules): letters, digits and
 * hyphens, at most 39 characters, never starting or ending with a
 * hyphen. Blank means unset and is always allowed.
 */
fun githubUsernameError(name: String): String? {
    if (name.isBlank()) return null
    if (name.length > 39) return "Usernames are at most 39 characters."
    if (!name.matches(Regex("^[A-Za-z0-9][A-Za-z0-9-]*$")) || name.endsWith("-")) {
        return "Letters, numbers and hyphens only, not starting or ending with a hyphen."
    }
    return null
}

val ApiSearchEntries: List<SearchEntry> = listOf(
    SearchEntry("Quote of the day", listOf("quote", "daily"), "APIs", "apis"),
    SearchEntry("Live scores", listOf("scores", "sport", "cricket", "football"), "APIs", "apis"),
    SearchEntry("Crypto prices", listOf("crypto", "bitcoin", "ethereum", "coingecko"), "APIs", "apis"),
    SearchEntry("NASA picture of the day", listOf("nasa", "apod", "space", "picture"), "APIs", "apis"),
    SearchEntry("GitHub activity", listOf("github", "repos", "commits"), "APIs", "apis"),
    SearchEntry("Currency rates", listOf("currency", "exchange", "rates"), "APIs", "apis"),
    SearchEntry("Dictionary", listOf("dictionary", "word", "definition"), "APIs", "apis"),
    SearchEntry("CoinGecko Demo key", listOf("coingecko", "key", "api key"), "APIs", "apis"),
    SearchEntry("NASA personal key", listOf("nasa", "key", "api key"), "APIs", "apis"),
    SearchEntry("GitHub username", listOf("github", "username"), "APIs", "apis"),
)

@Composable
fun ApiHubScreen(onBack: () -> Unit, onSpotlight: (String) -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HavenApp
    val settings = app.settings
    val state by settings.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    fun toggle(api: ApiId, on: Boolean) {
        scope.launch {
            settings.setApiEnabled(api.name, on)
            // 3.1 flow 16 / A12: the worker follows the toggle at
            // once (cancel when off, schedule when on), not at next
            // launch. Key edits need no scheduler call: the worker
            // is already scheduled and refreshApi gates on the key
            // at run time.
            try {
                ApiRefreshScheduler.applyWorkerAction(ctx, api, on)
            } catch (t: Throwable) {
                // scheduling is best-effort; cards still read cache
            }
        }
    }

    // 3.1 Phase C: one diagnostics load for every row's status line.
    var diags by remember { mutableStateOf<Map<String, ApiDiagnosticsRow>>(emptyMap()) }
    LaunchedEffect(Unit) {
        diags = try {
            app.apiStore.allDiagnostics().associateBy { it.api }
        } catch (t: Throwable) {
            emptyMap()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            ShellSectionLabel("Online sources")
            ShellSwitchRow(
                title = "Quote of the day",
                checked = state.apiQuote,
                onCheckedChange = { toggle(ApiId.QUOTE, it) },
                help = "One quote a day, saved on device.",
                status = apiHubStatusLine(state.apiQuote, false, diags[ApiId.QUOTE.name]),
            )
            ShellSwitchRow(
                title = "Live scores",
                checked = state.apiScores,
                onCheckedChange = { toggle(ApiId.SCORES, it) },
                help = "Cricket and football scores.",
                status = apiHubStatusLine(state.apiScores, false, diags[ApiId.SCORES.name]),
            )
            ShellSwitchRow(
                title = "Crypto prices",
                checked = state.apiCrypto,
                onCheckedChange = { toggle(ApiId.CRYPTO, it) },
                help = "Needs your free CoinGecko key below.",
                status = apiHubStatusLine(
                    state.apiCrypto,
                    state.coingeckoKey.isBlank(),
                    diags[ApiId.CRYPTO.name],
                ),
            )
            ShellSwitchRow(
                title = "NASA picture of the day",
                checked = state.apiApod,
                onCheckedChange = { toggle(ApiId.APOD, it) },
                help = "Daily space picture, saved on device.",
                status = apiHubStatusLine(state.apiApod, false, diags[ApiId.APOD.name]),
            )
            ShellSwitchRow(
                title = "GitHub activity",
                checked = state.apiGithub,
                onCheckedChange = { toggle(ApiId.GITHUB, it) },
                help = "Public repos and events, not live.",
                status = apiHubStatusLine(state.apiGithub, false, diags[ApiId.GITHUB.name]),
            )
            ShellSwitchRow(
                title = "Currency rates",
                checked = state.apiCurrency,
                onCheckedChange = { toggle(ApiId.CURRENCY, it) },
                help = "Daily exchange rates, saved on device.",
                status = apiHubStatusLine(state.apiCurrency, false, diags[ApiId.CURRENCY.name]),
            )
            ShellSwitchRow(
                title = "Dictionary",
                checked = state.apiDictionary,
                onCheckedChange = { toggle(ApiId.DICTIONARY, it) },
                help = "Word lookup, fetched on demand only.",
                status = apiHubStatusLine(state.apiDictionary, false, diags[ApiId.DICTIONARY.name]),
            )
            Spacer(Modifier.height(12.dp))
            GroupedSection(
                label = "Keys and account",
                rows = listOf(
                    {
                        ApiKeyField(
                            label = "CoinGecko Demo key",
                            value = state.coingeckoKey,
                            masked = true,
                            help = "Your free Demo key. Blank means crypto stays off.",
                            onSave = { scope.launch { settings.setCoingeckoKey(it) } },
                            // 3.1 A3: the crypto card's Add-key button deep-links here.
                            deepLinkField = "coingecko-key",
                        )
                    },
                    {
                        ApiKeyField(
                            label = "NASA personal key",
                            value = state.nasaKey,
                            masked = true,
                            help = "Blank uses the public demo key.",
                            onSave = { scope.launch { settings.setNasaKey(it) } },
                        )
                    },
                    {
                        ApiKeyField(
                            label = "GitHub username",
                            value = state.githubUsername,
                            masked = false,
                            help = "Whose public repos the card shows.",
                            onSave = { scope.launch { settings.setGithubUsername(it) } },
                            validate = ::githubUsernameError,
                        )
                    },
                ),
            )
            Spacer(Modifier.height(12.dp))
            ShellAdvancedFold(title = "About these sources") {
                ShellRow(
                    title = "Keys stay on this device",
                    value = "Never uploaded",
                    help = "Keys are stored only in this app.",
                )
                ShellRow(
                    title = "Offline fallback",
                    value = "Saved data",
                    help = "Cards show saved data when offline.",
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * Text field with an explicit Save: typing never writes until saved.
 * Grouped-section row (no inner card): label + Set/Not set, the field,
 * Paste / Show-Hide / Save, and a validation line. [validate] returns
 * an error string or null; keys have no format to validate, so only
 * the GitHub username passes one.
 */
@Composable
private fun ApiKeyField(
    label: String,
    value: String,
    masked: Boolean,
    help: String,
    onSave: (String) -> Unit,
    // 3.1 A3: when set, a panel deep link naming this field scrolls
    // here and focuses the text field.
    deepLinkField: String? = null,
    validate: (String) -> String? = { null },
) {
    var text by remember { mutableStateOf(value) }
    // Re-sync when the stored value changes elsewhere (e.g. a reset).
    LaunchedEffect(value) { if (text != value) text = value }
    val changed = text != value
    val error = validate(text)
    var shown by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    val row: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            // 3.1.1 (item 5): label ABOVE a full-width field — the
            // old label + status row squeezed the label to vertical
            // text in a narrow detail pane. The status moves to the
            // button row; every button is at least 72dp wide.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    modifier = Modifier.weight(1f),
                )
                HavenHelp(helpText = help)
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                visualTransformation = if (masked && !shown) PasswordVisualTransformation()
                    else VisualTransformation.None,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
            if (error != null) {
                Text(error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        clipboard.getText()?.text?.let { text = it }
                    },
                    modifier = Modifier.widthIn(min = 72.dp).heightIn(min = 48.dp),
                ) { Text(text = "Paste") }
                if (masked) {
                    TextButton(
                        onClick = { shown = !shown },
                        modifier = Modifier.widthIn(min = 72.dp).heightIn(min = 48.dp),
                    ) {
                        Text(text = if (shown) "Hide" else "Show")
                    }
                }
                TextButton(
                    enabled = changed && error == null,
                    onClick = { onSave(text) },
                    modifier = Modifier.widthIn(min = 72.dp).heightIn(min = 48.dp),
                ) {
                    Text(text = "Save")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (value.isBlank()) "Not set" else "Set",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (deepLinkField != null) {
        DeepLinkAnchor(field = deepLinkField, focusRequester = focusRequester) { row() }
    } else {
        row()
    }
}
