package com.f1pulse.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.f1pulse.app.R
import com.f1pulse.app.ui.UiState
import com.f1pulse.app.ui.calendar.RaceRow
import com.f1pulse.app.ui.components.EmptyState
import com.f1pulse.app.ui.components.ErrorState
import com.f1pulse.app.ui.components.LoadingState
import com.f1pulse.app.ui.components.horizontalSwipeNavigation
import com.f1pulse.app.ui.nav.F1BottomBarLayout
import com.f1pulse.app.ui.theme.F1Red

@Composable
fun HistoryScreen(
    onNavigateToRaceDetail: (Int, Int) -> Unit,
    onNavigatePrevious: () -> Unit = {},
    onNavigateNext: () -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedSeason by viewModel.selectedSeason.collectAsStateWithLifecycle()
    var showSeasonPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .horizontalSwipeNavigation(
                onSwipeLeft = onNavigateNext,
                onSwipeRight = onNavigatePrevious,
            ),
    ) {
        Text(
            text = stringResource(R.string.nav_history),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )

        val refreshing = state is UiState.Loading ||
            (state as? UiState.Success)?.data?.isRefreshing == true
        SeasonToolbar(
            season = selectedSeason,
            refreshing = refreshing,
            onChooseSeason = { showSeasonPicker = true },
            onRefresh = viewModel::refresh,
        )

        when (val current = state) {
            is UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(
                message = current.message,
                onRetry = viewModel::refresh,
            )
            is UiState.Success -> {
                val data = current.data
                if (data.races.isEmpty()) {
                    EmptyState(text = stringResource(R.string.history_no_results))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 12.dp,
                            end = 16.dp,
                            bottom = F1BottomBarLayout.ContentPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = data.races,
                            key = { "history_${it.season}_${it.round}" },
                        ) { race ->
                            RaceRow(
                                race = race,
                                timeZoneMode = data.timeZoneMode,
                                onClick = {
                                    onNavigateToRaceDetail(race.season, race.round)
                                },
                            )
                        }
                    }
                }

            }
        }

        if (showSeasonPicker) {
            SeasonPickerDialog(
                seasons = viewModel.availableSeasons,
                selectedSeason = selectedSeason,
                onSelect = {
                    showSeasonPicker = false
                    viewModel.selectSeason(it)
                },
                onDismiss = { showSeasonPicker = false },
            )
        }
    }
}

@Composable
private fun SeasonToolbar(
    season: Int,
    refreshing: Boolean,
    onChooseSeason: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onChooseSeason),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.history_season),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = season.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = stringResource(R.string.history_choose_season),
                    tint = F1Red,
                )
            }
        }
        IconButton(onClick = onRefresh, enabled = !refreshing) {
            if (refreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = F1Red,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.refresh),
                    tint = F1Red,
                )
            }
        }
    }
}

@Composable
private fun SeasonPickerDialog(
    seasons: List<Int>,
    selectedSeason: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_choose_season)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(seasons, key = { it }) { season ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(season) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = season.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (season == selectedSeason) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                            color = if (season == selectedSeason) {
                                F1Red
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f),
                        )
                        if (season == selectedSeason) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = F1Red,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.history_close))
            }
        },
    )
}
