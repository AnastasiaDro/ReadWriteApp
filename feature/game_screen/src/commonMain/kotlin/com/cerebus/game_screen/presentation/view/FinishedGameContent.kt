package com.cerebus.game_screen.presentation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cerebus.core.game_engine.domain.logic.SrsAvailability
import com.cerebus.core.utils.nowMillis
import com.cerebus.game_screen.presentation.GameScreenAction
import com.cerebus.game_screen.presentation.GameUiState
import org.jetbrains.compose.resources.stringResource
import readwriteapp.feature.game_screen.generated.resources.Res
import readwriteapp.feature.game_screen.generated.resources.game_back_to_student
import readwriteapp.feature.game_screen.generated.resources.game_learn_more
import readwriteapp.feature.game_screen.generated.resources.game_learn_more_hint
import readwriteapp.feature.game_screen.generated.resources.game_random_review
import readwriteapp.feature.game_screen.generated.resources.game_random_review_hint
import readwriteapp.feature.game_screen.generated.resources.game_repeat_last_session
import readwriteapp.feature.game_screen.generated.resources.game_repeat_last_session_hint
import readwriteapp.feature.game_screen.generated.resources.game_srs_status_all_done_today
import readwriteapp.feature.game_screen.generated.resources.game_srs_status_available_now
import readwriteapp.feature.game_screen.generated.resources.game_srs_status_card_few
import readwriteapp.feature.game_screen.generated.resources.game_srs_status_card_many
import readwriteapp.feature.game_screen.generated.resources.game_srs_status_card_one
import readwriteapp.feature.game_screen.generated.resources.game_srs_status_later_today
import readwriteapp.feature.game_screen.generated.resources.game_srs_status_next_due
import readwriteapp.feature.game_screen.generated.resources.game_srs_status_no_new_today
import readwriteapp.feature.game_screen.generated.resources.game_srs_status_no_reviews_now
import readwriteapp.feature.game_screen.generated.resources.game_srs_status_remaining_new


@Composable
fun FinishedGameContent(
    state: GameUiState.Finished,
    onAction: (GameScreenAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Вы завершили колоду",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "\"${state.deckTitle}\"",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )

        Text(
            text = "${state.correctAnswers} / ${state.totalCards} правильных ответов",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 28.dp),
        )

        state.srsAvailability?.let { availability ->
            FinishedSrsStatus(
                availability = availability,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth(0.86f),
            )
        }

        Button(
            onClick = { onAction(GameScreenAction.OnLearnMoreClick) },
            modifier = Modifier
                .padding(top = if (state.srsAvailability != null) 18.dp else 24.dp)
                .fillMaxWidth(0.86f),
        ) {
            Text(stringResource(Res.string.game_learn_more))
        }

        Text(
            text = stringResource(Res.string.game_learn_more_hint),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth(0.86f),
        )

        Button(
            onClick = { onAction(GameScreenAction.OnRetryClick) },
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(0.86f),
        ) {
            Text(stringResource(Res.string.game_repeat_last_session))
        }

        Text(
            text = stringResource(Res.string.game_repeat_last_session_hint),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth(0.86f),
        )

        Button(
            onClick = { onAction(GameScreenAction.OnRandomReviewClick) },
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(0.86f),
        ) {
            Text(stringResource(Res.string.game_random_review))
        }

        Text(
            text = stringResource(Res.string.game_random_review_hint),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth(0.86f),
        )

        Button(
            onClick = { onAction(GameScreenAction.OnBackToStudentClick) },
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(0.86f),
        ) {
            Text(stringResource(Res.string.game_back_to_student))
        }
    }
}

@Composable
private fun FinishedSrsStatus(
    availability: SrsAvailability,
    modifier: Modifier = Modifier,
) {
    val summary = rememberFinishedSrsSummary(availability)

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            summary.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            summary.detail?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun rememberFinishedSrsSummary(
    availability: SrsAvailability,
): SrsSummary {
    val cardOne = stringResource(Res.string.game_srs_status_card_one)
    val cardFew = stringResource(Res.string.game_srs_status_card_few)
    val cardMany = stringResource(Res.string.game_srs_status_card_many)
    val availableLabel = formatSrsCountLabel(availability.availableNow, cardOne, cardFew, cardMany)
    val laterLabel = formatSrsCountLabel(availability.laterTodayCount, cardOne, cardFew, cardMany)
    val remainingNewLabel = formatSrsCountLabel(availability.remainingNewToday, cardOne, cardFew, cardMany)
    val newStatusLine = if (availability.remainingNewToday == 0) {
        stringResource(Res.string.game_srs_status_no_new_today)
    } else {
        stringResource(Res.string.game_srs_status_remaining_new, remainingNewLabel)
    }
    val nextDueAtEpochMillis = availability.nextDueAtEpochMillis

    return when {
        availability.availableNow > 0 -> SrsSummary(
            title = stringResource(Res.string.game_srs_status_available_now, availableLabel),
            subtitle = if (availability.laterTodayCount > 0) {
                stringResource(Res.string.game_srs_status_later_today, laterLabel)
            } else {
                newStatusLine
            },
            detail = if (availability.laterTodayCount > 0) newStatusLine else null,
        )

        availability.laterTodayCount > 0 && nextDueAtEpochMillis != null -> SrsSummary(
            title = stringResource(Res.string.game_srs_status_no_reviews_now),
            subtitle = stringResource(
                Res.string.game_srs_status_next_due,
                laterLabel,
                formatSrsDelayLabel(nextDueAtEpochMillis),
            ),
            detail = newStatusLine,
        )

        else -> SrsSummary(
            title = stringResource(Res.string.game_srs_status_all_done_today),
            subtitle = newStatusLine.takeIf { availability.remainingNewToday == 0 },
        )
    }
}

private data class SrsSummary(
    val title: String,
    val subtitle: String? = null,
    val detail: String? = null,
)

private fun formatSrsCountLabel(
    count: Int,
    one: String,
    few: String,
    many: String,
): String {
    return "$count ${selectPluralForm(count, one, few, many)}"
}

private fun selectPluralForm(
    count: Int,
    one: String,
    few: String,
    many: String,
): String {
    val normalized = count % 100
    if (normalized in 11..14) return many
    return when (count % 10) {
        1 -> one
        2, 3, 4 -> few
        else -> many
    }
}

private fun formatSrsDelayLabel(
    targetEpochMillis: Long,
): String {
    val deltaMinutes = ((targetEpochMillis - nowMillis()).coerceAtLeast(0L) + 59_999L) / 60_000L
    if (deltaMinutes < 60L) {
        return "${deltaMinutes.coerceAtLeast(1L)} min"
    }

    val hours = deltaMinutes / 60L
    val minutes = deltaMinutes % 60L
    return if (minutes == 0L) {
        "$hours h"
    } else {
        "$hours h $minutes min"
    }
}
