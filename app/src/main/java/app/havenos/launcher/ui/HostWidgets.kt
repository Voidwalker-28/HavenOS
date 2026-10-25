package app.havenos.launcher.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.HomeItem
import app.havenos.launcher.data.ItemKind
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.theme.LocalState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One entry in the fixed registry of HavenOS's own widgets. Spans are half-cell units. */
data class OwnWidgetDef(
    val label: String,
    val kind: Int,
    val colSpan: Int,
    val rowSpan: Int,
    val payload: String,
)

// Spans are half-cell units and ALWAYS even (whole cells only — Doc 3
// §2.3). The old 8×5 / 8×3 spans were half-cell tall and are fixed here.
val OWN_WIDGETS: List<OwnWidgetDef> = listOf(
    OwnWidgetDef("Clock — Serif", ItemKind.CLOCK, 8, 4, "serif"),
    OwnWidgetDef("Clock — Analog", ItemKind.CLOCK, 8, 4, "analog"),
    OwnWidgetDef("Clock — Digital", ItemKind.CLOCK, 8, 4, "digital"),
    OwnWidgetDef("Weather", ItemKind.WEATHER, 8, 4, ""),
    OwnWidgetDef("Agenda", ItemKind.AGENDA, 8, 4, ""),
    OwnWidgetDef("Calendar", ItemKind.CALENDAR, 8, 8, ""),
    OwnWidgetDef("Now Playing", ItemKind.NOW_PLAYING, 8, 4, ""),
    OwnWidgetDef("Storage", ItemKind.STORAGE, 8, 4, ""),
    OwnWidgetDef("Countdown", ItemKind.COUNTDOWN, 8, 4, ""),
    OwnWidgetDef("Tasks", ItemKind.TASKS, 8, 4, ""),
    OwnWidgetDef("Notes", ItemKind.NOTES, 8, 4, ""),
    OwnWidgetDef("Gallery", ItemKind.GALLERY, 8, 6, ""),
    OwnWidgetDef("Quick Tiles", ItemKind.QUICK_TILES, 12, 2, ""),
    OwnWidgetDef("Suggested", ItemKind.SUGGESTED, 8, 4, ""),
    OwnWidgetDef("Battery", ItemKind.BATTERY, 8, 4, ""),
    OwnWidgetDef("Quick search", ItemKind.QUICK_SEARCH, 8, 2, ""),
    OwnWidgetDef("At a glance", ItemKind.AGENDA, 8, 4, "glance"),
    OwnWidgetDef("Stack", ItemKind.STACK, 8, 4, "[2,3]"),
)

/** Display a span in units as cells, e.g. 5 units -> "2.5". */
internal fun fmtUnits(units: Int): String =
    if (units % 2 == 0) "${units / 2}" else "${units / 2}.5"

/** Render a hosted third-party widget (ItemKind.TP_WIDGET, payload = appWidgetId).
 * Failure never crashes and never blanks: a provider that is gone renders
 * "App removed · Remove widget"; a view that fails to create renders
 * "Problem loading widget · Tap to reload" and rebuilds on tap. Corners
 * clip to the 28dp frame radius; the frame background follows the
 * existing Home & Dock row (Follow look / Translucent = glass, Solid =
 * solid surface). Updates pause when the screen is off because
 * MainActivity stops the shared host listener on pause; the one shared
 * AppWidgetHost is the concurrency limit — views are only created for
 * items the pager actually renders. */
