package app.havenos.launcher.ui.settings.hubs

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.havenos.launcher.HavenApp
import app.havenos.launcher.data.DEFAULT_GESTURES
import app.havenos.launcher.data.DEFAULT_NAV_MAP
import app.havenos.launcher.data.ICON_SWIPE_ACTIONS
import app.havenos.launcher.data.NAV_ACTIONS
import app.havenos.launcher.data.encodeIconSwipes
import app.havenos.launcher.data.iconSwipeActionName
import app.havenos.launcher.data.parseGestures
import app.havenos.launcher.data.parseIconSwipes
import app.havenos.launcher.data.parseNavMap
import app.havenos.launcher.isNavServiceEnabled
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.abs
import app.havenos.launcher.ui.settings.shell.SearchEntry
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToDown

private fun homeActionName(action: String): String = when (action) {
    "drawer" -> "Open drawer"
    "search" -> "Search"
    "quickPanel" -> "Quick panel"
    "settings" -> "Open settings"
    "lock" -> "Lock screen"
    "shade" -> "Notifications"
    else -> "None"
}

private fun navActionName(action: String): String = when (action) {
    "home" -> "Home"
    "back" -> "Back"
    "recents" -> "Recents"
    "quickPanel" -> "Quick panel"
    else -> "Nothing"
}

private fun homeTriggerName(trigger: String): String = when (trigger) {
    "swipeUp" -> "Swipe up"
    "swipeDown" -> "Swipe down"
    "doubleTap" -> "Double tap"
    "pinchIn" -> "Pinch in"
    "twoFingerDown" -> "Two-finger down"
    else -> trigger
}

private fun navTriggerName(trigger: String): String = when (trigger) {
    "up" -> "Bottom swipe"
    "upHold" -> "Bottom hold"
    "left" -> "Left edge"
    "right" -> "Right edge"
    else -> trigger
}

private fun encodeGestures(map: Map<String, String>): String {
    val obj = JSONObject()
    for (t in listOf("swipeUp", "swipeDown", "doubleTap", "pinchIn", "twoFingerDown")) {
        obj.put(t, map[t] ?: "none")
    }
    return obj.toString()
}

private fun encodeNavMap(map: Map<String, String>): String {
    val obj = JSONObject()
    for (t in listOf("up", "upHold", "left", "right")) obj.put(t, map[t] ?: "none")
    return obj.toString()
}

private sealed interface Selected {
    data class Home(val trigger: String) : Selected
    data class Nav(val trigger: String) : Selected
}

