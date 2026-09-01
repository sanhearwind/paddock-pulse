package com.f1pulse.app.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
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
import com.f1pulse.app.MainActivity
import com.f1pulse.app.ui.theme.TeamPalette

/**
 * Favorite team widget — smallest widget (1x2 horizontal / 2x1 vertical).
 *
 * Shows a single user-selected constructor's logo, position and points over a
 * team-color gradient. Configurable via [com.f1pulse.app.ui.widget.FavoriteTeamConfigScreen].
 *
 * If not yet configured, shows a "tap to configure" prompt.
 */
class FavoriteTeamWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val constructorId = FavoriteWidgetStore.read(context)
        val season = WidgetDataLoader.standingsSeason()
        val isLightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES

        val teamData: WidgetConstructorStanding? = constructorId?.let {
            WidgetDataLoader.findConstructor(context, season, it)
        }
        val teamLogo = teamData?.constructorId?.let {
            WidgetImageLoader.loadTeamLogo(context, it, isLightMode)
        }

        provideContent {
            F1WidgetTheme {
                if (teamData == null) {
                    UnconfiguredContent(isLightMode)
                } else {
                    FavoriteTeamContent(teamData, teamLogo, isLightMode)
                }
            }
        }
    }
}

// ── Unconfigured state ────────────────────────────────────────────────────────

@Composable
private fun UnconfiguredContent(isLight: Boolean) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(F1WidgetColors.surface)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            F1WidgetText(
                "F1",
                color = F1WidgetPalette.primary(),
                fontSize = 20f,
                isLight = isLight,
                font = "black",
            )
            F1WidgetText(
                "TAP TO CONFIGURE",
                color = F1WidgetPalette.textMuted(isLight),
                fontSize = 9f,
                isLight = isLight,
                font = "bold",
            )
        }
    }
}

// ── Team mode ─────────────────────────────────────────────────────────────────

@Composable
private fun FavoriteTeamContent(
    team: WidgetConstructorStanding,
    logo: Bitmap?,
    isLight: Boolean,
) {
    val context = LocalContext.current
    val layout = widgetLayout()
    val teamColor = TeamPalette.forHex(team.hex).toArgb()
    val gradient = GradientBitmapCache.get(teamColor)
    val contentPadding = when (layout) {
        WidgetLayout.Landscape -> 12.dp
        WidgetLayout.Portrait -> 10.dp
        WidgetLayout.Square -> 10.dp
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(ImageProvider(gradient))
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        if (isTinyWidget()) {
            TinyTeamLayout(team, logo, isLight)
        } else {
            when (layout) {
                WidgetLayout.Landscape -> LandscapeLayout(team, logo, isLight, contentPadding)
                WidgetLayout.Portrait -> PortraitLayout(team, logo, isLight, contentPadding)
                WidgetLayout.Square -> SquareLayout(team, logo, isLight, contentPadding)
            }
        }
    }
}


@Composable
private fun TinyTeamLayout(
    team: WidgetConstructorStanding,
    logo: Bitmap?,
    isLight: Boolean,
) {
    val size = LocalSize.current
    val availableWidth = (size.width.value - 42f).coerceAtLeast(24f)
    Row(modifier = GlanceModifier.fillMaxSize().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (logo != null) {
            Image(provider = ImageProvider(logo), contentDescription = team.name, modifier = GlanceModifier.width(24.dp).height(24.dp))
        } else {
            Box(modifier = GlanceModifier.width(24.dp).height(24.dp), contentAlignment = Alignment.Center) {
                F1WidgetText(team.name.take(1).uppercase(), F1WidgetPalette.onPrimary(), 12f, isLight, font = "black")
            }
        }
        Spacer(GlanceModifier.width(4.dp))
        F1WidgetText("P${team.pos} · ${WidgetDataLoader.formatPoints(team.points)} PTS", F1WidgetPalette.textPrimary(isLight), 10f, isLight, font = "bold", maxWidthDp = availableWidth, modifier = GlanceModifier.defaultWeight())
    }
}