@Composable
fun TpWidgetView(
    app: HavenApp,
    item: HomeItem,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
) {
    val state = LocalState.current
    val scope = rememberCoroutineScope()
    val id = item.payload.toIntOrNull()
    var reloadKey by remember { mutableStateOf(0) }
    var loadFailed by remember { mutableStateOf(false) }
    val info = remember(id, reloadKey) {
        if (id == null) {
            null
        } else {
            try {
                app.widgetManager.getAppWidgetInfo(id)
            } catch (t: Throwable) {
                null
            }
        }
    }
    if (id == null || info == null) {
        WidgetProviderRemoved(
            onRemove = {
                if (onRemove != null) {
                    onRemove()
                } else {
                    scope.launch {
                        try {
                            if (id != null) app.widgetHost.deleteAppWidgetId(id)
                        } catch (t: Throwable) {
                            // already gone
                        }
                        val remaining = app.db.homeItems().all().filter { it.id != item.id }
                        app.db.homeItems().replaceAll(remaining)
                    }
                }
            },
            modifier = modifier,
        )
        return
    }
    if (loadFailed) {
        WidgetTpError(
            onReload = { loadFailed = false; reloadKey++ },
            modifier = modifier,
        )
        return
    }
    val hostView: @Composable (Modifier) -> Unit = { m ->
        AndroidView(
            factory = { ctx ->
                try {
                    app.widgetHost.createView(ctx, id, info)
                } catch (t: Throwable) {
                    TextView(ctx).apply {
                        text = "Problem loading widget"
                        post { loadFailed = true }
                    }
                }
            },
            update = { view ->
                try {
                    val wDp = item.colSpan * 36
                    val hDp = item.rowSpan * 36
                    val opts = Bundle().apply {
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, wDp)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, hDp)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, wDp)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, hDp)
                    }
                    if (view is android.appwidget.AppWidgetHostView) {
                        view.updateAppWidgetSize(opts, wDp, hDp, wDp, hDp)
                    } else if (view is TextView) {
                        view.post { loadFailed = true }
                    }
                } catch (t: Throwable) {
                    // size hints are best-effort
                }
            },
            modifier = m,
        )
    }
    if (state.widgetBg == 1) {
        Box(
            Modifier.then(modifier).clip(RoundedCornerShape(28.dp))
                .background(LocalPalette.current.surface),
        ) {
            hostView(Modifier.fillMaxSize())
        }
    } else {
        GlassSurface(
            shape = RoundedCornerShape(28.dp),
            modifier = modifier.clip(RoundedCornerShape(28.dp)),
        ) {
            hostView(Modifier.fillMaxSize())
        }
    }
}

