package com.f1pulse.app.ui.widget

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import com.f1pulse.app.R
import com.f1pulse.app.ui.theme.F1Red
import com.f1pulse.app.ui.theme.TeamPalette
import com.f1pulse.app.widget.FavoriteDriverWidget
import com.f1pulse.app.widget.FavoriteDriverWidgetStore
import com.f1pulse.app.widget.FavoriteTeamWidget
import com.f1pulse.app.widget.FavoriteWidgetStore
import com.f1pulse.app.widget.WidgetConstructorStanding
import com.f1pulse.app.widget.WidgetDataLoader
import com.f1pulse.app.widget.WidgetDriverStanding
import com.f1pulse.app.widget.WidgetImageLoader
import kotlinx.coroutines.launch

// ── Shared scaffolding ────────────────────────────────────────────────────────

@Composable
private fun ConfigScaffold(
    titleRes: Int,
    descRes: Int,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Text(
            stringResource(descRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        content()
    }
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = F1Red)
    }
}

// ── Favorite team config ──────────────────────────────────────────────────────

/**
 * Configuration screen for [FavoriteTeamWidget]. Lists all constructors — pick
 * one to follow. On selection: saves to [FavoriteWidgetStore], triggers widget
 * update, pops back.
 */
@Composable
fun FavoriteTeamConfigScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var constructors by remember { mutableStateOf<List<WidgetConstructorStanding>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    val currentConstructorId = remember { FavoriteWidgetStore.read(context) }

    LaunchedEffect(Unit) {
        val season = WidgetDataLoader.standingsSeason()
        constructors = WidgetDataLoader.loadAllConstructors(context, season)
        loading = false
    }

    ConfigScaffold(
        titleRes = R.string.widget_favorite_team_config_title,
        descRes = R.string.widget_favorite_team_config_desc,
        onBack = onBack,
    ) {
        if (loading) {
            LoadingBox()
        } else {
            TeamList(
                constructors = constructors,
                currentConstructorId = currentConstructorId,
                onSelect = { constructorId ->
                    scope.launch {
                        FavoriteWidgetStore.save(context, constructorId)
                        FavoriteTeamWidget().updateAll(context)
                        onBack()
                    }
                },
            )
        }
    }
}

@Composable
private fun TeamList(
    constructors: List<WidgetConstructorStanding>,
    currentConstructorId: String?,
    onSelect: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(constructors, key = { it.constructorId ?: it.name }) { team ->
            val isSelected = team.constructorId == currentConstructorId
            SelectableTeamCard(
                team = team,
                isSelected = isSelected,
                onClick = { team.constructorId?.let(onSelect) },
            )
        }
    }
}

@Composable
private fun SelectableTeamCard(
    team: WidgetConstructorStanding,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val isLight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES
    val teamColor = TeamPalette.forHex(team.hex)
    val logo = remember(team.constructorId) {
        team.constructorId?.let { WidgetImageLoader.loadTeamLogo(context, it, isLight) }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, teamColor) else null,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Team color bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(teamColor),
            )
            Spacer(Modifier.width(12.dp))

            // Logo or initial
            if (logo != null) {
                androidx.compose.foundation.Image(
                    bitmap = logo.asImageBitmap(),
                    contentDescription = team.name,
                    modifier = Modifier.size(32.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(teamColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        team.name.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    team.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "P${team.pos} · ${WidgetDataLoader.formatPoints(team.points)} PTS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(F1Red),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Favorite driver config ────────────────────────────────────────────────────

/**
 * Configuration screen for [FavoriteDriverWidget]. Lists all drivers — pick one
 * to follow. On selection: saves to [FavoriteDriverWidgetStore], triggers widget
 * update, pops back.
 */
@Composable
fun FavoriteDriverConfigScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var drivers by remember { mutableStateOf<List<WidgetDriverStanding>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    val currentDriverCode = remember { FavoriteDriverWidgetStore.read(context) }

    LaunchedEffect(Unit) {
        val season = WidgetDataLoader.standingsSeason()
        drivers = WidgetDataLoader.loadAllDrivers(context, season)
        loading = false
    }

    ConfigScaffold(
        titleRes = R.string.widget_favorite_driver_config_title,
        descRes = R.string.widget_favorite_driver_config_desc,
        onBack = onBack,
    ) {
        if (loading) {
            LoadingBox()
        } else {
            DriverList(
                drivers = drivers,
                currentDriverCode = currentDriverCode,
                onSelect = { code ->
                    scope.launch {
                        FavoriteDriverWidgetStore.save(context, code)
                        FavoriteDriverWidget().updateAll(context)
                        onBack()
                    }
                },
            )
        }
    }
}

@Composable
private fun DriverList(
    drivers: List<WidgetDriverStanding>,
    currentDriverCode: String?,
    onSelect: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(drivers, key = { it.code }) { driver ->
            val isSelected = driver.code == currentDriverCode
            SelectableDriverCard(
                driver = driver,
                isSelected = isSelected,
                onClick = { onSelect(driver.code) },
            )
        }
    }
}

@Composable
private fun SelectableDriverCard(
    driver: WidgetDriverStanding,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val isLight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES
    val teamColor = TeamPalette.forHex(driver.teamHex)
    val headshot = remember(driver.code) {
        WidgetImageLoader.loadHeadshot(context, driver.code)
    }
    val logo = remember(driver.constructorId) {
        driver.constructorId?.let { WidgetImageLoader.loadTeamLogo(context, it, isLight) }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, teamColor) else null,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Team color bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(teamColor),
            )
            Spacer(Modifier.width(12.dp))

            // Headshot or code badge
            if (headshot != null) {
                androidx.compose.foundation.Image(
                    bitmap = headshot.asImageBitmap(),
                    contentDescription = driver.code,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(teamColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        driver.code.take(3),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        driver.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(6.dp))
                    if (logo != null) {
                        androidx.compose.foundation.Image(
                            bitmap = logo.asImageBitmap(),
                            contentDescription = driver.teamName,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Text(
                    driver.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "P${driver.pos} · ${WidgetDataLoader.formatPoints(driver.points)} PTS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(F1Red),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
