package com.f1pulse.app.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.f1pulse.app.MainActivity
import com.f1pulse.app.core.time.TimeFormatter
import java.time.Duration
import java.time.Instant

/**
 * Countdown widget uses SizeMode.Exact and a width/height content budget.
 * The countdown always targets the race session (not the next immediate session).
 * Narrow or short allocations use one bounded vertical layout; larger allocations
 * can use the landscape/portrait arrangements without bitmap clipping.
 */
class CountdownWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val season = WidgetDataLoader.scheduleSeason()
        val data = WidgetDataLoader.loadNextRace(context, season)
        val refreshStatus = WidgetRefreshStatusStore.read(context)
        val isLightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES
        val trackThumbnail = WidgetImageLoader.loadTrackThumbnail(
            context = context,
            circuitId = data?.circuitId,
            isLightMode = isLightMode,
        )
        provideContent {
            F1WidgetTheme {
                CountdownContent(data, refreshStatus, isLightMode, trackThumbnail)
            }
        }
    }
}

@Composable
private fun CountdownContent(
    data: WidgetNextRace?,
    refreshStatus: WidgetRefreshStatus,
    isLight: Boolean,
    trackThumbnail: Bitmap?,
) {
    val context = LocalContext.current
    val layout = widgetLayout()
    val size = LocalSize.current
    val narrow = size.width.value < CountdownWidgetLayoutPolicy.TRACK_LAYOUT_MIN_WIDTH_DP
    val useTrackLayout = CountdownWidgetLayoutPolicy.useTrackLayout(
        widthDp = size.width.value,
        thumbnailAvailable = trackThumbnail != null,
    )
    val zone = data?.displayZone ?: java.time.ZoneId.systemDefault()
    // 倒计时始终指向正赛时间
    val target = data?.raceInstant
    val remaining = if (target != null) Duration.between(Instant.now(), target) else null
    val isLive = data?.isLive == true && data.selectedSession?.label == "RACE"

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(F1WidgetColors.surface)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Top accent line (3dp red)
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(F1WidgetColors.primary),
            ) {}

            // Give the body the height left after the accent line. Without a
            // weighted slot, fillMaxSize() is measured against the whole
            // RemoteViews area and the countdown drifts upward or clips below.
            Box(modifier = GlanceModifier.defaultWeight()) {
                when {
                    useTrackLayout -> WideTrackCountdown(
                        data = data,
                        refreshStatus = refreshStatus,
                        isLight = isLight,
                        target = target,
                        zone = zone,
                        remaining = remaining,
                        isLive = isLive,
                        trackThumbnail = checkNotNull(trackThumbnail),
                    )
                    narrow -> NarrowCenteredCountdown(
                        data, refreshStatus, isLight, target, zone, isLive,
                    )
                    layout == WidgetLayout.Landscape -> LandscapeCountdown(
                        data, refreshStatus, isLight, target, zone, remaining, isLive,
                    )
                    layout == WidgetLayout.Portrait -> PortraitCountdown(
                        data, refreshStatus, isLight, target, zone, remaining, isLive,
                    )
                    else -> SquareCountdown(
                        data, refreshStatus, isLight, target, zone, isLive,
                    )
                }
            }
        }
    }
}

/**
 * Shared header: session label (left) + refresh / live status (right).
 * The old in-header elastic spacer is gone; a fixed gap separates the
 * two texts. [compact] toggles short labels (Square) vs full labels.
 */
@Composable
private fun CountdownHeader(
    data: WidgetNextRace?,
    refreshStatus: WidgetRefreshStatus,
    isLive: Boolean,
    isLight: Boolean,
    compact: Boolean,
    fillWidth: Boolean = true,
    maxWidthDp: Float = 0f,
) {
    Row(
        modifier = if (fillWidth) GlanceModifier.fillMaxWidth() else GlanceModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        F1WidgetText(
            if (compact) {
                if (isLive) "LIVE · RACE"
                else "NEXT RACE"
            } else {
                if (isLive) "CURRENT SESSION · RACE"
                else "NEXT RACE"
            },
            color = F1WidgetPalette.primary(),
            fontSize = 11f,
            isLight = isLight,
            font = "bold",
            maxWidthDp = (maxWidthDp * 0.62f).coerceAtLeast(24f),
        )
        Spacer(GlanceModifier.width(8.dp))
        F1WidgetText(
            when {
                refreshStatus.failed && compact -> "⚠"
                refreshStatus.failed -> "⚠ 网络问题"
                isLive -> "LIVE"
                compact -> "↻"
                else -> "刷新"
            },
            color = if (refreshStatus.failed || isLive) F1WidgetPalette.primary() else F1WidgetPalette.textMuted(isLight),
            fontSize = if (compact) 9f else 10f,
            isLight = isLight,
            font = "bold",
            maxWidthDp = (maxWidthDp * 0.18f).coerceAtLeast(18f),
            modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetsAction>()),
        )
    }
}

