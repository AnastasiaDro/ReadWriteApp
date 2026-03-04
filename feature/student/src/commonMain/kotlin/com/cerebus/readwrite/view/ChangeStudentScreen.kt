package com.cerebus.readwrite.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cerebus.create_screen.navigation.DeckNavigationState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import readwriteapp.feature.student.generated.resources.Res
import readwriteapp.feature.student.generated.resources.active_student_no_decks
import readwriteapp.feature.student.generated.resources.change_student_back
import readwriteapp.feature.student.generated.resources.change_student_create
import readwriteapp.feature.student.generated.resources.change_student_last_deck
import readwriteapp.feature.student.generated.resources.change_student_title
import readwriteapp.feature.student.generated.resources.create_student_avatar_placeholder

@Composable
fun ChangeStudentRoute(
    onBackClick: () -> Unit,
    onOpenDeck: (String) -> Unit,
    onOpenCreateStudent: () -> Unit,
) {
    val viewModel = koinViewModel<ChangeStudentViewModel>()
    val state by viewModel.uiState.collectAsState()
    val effect by viewModel.effects.collectAsState()

    LaunchedEffect(effect) {
        when (val current = effect) {
            ChangeStudentEffect.NavigateBack -> {
                onBackClick()
                viewModel.consumeEffect()
            }

            is ChangeStudentEffect.OpenDeck -> {
                DeckNavigationState.selectedDeckId = current.deckId
                onOpenDeck(current.deckId)
                viewModel.consumeEffect()
            }

            ChangeStudentEffect.OpenCreateStudent -> {
                onOpenCreateStudent()
                viewModel.consumeEffect()
            }

            null -> Unit
        }
    }

    ChangeStudentScreen(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
    )
}

@Composable
private fun ChangeStudentScreen(
    state: ChangeStudentUiState,
    onAction: (ChangeStudentAction) -> Unit,
    onBackClick: () -> Unit,
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 10.dp,
                bottom = 10.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextButton(onClick = onBackClick) {
            Text(text = stringResource(Res.string.change_student_back))
        }
        Text(
            text = stringResource(Res.string.change_student_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.students) { student ->
                StudentRow(
                    item = student,
                    isActive = student.studentId == state.activeStudentId,
                    onStudentClick = { onAction(ChangeStudentAction.OnStudentClick(student.studentId)) },
                    onDeckClick = { deckId ->
                        onAction(ChangeStudentAction.OnDeckClick(student.studentId, deckId))
                    },
                )
            }
        }

        Button(
            onClick = { onAction(ChangeStudentAction.OnCreateStudentClick) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(Res.string.change_student_create))
        }
    }
}

@Composable
private fun StudentRow(
    item: ChangeStudentListItem,
    isActive: Boolean,
    onStudentClick: () -> Unit,
    onDeckClick: (String) -> Unit,
) {
    val rowBackground = if (isActive) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(rowBackground)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium,
            )
            .clickable(onClick = onStudentClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.create_student_avatar_placeholder),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = item.studentName,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(Res.string.change_student_last_deck),
                style = MaterialTheme.typography.bodyMedium,
            )

            val deck = item.lastLessonDeck
            if (deck == null) {
                Text(
                    text = stringResource(Res.string.active_student_no_decks),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                DeckInlineItem(
                    deck = deck,
                    onClick = { onDeckClick(deck.id) },
                )
            }
        }
    }
}
