package com.f1pulse.app.ui.calendar

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.f1pulse.app.core.CountryFlags
import com.f1pulse.app.core.time.RaceTiming
import com.f1pulse.app.core.time.TimeFormatter
import com.f1pulse.app.core.time.TimeZoneMode
import com.f1pulse.app.domain.model.Race
import com.f1pulse.app.ui.UiState
import com.f1pulse.app.ui.components.EmptyState
import com.f1pulse.app.ui.components.ErrorState
import com.f1pulse.app.ui.components.HorizontalPageTransition
import com.f1pulse.app.ui.components.LoadingState
import com.f1pulse.app.ui.components.horizontalSwipeNavigation
import com.f1pulse.app.ui.nav.F1BottomBarLayout
import com.f1pulse.app.ui.theme.F1Green
import com.f1pulse.app.ui.theme.F1Red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateToRaceDetail: (Int, Int) -> Unit,
    selectedTabIndex: Int = 0,
    onSelectedTabChange: (Int) -> Unit = {},
    onNavigatePrevious: () -> Unit = {},
    onNavigateNext: () -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tabIndex = selectedTabIndex.coerceIn(0, 1)
    val tabs = listOf(
        stringResource(R.string.calendar_upcoming),
        stringResource(R.string.calendar_past),
    )

    Column(
        Modifier
            .fillMaxSize()
            .horizontalSwipeNavigation(
                onSwipeLeft = {
                    if (tabIndex == 0) onSelectedTabChange(1) else onNavigateNext()
                },
                onSwipeRight = {
                    if (tabIndex == 1) onSelectedTabChange(0) else onNavigatePrevious()
                },
            ),
    ) {
        // Title header extends behind status bar, content respects notch
        Text(
            stringResource(R.string.nav_calendar),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = F1Red,
            indicator = { tabPositions ->
                if (tabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        color = F1Red,
                    )
                }
            },
        ) {
            tabs.forEachIndexed { i, t ->
                Tab(
                    selected = tabIndex == i,
                    onClick = { onSelectedTabChange(i) },
                    text = { Text(t) },
                    selectedContentColor = F1Red,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalPageTransition(
            page = tabIndex,
            modifier = Modifier.weight(1f),
        ) { visibleTab ->
            when (val s = state) {
            is UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(message = s.message, onRetry = viewModel::refresh)
            is UiState.Success -> {
                val races = if (visibleTab == 0) s.data.upcoming else s.data.past
                if (races.isEmpty()) {
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(races, key = { "${it.season}_${it.round}" }) { race ->
                            RaceRow(
                                race = race,
                                timeZoneMode = s.data.timeZoneMode,
                                onClick = { onNavigateToRaceDetail(race.season, race.round) },
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
internal fun RaceRow(
    race: Race,
    timeZoneMode: TimeZoneMode,
    onClick: () -> Unit,
) {
    val raceInstant = race.sessions.race
    val flagEmoji = remember(race.circuit.country) {
        CountryFlags.toEmoji(CountryFlags.fromLocation(race.circuit.country))
    }
    val isLive = remember(raceInstant) { RaceTiming.isLive(raceInstant) }
    val zone = remember(race.circuit.timeZone, timeZoneMode) {
        when (timeZoneMode) {
            TimeZoneMode.DEVICE -> java.time.ZoneId.systemDefault()
            TimeZoneMode.RACE_LOCAL -> race.circuit.timeZone
                ?.let { runCatching { java.time.ZoneId.of(it) }.getOrNull() }
                ?: java.time.ZoneId.systemDefault()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Round badge
            Box(
                Modifier
                    .width(48.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(F1Red),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "R${race.round}",
                        style = MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color.White,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            // Race name + circuit
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (flagEmoji != null) {
                        Text(flagEmoji, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        race.raceName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                Text(
                    race.circuit.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                if (raceInstant != null) {
                    Text(
                        TimeFormatter.formatSessionTime(raceInstant, zone),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(if (timeZoneMode == TimeZoneMode.DEVICE) R.string.timezone_device_label else R.string.timezone_race_local_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Status badge
            StatusBadge(isFinished = race.isFinished, isLive = isLive)
        }
    }
}

@Composable
private fun StatusBadge(isFinished: Boolean, isLive: Boolean) {
    val (text, color) = when {
        isLive -> stringResource(R.string.session_live) to F1Green
        isFinished -> stringResource(R.string.session_completed) to MaterialTheme.colorScheme.onSurfaceVariant
        else -> stringResource(R.string.session_upcoming) to F1Red
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}