/** Shared countdown block: live indicator, remaining-time display, or "complete". */
@Composable
private fun CountdownSection(
    remaining: Duration?,
    isLive: Boolean,
    isLight: Boolean,
    compact: Boolean = false,
    maxWidthDp: Float = 0f,
) {
    if (isLive) {
        if (compact) {
            F1WidgetText(
                "LIVE",
                F1WidgetPalette.primary(),
                22f,
                isLight,
                font = "black",
                maxWidthDp = maxWidthDp,
            )
        } else {
            LiveIndicator(isLight)
        }
    } else if (remaining != null && !remaining.isNegative) {
        if (compact) {
            F1WidgetText(
                WidgetDataLoader.formatCountdown(Instant.now().plus(remaining)),
                F1WidgetPalette.textPrimary(isLight),
                22f,
                isLight,
                font = "black",
                maxWidthDp = maxWidthDp,
            )
        } else {
            CountdownDisplay(remaining, isLight)
        }
    } else {
        F1WidgetText(
            "Session complete",
            color = F1WidgetPalette.textMuted(isLight),
            fontSize = 14f,
            isLight = isLight,
            font = "regular",
            maxWidthDp = maxWidthDp,
        )
    }
}
/**
 * Narrow layout: metadata stays pinned to the top while the countdown value
 * and race time are centered against the whole widget card.
 */
@Composable
private fun NarrowCenteredCountdown(
    data: WidgetNextRace?,
    refreshStatus: WidgetRefreshStatus,
    isLight: Boolean,
    target: Instant?,
    zone: java.time.ZoneId,
    isLive: Boolean,
) {
    val size = LocalSize.current
    val contentPadding = 10.dp
    val availableWidth = (size.width.value - contentPadding.value * 2f).coerceAtLeast(40f)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CountdownHeader(
                data = data,
                refreshStatus = refreshStatus,
                isLive = isLive,
                isLight = isLight,
                compact = true,
                maxWidthDp = availableWidth,
            )
            Spacer(GlanceModifier.height(4.dp))
            F1WidgetText(
                data?.raceName ?: "—",
                color = F1WidgetPalette.textPrimary(isLight),
                fontSize = 14f,
                isLight = isLight,
                font = "bold",
                maxWidthDp = availableWidth,
            )
        }

        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CompactCountdownValue(
                    data = data,
                    target = target,
                    zone = zone,
                    isLive = isLive,
                    isLight = isLight,
                    maxWidthDp = availableWidth,
                    showTimeZone = false,
                )
            }
        }
    }
}
/**
 * Wide layout: the race name and countdown stay vertically centered in the
 * left region while the circuit outline occupies a bounded right-hand slot.
 * The thumbnail bitmap is pre-tinted for the active light/dark mode.
 */
