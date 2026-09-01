package com.f1pulse.app.ui.constructordetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.f1pulse.app.R
import com.f1pulse.app.core.CountryFlags
import com.f1pulse.app.core.TeamAccessibleColors
import com.f1pulse.app.domain.model.ConstructorStanding
import com.f1pulse.app.domain.model.DriverStanding
import com.f1pulse.app.domain.model.Team
import com.f1pulse.app.ui.UiState
import com.f1pulse.app.ui.components.AdaptiveBackButton
import com.f1pulse.app.ui.components.CarImage
import com.f1pulse.app.ui.components.CarNumber
import com.f1pulse.app.ui.components.ErrorState
import com.f1pulse.app.ui.components.LoadingState
import com.f1pulse.app.ui.components.SectionHeader
import com.f1pulse.app.ui.components.TeamLogo
import com.f1pulse.app.ui.components.driverHeadshotAsset
import com.f1pulse.app.ui.components.shimmerBrush
import com.f1pulse.app.ui.components.teamGradient
import com.f1pulse.app.ui.theme.F1Red
import com.f1pulse.app.ui.theme.Formula1WideFamily
import com.f1pulse.app.ui.theme.TeamPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstructorDetailScreen(
    onBack: () -> Unit,
    onNavigateToDriverDetail: (Int, String) -> Unit = { _, _ -> },
    viewModel: ConstructorDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backButtonBackground = when (val current = state) {
        is UiState.Success -> TeamPalette.forHex(
            TeamAccessibleColors.hexFor(current.data.standing.team.constructorId),
        )
        else -> MaterialTheme.colorScheme.background
    }

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            is UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(message = s.message, onRetry = viewModel::refresh)
            is UiState.Success -> ConstructorDetailContent(
                data = s.data,
                onNavigateToDriverDetail = onNavigateToDriverDetail,
            )
        }

        // The icon tint follows the luminance of its backing plate. On light
        // or white content it becomes black; on dark content it remains white.
        AdaptiveBackButton(
            onClick = onBack,
            backgroundColor = backButtonBackground,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 4.dp, top = 4.dp),
        )
    }
}

@Composable
private fun ConstructorDetailContent(
    data: ConstructorDetailUiData,
    onNavigateToDriverDetail: (Int, String) -> Unit,
) {
    val standing = data.standing
    val team = standing.team
    val season = remember { java.time.Year.now().value }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Hero — 赛车图放在车队渐变背景中
        ConstructorHeroSection(team, standing)

        // Below hero: horizontal padding 16dp for all cards
        Column(Modifier.padding(horizontal = 16.dp)) {
            // Team info — 车队详细资料
            SectionHeader(stringResource(R.string.driver_team))
            TeamInfoCard(team)
            Spacer(Modifier.height(16.dp))

            // Drivers lineup
            if (data.drivers.isNotEmpty()) {
                SectionHeader(stringResource(R.string.constructor_drivers))
                DriversCard(data.drivers, season, onNavigateToDriverDetail)
                Spacer(Modifier.height(16.dp))
            }

            // Championship stats
            SectionHeader(stringResource(R.string.driver_championship))
            ChampionshipCard(standing)

            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Hero 区：车队渐变背景 + 赛车图片居中放置 + 车队名称（普通 F1 字体）。
 * 不再使用花体字、大 logo、大排名。
 */
@Composable
private fun ConstructorHeroSection(team: Team, standing: ConstructorStanding) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .teamGradient(team.constructorId),
    ) {
        // 赛车图片居中放置在渐变背景中
        CarImage(
            constructorId = team.constructorId,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            height = 120.dp,
        )

        // 车队名称在底部，使用普通 F1 字体
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    team.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Formula1WideFamily,
                )
                Spacer(Modifier.height(8.dp))
                // 车队色条
                team.colourHex?.let { hex ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(width = 8.dp, height = 24.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(TeamPalette.forHex(hex)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "P${standing.position} · ${standing.points.toInt()} PTS",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            fontFamily = Formula1WideFamily,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
            // 小 logo 在右下角
            TeamLogo(
                constructorId = team.constructorId,
                colourHex = team.colourHex,
                size = 40.dp,
            )
        }
    }
}

@Composable
private fun DriversCard(
    drivers: List<DriverStanding>,
    season: Int,
    onNavigateToDriverDetail: (Int, String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            drivers.forEachIndexed { index, st ->
                if (index > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
                DriverLineupRow(st) {
                    if (st.driver.driverId.isNotBlank()) {
                        onNavigateToDriverDetail(season, st.driver.driverId)
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverLineupRow(
    standing: DriverStanding,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val driver = standing.driver
    val flag = remember(driver.countryCode, driver.nationality) {
        CountryFlags.toEmoji(driver.countryCode ?: CountryFlags.fromLocation(driver.nationality))
    }
    val teamHex = standing.team?.colourHex

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Circular headshot
        val localHeadshot = driverHeadshotAsset(driver.code)
        val headshotModel = driver.headshotUrl?.takeIf { it.isNotBlank() } ?: localHeadshot
        if (headshotModel != null) {
            val request = ImageRequest.Builder(context)
                .data(headshotModel)
                .crossfade(true)
                .build()
            SubcomposeAsyncImage(
                model = request,
                contentDescription = driver.fullName,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                loading = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(shimmerBrush())
                    )
                },
                error = {
                    if (!driver.headshotUrl.isNullOrBlank() && localHeadshot != null) {
                        AsyncImage(
                            model = localHeadshot,
                            contentDescription = driver.fullName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(TeamPalette.forHex(teamHex)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                driver.code ?: "?",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                },
            )
        } else {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TeamPalette.forHex(teamHex)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    driver.code ?: "?",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Name + position/points
        Column(modifier = Modifier.weight(1f)) {
            Text(
                driver.fullName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "P${standing.position} · ${standing.points.toInt()} PTS",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }

        // Driver number (team-colored)
        CarNumber(
            driverCode = driver.code,
            teamHex = teamHex,
            size = 28.dp,
        )

        if (flag != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                flag,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun TeamInfoCard(team: Team) {
    val flag = remember(team.nationality) {
        CountryFlags.fromLocation(team.nationality)
    }?.let { CountryFlags.toEmoji(it) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            InfoRow(stringResource(R.string.driver_nationality), buildString {
                if (flag != null) append("$flag ")
                append(team.nationality ?: "—")
            })
            InfoRow("车队代号", team.constructorId)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ChampionshipCard(standing: ConstructorStanding) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(

                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    "P${standing.position}",
                    style = MaterialTheme.typography.displaySmall,
                    color = F1Red,
                    fontFamily = Formula1WideFamily,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    stringResource(R.string.standings_pos),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(

                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    standing.points.toInt().toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.standings_points),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(

                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    standing.wins.toString(),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    stringResource(R.string.standings_wins),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