@Composable
fun GesturesMotionHubScreen(
    onBack: () -> Unit,
    onSpotlight: (String) -> Unit,
    onTestAction: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val app = context.applicationContext as HavenApp
    val settings = app.settings
    val state by settings.state.collectAsState()
    val scope = rememberCoroutineScope()
    val landscape =
        LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val gestures = remember(state.gesturesJson) { parseGestures(state.gesturesJson) }
    val navMap = remember(state.navMapJson) { parseNavMap(state.navMapJson) }
    val iconSwipes = remember(state.iconSwipesJson) { parseIconSwipes(state.iconSwipesJson) }
    val apps by app.appRepo.apps.collectAsState()

    var selected by remember { mutableStateOf<Selected?>(null) }
    var detected by remember { mutableStateOf<String?>(null) }
    var pendingLock by remember { mutableStateOf<String?>(null) }
    var pendingMove by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var moreOpen by remember { mutableStateOf(false) }
    var fineTuneActive by remember { mutableStateOf(false) }
    var addPickerOpen by remember { mutableStateOf(false) }
    var swipeActionTarget by remember { mutableStateOf<String?>(null) }

    // Navigation-layer status, refreshed whenever the screen is resumed
    // (the user returns from system settings after enabling it).
    var navTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { navTick++ }
    val navServiceOn = remember(navTick) { isNavServiceEnabled(context) }
    val overlayOn = remember(navTick) { AndroidSettings.canDrawOverlays(context) }
    val navReady = state.navGesturesOn && navServiceOn && overlayOn

    val animatorOff = remember {
        try {
            AndroidSettings.Global.getFloat(
                context.contentResolver,
                AndroidSettings.Global.ANIMATOR_DURATION_SCALE, 1f,
            ) == 0f
        } catch (e: Exception) {
            false
        }
    }

    fun applyHomeGesture(trigger: String, action: String) {
        if (action == "lock") {
            pendingLock = trigger
            return
        }
        val other = if (action != "none") {
            gestures.entries.firstOrNull { it.key != trigger && it.value == action }?.key
        } else {
            null
        }
        if (other != null) {
            pendingMove = Triple(trigger, action, other)
        } else {
            val map = gestures.toMutableMap()
            map[trigger] = action
            scope.launch { settings.setGesturesJson(encodeGestures(map)) }
        }
    }

    fun startSystemSettings(intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            // system settings unavailable on this device
        }
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item { DsTopBar(onBack, "Gestures & motion") }

        item { DsSection("Device diagram") }
        item {
            val stripDp = state.navEdgeDp.dp.coerceIn(12.dp, 48.dp)
            val edgeColor = when {
                fineTuneActive -> MaterialTheme.colorScheme.primary
                state.navGesturesOn -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.height(360.dp)
                        .aspectRatio(if (landscape) 1.9f else 0.52f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp))
                        .pointerInput(gestures, navMap) {
                            awaitPointerEventScope {
                                var lastTapUp = 0L
                                while (true) {
                                    val first = awaitPointerEvent()
                                    if (first.changes.none { it.changedToDown() }) continue
                                    val positions = mutableMapOf<Long, Offset>()
                                    val starts = mutableMapOf<Long, Offset>()
                                    var maxPointers = 0
                                    var startCentroid = Offset.Zero
                                    var lastCentroid = Offset.Zero
                                    var startDist = 0f
                                    var minRatio = 1f
                                    var downTime = 0L
                                    var maxMove = 0f
                                    for (c in first.changes) {
                                        if (c.changedToDown()) {
                                            positions[c.id.value] = c.position
                                            starts[c.id.value] = c.position
                                            downTime = c.uptimeMillis
                                        } else if (c.pressed) {
                                            positions[c.id.value] = c.position
                                            starts[c.id.value] = c.position
                                        }
                                    }
                                    if (positions.isNotEmpty()) {
                                        startCentroid = positions.values.reduce { a, b -> a + b } /
                                            positions.size.toFloat()
                                        lastCentroid = startCentroid
                                    }
                                    maxPointers = maxOf(maxPointers, positions.size)
                                    var done = false
                                    var upTime = downTime
                                    while (!done) {
                                        val ev = awaitPointerEvent()
                                        for (c in ev.changes) {
                                            when {
                                                c.changedToDown() -> {
                                                    positions[c.id.value] = c.position
                                                    starts[c.id.value] = c.position
                                                }
                                                (!c.pressed && c.previousPressed) -> {
                                                    positions.remove(c.id.value)
                                                    upTime = c.uptimeMillis
                                                    if (positions.isEmpty()) done = true
                                                }
                                                c.pressed -> positions[c.id.value] = c.position
                                            }
                                        }
                                        if (positions.isNotEmpty()) {
                                            val centroid = positions.values.reduce { a, b -> a + b } /
                                                positions.size.toFloat()
                                            lastCentroid = centroid
                                            maxMove = maxOf(
                                                maxMove,
                                                abs(centroid.x - startCentroid.x) +
                                                    abs(centroid.y - startCentroid.y),
                                            )
                                            if (positions.size == 2) {
                                                val pts = positions.values.toList()
                                                val dist = (pts[0] - pts[1]).getDistance()
                                                if (startDist == 0f) startDist = dist
                                                if (startDist > 0f) {
                                                    minRatio = minOf(minRatio, dist / startDist)
                                                }
                                            }
                                        }
                                        maxPointers = maxOf(maxPointers, positions.size)
                                    }
                                    val dy = lastCentroid.y - startCentroid.y
                                    val duration = upTime - downTime
                                    val trigger: String? = when {
                                        maxPointers >= 2 && minRatio < 0.8f -> "pinchIn"
                                        maxPointers >= 2 && dy > 80f -> "twoFingerDown"
                                        maxPointers == 1 && duration < 300 && maxMove < 30f &&
                                            upTime - lastTapUp < 350 -> "doubleTap"
                                        maxPointers == 1 && dy < -80f -> "swipeUp"
                                        maxPointers == 1 && dy > 80f -> "swipeDown"
                                        else -> null
                                    }
                                    if (trigger != null) {
                                        val action = gestures[trigger] ?: "none"
                                        detected = "${homeTriggerName(trigger)} → ${homeActionName(action)}"
                                        if (trigger == "doubleTap") lastTapUp = 0L
                                    }
                                    if (maxPointers == 1 && duration < 300 && maxMove < 30f) {
                                        lastTapUp = upTime
                                    }
                                }
                            }
                        },
                ) {
                    // Navigation edge zones (only live while the layer is on).
                    Box(
                        Modifier.align(Alignment.CenterStart)
                            .width(stripDp).fillMaxHeight(0.6f)
                            .background(edgeColor, RoundedCornerShape(0.dp, 8.dp, 8.dp, 0.dp))
                            .clickable(enabled = state.navGesturesOn) {
                                selected = Selected.Nav("left")
                                onSpotlight("nav-gestures")
                            },
                    )
                    Box(
                        Modifier.align(Alignment.CenterEnd)
                            .width(stripDp).fillMaxHeight(0.6f)
                            .background(edgeColor, RoundedCornerShape(8.dp, 0.dp, 0.dp, 8.dp))
                            .clickable(enabled = state.navGesturesOn) {
                                selected = Selected.Nav("right")
                                onSpotlight("nav-gestures")
                            },
                    )
                    Box(
                        Modifier.align(Alignment.BottomCenter)
                            .fillMaxWidth().height(stripDp)
                            .background(edgeColor, RoundedCornerShape(8.dp, 8.dp, 0.dp, 0.dp))
                            .clickable(enabled = state.navGesturesOn) {
                                selected = Selected.Nav("up")
                                onSpotlight("nav-gestures")
                            },
                    )
                    // Home gesture zones.
                    Row(
                        Modifier.align(Alignment.TopCenter).fillMaxWidth()
                            .fillMaxHeight(0.16f),
                    ) {
                        Box(
                            Modifier.weight(1f).fillMaxHeight()
                                .clickable {
                                    selected = Selected.Home("swipeDown")
                                    onSpotlight("gestures")
                                },
                            contentAlignment = Alignment.Center,
                        ) { Text("Swipe down", fontSize = 11.sp, textAlign = TextAlign.Center) }
                        Box(
                            Modifier.weight(1f).fillMaxHeight()
                                .clickable {
                                    selected = Selected.Home("twoFingerDown")
                                    onSpotlight("gestures")
                                },
                            contentAlignment = Alignment.Center,
                        ) { Text("Two-finger", fontSize = 11.sp, textAlign = TextAlign.Center) }
                    }
                    Box(
                        Modifier.align(Alignment.Center).fillMaxWidth(0.7f).fillMaxHeight(0.44f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                            .clickable {
                                selected = Selected.Home("pinchIn")
                                onSpotlight("gestures")
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Pinch in", fontSize = 11.sp)
                        Box(
                            Modifier.size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    selected = Selected.Home("doubleTap")
                                    onSpotlight("gestures")
                                },
                            contentAlignment = Alignment.Center,
                        ) { Text("Tap ×2", fontSize = 11.sp) }
                    }
                    Box(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(72.dp)
                            .padding(bottom = stripDp)
                            .clickable {
                                selected = Selected.Home("swipeUp")
                                onSpotlight("gestures")
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text("Swipe up", fontSize = 11.sp) }
                }
            }
        }
        item {
            Text(
                detected ?: "Try a gesture on the diagram.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp, 16.dp, 0.dp),
                textAlign = TextAlign.Center,
            )
        }

        // Action picker for the selected hotspot.
        selected?.let { sel ->
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 0.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        when (sel) {
                            is Selected.Home -> {
                                val action = gestures[sel.trigger] ?: "none"
                                Text(homeTriggerName(sel.trigger), fontSize = 16.sp)
                                Spacer(Modifier.height(8.dp))
                                for (opt in listOf("drawer", "search", "quickPanel", "settings", "lock", "shade", "none")) {
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .clickable { applyHomeGesture(sel.trigger, opt) }
                                            .padding(0.dp, 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = opt == action,
                                            onClick = { applyHomeGesture(sel.trigger, opt) },
                                        )
                                        Text(homeActionName(opt), fontSize = 15.sp)
                                    }
                                }
                                if (onTestAction != null && action != "none") {
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { onTestAction(action) }) { Text("Test") }
                                }
                            }
                            is Selected.Nav -> {
                                val action = navMap[sel.trigger] ?: "none"
                                Text(navTriggerName(sel.trigger), fontSize = 16.sp)
                                Spacer(Modifier.height(8.dp))
                                for (opt in NAV_ACTIONS) {
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .clickable(enabled = navReady) {
                                                val map = navMap.toMutableMap()
                                                map[sel.trigger] = opt
                                                scope.launch { settings.setNavMapJson(encodeNavMap(map)) }
                                            }
                                            .padding(0.dp, 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = opt == action,
                                            enabled = navReady,
                                            onClick = {
                                                val map = navMap.toMutableMap()
                                                map[sel.trigger] = opt
                                                scope.launch { settings.setNavMapJson(encodeNavMap(map)) }
                                            },
                                        )
                                        Text(navActionName(opt), fontSize = 15.sp)
                                    }
                                }
                                if (!navReady) {
                                    Text(
                                        "Turn navigation gestures on first.",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            val navValue = when {
                !state.navGesturesOn -> "Off"
                !navServiceOn || !overlayOn -> "Needs setup"
                else -> "On"
            }
            DsRow(
                title = "Navigation gestures",
                value = navValue,
                help = "Edge swipes work in every app.",
                trailing = {
                    androidx.compose.material3.Switch(
                        checked = state.navGesturesOn,
                        onCheckedChange = { on ->
                            scope.launch { settings.setNavGesturesOn(on) }
                        },
                    )
                },
            )
        }
        if (state.navGesturesOn && (!navServiceOn || !overlayOn)) {
            item {
                Row(Modifier.fillMaxWidth().padding(16.dp, 0.dp)) {
                    if (!navServiceOn) {
                        OutlinedButton(onClick = {
                            startSystemSettings(Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS))
                        }) { Text("Accessibility", fontSize = 14.sp) }
                        Spacer(Modifier.width(8.dp))
                    }
                    if (!overlayOn) {
                        OutlinedButton(onClick = {
                            startSystemSettings(
                                Intent(
                                    AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        }) { Text("Allow overlay", fontSize = 14.sp) }
                    }
                }
            }
        }
        item {
            DsSwitchRow(
                title = "Haptics",
                checked = state.navHaptics,
                help = "Buzz when a navigation gesture fires.",
                onCheckedChange = { on -> scope.launch { settings.setNavHaptics(on) } },
            )
        }
        item {
            DsSwitchRow(
                title = "Reduce motion",
                checked = state.reduceMotion,
                help = "Instant panel, dock and edit transitions.",
                onCheckedChange = { on -> scope.launch { settings.setReduceMotion(on) } },
            )
        }

        item {
            DsSection("Motion speed")
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for ((index, label) in listOf("Off", "Fast", "Normal", "Slow").withIndex()) {
                    val sel = index == state.animSpeed
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(50))
                            .background(
                                if (sel) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            )
                            .clickable { scope.launch { settings.setAnimSpeed(index) } }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            Text(
                "How fast transitions run. Off makes them instant.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(16.dp, 4.dp, 16.dp, 0.dp),
            )
        }

        item {
            DsMoreOptions(expanded = moreOpen, onToggle = { moreOpen = !moreOpen }) {
                DsSection("Fine tune")
                DsSliderRow(
                    title = "Sensitivity",
                    valueText = "${state.navSensitivity}",
                    sliderValue = state.navSensitivity.toFloat(),
                    range = 0f..100f,
                    help = "Higher needs a shorter swipe.",
                    onValueChange = { v ->
                        fineTuneActive = true
                        scope.launch { settings.setNavSensitivity(v.toInt()) }
                    },
                    onValueChangeFinished = { fineTuneActive = false },
                )
                DsSliderRow(
                    title = "Edge thickness",
                    valueText = "${state.navEdgeDp} dp",
                    sliderValue = state.navEdgeDp.toFloat(),
                    range = 12f..48f,
                    help = "Width of the edge swipe zones.",
                    onValueChange = { v ->
                        fineTuneActive = true
                        scope.launch { settings.setNavEdgeDp(v.toInt()) }
                    },
                    onValueChangeFinished = { fineTuneActive = false },
                )
                DsSection("Icon swipes")
                DsRow(
                    title = "Icon swipes",
                    value = "${iconSwipes.size} set",
                    help = "Flick an icon up to run its action.",
                )
                for ((pkg, action) in iconSwipes.entries.sortedBy { (pkg, _) ->
                    apps.firstOrNull { it.pkg == pkg }?.label ?: pkg
                }) {
                    val label = apps.firstOrNull { it.pkg == pkg }?.label ?: pkg
                    DsRow(
                        title = label,
                        value = iconSwipeActionName(action),
                        onClick = { swipeActionTarget = pkg },
                    )
                }
                DsRow(
                    title = "Add icon swipe",
                    value = "Pick app",
                    onClick = { addPickerOpen = true },
                )
                DsSection("Demos")
                if (animatorOff) {
                    Text(
                        "System animations are off.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(16.dp, 0.dp),
                    )
                }
                DemoCard(title = "App open", static = animatorOff || state.reduceMotion) { p ->
                    Box(
                        Modifier.fillMaxWidth().height(72.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val size = 28.dp + (44.dp * p)
                        Box(
                            Modifier.size(size)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f + 0.65f * p)),
                        )
                    }
                }
                DemoCard(title = "Drawer", static = animatorOff || state.reduceMotion) { p ->
                    Box(Modifier.fillMaxWidth().height(72.dp)) {
                        Box(
                            Modifier.align(Alignment.BottomCenter)
                                .fillMaxWidth(0.7f)
                                .height((20 + 44 * p).dp)
                                .clip(RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        )
                    }
                }
                DemoCard(title = "Scroll", static = animatorOff || state.reduceMotion) { p ->
                    Box(
                        Modifier.fillMaxWidth().height(72.dp).padding(16.dp, 8.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    ) {
                        Column(
                            Modifier.graphicsLayer { translationY = -p * 40f },
                        ) {
                            for (i in 0..4) {
                                Box(
                                    Modifier.fillMaxWidth().height(10.dp)
                                        .padding(top = if (i == 0) 0.dp else 6.dp)
                                        .background(
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(5.dp),
                                        ),
                                )
                            }
                        }
                    }
                }
                DsRow(
                    title = "Reset gestures",
                    value = "Defaults",
                    onClick = {
                        scope.launch {
                            settings.resetSection(8)
                            settings.setGesturesJson(DEFAULT_GESTURES)
                            settings.setNavMapJson(DEFAULT_NAV_MAP)
                        }
                    },
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    pendingLock?.let { trigger ->
        AlertDialog(
            onDismissRequest = { pendingLock = null },
            title = { Text("Lock screen") },
            text = { Text("Locking needs device admin. You will be asked when it first runs.") },
            confirmButton = {
                TextButton(onClick = {
                    val map = gestures.toMutableMap()
                    map[trigger] = "lock"
                    scope.launch { settings.setGesturesJson(encodeGestures(map)) }
                    pendingLock = null
                }) { Text("Bind") }
            },
            dismissButton = {
                TextButton(onClick = { pendingLock = null }) { Text("Cancel") }
            },
        )
    }

    pendingMove?.let { (trigger, action, other) ->
        AlertDialog(
            onDismissRequest = { pendingMove = null },
            title = { Text("Move action?") },
            text = {
                Text(
                    "“${homeActionName(action)}” is also bound to " +
                        "${homeTriggerName(other)}. Move it here?",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val map = gestures.toMutableMap()
                    map[other] = "none"
                    map[trigger] = action
                    scope.launch { settings.setGesturesJson(encodeGestures(map)) }
                    pendingMove = null
                }) { Text("Move") }
            },
            dismissButton = {
                TextButton(onClick = { pendingMove = null }) { Text("Cancel") }
            },
        )
    }

    // Add an icon swipe: pick an app, then its action.
    if (addPickerOpen) {
        var query by remember { mutableStateOf("") }
        var picked by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { addPickerOpen = false },
            title = { Text("Pick app") },
            text = {
                Column {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text("Search apps") },
                    )
                    val shown = apps.filter {
                        query.isBlank() || it.label.contains(query, ignoreCase = true)
                    }.sortedBy { it.label }.take(60)
                    LazyColumn(Modifier.height(320.dp)) {
                        for (entry in shown) {
                            item {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable { picked = entry.pkg }
                                        .padding(0.dp, 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = picked == entry.pkg,
                                        onClick = { picked = entry.pkg },
                                    )
                                    Text(
                                        entry.label,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                    if (picked != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("Action for this icon:", fontSize = 14.sp)
                        for (opt in ICON_SWIPE_ACTIONS) {
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            val map = iconSwipes.toMutableMap()
                                            if (opt == "none") map.remove(picked!!)
                                            else map[picked!!] = opt
                                            settings.setIconSwipesJson(encodeIconSwipes(map))
                                        }
                                        addPickerOpen = false
                                    }
                                    .padding(0.dp, 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = false, onClick = {
                                    scope.launch {
                                        val map = iconSwipes.toMutableMap()
                                        if (opt == "none") map.remove(picked!!)
                                        else map[picked!!] = opt
                                        settings.setIconSwipesJson(encodeIconSwipes(map))
                                    }
                                    addPickerOpen = false
                                })
                                Text(iconSwipeActionName(opt), fontSize = 15.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { addPickerOpen = false }) { Text("Close") }
            },
        )
    }

    // Change or remove one icon swipe.
    swipeActionTarget?.let { pkg ->
        AlertDialog(
            onDismissRequest = { swipeActionTarget = null },
            title = { Text("Icon swipe action") },
            text = {
                Column {
                    for (opt in ICON_SWIPE_ACTIONS) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        val map = iconSwipes.toMutableMap()
                                        if (opt == "none") map.remove(pkg) else map[pkg] = opt
                                        settings.setIconSwipesJson(encodeIconSwipes(map))
                                    }
                                    swipeActionTarget = null
                                }
                                .padding(0.dp, 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = iconSwipes[pkg] == opt,
                                onClick = {
                                    scope.launch {
                                        val map = iconSwipes.toMutableMap()
                                        if (opt == "none") map.remove(pkg) else map[pkg] = opt
                                        settings.setIconSwipesJson(encodeIconSwipes(map))
                                    }
                                    swipeActionTarget = null
                                },
                            )
                            Text(iconSwipeActionName(opt), fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { swipeActionTarget = null }) { Text("Close") }
            },
        )
    }
}

@Composable
private fun DemoCard(
    title: String,
    static: Boolean,
    content: @Composable (Float) -> Unit,
) {
    var playing by remember { mutableStateOf(false) }
    val animatedProgress by rememberInfiniteTransition(label = "demo").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "demoProgress",
    )
    val progress = if (playing && !static) animatedProgress else 0.6f
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(16.dp, 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (!static) {
                    TextButton(onClick = { playing = !playing }) {
                        Text(if (playing) "Pause" else "Play")
                    }
                }
            }
            content(progress)
        }
    }
}

val GesturesMotionEntries: List<SearchEntry> = listOf(
    SearchEntry("Swipe up", listOf("gesture", "swipe", "up", "drawer"), "Gestures & Motion", "gestures-motion:swipe-up"),
    SearchEntry("Swipe down", listOf("gesture", "swipe", "down", "panel"), "Gestures & Motion", "gestures-motion:swipe-down"),
    SearchEntry("Double tap", listOf("gesture", "double", "tap"), "Gestures & Motion", "gestures-motion:double-tap"),
    SearchEntry("Pinch in", listOf("gesture", "pinch"), "Gestures & Motion", "gestures-motion:pinch-in"),
    SearchEntry("Two-finger down", listOf("gesture", "two", "finger", "swipe"), "Gestures & Motion", "gestures-motion:two-finger"),
    SearchEntry("Navigation gestures", listOf("navigation", "edge", "swipe", "home", "back", "recents"), "Gestures & Motion", "gestures-motion:nav"),
    SearchEntry("Haptics", listOf("haptics", "vibrate", "buzz"), "Gestures & Motion", "gestures-motion:haptics"),
    SearchEntry("Reduce motion", listOf("motion", "animation", "reduce"), "Gestures & Motion", "gestures-motion:reduce-motion"),
    SearchEntry("Motion speed", listOf("motion", "speed", "animation", "transitions", "fast", "slow", "off"), "Gestures & Motion", "gestures-motion:speed"),
    SearchEntry("Sensitivity", listOf("sensitivity", "navigation", "swipe"), "Gestures & Motion", "gestures-motion:nav-sensitivity"),
    SearchEntry("Edge thickness", listOf("edge", "thickness", "zones"), "Gestures & Motion", "gestures-motion:nav-edge"),
    SearchEntry("Icon swipes", listOf("icon", "swipe", "flick", "app"), "Gestures & Motion", "gestures-motion:icon-swipes"),
    SearchEntry("Demos", listOf("transition", "demo", "app", "open", "drawer", "scroll"), "Gestures & Motion", "gestures-motion:demos"),
)
