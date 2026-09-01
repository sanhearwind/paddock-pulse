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
import com.f1pulse.app.ui.theme.TeamPalette

/**
 * Favorite driver widget — smallest widget (1x2 horizontal / 2x1 vertical).
 *
 * Shows a single user-selected driver's headshot, team logo, code, position and
 * points over a team-color gradient. Configurable via
 * [com.f1pulse.app.ui.widget.FavoriteDriverConfigScreen].
 *
 * 当用户把 widget 放大到 2x2 及以上时，使用车手半身照而非大头照，
 * 半身照偏右放置，排名和积分居中偏左。
 *
 * If not yet configured, shows a "tap to configure" prompt.
 */
class FavoriteDriverWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val driverCode = FavoriteDriverWidgetStore.read(context)
        val season = WidgetDataLoader.standingsSeason()
        val isLightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES

        val driverData: WidgetDriverStanding? = driverCode?.let {
            WidgetDataLoader.findDriver(context, season, it)
        }
        val headshot = driverData?.let { WidgetImageLoader.loadHeadshot(context, it.code) }
        val halfBody = driverData?.let {
            // 2x2 及以上目标高度约 widget 高度，按 dp 转 px 约 2-3 倍
            WidgetImageLoader.loadHalfBody(context, it.code, targetHeightPx = 480)
        }
        val teamLogo = driverData?.constructorId?.let {
            WidgetImageLoader.loadTeamLogo(context, it, isLightMode = false)
        }

        provideContent {
            F1WidgetTheme {
                if (driverData == null) {
                    UnconfiguredContent(isLightMode)
                } else {
                    FavoriteDriverContent(driverData, headshot, halfBody, teamLogo, isLightMode)
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

// ── Driver mode ───────────────────────────────────────────────────────────────

@Composable
private fun FavoriteDriverContent(
    driver: WidgetDriverStanding,
    headshot: Bitmap?,
    halfBody: Bitmap?,
    logo: Bitmap?,
    isLight: Boolean,
) {
    val context = LocalContext.current
    val layout = widgetLayout()
    val size = LocalSize.current
    val teamColor = TeamPalette.forHex(driver.teamHex).toArgb()
    val gradient = GradientBitmapCache.get(teamColor)
    val contentPadding = when (layout) {
        WidgetLayout.Landscape -> 12.dp
        WidgetLayout.Portrait -> 10.dp
        WidgetLayout.Square -> 10.dp
    }
    // 2x2 或同等高度的横向尺寸使用参考图式半身主视觉；窄纵向继续使用紧凑布局
    val useHalfBody = halfBody != null && layout != WidgetLayout.Portrait && size.width.value >= 130f && size.height.value >= 130f

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(ImageProvider(gradient))
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        if (isTinyWidget()) {
            TinyDriverLayout(driver, headshot, isLight)
        } else if (useHalfBody) {
            HalfBodyDriverLayout(driver, halfBody, logo, isLight, contentPadding)
        } else {
            when (layout) {
                WidgetLayout.Landscape -> LandscapeDriverLayout(driver, headshot, logo, isLight, contentPadding)
                WidgetLayout.Portrait -> PortraitDriverLayout(driver, headshot, logo, isLight, contentPadding)
                WidgetLayout.Square -> SquareDriverLayout(driver, headshot, logo, isLight, contentPadding)
            }
        }
    }
}


@Composable
private fun TinyDriverLayout(
    driver: WidgetDriverStanding,
    headshot: Bitmap?,
    isLight: Boolean,
) {
    val size = LocalSize.current
    val availableWidth = (size.width.value - 42f).coerceAtLeast(24f)
    Row(modifier = GlanceModifier.fillMaxSize().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (headshot != null) {
            Image(provider = ImageProvider(headshot), contentDescription = driver.code, modifier = GlanceModifier.width(24.dp).height(24.dp))
        } else {
            Box(modifier = GlanceModifier.width(24.dp).height(24.dp), contentAlignment = Alignment.Center) {
                F1WidgetText((driver.driverNumber ?: driver.code.take(2)).toString(), F1WidgetPalette.onPrimary(), 11f, isLight, font = "bold")
            }
        }
        Spacer(GlanceModifier.width(4.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            F1WidgetText("${driver.code} · P${driver.pos}", F1WidgetPalette.textPrimary(isLight), 10f, isLight, font = "bold", maxWidthDp = availableWidth)
            F1WidgetText("${WidgetDataLoader.formatPoints(driver.points)} PTS", F1WidgetPalette.textMuted(isLight), 8f, isLight, font = "regular", maxWidthDp = availableWidth)
        }
    }
}

@Composable
private fun LandscapeDriverLayout(
    driver: WidgetDriverStanding,
    headshot: Bitmap?,
    logo: Bitmap?,
    isLight: Boolean,
    padding: androidx.compose.ui.unit.Dp,
) {
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Headshot (40dp), fallback to car number badge
        if (headshot != null) {
            Image(
                provider = ImageProvider(headshot),
                contentDescription = driver.code,
                modifier = GlanceModifier.width(40.dp).height(40.dp),
            )
        } else {
            Box(
                modifier = GlanceModifier.width(40.dp).height(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                F1WidgetText(
                    (driver.driverNumber ?: driver.code.take(2)).toString(),
                    color = 0xFFFFFFFF.toInt(),
                    fontSize = 16f,
                    isLight = isLight,
                    font = "bold",
                )
            }
        }
        Spacer(GlanceModifier.width(8.dp))
        // Info column
        Column(
            modifier = GlanceModifier.defaultWeight(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                F1WidgetText(
                    "P${driver.pos}",
                    color = 0xFFFFFFFF.toInt(),
                    fontSize = 18f,
                    isLight = isLight,
                    font = "black",
                )
                Spacer(GlanceModifier.width(6.dp))
                F1WidgetText(
                    driver.code,
                    color = 0xFFFFFFFF.toInt(),
                    fontSize = 15f,
                    isLight = isLight,
                    font = "bold",
                )
                if (logo != null) {
                    Spacer(GlanceModifier.width(4.dp))
                    Image(
                        provider = ImageProvider(logo),
                        contentDescription = driver.teamName,
                        modifier = GlanceModifier.width(14.dp).height(14.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                F1WidgetText(
                    WidgetDataLoader.formatPoints(driver.points),
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

/**
 * 竖向布局改为左右结构：左侧车手大头照，右侧信息垂直排列。
 * 避免在窄宽度下文字堆叠在头像下方导致拥挤。
 */
@Composable
private fun PortraitDriverLayout(
    driver: WidgetDriverStanding,
    headshot: Bitmap?,
    logo: Bitmap?,
    isLight: Boolean,
    padding: androidx.compose.ui.unit.Dp,
) {
    val size = LocalSize.current
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: headshot
        if (headshot != null) {
            Image(
                provider = ImageProvider(headshot),
                contentDescription = driver.code,
                modifier = GlanceModifier.width(40.dp).height(40.dp),
            )
        } else {
            Box(
                modifier = GlanceModifier.width(40.dp).height(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                F1WidgetText(
                    (driver.driverNumber ?: driver.code.take(2)).toString(),
                    color = 0xFFFFFFFF.toInt(),
                    fontSize = 16f,
                    isLight = isLight,
                    font = "bold",
                )
            }
        }
        Spacer(GlanceModifier.width(8.dp))
        // Right: code, position, points
        Column(
            modifier = GlanceModifier.defaultWeight(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                F1WidgetText(
                    driver.code,
                    color = 0xFFFFFFFF.toInt(),
                    fontSize = 15f,
                    isLight = isLight,
                    font = "bold",
                )
                if (logo != null) {
                    Spacer(GlanceModifier.width(4.dp))
                    Image(
                        provider = ImageProvider(logo),
                        contentDescription = driver.teamName,
                        modifier = GlanceModifier.width(14.dp).height(14.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                F1WidgetText(
                    "P${driver.pos}",
                    color = 0xFFFFFFFF.toInt(),
                    fontSize = 18f,
                    isLight = isLight,
                    font = "black",
                )
                Spacer(GlanceModifier.width(6.dp))
                F1WidgetText(
                    WidgetDataLoader.formatPoints(driver.points),
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

/**
 * 2x2 及以上布局：使用半身照，半身照偏右，排名和积分居中偏左。
 */
@Composable
private fun HalfBodyDriverLayout(
    driver: WidgetDriverStanding,
    halfBody: Bitmap,
    logo: Bitmap?,
    isLight: Boolean,
    padding: androidx.compose.ui.unit.Dp,
) {
    val size = LocalSize.current
    val cardWidth = size.width.value.coerceAtLeast(130f)
    val cardHeight = size.height.value.coerceAtLeast(130f)
    val portraitAspect = 0.56f
    // Cap the design width, but let the ImageView match the host's actual
    // height. Some launchers briefly reuse a RemoteViews layout generated for
    // an older widget size; a fixed dp height can then overflow and clip the
    // driver. MATCH_PARENT is resolved against the current host container.
    val imageWidth = (cardHeight.coerceAtMost(200f) * portraitAspect)
        .coerceAtMost(cardWidth * 0.62f)
        .coerceAtLeast(64f)
    val infoWidth = (cardWidth * 0.58f)
        .coerceAtMost(cardWidth - padding.value)
        .coerceAtLeast(76f)
    val compactHero = cardWidth < 190f || cardHeight < 190f

    Box(modifier = GlanceModifier.fillMaxSize()) {
        // The portrait is the visual anchor: edge-to-edge on the right and
        // bottom-aligned, while the information layer remains above it.
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(top = 4.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Image(
                provider = ImageProvider(halfBody),
                contentDescription = driver.code,
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier
                    .width(imageWidth.dp)
                    .fillMaxHeight(),
            )
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(start = padding, top = padding, bottom = padding),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column(modifier = GlanceModifier.width(infoWidth.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    F1WidgetText(
                        driver.code,
                        color = 0xFFFFFFFF.toInt(),
                        fontSize = if (compactHero) 18f else 22f,
                        isLight = isLight,
                        font = "bold",
                        maxWidthDp = (infoWidth - if (logo != null) 28f else 0f).coerceAtLeast(36f),
                    )
                    if (logo != null) {
                        Spacer(GlanceModifier.width(6.dp))
                        Image(
                            provider = ImageProvider(logo),
                            contentDescription = driver.teamName,
                            modifier = GlanceModifier
                                .width(if (compactHero) 18.dp else 22.dp)
                                .height(if (compactHero) 18.dp else 22.dp),
                        )
                    }
                }
                Spacer(GlanceModifier.height(if (compactHero) 4.dp else 6.dp))
                F1WidgetText(
                    "P${driver.pos}",
                    color = 0xFFFFFFFF.toInt(),
                    fontSize = if (compactHero) 44f else 56f,
                    isLight = isLight,
                    font = "black",
                    maxWidthDp = infoWidth,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    F1WidgetText(
                        WidgetDataLoader.formatPoints(driver.points),
                        color = 0xFFFFFFFF.toInt(),
                        fontSize = if (compactHero) 25f else 31f,
                        isLight = isLight,
                        font = "black",
                        maxWidthDp = (infoWidth - 28f).coerceAtLeast(42f),
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    F1WidgetText(
                        "PTS",
                        color = 0xE6FFFFFF.toInt(),
                        fontSize = if (compactHero) 9f else 11f,
                        isLight = isLight,
                        font = "bold",
                    )
                }
            }
        }
    }
}

/**
 * Square: centered, compact middle-ground. Main container is a centered
 * [Column] with a larger 48dp headshot. Exactly one elastic region is
 * allowed; this layout uses none and relies on [Alignment.CenterVertically]
 * to distribute the fixed-gap content vertically.
 */
@Composable
private fun SquareDriverLayout(
    driver: WidgetDriverStanding,
    headshot: Bitmap?,
    logo: Bitmap?,
    isLight: Boolean,
    padding: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Larger headshot (48dp), fallback to car number badge
        if (headshot != null) {
            Image(
                provider = ImageProvider(headshot),
                contentDescription = driver.code,
                modifier = GlanceModifier.width(48.dp).height(48.dp),
            )
        } else {
            Box(
                modifier = GlanceModifier.width(48.dp).height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                F1WidgetText(
                    (driver.driverNumber ?: driver.code.take(2)).toString(),
                    color = 0xFFFFFFFF.toInt(),
                    fontSize = 18f,
                    isLight = isLight,
                    font = "bold",
                )
            }
        }
        Spacer(GlanceModifier.height(4.dp))
        F1WidgetText(
            driver.code,
            color = 0xFFFFFFFF.toInt(),
            fontSize = 15f,
            isLight = isLight,
            font = "bold",
        )
        Spacer(GlanceModifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            F1WidgetText(
                "P${driver.pos}",
                color = 0xFFFFFFFF.toInt(),
                fontSize = 14f,
                isLight = isLight,
                font = "black",
            )
            if (logo != null) {
                Spacer(GlanceModifier.width(4.dp))
                Image(
                    provider = ImageProvider(logo),
                    contentDescription = driver.teamName,
                    modifier = GlanceModifier.width(12.dp).height(12.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            F1WidgetText(
                WidgetDataLoader.formatPoints(driver.points),
                color = 0xFFFFFFFF.toInt(),
                fontSize = 16f,
                isLight = isLight,
                font = "bold",
            )
            Spacer(GlanceModifier.width(2.dp))
            F1WidgetText(
                "PTS",
                color = 0xAAFFFFFF.toInt(),
                fontSize = 8f,
                isLight = isLight,
                font = "bold",
            )
        }
    }
    }
}

class FavoriteDriverWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = FavoriteDriverWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetInitializer.scheduleFromSettings(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetInitializer.cancelIfNoWidgets(context)
    }
}
