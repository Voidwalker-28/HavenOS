package app.havenos.launcher.ui.island

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.havenos.launcher.theme.GlassSurface
import app.havenos.launcher.theme.LocalPalette
import app.havenos.launcher.ui.glass.GlassLevel
import app.havenos.launcher.ui.glass.HavenIcon
import app.havenos.launcher.ui.glass.glassHairlineAlpha
import app.havenos.launcher.ui.glass.vector

/**
 * Home Dynamic Island (v3 Track B, ORIGINAL code).
 *
 * Top-center pill over the home shell. Renders NOTHING when no live
 * activity exists (hidden-when-idle: an enabled island never implies
 * activity that isn't there), and nothing at all when the caller gates
 * it off. Tap toggles collapsed <-> expanded; expanded lists EVERY
 * live activity stacked (improvement over the reference, which shows
 * only the top-priority one), with real media transport controls and
 * per-app notification rows that launch the app.
 *
 * Surface is the shared Minimal GlassSurface, so the pill matches the
 * dock/cards; the accent color marks the live indicator and the
 * play/pause control. 3.1 Phase C: the pill keeps GlassSurface (the
 * home blur mechanism — tint-only glass() would lose blur over the
 * home); the Glass3 treatment is the hairline divider between stacked
 * activities in the expanded panel.
 *
 * The caller (HavenRoot) places this AFTER the pager but BEFORE the
 * drawer/search/settings overlays, so those cover the island when
 * open; the caller also hides it in edit mode.
 */
@Composable
fun IslandHomeOverlay(
    labelForPkg: (String) -> String,
    onLaunchPkg: (String) -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    // Burst the user dismissed (timestamp, not a boolean): a later
    // burst re-arms the pill; dismissing never hides media/charging.
    var dismissedBurstAt by remember { mutableLongStateOf(0L) }
    val activities = rememberIslandActivities(labelForPkg, dismissedBurstAt)
    var expanded by remember { mutableStateOf(false) }
    // Collapsing when the last activity leaves keeps no stale panel.
    LaunchedEffect(activities.isEmpty()) {
        if (activities.isEmpty()) expanded = false
    }
    if (activities.isEmpty()) return

    val palette = LocalPalette.current
    val top = activities.first()
    val anim: androidx.compose.animation.core.FiniteAnimationSpec<androidx.compose.ui.unit.IntSize> =
        if (reduceMotion) snap() else spring()
    Box(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        GlassSurface(
            modifier = Modifier
                .then(if (expanded) Modifier.fillMaxWidth(0.92f).widthIn(max = 480.dp) else Modifier)
                .animateContentSize(anim)
                .semantics {
                    contentDescription = islandSummary(top)
                    role = Role.Button
                }
                .clickable { expanded = !expanded },
            shape = if (expanded) RoundedCornerShape(24.dp) else RoundedCornerShape(50),
        ) {
            if (!expanded) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        collapsedGlyph(top).vector(),
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        collapsedText(top),
                        color = palette.onSurface,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 300.dp),
                    )
                }
            } else {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    for ((index, activity) in activities.withIndex()) {
                        when (activity) {
                            is IslandActivity.Media -> MediaExpandedRow(
                                activity = activity,
                                accent = palette.accent,
                                onSurface = palette.onSurface,
                                muted = palette.muted,
                            )
                            is IslandActivity.Charging -> Text(
                                "Charging · ${activity.percent}%",
                                color = palette.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            is IslandActivity.Notifications -> {
                                Text(
                                    if (activity.total == 1) "1 notification"
                                    else "${activity.total} notifications",
                                    color = palette.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                for (app in activity.apps) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { onLaunchPkg(app.pkg) }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            app.label,
                                            color = palette.onSurface,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            "${app.count}",
                                            color = palette.accent,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        // 3.1 Phase C: Glass3 hairline between stacked activities.
                        if (index < activities.size - 1) {
                            Box(
                                Modifier.fillMaxWidth().height(1.dp)
                                    .background(
                                        palette.onSurface.copy(alpha = glassHairlineAlpha(GlassLevel.GLASS3)),
                                    ),
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    // Dismiss only ever dismisses the transient
                    // notification burst (and collapses); media and
                    // charging stay live until their source ends.
                    val burst = activities.firstOrNull { it is IslandActivity.Notifications }
                    if (burst != null) {
                        Row(
                            Modifier
                                .clickable {
                                    dismissedBurstAt = System.currentTimeMillis()
                                    expanded = false
                                }
                                .padding(vertical = 4.dp),
                        ) {
                            Text(
                                "Dismiss alert",
                                color = palette.muted,
                                fontSize = 13.sp,
                            )
                        }
                    } else {
                        Text(
                            "Tap the pill to collapse",
                            color = palette.muted,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun islandSummary(top: IslandActivity): String = when (top) {
    is IslandActivity.Media -> "Now playing: ${top.title}"
    is IslandActivity.Charging -> "Charging, ${top.percent} percent"
    is IslandActivity.Notifications -> if (top.total == 1) "1 notification" else "${top.total} notifications"
}

// 3.1 Phase B4: the collapsed glyph is an icon, not a text glyph.
// Mapping preserves the old display exactly: collapsed shows state
// (playing -> play, paused -> pause), charging -> bolt, notifications
// -> bell (the old dot had no icon equivalent).
private fun collapsedGlyph(top: IslandActivity): HavenIcon = when (top) {
    is IslandActivity.Media -> if (top.playing) HavenIcon.MediaPlay else HavenIcon.MediaPause
    is IslandActivity.Charging -> HavenIcon.MediaCharging
    is IslandActivity.Notifications -> HavenIcon.Bell
}

private fun collapsedText(top: IslandActivity): String = when (top) {
    is IslandActivity.Media -> top.title
    is IslandActivity.Charging -> "Charging · ${top.percent}%"
    is IslandActivity.Notifications -> if (top.total == 1) "1 notification" else "${top.total} notifications"
}

@Composable
private fun MediaExpandedRow(
    activity: IslandActivity.Media,
    accent: androidx.compose.ui.graphics.Color,
    onSurface: androidx.compose.ui.graphics.Color,
    muted: androidx.compose.ui.graphics.Color,
) {
    Column {
        Text(
            activity.title,
            color = onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (activity.artist.isNotBlank()) {
            Text(
                activity.artist,
                color = muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clickable { activity.skipPrevious() }
                    .semantics { contentDescription = "Previous track" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    HavenIcon.MediaPrevious.vector(),
                    contentDescription = null,
                    tint = onSurface,
                    modifier = Modifier.size(16.dp),
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clickable { activity.togglePlayPause() }
                    .semantics {
                        contentDescription = if (activity.playing) "Pause" else "Play"
                    },
                contentAlignment = Alignment.Center,
            ) {
                // Expanded row shows the ACTION (playing -> pause icon).
                Icon(
                    (if (activity.playing) HavenIcon.MediaPause else HavenIcon.MediaPlay).vector(),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clickable { activity.skipNext() }
                    .semantics { contentDescription = "Next track" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    HavenIcon.MediaNext.vector(),
                    contentDescription = null,
                    tint = onSurface,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
