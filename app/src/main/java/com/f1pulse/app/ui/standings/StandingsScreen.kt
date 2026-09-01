package com.f1pulse.app.ui.standings

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.f1pulse.app.R
import com.f1pulse.app.domain.model.ConstructorStanding
import com.f1pulse.app.domain.model.DriverStanding
import com.f1pulse.app.ui.UiState
import com.f1pulse.app.ui.components.CarNumber
import com.f1pulse.app.ui.components.DriverHeadshot
import com.f1pulse.app.ui.components.EmptyState
import com.f1pulse.app.ui.components.ErrorState
import com.f1pulse.app.ui.components.HorizontalPageTransition
import com.f1pulse.app.ui.components.LoadingState
import com.f1pulse.app.ui.components.horizontalSwipeNavigation
import com.f1pulse.app.ui.nav.F1BottomBarLayout
import com.f1pulse.app.ui.components.TeamColoredChip
import com.f1pulse.app.ui.components.TeamLogo
import com.f1pulse.app.ui.theme.F1Pos1
import com.f1pulse.app.ui.theme.F1Pos2
import com.f1pulse.app.ui.theme.F1Pos3
import com.f1pulse.app.ui.theme.F1Red
import com.f1pulse.app.ui.theme.TeamPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandingsScreen(
    onNavigateToDriverDetail: (Int, String) -> Unit,
    onNavigateToConstructorDetail: (Int, String) -> Unit = { _, _ -> },
    selectedTab: StandingsTab = StandingsTab.DRIVERS,
    onSelectedTabChange: (StandingsTab) -> Unit = {},
    onNavigatePrevious: () -> Unit = {},
    onNavigateNext: () -> Unit = {},
    viewModel: StandingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tab = selectedTab
    val season = remember { java.time.Year.now().value }

    Column(
        Modifier
            .fillMaxSize()
            .horizontalSwipeNavigation(
                onSwipeLeft = {
                    if (tab == StandingsTab.DRIVERS) {
                        onSelectedTabChange(StandingsTab.CONSTRUCTORS)
                    } else {
                        onNavigateNext()
                    }
                },
                onSwipeRight = {
                    if (tab == StandingsTab.CONSTRUCTORS) {
                        onSelectedTabChange(StandingsTab.DRIVERS)
                    } else {
                        onNavigatePrevious()
                    }
                },
            ),
    ) {
        // Title header extends behind status bar, content respects notch
        Text(
            stringResource(R.string.nav_standings),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        val selectedTabIndex = if (tab == StandingsTab.DRIVERS) 0 else 1
        TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = F1Red,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = F1Red,
                        )
                    }
                },
            ) {
                Tab(
                    selected = tab == StandingsTab.DRIVERS,
                    onClick = { onSelectedTabChange(StandingsTab.DRIVERS) },
                    text = { Text(stringResource(R.string.standings_drivers)) },
                    selectedContentColor = F1Red,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Tab(
                    selected = tab == StandingsTab.CONSTRUCTORS,
                    onClick = { onSelectedTabChange(StandingsTab.CONSTRUCTORS) },
                    text = { Text(stringResource(R.string.standings_constructors)) },
                    selectedContentColor = F1Red,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalPageTransition(
                page = selectedTabIndex,
                modifier = Modifier.weight(1f),
            ) { visibleTab ->
                when (val s = state) {
                is UiState.Loading -> LoadingState()
                is UiState.Error -> ErrorState(message = s.message, onRetry = viewModel::refresh)
                is UiState.Success -> {
                    val empty = if (visibleTab == 0) s.data.drivers.isEmpty() else s.data.constructors.isEmpty()
                    if (empty) {
                        EmptyState(text = stringResource(R.string.error_empty))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 16.dp,
                                top = 12.dp,
                                end = 16.dp,
                                bottom = F1BottomBarLayout.ContentPadding,
                            ),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (visibleTab == 0) {
                                items(
                                    s.data.drivers,
                                    key = { "d_${it.driver.driverId.ifBlank { "${it.position}_${it.driver.fullName}" }}" },
                                    contentType = { "driver" },
                                ) { st ->
                                    DriverStandingRow(
                                        standing = st,
                                        onClick = {
                                            if (st.driver.driverId.isNotBlank()) {
                                                onNavigateToDriverDetail(season, st.driver.driverId)
                                            }
                                        },
                                    )
                                }
                            } else {
                                items(
                                    s.data.constructors,
                                    key = { "c_${it.team.constructorId.ifBlank { "${it.position}_${it.team.name}" }}" },
                                    contentType = { "constructor" },
                                ) { st ->
                                    ConstructorStandingRow(
                                        standing = st,
                                        onClick = {
                                            val cid = st.team.constructorId
                                            if (cid.isNotBlank()) {
                                                onNavigateToConstructorDetail(season, cid)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }
}

@Composable
private fun DriverStandingRow(
    standing: DriverStanding,
    onClick: () -> Unit,
) {
    val posColor = when (standing.position) {
        1 -> F1Pos1
        2 -> F1Pos2
        3 -> F1Pos3
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = standing.driver.driverId.isNotBlank(), onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Team color vertical bar (far left)
        standing.team?.colourHex?.let { hex ->
            Box(
                Modifier
                    .width(3.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TeamPalette.forHex(hex))
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            "${standing.position}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = posColor,
            modifier = Modifier.width(40.dp),
        )
        CarNumber(
            driverCode = standing.driver.code,
            teamHex = standing.team?.colourHex,
            size = 28.dp,
        )
        Spacer(Modifier.width(8.dp))
        DriverHeadshot(
            url = standing.driver.headshotUrl,
            code = standing.driver.code,
            size = 40.dp,
            teamHex = standing.team?.colourHex,
            useSquareAsset = true,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                standing.driver.code ?: standing.driver.fullName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                standing.driver.fullName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TeamColoredChip(
                colourHex = standing.team?.colourHex,
                label = standing.team?.name ?: "",
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${standing.points.toInt()}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.standings_points),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConstructorStandingRow(
    standing: ConstructorStanding,
    onClick: () -> Unit = {},
) {
    val posColor = when (standing.position) {
        1 -> F1Pos1
        2 -> F1Pos2
        3 -> F1Pos3
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = standing.team.constructorId.isNotBlank(), onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Team color vertical bar (far left)
        standing.team.colourHex?.let { hex ->
            Box(
                Modifier
                    .width(3.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TeamPalette.forHex(hex))
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            "${standing.position}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = posColor,
            modifier = Modifier.width(40.dp),
            maxLines = 1,
            softWrap = false,
        )
        TeamLogo(
            constructorId = standing.team.constructorId,
            colourHex = standing.team.colourHex,
            size = 40.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                standing.team.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            "${standing.points.toInt()}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}
