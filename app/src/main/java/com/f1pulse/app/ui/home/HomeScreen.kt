package com.f1pulse.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.f1pulse.app.R
import com.f1pulse.app.core.CountryFlags
import com.f1pulse.app.core.time.TimeFormatter
import com.f1pulse.app.core.time.TimeZoneMode
import com.f1pulse.app.domain.model.Race
import com.f1pulse.app.ui.UiState
import com.f1pulse.app.ui.components.EmptyState
import com.f1pulse.app.ui.components.ErrorState
import com.f1pulse.app.ui.components.HomeRaceCountdown
import com.f1pulse.app.ui.components.LoadingState
import com.f1pulse.app.ui.components.PodiumCard
import com.f1pulse.app.ui.components.SectionHeader
import com.f1pulse.app.ui.components.horizontalSwipeNavigation
import com.f1pulse.app.ui.nav.F1BottomBarLayout
import com.f1pulse.app.ui.theme.F1Red
import com.f1pulse.app.ui.theme.NorthwellCleanAltFamily

@Composable
fun HomeScreen(
    onNavigateToStandings: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRaceDetail: (Int, Int) -> Unit,
    onNavigateToDriverDetail: (Int, String) -> Unit,
    onSwipeLeft: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalSwipeNavigation(onSwipeLeft = onSwipeLeft),
    ) {
        when (val s = state) {
            is UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(message = s.message, onRetry = viewModel::refresh)
            is UiState.Success -> HomeContent(
                data = s.data,
                onRefresh = viewModel::refresh,
                onNavigateToStandings = onNavigateToStandings,
                onNavigateToRaceDetail = onNavigateToRaceDetail,
                onNavigateToDriverDetail = onNavigateToDriverDetail,
                onNavigateToSettings = onNavigateToSettings,
            )
        }
    }
}

@Composable
private fun HomeTopBar(onNavigateToSettings: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val logoColorFilter = if (!isDark) ColorFilter.tint(F1Red, BlendMode.SrcIn) else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_f1_logo),
            contentDescription = "F1",
            colorFilter = logoColorFilter,
            modifier = Modifier.height(22.dp).padding(top = 2.dp),
        )
        Spacer(Modifier.width(4.dp))
        Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.Center) {
            Text(
                "Pulse",
                fontFamily = NorthwellCleanAltFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 32.sp,
                color = F1Red,
                letterSpacing = 1.sp,
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onNavigateToSettings) {
            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_settings))
        }
    }
}

@Composable
private fun HomeContent(
    data: HomeUiData,
    onRefresh: () -> Unit,
    onNavigateToStandings: () -> Unit,
    onNavigateToRaceDetail: (Int, Int) -> Unit,
    onNavigateToDriverDetail: (Int, String) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = F1BottomBarLayout.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item(key = "app-bar") { HomeTopBar(onNavigateToSettings) }

        if (data.isOffline) {
            item(key = "offline") {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    OfflineBanner()
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        item(key = "section-countdown") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionHeader(stringResource(R.string.home_lights_out))
                val next = data.nextRace
                if (next != null && next.sessions.race != null) {
                    HomeRaceCountdown(
                        target = next.sessions.race,
                    )
                    Spacer(Modifier.height(8.dp))
                    NextRaceCard(
                        race = next,
                        zone = data.zone,
                        timeZoneMode = data.timeZoneMode,
                        onClick = { onNavigateToRaceDetail(next.season, next.round) },
                    )
                } else {
                    EmptyState(text = stringResource(R.string.home_no_upcoming))
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        item(key = "section-podium") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionHeader(stringResource(R.string.home_last_result))
                val top3 = data.lastResultTop3
                if (top3.size >= 3) {
                    PodiumCard(
                        p1 = top3[0],
                        p2 = top3[1],
                        p3 = top3[2],
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.home_results_network_issue),
                        color = F1Red,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        item(key = "section-standings-header") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionHeader(stringResource(R.string.home_standings_snapshot))
            }
        }

        if (data.podium.isNotEmpty()) {
            item(key = "section-standings-card") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    data.podium.forEach { standing ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (standing.driver.driverId.isNotEmpty()) {
                                        onNavigateToDriverDetail(data.season, standing.driver.driverId)
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "P${standing.position}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = F1Red,
                                modifier = Modifier.width(40.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    standing.driver.code ?: standing.driver.fullName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    standing.team?.name ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "${standing.points.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(48.dp),
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                    TextButton(onClick = onNavigateToStandings, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_view_all), color = F1Red)
                    }
                }
            }
        }

        item(key = "section-bottom-spacer") {
            Spacer(Modifier.height(24.dp + 80.dp))
        }
    }
}

@Composable
private fun NextRaceCard(
    race: Race,
    zone: java.time.ZoneId,
    timeZoneMode: TimeZoneMode,
    onClick: () -> Unit,
) {
    val raceInstant = race.sessions.race
    val flagEmoji = remember(race.circuit.country) {
        CountryFlags.toEmoji(CountryFlags.fromLocation(race.circuit.country))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (flagEmoji != null) {
                Text(flagEmoji, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    race.raceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    race.circuit.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (raceInstant != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                TimeFormatter.formatFullDateTime(raceInstant, zone),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = F1Red,
            )
            Text(
                stringResource(
                    if (timeZoneMode == TimeZoneMode.DEVICE) R.string.timezone_device_label
                    else R.string.timezone_race_local_label,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OfflineBanner() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(F1Red.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            stringResource(R.string.no_connection),
            style = MaterialTheme.typography.bodySmall,
            color = F1Red,
        )
    }
}