/**
 * Widget picker sheet — ENTRY POINT FOR WORKER A (edit toolbar).
 * Call this composable with the current grid geometry; [onPlace] receives
 * the chosen kind/span/payload and the host places it (first free spot,
 * current page first — see Home.placeWidget). [onDragWidget] is the
 * exact-placement seam: long-pressing an own-widget row reports the
 * chosen widget and dismisses, so the host can start its drag with a home
 * ghost; the default host wires it to the same tap-place path until
 * Worker A's ghost drag lands. Third-party rows have no drag path yet
 * (they must bind first), so long-press on them only selects.
 *
 * Layout: left list (HavenOS widgets FIRST, then installed apps with
 * widgets, searchable) + right preview with size chips (only
 * provider-allowed sizes) and live previews (built-ins render real data).
 * On narrow screens the preview sits under the list. Tap Add = first free
 * spot; cancel/dismiss = nothing added.
 * [cellDp] is the current width of one grid cell in dp (span derivation).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWidgetSheet(
    app: HavenApp,
    cellDp: Int,
    colsU: Int,
    rowsU: Int,
    onDismiss: () -> Unit,
    onPlace: (kind: Int, colSpan: Int, rowSpan: Int, payload: String) -> Unit,
    onDragWidget: (kind: Int, colSpan: Int, rowSpan: Int, payload: String) -> Unit = { _, _, _, _ -> },
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val pm = context.packageManager
    val sheetState = rememberModalBottomSheetState()

    val providers = remember {
        try {
            app.widgetManager.installedProviders
        } catch (t: Throwable) {
            emptyList()
        }
    }
    val grouped = remember(providers) { providers.groupBy { it.provider.packageName } }

    var pickerQuery by remember { mutableStateOf("") }

    fun appLabelOf(pkg: String): String = try {
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (t: Throwable) {
        pkg
    }

    fun providerLabelOf(info: AppWidgetProviderInfo): String = try {
        info.loadLabel(pm).ifBlank { info.provider.className }
    } catch (t: Throwable) {
        info.provider.className
    }

    val q = pickerQuery.trim()
    val ownMatches = remember(q) {
        if (q.isEmpty()) {
            OWN_WIDGETS
        } else {
            OWN_WIDGETS.filter { it.label.contains(q, ignoreCase = true) }
        }
    }
    val providerMatches = remember(q, grouped) {
        grouped.mapNotNull { (pkg, infos) ->
            val appLabel = appLabelOf(pkg)
            val matched = if (q.isEmpty()) {
                infos
            } else {
                infos.filter {
                    providerLabelOf(it).contains(q, ignoreCase = true) ||
                        appLabel.contains(q, ignoreCase = true)
                }
            }
            if (matched.isEmpty()) null else Triple(pkg, appLabel, matched)
        }
    }

    var selectedOwn by remember { mutableStateOf<OwnWidgetDef?>(OWN_WIDGETS.firstOrNull()) }
    var selectedProvider by remember { mutableStateOf<AppWidgetProviderInfo?>(null) }
    val effOwn = selectedOwn?.takeIf { it in ownMatches }
    val effProvider = selectedProvider?.takeIf { p ->
        providerMatches.any { (_, _, infos) -> p in infos }
    }
    val selectionKey = effOwn?.label ?: effProvider?.provider?.flattenToString() ?: "none"

    fun minCellsOf(info: AppWidgetProviderInfo): Pair<Int, Int> {
        if (info.minWidth <= 0 || info.minHeight <= 0 || cellDp <= 0) return 4 to 2
        val w = (info.minWidth + cellDp - 1) / cellDp
        val h = (info.minHeight + cellDp - 1) / cellDp
        return w.coerceAtLeast(1) to h.coerceAtLeast(1)
    }

    fun spanFor(info: AppWidgetProviderInfo): Pair<Int, Int> {
        val (w, h) = minCellsOf(info)
        return (w * 2).coerceIn(2, colsU.coerceAtLeast(2)) to
            (h * 2).coerceIn(2, rowsU.coerceAtLeast(2))
    }

    val selectedSizes: List<Pair<Int, Int>> = when {
        effProvider != null -> {
            val (mw, mh) = minCellsOf(effProvider)
            allowedCellSizes(mw, mh, effProvider.resizeMode)
        }
        effOwn != null -> builtinWidgetSizes(effOwn.kind)
        else -> emptyList()
    }
    var sizeOverride by remember(selectionKey) { mutableStateOf<Pair<Int, Int>?>(null) }
    val selectedSize = sizeOverride ?: selectedSizes.firstOrNull() ?: (4 to 2)

    var pendingId by remember { mutableStateOf<Int?>(null) }
    var pendingInfo by remember { mutableStateOf<AppWidgetProviderInfo?>(null) }
    var pendingSizeUnits by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var bindError by remember { mutableStateOf<String?>(null) }

    fun finishPlace() {
        val id = pendingId
        val info = pendingInfo
        val sizeUnits = pendingSizeUnits
        pendingId = null
        pendingInfo = null
        pendingSizeUnits = null
        if (id != null && info != null) {
            val (w, h) = sizeUnits ?: spanFor(info)
            onPlace(ItemKind.TP_WIDGET, w, h, id.toString())
        }
        onDismiss()
    }

    fun cancelPending() {
        pendingId?.let { id ->
            try {
                app.widgetHost.deleteAppWidgetId(id)
            } catch (t: Throwable) {
                // already gone
            }
        }
        pendingId = null
        pendingInfo = null
        pendingSizeUnits = null
        bindError = "Widget could not be added — tap it again to retry."
    }

    val configureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            finishPlace()
        } else {
            cancelPending()
        }
    }

    val bindLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = pendingId
        val info = pendingInfo
        if (result.resultCode == Activity.RESULT_OK && id != null && info != null) {
            val configure = info.configure
            if (configure != null) {
                try {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .setComponent(configure)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    configureLauncher.launch(intent)
                } catch (t: Throwable) {
                    cancelPending()
                }
            } else {
                finishPlace()
            }
        } else {
            cancelPending()
        }
    }

    fun startBind(info: AppWidgetProviderInfo, sizeCells: Pair<Int, Int>) {
        bindError = null
        val id = try {
            app.widgetHost.allocateAppWidgetId()
        } catch (t: Throwable) {
            bindError = "Could not allocate a widget id — try again."
            return
        }
        pendingId = id
        pendingInfo = info
        pendingSizeUnits = sizeCells.first * 2 to sizeCells.second * 2
        val allowed = try {
            app.widgetManager.bindAppWidgetIdIfAllowed(id, info.provider)
        } catch (t: Throwable) {
            false
        }
        if (allowed) {
            val configure = info.configure
            if (configure != null) {
                try {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .setComponent(configure)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    configureLauncher.launch(intent)
                } catch (t: Throwable) {
                    cancelPending()
                }
            } else {
                finishPlace()
            }
        } else {
            try {
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                bindLauncher.launch(intent)
            } catch (t: Throwable) {
                cancelPending()
            }
        }
    }

    fun placeSelected() {
        val own = effOwn
        val provider = effProvider
        when {
            own != null -> {
                onPlace(own.kind, selectedSize.first * 2, selectedSize.second * 2, own.payload)
                onDismiss()
            }
            provider != null -> startBind(provider, selectedSize)
            else -> Unit
        }
    }

    fun dragOwn(own: OwnWidgetDef) {
        val size = builtinWidgetSizes(own.kind).firstOrNull() ?: (4 to 2)
        onDragWidget(own.kind, size.first * 2, size.second * 2, own.payload)
        onDismiss()
    }

    @Composable
    fun previewPane() {
        Column(Modifier.fillMaxWidth()) {
            val own = effOwn
            val provider = effProvider
            when {
                own != null -> {
                    Box(Modifier.fillMaxWidth().height(120.dp)) {
                        WidgetContent(
                            app = app,
                            item = HomeItem(
                                id = "preview:" + own.label,
                                kind = own.kind,
                                page = 0,
                                col = 0,
                                row = 0,
                                colSpan = own.colSpan,
                                rowSpan = own.rowSpan,
                                payload = own.payload,
                            ),
                            appsByKey = emptyMap(),
                            onLaunch = {},
                            modifier = Modifier.fillMaxSize(),
                            editMode = false,
                        )
                        // Shield: preview-internal buttons never receive touches.
                        Box(Modifier.matchParentSize())
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(own.label, color = palette.onSurface, fontSize = 15.sp, maxLines = 1)
                }
                provider != null -> {
                    ProviderWidgetCard(
                        info = provider,
                        label = providerLabelOf(provider),
                        sizeText = "",
                        onSelect = {},
                    )
                }
                else -> {
                    Text("Select a widget", color = palette.muted, fontSize = 13.sp)
                }
            }
            if (selectedSizes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Size", color = palette.muted, fontSize = 12.sp)
                    for (size in selectedSizes) {
                        val selected = size == selectedSize
                        Box(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) palette.accent.copy(alpha = 0.25f)
                                    else palette.glassBorder,
                                )
                                .clickable { sizeOverride = size }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${size.first} × ${size.second}",
                                color = if (selected) palette.accent else palette.onSurface,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            androidx.compose.material3.Button(
                onClick = { placeSelected() },
                enabled = effOwn != null || effProvider != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add widget") }
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap adds to the first free spot; long-press an own widget to place it exactly.",
                color = palette.muted, fontSize = 11.sp,
            )
        }
    }

    @Composable
    fun listPane(listModifier: Modifier) {
        LazyColumn(listModifier) {
            if (ownMatches.isNotEmpty()) {
                item {
                    Text(
                        "HavenOS widgets", color = palette.accent, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(ownMatches, key = { it.label }) { def ->
                    OwnWidgetCard(
                        app = app,
                        def = def,
                        onLongPress = { dragOwn(def) },
                    ) {
                        selectedOwn = def
                        selectedProvider = null
                    }
                }
            }
            for ((_, appLabel, infos) in providerMatches) {
                item {
                    Text(
                        appLabel, color = palette.accent, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(infos, key = { it.provider.flattenToString() }) { info ->
                    val (w, h) = spanFor(info)
                    ProviderWidgetCard(
                        info = info,
                        label = providerLabelOf(info),
                        sizeText = "${fmtUnits(w)} × ${fmtUnits(h)} cells",
                    ) {
                        selectedProvider = info
                        selectedOwn = null
                    }
                }
            }
            if (ownMatches.isEmpty() && providerMatches.isEmpty()) {
                item {
                    Text(
                        "No widgets match", color = palette.muted, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Add widget", color = palette.onSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pickerQuery,
                onValueChange = { pickerQuery = it },
                placeholder = { Text("Search widgets") },
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            if (bindError != null) {
                Text(
                    bindError!!, color = palette.accent, fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            BoxWithConstraints(Modifier.fillMaxWidth().height(560.dp)) {
                if (maxWidth >= 600.dp) {
                    Row(Modifier.fillMaxSize()) {
                        listPane(Modifier.weight(0.45f).padding(end = 16.dp))
                        Box(Modifier.weight(0.55f)) { previewPane() }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        listPane(Modifier.weight(1f))
                        Spacer(Modifier.height(12.dp))
                        previewPane()
                    }
                }
            }
        }
    }
}

/** Own-widget picker card: the real widget rendered live as its own preview. */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun OwnWidgetCard(
    app: HavenApp,
    def: OwnWidgetDef,
    onLongPress: (() -> Unit)? = null,
    onSelect: () -> Unit,
) {
    val palette = LocalPalette.current
    Column(
        Modifier.fillMaxWidth()
            .combinedClickable(
                onClick = { onSelect() },
                onLongClick = onLongPress,
            )
            .padding(vertical = 6.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(104.dp)) {
            // shortcut: previews render the real widget; stack previews render a live member preview
            WidgetContent(
                app = app,
                item = HomeItem(
                    id = "preview:" + def.label,
                    kind = def.kind,
                    page = 0,
                    col = 0,
                    row = 0,
                    colSpan = def.colSpan,
                    rowSpan = def.rowSpan,
                    payload = def.payload,
                ),
                appsByKey = emptyMap(),
                onLaunch = {},
                modifier = Modifier.fillMaxSize(),
                editMode = false,
            )
            // Shield on top: preview-internal buttons never receive touches; the whole card selects.
            Box(Modifier.matchParentSize().clickable { onSelect() })
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(def.label, color = palette.onSurface, fontSize = 15.sp, maxLines = 1,
                modifier = Modifier.weight(1f))
            Text("${fmtUnits(def.colSpan)} × ${fmtUnits(def.rowSpan)} cells",
                color = palette.muted, fontSize = 12.sp)
        }
    }
}

