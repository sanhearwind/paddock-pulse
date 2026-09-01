package com.f1pulse.app.ui.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.f1pulse.app.R
import com.f1pulse.app.ui.theme.F1Red
import com.f1pulse.app.widget.ConstructorStandingsWidgetReceiver
import com.f1pulse.app.widget.CountdownWidgetReceiver
import com.f1pulse.app.widget.DriverStandingsWidgetReceiver
import com.f1pulse.app.widget.FavoriteDriverWidgetReceiver
import com.f1pulse.app.widget.FavoriteTeamWidgetReceiver
import com.f1pulse.app.widget.LastSessionWidgetReceiver
import com.f1pulse.app.widget.NextRaceInfoWidgetReceiver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Full-page widget picker. Shows 6 widget cards with descriptions.
 * Tapping a card pins that widget to the home screen via
 * GlanceAppWidgetManager.requestPinGlanceAppWidget (Android 8+).
 *
 * The favorite team & driver widgets open their own config screen after pinning.
 */
@Composable
fun WidgetPickerScreen(
    onBack: () -> Unit,
    onNavigateToFavoriteTeamConfig: () -> Unit = {},
    onNavigateToFavoriteDriverConfig: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pinFailed by remember { mutableStateOf(false) }

    if (pinFailed) {
        AlertDialog(
            onDismissRequest = { pinFailed = false },
            title = { Text(stringResource(R.string.widget_pin_failed_title)) },
            text = { Text(stringResource(R.string.widget_pin_failed)) },
            confirmButton = {
                TextButton(onClick = { pinFailed = false }) {
                    Text(stringResource(R.string.settings_refresh_failed_confirm), color = F1Red)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

    val widgets = remember {
        listOf(
            WidgetOption(
                labelRes = R.string.widget_countdown_label,
                descRes = R.string.widget_countdown_desc,
                receiver = CountdownWidgetReceiver::class.java,
            ),
            WidgetOption(
                labelRes = R.string.widget_next_race_label,
                descRes = R.string.widget_next_race_desc,
                receiver = NextRaceInfoWidgetReceiver::class.java,
            ),
            WidgetOption(
                labelRes = R.string.widget_drivers_label,
                descRes = R.string.widget_drivers_desc,
                receiver = DriverStandingsWidgetReceiver::class.java,
            ),
            WidgetOption(
                labelRes = R.string.widget_constructors_label,
                descRes = R.string.widget_constructors_desc,
                receiver = ConstructorStandingsWidgetReceiver::class.java,
            ),
            WidgetOption(
                labelRes = R.string.widget_favorite_team_label,
                descRes = R.string.widget_favorite_team_desc,
                receiver = FavoriteTeamWidgetReceiver::class.java,
                postPinAction = PostPinAction.OpenTeamConfig,
            ),
            WidgetOption(
                labelRes = R.string.widget_favorite_driver_label,
                descRes = R.string.widget_favorite_driver_desc,
                receiver = FavoriteDriverWidgetReceiver::class.java,
                postPinAction = PostPinAction.OpenDriverConfig,
            ),
            WidgetOption(
                labelRes = R.string.widget_last_session_label,
                descRes = R.string.widget_last_session_desc,
                receiver = LastSessionWidgetReceiver::class.java,
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // Top bar with back button
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
                stringResource(R.string.settings_add_widget),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        // Description
        Text(
            stringResource(R.string.settings_add_widget_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        // Widget cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            widgets.forEach { widget ->
                WidgetCard(
                    label = stringResource(widget.labelRes),
                    description = stringResource(widget.descRes),
                    onTap = {
                        scope.launch {
                            try {
                                val sent = when (widget.receiver) {
                                    CountdownWidgetReceiver::class.java ->
                                        GlanceAppWidgetManager(context)
                                            .requestPinGlanceAppWidget(CountdownWidgetReceiver::class.java)
                                    NextRaceInfoWidgetReceiver::class.java ->
                                        GlanceAppWidgetManager(context)
                                            .requestPinGlanceAppWidget(NextRaceInfoWidgetReceiver::class.java)
                                    DriverStandingsWidgetReceiver::class.java ->
                                        GlanceAppWidgetManager(context)
                                            .requestPinGlanceAppWidget(DriverStandingsWidgetReceiver::class.java)
                                    ConstructorStandingsWidgetReceiver::class.java ->
                                        GlanceAppWidgetManager(context)
                                            .requestPinGlanceAppWidget(ConstructorStandingsWidgetReceiver::class.java)
                                    FavoriteTeamWidgetReceiver::class.java ->
                                        GlanceAppWidgetManager(context)
                                            .requestPinGlanceAppWidget(FavoriteTeamWidgetReceiver::class.java)
                                    FavoriteDriverWidgetReceiver::class.java ->
                                        GlanceAppWidgetManager(context)
                                            .requestPinGlanceAppWidget(FavoriteDriverWidgetReceiver::class.java)
                                    LastSessionWidgetReceiver::class.java ->
                                        GlanceAppWidgetManager(context)
                                            .requestPinGlanceAppWidget(LastSessionWidgetReceiver::class.java)
                                    else -> false
                                }
                                if (!sent) {
                                    pinFailed = true
                                } else {
                                    when (widget.postPinAction) {
                                        PostPinAction.OpenTeamConfig -> onNavigateToFavoriteTeamConfig()
                                        PostPinAction.OpenDriverConfig -> onNavigateToFavoriteDriverConfig()
                                        PostPinAction.None -> Unit
                                    }
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                pinFailed = true
                            }
                        }
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WidgetCard(
    label: String,
    description: String,
    onTap: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // F1 logo icon in red circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(F1Red.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_f1_logo),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(F1Red, BlendMode.SrcIn),
                    modifier = Modifier
                        .width(32.dp)
                        .height(16.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Add button (+)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(F1Red),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private data class WidgetOption(
    val labelRes: Int,
    val descRes: Int,
    val receiver: Class<out android.content.BroadcastReceiver>,
    val postPinAction: PostPinAction = PostPinAction.None,
)

private enum class PostPinAction {
    None,
    OpenTeamConfig,
    OpenDriverConfig,
}