@Composable
private fun LandscapeLayout(
    team: WidgetConstructorStanding,
    logo: Bitmap?,
    isLight: Boolean,
    padding: androidx.compose.ui.unit.Dp,
) {
    val size = LocalSize.current
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Team logo (40dp)
        if (logo != null) {
            Image(
                provider = ImageProvider(logo),
                contentDescription = team.name,
                modifier = GlanceModifier.width(40.dp).height(40.dp),
            )
        } else {
            Box(
                modifier = GlanceModifier.width(40.dp).height(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                F1WidgetText(
                    team.name.take(1).uppercase(),
                    color = 0xFFFFFFFF.toInt(),
                    fontSize = 20f,
                    isLight = isLight,
                    font = "black",
                )
            }
        }
        Spacer(GlanceModifier.width(10.dp))
        // Info column (single elastic region)
        Column(
            modifier = GlanceModifier.defaultWeight(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                F1WidgetText(
                    "P${team.pos}",
                    color = 0xFFFFFFFF.toInt(),
                    fontSize = 18f,
                    isLight = isLight,
                    font = "black",
                )
                Spacer(GlanceModifier.width(8.dp))
                // Full team name with Canvas-level auto-fit instead of pre-truncation.
                F1WidgetText(
                    team.name,
                    color = 0xCCFFFFFF.toInt(),
                    fontSize = 11f,
                    isLight = isLight,
                    font = "regular",
                    maxWidthDp = (size.width.value - 112f).coerceAtLeast(30f),
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                F1WidgetText(
                    WidgetDataLoader.formatPoints(team.points),
                    color = 0xFFFFFFFF.toInt(),
                    fontSize = 20f,
                    isLight = isLight,
                    font = "bold",
                )
                Spacer(GlanceModifier.width(3.dp))
                F1WidgetText(
                    "PTS",
                    color = 0xAAFFFFFF.toInt(),
                    fontSize = 9f,
                    isLight = isLight,
                    font = "bold",
                )
            }
        }
    }
}

/**
 * 竖向布局改为左右结构：左侧车队 logo，右侧信息垂直排列。
 * 避免在窄宽度下文字堆叠在 logo 下方导致拥挤。
 */
@Composable
private fun PortraitLayout(
    team: WidgetConstructorStanding,
    logo: Bitmap?,
    isLight: Boolean,
    padding: androidx.compose.ui.unit.Dp,
) {
    val size = LocalSize.current
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: team logo
        if (logo != null) {
            Image(
                provider = ImageProvider(logo),
                contentDescription = team.name,
                modifier = GlanceModifier.width(40.dp).height(40.dp),
            )
        } else {
            Box(
                modifier = GlanceModifier.width(40.dp).height(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                F1WidgetText(
                    team.name.take(1).uppercase(),
                    color = 0xFFFFFFFF.toInt(),
                    fontSize = 20f,
                    isLight = isLight,
                    font = "black",
                )
            }
        }
        Spacer(GlanceModifier.width(10.dp))
        // Right: team name, position, points
        Column(
            modifier = GlanceModifier.defaultWeight(),
        ) {
            F1WidgetText(
                team.name,
                color = 0xCCFFFFFF.toInt(),
                fontSize = 11f,
                isLight = isLight,
                font = "regular",
                maxWidthDp = (size.width.value - 72f).coerceAtLeast(30f),
            )
            Row(verticalAlignment = Alignment.Bottom) {
                F1WidgetText(
                    "P${team.pos}",
                    color = 0xFFFFFFFF.toInt(),
                    fontSize = 18f,
                    isLight = isLight,
                    font = "black",
                )
                Spacer(GlanceModifier.width(8.dp))
                F1WidgetText(
                    WidgetDataLoader.formatPoints(team.points),
                    color = 0xFFFFFFFF.toInt(),
                    fontSize = 18f,
                    isLight = isLight,
                    font = "bold",
                )
                Spacer(GlanceModifier.width(3.dp))
                F1WidgetText(
                    "PTS",
                    color = 0xAAFFFFFF.toInt(),
                    fontSize = 9f,
                    isLight = isLight,
                    font = "bold",
                )
            }
        }
    }
}

@Composable
private fun SquareLayout(
    team: WidgetConstructorStanding,
    logo: Bitmap?,
    isLight: Boolean,
    padding: androidx.compose.ui.unit.Dp,
) {
    val size = LocalSize.current
    val logoSize = adaptiveDp(48f).dp
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Team logo (48dp, scaled with adaptiveDp)
        if (logo != null) {
            Image(
                provider = ImageProvider(logo),
                contentDescription = team.name,
                modifier = GlanceModifier.width(logoSize).height(logoSize),
            )
        } else {
            Box(
                modifier = GlanceModifier.width(logoSize).height(logoSize),
                contentAlignment = Alignment.Center,
            ) {
                F1WidgetText(
                    team.name.take(1).uppercase(),
                    color = 0xFFFFFFFF.toInt(),
                    fontSize = 24f,
                    isLight = isLight,
                    font = "black",
                )
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        // Full team name, centered, Canvas-level auto-fit.
        F1WidgetText(
            team.name,
            color = 0xCCFFFFFF.toInt(),
            fontSize = 12f,
            isLight = isLight,
            font = "regular",
            maxWidthDp = (size.width.value - 20f).coerceAtLeast(30f),
        )
        Spacer(GlanceModifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            F1WidgetText(
                "P${team.pos}",
                color = 0xFFFFFFFF.toInt(),
                fontSize = 14f,
                isLight = isLight,
                font = "black",
            )
            Spacer(GlanceModifier.width(6.dp))
            F1WidgetText(
                WidgetDataLoader.formatPoints(team.points),
                color = 0xFFFFFFFF.toInt(),
                fontSize = 16f,
                isLight = isLight,
                font = "bold",
            )
            Spacer(GlanceModifier.width(3.dp))
            F1WidgetText(
                "PTS",
                color = 0xAAFFFFFF.toInt(),
                fontSize = 9f,
                isLight = isLight,
                font = "bold",
            )
        }
    }
    }
}

class FavoriteTeamWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = FavoriteTeamWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetInitializer.scheduleFromSettings(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetInitializer.cancelIfNoWidgets(context)
    }
}
