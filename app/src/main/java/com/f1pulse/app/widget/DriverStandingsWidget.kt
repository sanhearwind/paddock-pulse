package com.f1pulse.app.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Driver standings widget v1.6 — F1 brand font via text-to-bitmap rendering.
 * All Text() replaced with F1WidgetText() (Canvas + F1 Typeface → transparent bitmap).
 */
class DriverStandingsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val season = WidgetDataLoader.standingsSeason()
        // A bounded list covers every realistic widget height. Loading all
        // standings and every image causes avoidable I/O and memory spikes.
        val drivers = WidgetDataLoader.loadTopDrivers(context, season, limit = 20)
        val isLightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES
        val (headshots, logos) = withContext(Dispatchers.IO) {
            drivers.associate { it.code to WidgetImageLoader.loadHeadshot(context, it.code) } to
                drivers.associate { it.constructorId to WidgetImageLoader.loadTeamLogo(context, it.constructorId, isLightMode) }
        }
        val refreshStatus = WidgetRefreshStatusStore.read(context)
        provideContent {
            F1WidgetTheme {
                DriverStandingsContent(drivers, headshots, logos, refreshStatus, isLightMode)
            }
        }
    }
}

@Composable
private fun DriverStandingsContent(
    drivers: List<WidgetDriverStanding>,
    headshots: Map<String, Bitmap?>,
    logos: Map<String?, Bitmap?>,
    refreshStatus: WidgetRefreshStatus,
    isLight: Boolean,
) {
    val context = LocalContext.current
    val layout = widgetLayout()
    val size = LocalSize.current
    val visibleCount = StandingsWidgetLayout.visibleRowCount(size.height.value, drivers.size)
    val compact = size.width.value < 180f || size.height.value < 120f
    // Height controls row count; the remaining list area is shared evenly by visible rows.
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
            // Top accent line
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
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    F1WidgetText(
                        if (layout == WidgetLayout.Square) "DRIVERS" else "DRIVERS' STANDINGS",
                        color = F1WidgetPalette.primary(),
                        fontSize = 11f,
                        isLight = isLight,
                        font = "bold",
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    F1WidgetText(
                        when {
                            refreshStatus.failed && layout == WidgetLayout.Square -> "⚠"
                            refreshStatus.failed -> "⚠ 网络问题"
                            layout == WidgetLayout.Square -> "↻"
                            else -> "刷新"
                        },
                        color = if (refreshStatus.failed) F1WidgetPalette.primary() else F1WidgetPalette.textMuted(isLight),
                        fontSize = 10f,
                        isLight = isLight,
                        font = "bold",
                        modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetsAction>()),
                    )
                }

                if (drivers.isEmpty()) {
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
                        drivers.take(visibleCount).forEach { st ->
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .defaultWeight(),
                                contentAlignment = Alignment.Center,
                            ) {
                                DriverRow(st, headshots[st.code], logos[st.constructorId], isLight, compact)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverRow(st: WidgetDriverStanding, headshot: Bitmap?, logo: Bitmap?, isLight: Boolean, compact: Boolean) {
    val size = LocalSize.current
    val teamColor = ColorProvider(TeamPalette.forHex(st.teamHex))
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Team color vertical bar
        Box(
            modifier = GlanceModifier
                .width(3.dp)
                .height(32.dp)
                .background(teamColor),
        ) {}
        Spacer(GlanceModifier.width(6.dp))

        // Position
        F1WidgetText(
            "P${st.pos}",
            color = F1WidgetPalette.positionColor(st.pos, isLight),
            fontSize = 13f,
            isLight = isLight,
            font = "bold",
        )

        if (compact) {
            Spacer(GlanceModifier.width(6.dp))
        } else {
        // Driver headshot (28dp), fallback to car number badge
        if (headshot != null) {
            Image(
                provider = ImageProvider(headshot),
                contentDescription = st.code,
                modifier = GlanceModifier.width(28.dp).height(28.dp),
            )
        } else {
            Box(
                modifier = GlanceModifier
                    .width(28.dp)
                    .height(28.dp)
                    .background(teamColor)
                    .padding(2.dp),
                contentAlignment = Alignment.Center,
            ) {
                F1WidgetText(
                    st.code.take(3),
                    color = F1WidgetPalette.onPrimary(),
                    fontSize = 11f,
                    isLight = isLight,
                    font = "bold",
                )
            }
        }
        Spacer(GlanceModifier.width(8.dp))
        }

        // Driver code (with team logo) + team name
        Column(
            modifier = GlanceModifier.defaultWeight(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!compact && logo != null) {
                    Image(
                        provider = ImageProvider(logo),
                        contentDescription = st.teamName,
                        modifier = GlanceModifier.width(16.dp).height(16.dp),
                    )
                    Spacer(GlanceModifier.width(4.dp))
                }
                F1WidgetText(
                    st.code,
                    color = F1WidgetPalette.textPrimary(isLight),
                    fontSize = 13f,
                    isLight = isLight,
                    font = "bold",
                )
            }
            if (!compact) {
            // Full team name with Canvas-level auto-fit instead of pre-truncation.
            // Available width = widget width minus padding, left elements and the points column.
            F1WidgetText(
                st.teamName,
                color = F1WidgetPalette.textMuted(isLight),
                fontSize = 10f,
                isLight = isLight,
                font = "regular",
                maxWidthDp = (size.width.value - 150f).coerceAtLeast(40f),
            )
            }
        }

        // Points
        Column(
            horizontalAlignment = Alignment.End,
        ) {
            F1WidgetText(
                WidgetDataLoader.formatPoints(st.points),
                color = F1WidgetPalette.textPrimary(isLight),
                fontSize = 15f,
                isLight = isLight,
                font = "bold",
            )
            F1WidgetText(
                "PTS",
                color = F1WidgetPalette.textMuted(isLight),
                fontSize = 8f,
                isLight = isLight,
                font = "bold",
            )
        }
    }
}

class DriverStandingsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = DriverStandingsWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetInitializer.scheduleFromSettings(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetInitializer.cancelIfNoWidgets(context)
    }
}