/** System-widget picker card: provider preview image, falling back to its icon, then its label. */
@Composable
private fun ProviderWidgetCard(
    info: AppWidgetProviderInfo,
    label: String,
    sizeText: String,
    onSelect: () -> Unit,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    // Drawable loads can block on provider I/O: resolve preview, else icon, off the main thread.
    val loaded by produceState<Pair<Drawable, Boolean>?>(initialValue = null, info) {
        value = withContext(Dispatchers.IO) {
            val p = try {
                info.loadPreviewImage(context, 0)
            } catch (t: Throwable) {
                null
            }
            if (p != null) {
                p to true
            } else {
                val i = try {
                    info.loadIcon(context, 0)
                } catch (t: Throwable) {
                    null
                }
                i?.let { it to false }
            }
        }
    }
    val preview = loaded?.takeIf { it.second }?.first
    val icon = loaded?.takeUnless { it.second }?.first
    Column(Modifier.fillMaxWidth().clickable { onSelect() }.padding(vertical = 6.dp)) {
        Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
            when {
                preview != null -> AndroidView(
                    factory = { ctx -> ImageView(ctx).apply { setImageDrawable(preview) } },
                    modifier = Modifier.fillMaxSize(),
                )
                icon != null -> AndroidView(
                    factory = { ctx -> ImageView(ctx).apply { setImageDrawable(icon) } },
                    modifier = Modifier.size(48.dp),
                )
                else -> Text(label, color = palette.onSurface, fontSize = 15.sp)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = palette.onSurface, fontSize = 15.sp, maxLines = 1,
                modifier = Modifier.weight(1f))
            Text(sizeText, color = palette.muted, fontSize = 12.sp)
        }
    }
}
