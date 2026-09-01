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
import androidx.glance.unit.ColorProvider
import com.f1pulse.app.MainActivity
import com.f1pulse.app.ui.theme.TeamPalette

/**
 * Shows the classification of the most recently completed session — practice, qualifying or
 * race, whichever ran last.
 *
 * Jolpica/Ergast publishes no practice results at all, so this is sourced from OpenF1's
 * `session_result`. On the free OpenF1 tier a session stays unavailable until roughly
 * 30 minutes after it ends, so briefly after a session the widget still shows the previous
 * one rather than an empty state.
 */
class LastSessionWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val season = WidgetDataLoader.scheduleSeason()
        val lastSession = WidgetDataLoader.loadLastSession(context, season, limit = 20)
        val isLightMode = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES
        val refreshStatus = WidgetRefreshStatusStore.read(context)
        provideContent {
            F1WidgetTheme {
                LastSessionContent(lastSession, refreshStatus, isLightMode)
            }
        }
    }
}

@Composable
private fun LastSessionContent(
    session: WidgetLastSession?,
    refreshStatus: WidgetRefreshStatus,
    isLight: Boolean,
) {
    val context = LocalContext.current
    val layout = widgetLayout()
    val size = LocalSize.current
    val rows = session?.rows.orEmpty()
    val visibleCount = StandingsWidgetLayout.visibleRowCount(size.height.value, rows.size)
    val compact = size.width.value < 180f || size.height.value < 120f
    val contentPadding = when (layout) {
        WidgetLayout.Landscape -> 12.dp
        WidgetLayout.Portrait -> 14.dp
        WidgetLayout.Square -> 10.dp
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(F1WidgetColors.surface)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(F1WidgetColors.primary),
            ) {}

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    F1WidgetText(
                        session?.sessionLabel ?: "LAST SESSION",
                        color = F1WidgetPalette.primary(),
                        fontSize = 11f,
                        isLight = isLight,
                        font = "bold",
                        maxWidthDp = (size.width.value - 90f).coerceAtLeast(50f),
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    F1WidgetText(
                        when {
                            refreshStatus.failed && layout == WidgetLayout.Square -> "⚠"
                            refreshStatus.failed -> "⚠ 网络问题"
                            layout == WidgetLayout.Square -> "↻"
                            else -> "刷新"
                        },
                        color = if (refreshStatus.failed) F1WidgetPalette.primary()
                        else F1WidgetPalette.textMuted(isLight),
                        fontSize = 10f,
                        isLight = isLight,
                        font = "bold",
                        modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetsAction>()),
                    )
                }

                if (session != null && !compact) {
                    F1WidgetText(
                        session.raceName,
                        color = F1WidgetPalette.textMuted(isLight),
                        fontSize = 10f,
                        isLight = isLight,
                        font = "regular",
                        maxWidthDp = (size.width.value - 30f).coerceAtLeast(60f),
                    )
                }

                if (rows.isEmpty()) {
                    F1WidgetText(
                        "暂无数据",
                        color = F1WidgetPalette.textMuted(isLight),
                        fontSize = 12f,
                        isLight = isLight,
                        font = "regular",
                    )
                } else {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight(),
                    ) {
                        rows.take(visibleCount).forEach { row ->
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .defaultWeight(),
                                contentAlignment = Alignment.Center,
                            ) {
                                SessionResultWidgetRow(row, isLight, compact)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionResultWidgetRow(
    row: WidgetSessionResultRow,
    isLight: Boolean,
    compact: Boolean,
) {
    val size = LocalSize.current
    val teamColor = ColorProvider(TeamPalette.forHex(row.teamHex))
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width(3.dp)
                .height(28.dp)
                .background(teamColor),
        ) {}
        Spacer(GlanceModifier.width(6.dp))

        F1WidgetText(
            "P${row.pos}",
            color = F1WidgetPalette.positionColor(row.pos, isLight),
            fontSize = 13f,
            isLight = isLight,
            font = "bold",
        )
        Spacer(GlanceModifier.width(8.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            F1WidgetText(
                row.code,
                color = F1WidgetPalette.textPrimary(isLight),
                fontSize = 13f,
                isLight = isLight,
                font = "bold",
            )
            if (!compact && row.teamName != null) {
                F1WidgetText(
                    row.teamName,
                    color = F1WidgetPalette.textMuted(isLight),
                    fontSize = 10f,
                    isLight = isLight,
                    font = "regular",
                    maxWidthDp = (size.width.value - 160f).coerceAtLeast(40f),
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            F1WidgetText(
                row.statusText ?: row.timeText ?: "—",
                color = F1WidgetPalette.textPrimary(isLight),
                fontSize = 12f,
                isLight = isLight,
                font = "bold",
            )
            if (!compact && row.secondaryText != null) {
                F1WidgetText(
                    row.secondaryText,
                    color = F1WidgetPalette.textMuted(isLight),
                    fontSize = 9f,
                    isLight = isLight,
                    font = "regular",
                )
            }
        }
    }
}

class LastSessionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = LastSessionWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetInitializer.scheduleFromSettings(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetInitializer.cancelIfNoWidgets(context)
    }
}
