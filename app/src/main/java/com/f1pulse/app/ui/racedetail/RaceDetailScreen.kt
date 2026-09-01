package com.f1pulse.app.ui.racedetail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.f1pulse.app.R
import com.f1pulse.app.core.CountryFlags
import com.f1pulse.app.core.time.TimeFormatter
import com.f1pulse.app.core.time.TimeZoneMode
import com.f1pulse.app.core.time.chronologicalSchedule
import com.f1pulse.app.domain.model.RaceResult
import com.f1pulse.app.domain.model.SessionResult
import com.f1pulse.app.domain.model.SessionType
import com.f1pulse.app.domain.model.Weather
import com.f1pulse.app.ui.UiState
import com.f1pulse.app.ui.components.AdaptiveBackButton
import com.f1pulse.app.ui.components.CircuitMapSvg
import com.f1pulse.app.ui.components.DriverHeadshot
import com.f1pulse.app.ui.components.EmptyState
import com.f1pulse.app.ui.components.ErrorState
import com.f1pulse.app.ui.components.LoadingState
import com.f1pulse.app.ui.components.PositionDelta
import com.f1pulse.app.ui.components.RaceCardImage
import com.f1pulse.app.ui.components.SectionHeader
import com.f1pulse.app.ui.components.TeamColoredChip
import com.f1pulse.app.ui.components.TeamLogo
import com.f1pulse.app.ui.theme.F1Red
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceDetailScreen(
    onBack: () -> Unit,
    viewModel: RaceDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            is UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(message = s.message, onRetry = viewModel::refresh)
            is UiState.Success -> RaceDetailContent(s.data, viewModel::refresh)
        }

        // The icon tint follows the luminance of its backing plate. On light
        // or white content it becomes black; on dark content it remains white.
        AdaptiveBackButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 4.dp, top = 4.dp),
        )
    }
}

@Composable
private fun RaceDetailContent(
    data: RaceDetailUiData,
    onRefresh: () -> Unit,
) {
    val race = data.race
    val zone = data.zone
    var expandedCurrentSessionKey by rememberSaveable(race.season, race.round) {
        mutableStateOf<Int?>(null)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // -- Race header banner --
        RaceHeaderBanner(race)

        Column(Modifier.padding(horizontal = 16.dp)) {
            // -- Circuit map --
            Spacer(Modifier.height(20.dp))
            SectionHeader(stringResource(R.string.race_circuit))
            CircuitCard(race)

            // -- Schedule times --
            Spacer(Modifier.height(20.dp))
            SectionHeader(stringResource(R.string.race_sessions))
            ScheduleCard(race, zone, data.timeZoneMode)

            // -- Fast facts --
            Spacer(Modifier.height(20.dp))
            SectionHeader(stringResource(R.string.race_fast_facts))
            FastFactsCard(race)

            // -- Current session (rolls forward through the weekend) --
            if (data.currentSession != null) {
                val currentSessionKey = data.currentSession.session.detail.sessionKey
                val isCurrentSessionExpanded = expandedCurrentSessionKey == currentSessionKey
                Spacer(Modifier.height(20.dp))
                SectionHeader(stringResource(R.string.race_current_session))
                CurrentSessionCard(
                    current = data.currentSession,
                    zone = zone,
                    rankingExpanded = isCurrentSessionExpanded,
                    onRankingToggle = {
                        expandedCurrentSessionKey = if (isCurrentSessionExpanded) {
                            null
                        } else {
                            currentSessionKey
                        }
                    },
                )
            }

            // -- Weather --
            if (data.weather != null) {
                Spacer(Modifier.height(20.dp))
                SectionHeader(stringResource(R.string.race_weather))
                WeatherCard(data.weather)
            }

            // -- Results --
            Spacer(Modifier.height(20.dp))
            SectionHeader("RESULTS")
            ResultsSection(data)

            Spacer(Modifier.height(32.dp))
        }
    }
}

