package com.cerebus.readwrite.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.cerebus.readwrite.navigation.StudentNavigationState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import readwriteapp.feature.student.generated.resources.Res
import readwriteapp.feature.student.generated.resources.active_student_change
import readwriteapp.feature.student.generated.resources.active_student_create
import readwriteapp.feature.student.generated.resources.active_student_fallback_name
import readwriteapp.feature.student.generated.resources.active_student_last_lesson
import readwriteapp.feature.student.generated.resources.active_student_more
import readwriteapp.feature.student.generated.resources.active_student_no_decks
import readwriteapp.feature.student.generated.resources.active_student_other_decks
import readwriteapp.feature.student.generated.resources.active_student_start
import readwriteapp.feature.student.generated.resources.create_student_avatar_placeholder

@Composable
fun ActiveStudentRoute(
    onOpenDeck: (String) -> Unit,
    onOpenGame: (String) -> Unit,
    onOpenDeckList: (Boolean) -> Unit,
    onOpenChangeStudent: () -> Unit,
) {
    val viewModel = koinViewModel<ActiveStudentViewModel>()
    val state by viewModel.uiState.collectAsState()
    val effect by viewModel.effects.collectAsState()
    val deckChangedVersion by DeckNavigationState.deckChangedVersion.collectAsState()
    val studentChangedVersion by StudentNavigationState.studentChangedVersion.collectAsState()

    LaunchedEffect(effect) {
        when (val current = effect) {
            is ActiveStudentEffect.OpenDeck -> {
                DeckNavigationState.selectedDeckId = current.deckId
                onOpenDeck(current.deckId)
                viewModel.consumeEffect()
            }

            is ActiveStudentEffect.OpenGame -> {
                DeckNavigationState.selectedDeckId = current.deckId
                onOpenGame(current.deckId)
                viewModel.consumeEffect()
            }

            is ActiveStudentEffect.OpenDeckList -> {
                onOpenDeckList(current.openCreateDialog)
                viewModel.consumeEffect()
            }

            ActiveStudentEffect.OpenChangeStudent -> {
                onOpenChangeStudent()
                viewModel.consumeEffect()
            }

            null -> Unit
        }
    }

    LaunchedEffect(deckChangedVersion) {
        if (deckChangedVersion > 0) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(studentChangedVersion) {
        if (studentChangedVersion > 0) {
            viewModel.refresh()
        }
    }

    ActiveStudentScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun ActiveStudentScreen(
    state: ActiveStudentUiState,
    onAction: (ActiveStudentAction) -> Unit,
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

    val displayName = state.studentName.ifBlank {
        stringResource(Res.string.active_student_fallback_name)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 16.dp,
            ),
    ) {
        TextButton(
            onClick = { onAction(ActiveStudentAction.OnChangeStudentClick) },
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Text(text = stringResource(Res.string.active_student_change))
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.create_student_avatar_placeholder),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "$displayName 👧",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Text(
                text = stringResource(Res.string.active_student_last_lesson),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (state.lastLessonDeck == null) {
                Text(
                    text = stringResource(Res.string.active_student_no_decks),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Column(
                    modifier = Modifier.wrapContentSize(align = Alignment.CenterStart),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DeckInlineItem(
                        deck = state.lastLessonDeck,
                        onClick = { onAction(ActiveStudentAction.OnDeckClick(state.lastLessonDeck.id)) },
                    )
                    Button(
                        onClick = { onAction(ActiveStudentAction.OnStartClick) },
                    ) {
                        Text(text = stringResource(Res.string.active_student_start))
                    }
                }
            }

            Text(
                text = stringResource(Res.string.active_student_other_decks),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (state.decks.isEmpty()) {
                Text(
                    text = stringResource(Res.string.active_student_no_decks),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = { onAction(ActiveStudentAction.OnCreateDeckClick) }) {
                    Text(text = stringResource(Res.string.active_student_create))
                }
            } else {
                LazyRow(
                   // modifier = Modifier
                        //.wrapContentSize()
                       // .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.decks) { deck ->
                        DeckInlineItem(
                            deck = deck,
                            onClick = { onAction(ActiveStudentAction.OnDeckClick(deck.id)) },
                        )
                    }
                }
                Button(onClick = { onAction(ActiveStudentAction.OnMoreDecksClick) }) {
                    Text(text = stringResource(Res.string.active_student_more))
                }
            }
        }
    }
}