@Composable
private fun WideTrackCountdown(
    data: WidgetNextRace?,
    refreshStatus: WidgetRefreshStatus,
    isLight: Boolean,
    target: Instant?,
    zone: java.time.ZoneId,
    remaining: Duration?,
    isLive: Boolean,
    trackThumbnail: Bitmap,
) {
    val size = LocalSize.current
    val contentPadding = 12f
    val gap = 12f
    val thumbnailWidth = (size.width.value * 0.34f).coerceIn(92f, 180f)
    val leftWidth = (size.width.value - contentPadding * 2f - gap - thumbnailWidth)
        .coerceAtLeast(120f)
    val thumbnailHeight = (size.height.value - contentPadding * 2f)
        .coerceIn(48f, 168f)
    val compactCountdown = leftWidth < 260f
    val showFullHeader = size.height.value >= 142f
    val showRaceTime = size.height.value >= 96f

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(contentPadding.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .width(leftWidth.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    F1WidgetText(
                        data?.raceName ?: "—",
                        color = F1WidgetPalette.textPrimary(isLight),
                        fontSize = if (compactCountdown) 15f else 17f,
                        isLight = isLight,
                        font = "bold",
                        maxWidthDp = leftWidth,
                    )
                    Spacer(GlanceModifier.height(if (compactCountdown) 6.dp else 10.dp))
                    CountdownSection(
                        remaining = remaining,
                        isLive = isLive,
                        isLight = isLight,
                        compact = compactCountdown,
                        maxWidthDp = leftWidth,
                    )
                    if (showRaceTime) {
                        Spacer(GlanceModifier.height(5.dp))
                        F1WidgetText(
                            target?.let {
                                "RACE · ${TimeFormatter.formatTimeOnly(it, zone)} · ${data?.timeZoneLabel.orEmpty()}"
                            } ?: "Session complete",
                            color = F1WidgetPalette.textMuted(isLight),
                            fontSize = 9f,
                            isLight = isLight,
                            font = "regular",
                            maxWidthDp = leftWidth,
                        )
                    }
                }
            }

            Spacer(GlanceModifier.width(gap.dp))

            Box(
                modifier = GlanceModifier
                    .width(thumbnailWidth.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(trackThumbnail),
                    contentDescription = data?.circuitName,
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier
                        .width(thumbnailWidth.dp)
                        .height(thumbnailHeight.dp),
                )
            }
        }

        Box(
            modifier = GlanceModifier
                .width(leftWidth.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopStart,
        ) {
            if (showFullHeader) {
                CountdownHeader(
                    data = data,
                    refreshStatus = refreshStatus,
                    isLive = isLive,
                    isLight = isLight,
                    compact = true,
                    maxWidthDp = leftWidth,
                )
            } else {
                F1WidgetText(
                    text = if (refreshStatus.failed) "⚠" else if (isLive) "LIVE" else "↻",
                    color = if (refreshStatus.failed || isLive) {
                        F1WidgetPalette.primary()
                    } else {
                        F1WidgetPalette.textMuted(isLight)
                    },
                    fontSize = 9f,
                    isLight = isLight,
                    font = "bold",
                    maxWidthDp = 28f,
                    modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetsAction>()),
                )
            }
        }
    }
}
/**
 * Landscape: header on the left, countdown on the right. Main container is a
 * [Row]. No elastic spacer — the two columns are separated by a fixed gap.
 */
@Composable
private fun LandscapeCountdown(
    data: WidgetNextRace?,
    refreshStatus: WidgetRefreshStatus,
    isLight: Boolean,
    target: Instant?,
    zone: java.time.ZoneId,
    remaining: Duration?,
    isLive: Boolean,
) {
    val size = LocalSize.current
    val contentPadding = 12.dp
    val halfWidth = size.width.value * 0.5f - contentPadding.value
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: header + race info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CountdownHeader(data, refreshStatus, isLive, isLight, compact = false, fillWidth = false, maxWidthDp = halfWidth)
            Spacer(GlanceModifier.height(8.dp))
            if (data != null) {
                F1WidgetText(
                    data.raceName,
                    color = F1WidgetPalette.textPrimary(isLight),
                    fontSize = 16f,
                    isLight = isLight,
                    font = "bold",
                    maxWidthDp = halfWidth,
                )
                F1WidgetText(
                    data.circuitName,
                    color = F1WidgetPalette.textMuted(isLight),
                    fontSize = 11f,
                    isLight = isLight,
                    font = "regular",
                    maxWidthDp = halfWidth,
                )
            } else {
                F1WidgetText(
                    "—",
                    color = F1WidgetPalette.textPrimary(isLight),
                    fontSize = 16f,
                    isLight = isLight,
                    font = "bold",
                )
            }
        }

        Spacer(GlanceModifier.width(16.dp))

        // Right: countdown + race time
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CountdownSection(remaining, isLive, isLight, compact = halfWidth < 220f, maxWidthDp = halfWidth)
            Spacer(GlanceModifier.height(8.dp))
            F1WidgetText(
                target?.let { "${TimeFormatter.formatFullDateTime(it, zone)} · ${data?.timeZoneLabel.orEmpty()}" } ?: "",
                color = F1WidgetPalette.textMuted(isLight),
                fontSize = 10f,
                isLight = isLight,
                font = "regular",
                maxWidthDp = halfWidth,
            )
        }
    }
}

