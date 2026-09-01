package com.f1pulse.app.ui.racedetail

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.f1pulse.app.R
import com.f1pulse.app.core.time.CurrentSession
import com.f1pulse.app.core.time.SessionPhase
import com.f1pulse.app.core.time.TimeFormatter
import com.f1pulse.app.domain.model.SessionResult
import com.f1pulse.app.domain.model.SessionType
import com.f1pulse.app.domain.usecase.SessionWithResults
import com.f1pulse.app.ui.theme.F1Red
import com.f1pulse.app.ui.theme.TeamPalette
import java.time.ZoneId

/** Localised label for a session type; falls back to OpenF1's own name. */
@Composable
fun sessionTypeLabel(type: SessionType?, fallback: String): String = when (type) {
    SessionType.FP1 -> stringResource(R.string.session_fp1)
    SessionType.FP2 -> stringResource(R.string.session_fp2)
    SessionType.FP3 -> stringResource(R.string.session_fp3)
    SessionType.QUALIFYING -> stringResource(R.string.session_qualifying)
    SessionType.SPRINT -> stringResource(R.string.session_sprint)
    SessionType.SPRINT_QUALIFYING -> stringResource(R.string.session_sprint_qualifying)
    SessionType.RACE -> stringResource(R.string.session_race)
    null -> fallback
}

/**
 * The weekend's "what should I be looking at right now" card.
 *
 * Rolls forward on its own: a running session while it runs, then that session's results
 * until the next one begins. Deliberately not wrapped in `SpoilerShield` — session results
 * are shown directly.
 */
@Composable
fun CurrentSessionCard(
    current: CurrentSession<SessionWithResults>,
    zone: ZoneId,
    modifier: Modifier = Modifier,
    rankingExpanded: Boolean = false,
    onRankingToggle: () -> Unit = {},
) {
    val detail = current.session.detail
    val results = current.session.results
    val label = sessionTypeLabel(detail.sessionType, detail.sessionName)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SessionPhaseBadge(current.phase)
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                TimeFormatter.formatSessionTime(detail.dateStart, zone),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val hasRankingContent = results.isNotEmpty() ||
                current.phase == SessionPhase.IN_PROGRESS ||
                current.phase == SessionPhase.FINISHED
            if (hasRankingContent) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                )
                val disclosureText = currentSessionDisclosureText(rankingExpanded)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRankingToggle)
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        disclosureText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        imageVector = if (rankingExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = disclosureText,
                    )
                }

                if (rankingExpanded) {
                    when {
                        results.isNotEmpty() -> {
                            val isRace = detail.sessionType == SessionType.RACE ||
                                detail.sessionType == SessionType.SPRINT
                            results.forEachIndexed { index, result ->
                                SessionResultRow(result, isRace)
                                if (index < results.lastIndex) {
                                    HorizontalDivider(
                                        Modifier.padding(vertical = 2.dp),
                                        0.5.dp,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                                    )
                                }
                            }
                        }
                        // The free OpenF1 tier withholds a session from 30min before it starts
                        // until 30min after it ends, so say so rather than looking broken.
                        current.phase == SessionPhase.IN_PROGRESS ||
                            current.phase == SessionPhase.FINISHED -> {
                            Text(
                                stringResource(R.string.session_results_pending),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

internal fun currentSessionDisclosureText(expanded: Boolean): String =
    if (expanded) "HIDE RANKING" else "SHOW RANKING"

@Composable
private fun SessionPhaseBadge(phase: SessionPhase) {
    val (text, color) = when (phase) {
        SessionPhase.IN_PROGRESS -> stringResource(R.string.session_live) to F1Red
        SessionPhase.FINISHED -> stringResource(R.string.session_completed) to
            MaterialTheme.colorScheme.onSurfaceVariant
        SessionPhase.UPCOMING -> stringResource(R.string.session_upcoming) to
            MaterialTheme.colorScheme.primary
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

/**
 * One row of an OpenF1 session classification.
 *
 * [isRace] switches the time column from "best lap" to "race time", and shows the gap for
 * everyone but the leader.
 */
@Composable
fun SessionResultRow(result: SessionResult, isRace: Boolean, englishOnly: Boolean = false) {
    val teamColor = TeamPalette.forHex(result.teamColourHex)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            result.position.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (result.position <= 3) F1Red else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
        )
        Box(
            Modifier
                .width(3.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(teamColor),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                result.driverName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            result.teamName?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            val status = when {
                result.dsq -> if (englishOnly) "DSQ" else stringResource(R.string.session_dsq)
                result.dns -> if (englishOnly) "DNS" else stringResource(R.string.session_dns)
                result.dnf -> if (englishOnly) "DNF" else stringResource(R.string.session_dnf)
                else -> null
            }
            Text(
                status ?: TimeFormatter.formatLapDuration(result.durationSeconds) ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val secondary = TimeFormatter.formatGap(result.gapSeconds)
                ?: result.laps?.let { if (englishOnly) "$it laps" else stringResource(R.string.session_laps_format, it) }
            secondary?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