// --──────────────────────────────────
// Header banner
// --──────────────────────────────────

@Composable
private fun RaceHeaderBanner(race: com.f1pulse.app.domain.model.Race) {
    val flag = remember(race.circuit.country) {
        CountryFlags.toEmoji(CountryFlags.fromLocation(race.circuit.country))
    }
    val cardAsset = com.f1pulse.app.core.RaceCardAssets.cardAsset(race.circuit.circuitId)

    Box(
        Modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        if (cardAsset != null) {
            // Race card image as background
            RaceCardImage(
                circuitId = race.circuit.circuitId,
                height = 220.dp,
                contentScale = ContentScale.Crop,
            )
        } else {
            // Fallback: F1Red gradient when no card image
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(F1Red, F1Red.copy(alpha = 0.3f))
                        )
                    )
            )
        }

        // Gradient scrim for text readability (transparent → black 0.75)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
        )

        // Content at bottom
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 20.dp),
        ) {
            if (flag != null) {
                Text(flag, style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(4.dp))
            }
            Text(
                race.raceName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Round ${race.round} \u00b7 ${race.circuit.locality}, ${race.circuit.country}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

// --──────────────────────────────────
// Circuit card
// --──────────────────────────────────

@Composable
private fun CircuitCard(race: com.f1pulse.app.domain.model.Race) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            CircuitMapSvg(
                svgAsset = race.circuit.svgAsset,
                circuitName = race.circuit.name,
                height = 220,
            )
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val flag = remember(race.circuit.country) {
                        CountryFlags.toEmoji(CountryFlags.fromLocation(race.circuit.country))
                    }
                    if (flag != null) {
                        Text(flag, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            race.circuit.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${race.circuit.locality}, ${race.circuit.country}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// --──────────────────────────────────
// Schedule card
// --──────────────────────────────────

@Composable
private fun ScheduleCard(
    race: com.f1pulse.app.domain.model.Race,
    zone: ZoneId,
    timeZoneMode: TimeZoneMode,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                stringResource(if (timeZoneMode == TimeZoneMode.DEVICE) R.string.timezone_device_desc else R.string.timezone_race_local_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            race.sessions.chronologicalSchedule().forEachIndexed { index, session ->
                if (index > 0) {
                    HorizontalDivider(
                        Modifier,
                        0.5.dp,
                        if (session.type == SessionType.RACE) {
                            F1Red.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                        },
                    )
                }
                SessionRow(
                    label = sessionTypeLabel(session.type, session.type.name),
                    instant = session.instant,
                    zone = zone,
                    highlight = session.type == SessionType.RACE,
                )
            }
        }
    }
}

@Composable
private fun SessionRow(
    label: String,
    instant: Instant?,
    zone: ZoneId,
    highlight: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (highlight) Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(F1Red.copy(alpha = 0.06f))
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) F1Red else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            instant?.let { TimeFormatter.formatSessionTime(it, zone) } ?: "\u2014",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) F1Red else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// --──────────────────────────────────
// Fast facts card
// --──────────────────────────────────

@Composable
private fun FastFactsCard(race: com.f1pulse.app.domain.model.Race) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(12.dp)) {
            FactRow(stringResource(R.string.race_length), race.circuit.lengthKm?.let { "$it km" })
            HorizontalDivider(Modifier, 0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            FactRow(stringResource(R.string.race_corners), race.circuit.corners?.toString())
            HorizontalDivider(Modifier, 0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            FactRow(stringResource(R.string.race_drs), race.circuit.drsZones?.toString())
            HorizontalDivider(Modifier, 0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            FactRow(stringResource(R.string.race_first_held), race.circuit.firstHeld?.toString())
            HorizontalDivider(Modifier, 0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            FactRow(stringResource(R.string.race_lap_record), race.circuit.lapRecord)
        }
    }
}

@Composable
private fun FactRow(label: String, value: String?) {
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
            value ?: "\u2014",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

// --──────────────────────────────────
// Weather card
// --──────────────────────────────────

@Composable
private fun WeatherCard(weather: Weather) {
    val wet = weather.rainfall > 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                WeatherCell(stringResource(R.string.weather_air), "${weather.airTemp.toInt()}°C")
                WeatherCell(stringResource(R.string.weather_track), "${weather.trackTemp.toInt()}°C")
                WeatherCell(stringResource(R.string.weather_humidity), "${weather.humidity.toInt()}%")
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(Modifier, 0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                WeatherCell(stringResource(R.string.weather_wind), "${weather.windSpeed} m/s")
                WeatherCell(
                    stringResource(R.string.weather_rain),
                    if (wet) stringResource(R.string.weather_wet) else stringResource(R.string.weather_dry),
                )
            }
        }
    }
}

@Composable
private fun WeatherCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = F1Red,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// --──────────────────────────────────
// Results section
// --──────────────────────────────────

/**
 * Each result tab owns its source shape and chronological position. Tabs are sorted newest-first
 * so the latest published classification is surfaced automatically as a weekend progresses.
 */
private sealed interface ResultTab {
    val key: String
    val label: String
    val sessionTime: Instant

    /** Jolpica race / qualifying / sprint classification. */
    data class Official(
        override val key: String,
        override val label: String,
        override val sessionTime: Instant,
        val results: List<RaceResult>,
        val isQuali: Boolean,
    ) : ResultTab

    /** OpenF1 session classification — the source that covers practice and sprint quali. */
    data class Session(
        override val key: String,
        override val label: String,
        override val sessionTime: Instant,
        val results: List<SessionResult>,
        val isRace: Boolean,
    ) : ResultTab
}

@Composable
private fun ResultsSection(data: RaceDetailUiData) {
    var selectedTabKey by rememberSaveable(data.race.season, data.race.round) {
        mutableStateOf<String?>(null)
    }

    val sessionTabs = data.sessions
        .filter {
            it.detail.sessionType == SessionType.FP1 ||
                it.detail.sessionType == SessionType.FP2 ||
                it.detail.sessionType == SessionType.FP3 ||
                it.detail.sessionType == SessionType.SPRINT_QUALIFYING
        }
        .filter { it.results.isNotEmpty() }
        .map {
            ResultTab.Session(
                key = "session-${it.detail.sessionKey}",
                label = resultSessionLabel(it.detail.sessionType),
                sessionTime = it.window.end,
                results = it.results,
                isRace = false,
            )
        }

    val tabs = buildList<ResultTab> {
        addAll(sessionTabs)
        if (data.raceResults.isNotEmpty()) {
            add(
                ResultTab.Official(
                    key = "race",
                    label = "Race",
                    sessionTime = data.race.sessions.race ?: Instant.EPOCH,
                    results = data.raceResults,
                    isQuali = false,
                ),
            )
        }
        if (data.qualiResults.isNotEmpty()) {
            add(
                ResultTab.Official(
                    key = "quali",
                    label = "Quali",
                    sessionTime = data.race.sessions.qualifying ?: Instant.EPOCH,
                    results = data.qualiResults,
                    isQuali = true,
                ),
            )
        }
        if (data.sprintResults.isNotEmpty()) {
            add(
                ResultTab.Official(
                    key = "sprint",
                    label = "Sprint",
                    sessionTime = data.race.sessions.sprint ?: Instant.EPOCH,
                    results = data.sprintResults,
                    isQuali = false,
                ),
            )
        }
    }.sortedByDescending { it.sessionTime }

    if (tabs.isEmpty()) {
        EmptyState(text = "No results yet")
        return
    }

    val latestKey = tabs.firstOrNull { it.key == "race" }?.key ?: tabs.first().key
    LaunchedEffect(latestKey) {
        selectedTabKey = latestKey
    }
    val selectedIndex = tabs.indexOfFirst { it.key == selectedTabKey }
        .takeIf { it >= 0 }
        ?: 0
    val selectedTab = tabs[selectedIndex]

    Column {
        TabRow(
            selectedTabIndex = selectedIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = F1Red,
            indicator = { tabPositions ->
                if (selectedIndex < tabPositions.size) {
                    SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        color = F1Red,
                    )
                }
            },
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { selectedTabKey = tab.key },
                    text = {
                        Text(
                            tab.label,
                            fontWeight = if (selectedIndex == index) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    },
                    selectedContentColor = F1Red,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        when (val tab = selectedTab) {
            is ResultTab.Official -> ResultsCard {
                tab.results.forEachIndexed { index, result ->
                    ResultRow(
                        result = result,
                        isQuali = tab.isQuali,
                        isPodium = !tab.isQuali && result.position <= 3,
                    )
                    if (index < tab.results.lastIndex) RowDivider()
                }
            }
            is ResultTab.Session -> ResultsCard {
                tab.results.forEachIndexed { index, result ->
                    Box(Modifier.padding(horizontal = 12.dp)) {
                        SessionResultRow(result = result, isRace = tab.isRace, englishOnly = true)
                    }
                    if (index < tab.results.lastIndex) RowDivider()
                }
            }
        }
    }
}

private fun resultSessionLabel(type: SessionType?): String = when (type) {
    SessionType.FP1 -> "FP1"
    SessionType.FP2 -> "FP2"
    SessionType.FP3 -> "FP3"
    SessionType.QUALIFYING -> "Quali"
    SessionType.SPRINT -> "Sprint"
    SessionType.SPRINT_QUALIFYING -> "Sprint Quali"
    SessionType.RACE -> "Race"
    null -> "Session"
}

@Composable
private fun ResultsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        Modifier.padding(horizontal = 12.dp),
        0.5.dp,
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
    )
}

@Composable
private fun ResultRow(
    result: RaceResult,
    isQuali: Boolean,
    isPodium: Boolean,
) {
    val positionBg = when (result.position) {
        1 -> F1Red
        2 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        3 -> F1Red.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val positionFg = when (result.position) {
        1, 2, 3 -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isPodium) Modifier.background(F1Red.copy(alpha = 0.03f)) else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Position badge
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(positionBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${result.position}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = positionFg,
            )
        }

        Spacer(Modifier.width(10.dp))

        // Driver headshot
        DriverHeadshot(
            url = result.driver.headshotUrl,
            code = result.driver.code,
            size = 40.dp,
            teamHex = result.team.colourHex,
        )

        Spacer(Modifier.width(10.dp))

        // Driver info + team
        Column(Modifier.weight(1f)) {
            Text(
                result.driver.code ?: result.driver.fullName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamLogo(
                    constructorId = result.team.constructorId,
                    colourHex = result.team.colourHex,
                    size = 16.dp,
                )
                Spacer(Modifier.width(4.dp))
                TeamColoredChip(colourHex = result.team.colourHex, label = result.team.name)
            }
        }

        // Position delta (race only)
        if (!isQuali) {
            val delta = result.grid?.let { it - result.position }
            PositionDelta(delta = delta)
            Spacer(Modifier.width(8.dp))
        }

        // Times / points
        if (isQuali) {
            Column(horizontalAlignment = Alignment.End) {
                QualiTime("Q1", result.q1)
                QualiTime("Q2", result.q2)
                QualiTime("Q3", result.q3)
            }
        } else {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    result.raceTimeStr ?: result.status ?: "\u2014",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    "Grid: ${result.grid ?: "\u2014"} \u00b7 Laps: ${result.laps ?: "\u2014"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${result.points.toInt()} PTS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = F1Red,
                )
            }
        }
    }
}

@Composable
private fun QualiTime(label: String, time: String?) {
    Text(
        "$label: ${time ?: "\u2014"}",
        style = MaterialTheme.typography.labelSmall,
    )
}