/**
 * Portrait: header on top, race name below, countdown centered in the remaining space.
 */
@Composable
private fun PortraitCountdown(
    data: WidgetNextRace?,
    refreshStatus: WidgetRefreshStatus,
    isLight: Boolean,
    target: Instant?,
    zone: java.time.ZoneId,
    remaining: Duration?,
    isLive: Boolean,
) {
    val size = LocalSize.current
    val contentPadding = 16.dp
    val availableWidth = size.width.value - contentPadding.value * 2f
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header: NEXT RACE
        CountdownHeader(data, refreshStatus, isLive, isLight, compact = size.width.value < 220f, maxWidthDp = availableWidth)
        Spacer(GlanceModifier.height(8.dp))

        // Race info
        if (data != null) {
            F1WidgetText(
                data.raceName,
                color = F1WidgetPalette.textPrimary(isLight),
                fontSize = 16f,
                isLight = isLight,
                font = "bold",
                maxWidthDp = availableWidth,
            )
            F1WidgetText(
                data.circuitName,
                color = F1WidgetPalette.textMuted(isLight),
                fontSize = 11f,
                isLight = isLight,
                font = "regular",
                maxWidthDp = availableWidth,
            )
        } else {
            F1WidgetText(
                "—",
                color = F1WidgetPalette.textPrimary(isLight),
                fontSize = 16f,
                isLight = isLight,
                font = "bold",
            )
        }

        Spacer(GlanceModifier.height(8.dp))

        // Countdown — centered in the remaining vertical space
        Box(
            // Consume the remaining height so the countdown is vertically centered.
            // fillMaxSize() here is measured as an unconstrained child by some launchers.
            modifier = GlanceModifier.defaultWeight(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CountdownSection(remaining, isLive, isLight, compact = availableWidth < 260f, maxWidthDp = availableWidth)
                Spacer(GlanceModifier.height(8.dp))
                F1WidgetText(
                    target?.let { "${TimeFormatter.formatFullDateTime(it, zone)} · ${data?.timeZoneLabel.orEmpty()}" } ?: "",
                    color = F1WidgetPalette.textMuted(isLight),
                    fontSize = 10f,
                    isLight = isLight,
                    font = "regular",
                    maxWidthDp = availableWidth,
                )
            }
        }
    }
}

/**
 * Square: centered, compact middle-ground. Main container is a centered
 * [Column]. Reuses [CompactCountdownBody] with auto-fit width.
 */
@Composable
private fun SquareCountdown(
    data: WidgetNextRace?,
    refreshStatus: WidgetRefreshStatus,
    isLight: Boolean,
    target: Instant?,
    zone: java.time.ZoneId,
    isLive: Boolean,
) {
    val size = LocalSize.current
    val contentPadding = 10.dp
    val availableWidth = size.width.value - contentPadding.value * 2f
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CountdownHeader(data, refreshStatus, isLive, isLight, compact = true, maxWidthDp = availableWidth)
        CompactCountdownBody(data, target, zone, isLive, isLight, maxWidthDp = availableWidth, compact = false)
    }
}

@Composable
private fun CompactCountdownBody(
    data: WidgetNextRace?,
    target: Instant?,
    zone: java.time.ZoneId,
    isLive: Boolean,
    isLight: Boolean,
    maxWidthDp: Float = 0f,
    compact: Boolean = false,
) {
    Spacer(GlanceModifier.height(4.dp))
    F1WidgetText(
        data?.raceName ?: "—",
        color = F1WidgetPalette.textPrimary(isLight),
        fontSize = 14f,
        isLight = isLight,
        font = "bold",
        maxWidthDp = maxWidthDp,
    )
    Spacer(GlanceModifier.height(if (compact) 6.dp else 12.dp))
    CompactCountdownValue(
        data = data,
        target = target,
        zone = zone,
        isLive = isLive,
        isLight = isLight,
        maxWidthDp = maxWidthDp,
        showTimeZone = !compact,
    )
}

