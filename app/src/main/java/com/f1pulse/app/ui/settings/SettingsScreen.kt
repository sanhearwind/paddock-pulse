package com.f1pulse.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.f1pulse.app.BuildConfig
import com.f1pulse.app.R
import com.f1pulse.app.data.prefs.ThemeMode
import com.f1pulse.app.core.time.TimeZoneMode
import com.f1pulse.app.ui.components.SectionHeader
import com.f1pulse.app.ui.components.horizontalSwipeNavigation
import com.f1pulse.app.ui.nav.F1BottomBarLayout
import com.f1pulse.app.ui.theme.F1Red
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToWidgetPicker: () -> Unit = {},
    onNavigateToFavoriteTeamConfig: () -> Unit = {},
    onNavigateToFavoriteDriverConfig: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = null)
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val refreshFailed by viewModel.refreshFailed.collectAsStateWithLifecycle()

    val s = settings
    if (s == null) {
        Box(modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var refreshMinutesDraft by remember(s.widgetRefreshMinutes) {
        mutableFloatStateOf(s.widgetRefreshMinutes.toFloat())
    }

    if (refreshFailed) {
        RefreshFailureDialog(onDismiss = viewModel::dismissRefreshError)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .horizontalSwipeNavigation(onSwipeRight = onSwipeRight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        SectionHeader(stringResource(R.string.settings_appearance))
        ThemePicker(s.themeMode, viewModel::setThemeMode)

        SectionHeader(stringResource(R.string.settings_timezone))
        TimeZonePicker(s.timeZoneMode, viewModel::setTimeZoneMode)

        SectionHeader(stringResource(R.string.settings_widget))

        // Widget picker entry card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToWidgetPicker),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_add_widget),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.settings_add_widget_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    ">",
                    style = MaterialTheme.typography.titleLarge,
                    color = F1Red,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Favorite team widget config entry card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToFavoriteTeamConfig),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.widget_favorite_team_config_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.widget_favorite_team_config_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    ">",
                    style = MaterialTheme.typography.titleLarge,
                    color = F1Red,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Favorite driver widget config entry card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToFavoriteDriverConfig),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.widget_favorite_driver_config_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.widget_favorite_driver_config_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    ">",
                    style = MaterialTheme.typography.titleLarge,
                    color = F1Red,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.settings_refresh_interval))
        Slider(
            value = refreshMinutesDraft,
            onValueChange = { refreshMinutesDraft = (it / 15f).roundToInt() * 15f },
            onValueChangeFinished = {
                viewModel.setWidgetRefreshMinutes(refreshMinutesDraft.roundToInt())
            },
            valueRange = 15f..240f,
            steps = (240 - 15) / 15 - 1,
            colors = SliderDefaults.colors(thumbColor = F1Red, activeTrackColor = F1Red),
        )
        Text(
            "${refreshMinutesDraft.roundToInt()} min · ${stringResource(R.string.settings_min_15min)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = viewModel::refreshNow,
            enabled = !refreshing,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, F1Red),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = F1Red),
        ) {
            if (refreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = F1Red,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(R.string.settings_refresh_now))
            }
        }

        SectionHeader(stringResource(R.string.settings_about))
        Text(
            stringResource(R.string.settings_data_sources),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(R.string.settings_data_sources_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.settings_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "${stringResource(R.string.settings_version)}: ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(F1BottomBarLayout.ContentPadding))
    }
}

@Composable
private fun ThemePicker(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.SYSTEM to R.string.settings_theme_system,
        ThemeMode.DARK to R.string.settings_theme_dark,
        ThemeMode.LIGHT to R.string.settings_theme_light,
    )
    options.forEach { (mode, label) ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = current == mode,
                onClick = { onSelect(mode) },
                colors = RadioButtonDefaults.colors(selectedColor = F1Red),
            )
            Text(stringResource(label))
        }
    }
}

@Composable
private fun TimeZonePicker(current: TimeZoneMode, onSelect: (TimeZoneMode) -> Unit) {
    val options = listOf(
        Triple(TimeZoneMode.DEVICE, R.string.settings_tz_device, R.string.timezone_device_desc),
        Triple(TimeZoneMode.RACE_LOCAL, R.string.settings_tz_race, R.string.timezone_race_local_desc),
    )
    options.forEach { (mode, label, description) ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = current == mode,
                onClick = { onSelect(mode) },
                colors = RadioButtonDefaults.colors(selectedColor = F1Red),
            )
            Column {
                Text(stringResource(label))
                Text(
                    stringResource(description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RefreshFailureDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(20.dp)) {
                Box(Modifier.fillMaxWidth().height(5.dp).background(F1Red))
                Spacer(Modifier.height(18.dp))
                Text("F1 // DATA LINK", color = F1Red, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.settings_refresh_failed_title), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.settings_refresh_failed_message), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, F1Red),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = F1Red),
                ) {
                    Text(stringResource(R.string.settings_refresh_failed_confirm))
                }
            }
        }
    }
}
