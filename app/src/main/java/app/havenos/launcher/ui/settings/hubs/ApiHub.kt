package app.havenos.launcher.ui.settings.hubs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.network.ApiId
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
        scope.launch { settings.setApiEnabled(api.name, on) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
        HubTopBar(onBack = onBack, title = "APIs")
        Spacer(Modifier.height(4.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            ShellSectionLabel("Online sources")
            ShellSwitchRow(
                title = "Quote of the day",
                checked = state.apiQuote,
                onCheckedChange = { toggle(ApiId.QUOTE, it) },
                help = "One quote a day, saved on device.",
            )
            ShellSwitchRow(
                title = "Live scores",
                checked = state.apiScores,
                onCheckedChange = { toggle(ApiId.SCORES, it) },
                help = "Cricket and football scores.",
            )
            ShellSwitchRow(
                title = "Crypto prices",
                checked = state.apiCrypto,
                onCheckedChange = { toggle(ApiId.CRYPTO, it) },
                help = "Needs your free CoinGecko key below.",
            )
            ShellSwitchRow(
                title = "NASA picture of the day",
                checked = state.apiApod,
                onCheckedChange = { toggle(ApiId.APOD, it) },
                help = "Daily space picture, saved on device.",
            )
            ShellSwitchRow(
                title = "GitHub activity",
                checked = state.apiGithub,
                onCheckedChange = { toggle(ApiId.GITHUB, it) },
                help = "Public repos and events, not live.",
            )
            ShellSwitchRow(
                title = "Currency rates",
                checked = state.apiCurrency,
                onCheckedChange = { toggle(ApiId.CURRENCY, it) },
                help = "Daily exchange rates, saved on device.",
            )
            ShellSwitchRow(
                title = "Dictionary",
                checked = state.apiDictionary,
                onCheckedChange = { toggle(ApiId.DICTIONARY, it) },
                help = "Word lookup, fetched on demand only.",
            )
            Spacer(Modifier.height(12.dp))
            ShellSectionLabel("Keys and account")
            ApiKeyField(
                label = "CoinGecko Demo key",
                value = state.coingeckoKey,
                masked = true,
                help = "Your free Demo key. Blank means crypto stays off.",
                onSave = { scope.launch { settings.setCoingeckoKey(it) } },
            )
            ApiKeyField(
                label = "NASA personal key",
                value = state.nasaKey,
                masked = true,
                help = "Blank uses the public demo key.",
                onSave = { scope.launch { settings.setNasaKey(it) } },
            )
            ApiKeyField(
                label = "GitHub username",
                value = state.githubUsername,
                masked = false,
                help = "Whose public repos the card shows.",
                onSave = { scope.launch { settings.setGithubUsername(it) } },
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

/** Text field with an explicit Save: typing never writes until saved. */
@Composable
private fun ApiKeyField(
    label: String,
    value: String,
    masked: Boolean,
    help: String,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(value) }
    // Re-sync when the stored value changes elsewhere (e.g. a reset).
    LaunchedEffect(value) { if (text != value) text = value }
    val changed = text != value
    ShellRow(title = label, value = if (value.isBlank()) "Not set" else "Set", help = help) {
        Column {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                visualTransformation = if (masked) PasswordVisualTransformation()
                    else VisualTransformation.None,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(enabled = changed, onClick = { onSave(text) }) {
                Text(text = "Save")
            }
        }
    }
}