@Composable
private fun CompactCountdownValue(
    data: WidgetNextRace?,
    target: Instant?,
    zone: java.time.ZoneId,
    isLive: Boolean,
    isLight: Boolean,
    maxWidthDp: Float,
    showTimeZone: Boolean,
) {
    F1WidgetText(
        if (isLive) "LIVE" else WidgetDataLoader.formatCountdown(target),
        color = if (isLive) F1WidgetPalette.primary() else F1WidgetPalette.textPrimary(isLight),
        fontSize = 22f,
        isLight = isLight,
        font = "black",
        maxWidthDp = maxWidthDp,
    )
    F1WidgetText(
        target?.let {
            "RACE · ${TimeFormatter.formatTimeOnly(it, zone)}"
        } ?: "Session complete",
        color = F1WidgetPalette.textSecondary(isLight),
        fontSize = 10f,
        isLight = isLight,
        font = "regular",
        maxWidthDp = maxWidthDp,
    )
    if (showTimeZone) {
        F1WidgetText(
            data?.timeZoneLabel.orEmpty(),
            color = F1WidgetPalette.textMuted(isLight),
            fontSize = 8f,
            isLight = isLight,
            font = "regular",
            maxWidthDp = maxWidthDp,
        )
    }
}

@Composable
private fun CountdownDisplay(remaining: Duration, isLight: Boolean) {
    val days = remaining.toDays().coerceAtLeast(0)
    val hours = remaining.toHours().rem(24).coerceAtLeast(0)
    val minutes = remaining.toMinutes().rem(60).coerceAtLeast(0)

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CountdownBlock(days.toString(), "DAYS", isLight)
        Spacer(GlanceModifier.width(12.dp))
        F1WidgetText(
            ":",
            color = F1WidgetPalette.textMuted(isLight),
            fontSize = 28f,
            isLight = isLight,
            font = "bold",
        )
        Spacer(GlanceModifier.width(12.dp))
        CountdownBlock(hours.toString().padStart(2, '0'), "HRS", isLight)
        Spacer(GlanceModifier.width(12.dp))
        F1WidgetText(
            ":",
            color = F1WidgetPalette.textMuted(isLight),
            fontSize = 28f,
            isLight = isLight,
            font = "bold",
        )
        Spacer(GlanceModifier.width(12.dp))
        CountdownBlock(minutes.toString().padStart(2, '0'), "MIN", isLight)
    }
}

@Composable
private fun CountdownBlock(value: String, unit: String, isLight: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        F1WidgetText(
            value,
            color = F1WidgetPalette.textPrimary(isLight),
            fontSize = 32f,
            isLight = isLight,
            font = "black",
        )
        F1WidgetText(
            unit,
            color = F1WidgetPalette.textMuted(isLight),
            fontSize = 9f,
            isLight = isLight,
            font = "bold",
        )
    }
}

@Composable
private fun LiveIndicator(isLight: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width(10.dp)
                .height(10.dp)
                .background(F1WidgetColors.primary),
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        F1WidgetText(
            "SESSION IN PROGRESS",
            color = F1WidgetPalette.primary(),
            fontSize = 16f,
            isLight = isLight,
            font = "bold",
        )
    }
}

@Composable
private fun SessionTimeline(data: WidgetNextRace, isLight: Boolean) {
    val now = Instant.now()
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        data.sessions.forEachIndexed { index, session ->
            val isSelected = session == data.selectedSession
            val dotColor = when {
                isSelected -> F1WidgetPalette.primary()
                session.isCompleted(now) -> F1WidgetPalette.textPrimary(isLight)
                else -> F1WidgetPalette.textMuted(isLight)
            }
            val dotSize = if (isSelected) 6.dp else 4.dp

            F1WidgetText(
                session.shortLabel,
                color = dotColor,
                fontSize = 8f,
                isLight = isLight,
                font = "bold",
            )
            Spacer(GlanceModifier.width(2.dp))
            Box(
                modifier = GlanceModifier
                    .width(dotSize)
                    .height(dotSize)
                    .background(dotColor),
            ) {}
            if (index < data.sessions.lastIndex) {
                Spacer(GlanceModifier.width(4.dp))
            }
        }
    }
}

class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = CountdownWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetInitializer.scheduleFromSettings(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetInitializer.cancelIfNoWidgets(context)
    }
}
