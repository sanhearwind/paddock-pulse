package com.f1pulse.app.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.f1pulse.app.MainActivity
import com.f1pulse.app.core.CountryFlags
import com.f1pulse.app.core.time.TimeFormatter
import java.time.ZoneId

/**
 * Next-race schedule widget. A short allocation keeps the reference layout's
 * QUALI + RACE rows; extra height is spent on sprint and practice rows.
 */
class NextRaceInfoWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val season = WidgetDataLoader.scheduleSeason()
        val data = WidgetDataLoader.loadNextRace(context, season)
        val refreshStatus = WidgetRefreshStatusStore.read(context)
        val isLightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
            Configuration.UI_MODE_NIGHT_YES

        provideContent {
            F1WidgetTheme {
                NextRaceContent(data, refreshStatus, isLightMode)
            }
        }
    }
}

@Composable
private fun NextRaceContent(
    data: WidgetNextRace?,
    refreshStatus: WidgetRefreshStatus,
    isLight: Boolean,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val zone = data?.displayZone ?: ZoneId.systemDefault()
    val veryShort = size.height.value < 150f
    val compact = veryShort || size.height.value < 210f || size.width.value < 220f
    val horizontalPadding = if (veryShort) 8.dp else 10.dp
    val rowHeight = when {
        veryShort -> 24f
        compact -> 32f
        else -> 36f
    }
    val rowGap = if (veryShort) 2f else 4f
    val rowBudget = when {
        veryShort -> 26f
        compact -> 40f
        else -> 40f
    }
    val fixedBudget = if (veryShort) 72f else 110f
    val maxSessions = ((size.height.value - fixedBudget) / rowBudget)
        .toInt()
        .coerceAtLeast(2)
    val sessions = data?.sessions?.prioritizeSessions(maxSessions).orEmpty()
    val availableWidth = (size.width.value - horizontalPadding.value * 2f).coerceAtLeast(72f)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(F1WidgetColors.surface)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            RaceWeekendHeader(
                refreshStatus = refreshStatus,
                isLight = isLight,
                availableWidth = size.width.value,
                compact = compact,
            )

            if (data == null) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(horizontalPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    F1WidgetText(
                        "暂无数据",
                        color = F1WidgetPalette.textMuted(isLight),
                        fontSize = 12f,
                        isLight = isLight,
                        font = "regular",
                        maxWidthDp = availableWidth,
                    )
                }
            } else {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(
                            start = horizontalPadding,
                            end = horizontalPadding,
                            top = if (veryShort) 4.dp else 8.dp,
                            bottom = if (veryShort) 4.dp else 6.dp,
                        ),
                ) {
                    RaceIdentity(
                        data = data,
                        isLight = isLight,
                        availableWidth = availableWidth,
                        veryShort = veryShort,
                    )
                    Spacer(GlanceModifier.height(if (veryShort) 4.dp else 8.dp))

                    sessions.forEachIndexed { index, session ->
                        ScheduleSessionCard(
                            session = session,
                            zone = zone,
                            isLight = isLight,
                            availableWidth = availableWidth,
                            rowHeight = rowHeight,
                            compact = compact,
                        )
                        if (index < sessions.lastIndex) {
                            Spacer(GlanceModifier.height(rowGap.dp))
                        }
                    }

                    Spacer(GlanceModifier.defaultWeight())
                    TimeZoneFooter(
                        label = data.timeZoneLabel,
                        isLight = isLight,
                        availableWidth = availableWidth,
                        veryShort = veryShort,
                    )
                }
            }
        }
    }
}

/**
 * Low-height priority is RACE + QUALI. As the available row budget grows,
 * sprint sessions and then practice sessions are added; display stays
 * chronological after selection.
 */
internal fun List<WidgetSession>.prioritizeSessions(maxSessions: Int): List<WidgetSession> {
    if (maxSessions <= 0) return emptyList()
    val priorityOrder = mapOf(
        "RACE" to 0,
        "QUALI" to 1,
        "SPRINT" to 2,
        "SQ" to 3,
        "FP3" to 4,
        "FP2" to 5,
        "FP1" to 6,
    )
    return sortedWith(
        compareBy(
            { priorityOrder[it.shortLabel] ?: priorityOrder[it.label] ?: Int.MAX_VALUE },
            WidgetSession::instant,
        ),
    ).take(maxSessions).sortedBy { it.instant }
}

@Composable
private fun RaceWeekendHeader(
    refreshStatus: WidgetRefreshStatus,
    isLight: Boolean,
    availableWidth: Float,
    compact: Boolean,
) {
    val headerHeight = if (compact) 27f else 32f
    val brandWidth = if (compact) 58f else 70f
    val refreshWidth = if (compact) 24f else 66f
    val titleWidth = (availableWidth - brandWidth - refreshWidth - 20f).coerceAtLeast(54f)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(headerHeight.dp)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width(brandWidth.dp)
                .height(headerHeight.dp)
                .cornerRadius(8.dp)
                .background(F1WidgetColors.primary),
            contentAlignment = Alignment.Center,
        ) {
            F1WidgetText(
                "F1",
                color = F1WidgetPalette.onPrimary(),
                fontSize = if (compact) 12f else 14f,
                isLight = isLight,
                font = "black",
                maxWidthDp = brandWidth - 16f,
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        F1WidgetText(
            "RACE WEEKEND",
            color = F1WidgetPalette.primary(),
            fontSize = if (compact) 9f else 11f,
            isLight = isLight,
            font = "bold",
            maxWidthDp = titleWidth,
        )
        Spacer(GlanceModifier.defaultWeight())
        F1WidgetText(
            when {
                refreshStatus.failed && compact -> "⚠"
                refreshStatus.failed -> "⚠ 网络问题"
                compact -> "↻"
                else -> "↻ 刷新"
            },
            color = if (refreshStatus.failed) {
                F1WidgetPalette.primary()
            } else {
                F1WidgetPalette.textMuted(isLight)
            },
            fontSize = if (compact) 9f else 10f,
            isLight = isLight,
            font = "regular",
            maxWidthDp = refreshWidth,
            modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetsAction>()),
        )
    }
}

