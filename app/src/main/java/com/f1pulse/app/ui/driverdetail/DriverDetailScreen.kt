package com.f1pulse.app.ui.driverdetail

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.f1pulse.app.R
import com.f1pulse.app.core.CountryFlags
import com.f1pulse.app.core.TeamAccessibleColors
import com.f1pulse.app.domain.model.Driver
import com.f1pulse.app.domain.model.DriverStanding
import com.f1pulse.app.domain.model.Team
import com.f1pulse.app.ui.UiState
import com.f1pulse.app.ui.components.AdaptiveBackButton
import com.f1pulse.app.ui.components.CarNumber
import com.f1pulse.app.ui.components.DriverArtName
import com.f1pulse.app.ui.components.ErrorState
import com.f1pulse.app.ui.components.LoadingState
import com.f1pulse.app.ui.components.SectionHeader
import com.f1pulse.app.ui.components.TeamLogo
import com.f1pulse.app.ui.components.shimmerBrush
import com.f1pulse.app.ui.components.teamGradient
import com.f1pulse.app.ui.components.driverHeadshotAsset
import com.f1pulse.app.ui.theme.F1Red
import com.f1pulse.app.ui.theme.Formula1WideFamily
import com.f1pulse.app.ui.theme.TeamPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDetailScreen(
    onBack: () -> Unit,
    onNavigateToConstructorDetail: (Int, String) -> Unit = { _, _ -> },
    viewModel: DriverDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backButtonBackground = when (val current = state) {
        is UiState.Success -> {
            val team = current.data.standing?.team ?: current.data.driver.team
            TeamPalette.forHex(TeamAccessibleColors.hexFor(team?.constructorId))
        }
        else -> MaterialTheme.colorScheme.background
    }

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            is UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(message = s.message, onRetry = viewModel::refresh)
            is UiState.Success -> DriverDetailContent(
                data = s.data,
                onNavigateToConstructorDetail = onNavigateToConstructorDetail,
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
private fun DriverDetailContent(
    data: DriverDetailUiData,
    onNavigateToConstructorDetail: (Int, String) -> Unit,
) {
    val driver = data.driver
    val displayTeam = data.standing?.team ?: driver.team
    val season = remember { java.time.Year.now().value }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Hero — full width, no horizontal padding
        DriverHeroSection(driver, displayTeam)

        // Below hero: horizontal padding 16dp for all cards
        Column(Modifier.padding(horizontal = 16.dp)) {
            // Team info (clickable → constructor detail)
            displayTeam?.let { team ->
                SectionHeader(stringResource(R.string.driver_team))
                TeamInfoCard(team) {
                    if (team.constructorId.isNotBlank()) {
                        onNavigateToConstructorDetail(season, team.constructorId)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Bio — 车手详细资料
            SectionHeader(stringResource(R.string.driver_nationality))
            BioCard(driver)
            Spacer(Modifier.height(16.dp))

            // Championship stats (if standing != null)
            val standing = data.standing
            if (standing != null) {
                SectionHeader(stringResource(R.string.driver_championship))
                ChampionshipCard(standing)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DriverHeroSection(driver: Driver, team: Team?) {
    val flag = remember(driver.countryCode, driver.nationality) {
        CountryFlags.toEmoji(driver.countryCode ?: CountryFlags.fromLocation(driver.nationality))
    }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .teamGradient(team?.constructorId),
    ) {
        // Headshot pinned to the bottom-right edge, touching the gradient base
        val localHeadshotModel = driverHeadshotAsset(driver.code)
        val headshotModel = driver.headshotUrl?.takeIf { it.isNotBlank() } ?: localHeadshotModel
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
                    .align(Alignment.BottomEnd)
                    .height(280.dp)
                    .fillMaxWidth(0.45f),
                loading = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(shimmerBrush())
                    )
                },
                error = {
                    if (!driver.headshotUrl.isNullOrBlank() && localHeadshotModel != null) {
                        AsyncImage(
                            model = localHeadshotModel,
                            contentDescription = driver.fullName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(TeamPalette.forHex(team?.colourHex)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                driver.code ?: "?",
                                color = Color.White,
                                style = MaterialTheme.typography.displaySmall,
                            )
                        }
                    }
                },
            )
        }
        // Name + meta overlaid on top, bottom-aligned
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                DriverArtName(
                    givenName = driver.givenName,
                    familyName = driver.familyName,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (flag != null) {
                        Text(
                            flag,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    CarNumber(
                        driverCode = driver.code,
                        teamHex = team?.colourHex,
                        size = 48.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamInfoCard(team: Team, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeamLogo(
                constructorId = team.constructorId,
                colourHex = team.colourHex,
                size = 48.dp,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                team.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            team.colourHex?.let { hex ->
                Box(
                    Modifier
                        .size(width = 8.dp, height = 32.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(TeamPalette.forHex(hex)),
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BioCard(driver: Driver) {
    val flag = remember(driver.countryCode, driver.nationality) {
        CountryFlags.toEmoji(driver.countryCode ?: CountryFlags.fromLocation(driver.nationality))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            BioRow(stringResource(R.string.driver_nationality), buildString {
                if (flag != null) append("$flag ")
                append(driver.nationality.ifBlank { "—" })
            })
            BioRow(stringResource(R.string.driver_dob), driver.dateOfBirth ?: "—")
            BioRow(stringResource(R.string.driver_number), driver.permanentNumber?.toString() ?: "—")
            BioRow("代号", driver.code ?: "—")
        }
    }
}

@Composable
private fun BioRow(label: String, value: String) {
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
private fun ChampionshipCard(standing: DriverStanding) {
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