@Composable
private fun RaceIdentity(
    data: WidgetNextRace,
    isLight: Boolean,
    availableWidth: Float,
    veryShort: Boolean,
) {
    val flag = data.countryFlagCode?.let { CountryFlags.toEmoji(it) }.orEmpty()
    val flagWidth = if (veryShort) 22f else 34f
    val textWidth = (availableWidth - flagWidth - 8f).coerceAtLeast(42f)

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width(flagWidth.dp)
                .height(if (veryShort) 22.dp else 34.dp),
            contentAlignment = Alignment.Center,
        ) {
            F1WidgetText(
                text = flag,
                color = F1WidgetPalette.textPrimary(isLight),
                fontSize = if (veryShort) 17f else 25f,
                isLight = isLight,
                font = "system",
                maxWidthDp = flagWidth,
                maxHeightDp = if (veryShort) 22f else 34f,
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.width(textWidth.dp)) {
            F1WidgetText(
                data.raceName,
                color = F1WidgetPalette.textPrimary(isLight),
                fontSize = if (veryShort) 11f else 14f,
                isLight = isLight,
                font = "bold",
                maxWidthDp = textWidth,
            )
            if (!veryShort) {
                F1WidgetText(
                    data.circuitName,
                    color = F1WidgetPalette.textMuted(isLight),
                    fontSize = 9f,
                    isLight = isLight,
                    font = "regular",
                    maxWidthDp = textWidth,
                )
            }
        }
    }
}

@Composable
private fun ScheduleSessionCard(
    session: WidgetSession,
    zone: ZoneId,
    isLight: Boolean,
    availableWidth: Float,
    rowHeight: Float,
    compact: Boolean,
) {
    val dateWidth = if (compact) 48f else 60f
    val iconWidth = if (compact) 20f else 24f
    val labelWidth = (availableWidth - dateWidth - iconWidth - 34f).coerceAtLeast(42f)
    val icon = when (session.shortLabel) {
        "R" -> "🏁"
        "Q", "SQ" -> "⏱"
        "S" -> "⚡"
        else -> "◷"
    }
    val label = when (session.shortLabel) {
        "FP1" -> "PRACTICE 1"
        "FP2" -> "PRACTICE 2"
        "FP3" -> "PRACTICE 3"
        "SQ" -> "SPRINT QUALI"
        "S" -> "SPRINT"
        "Q" -> "QUALI"
        "R" -> "RACE"
        else -> session.label
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(rowHeight.dp)
            .cornerRadius(9.dp)
            .background(F1WidgetColors.background)
            .padding(horizontal = if (compact) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width(iconWidth.dp)
                .height(iconWidth.dp),
            contentAlignment = Alignment.Center,
        ) {
            F1WidgetText(
                text = icon,
                color = F1WidgetPalette.textPrimary(isLight),
                fontSize = if (compact) 13f else 16f,
                isLight = isLight,
                font = "system",
                maxWidthDp = iconWidth,
                maxHeightDp = iconWidth,
            )
        }
        Spacer(GlanceModifier.width(6.dp))
        Box(
            modifier = GlanceModifier
                .width(2.dp)
                .height(if (compact) 18.dp else 22.dp)
                .background(F1WidgetColors.primary),
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        Column(
            modifier = GlanceModifier.defaultWeight(),
        ) {
            F1WidgetText(
                label,
                color = F1WidgetPalette.textPrimary(isLight),
                fontSize = if (compact) 11f else 13f,
                isLight = isLight,
                font = "bold",
                maxWidthDp = labelWidth,
            )
        }
        Column(
            modifier = GlanceModifier.width(dateWidth.dp),
            horizontalAlignment = Alignment.End,
        ) {
            F1WidgetText(
                session.instant?.let { TimeFormatter.formatDateOnly(it, zone) } ?: "—",
                color = F1WidgetPalette.textPrimary(isLight),
                fontSize = if (compact) 9f else 11f,
                isLight = isLight,
                font = "bold",
                maxWidthDp = dateWidth,
            )
            if (!compact || rowHeight >= 30f) {
                F1WidgetText(
                    session.instant?.let { TimeFormatter.formatTimeOnly(it, zone) }.orEmpty(),
                    color = F1WidgetPalette.textMuted(isLight),
                    fontSize = if (compact) 8f else 9f,
                    isLight = isLight,
                    font = "regular",
                    maxWidthDp = dateWidth,
                )
            }
        }
    }
}

@Composable
private fun TimeZoneFooter(
    label: String,
    isLight: Boolean,
    availableWidth: Float,
    veryShort: Boolean,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        F1WidgetText(
            "◎",
            color = F1WidgetPalette.textMuted(isLight),
            fontSize = if (veryShort) 8f else 10f,
            isLight = isLight,
            font = "regular",
        )
        Spacer(GlanceModifier.width(4.dp))
        F1WidgetText(
            label,
            color = F1WidgetPalette.textMuted(isLight),
            fontSize = if (veryShort) 7f else 8f,
            isLight = isLight,
            font = "regular",
            maxWidthDp = (availableWidth - 18f).coerceAtLeast(48f),
        )
    }
}

class NextRaceInfoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = NextRaceInfoWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetInitializer.scheduleFromSettings(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetInitializer.cancelIfNoWidgets(context)
    }
}