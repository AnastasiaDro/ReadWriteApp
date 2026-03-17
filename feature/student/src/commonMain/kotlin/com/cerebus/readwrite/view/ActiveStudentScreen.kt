package com.cerebus.readwrite.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cerebus.create_screen.navigation.DeckNavigationState
import com.cerebus.readwrite.media.rememberDeckArchivePicker
import com.cerebus.readwrite.media.rememberPlatformMessenger
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import readwriteapp.feature.student.generated.resources.Res
import readwriteapp.feature.student.generated.resources.active_student_change
import readwriteapp.feature.student.generated.resources.active_student_create_in_other
import readwriteapp.feature.student.generated.resources.active_student_fallback_name
import readwriteapp.feature.student.generated.resources.active_student_active_decks
import readwriteapp.feature.student.generated.resources.active_student_all_decks
import readwriteapp.feature.student.generated.resources.active_student_import
import readwriteapp.feature.student.generated.resources.active_student_learning_settings
import readwriteapp.feature.student.generated.resources.active_student_no_active_decks
import readwriteapp.feature.student.generated.resources.active_student_no_decks
import readwriteapp.feature.student.generated.resources.active_student_other_decks
import readwriteapp.feature.student.generated.resources.active_student_start
import readwriteapp.feature.student.generated.resources.active_student_studied_digits
import readwriteapp.feature.student.generated.resources.active_student_studied_english_letters
import readwriteapp.feature.student.generated.resources.active_student_error_import_deck_failed
import readwriteapp.feature.student.generated.resources.active_student_error_open_archive_picker_failed
import readwriteapp.feature.student.generated.resources.create_student_avatar_placeholder
import readwriteapp.feature.student.generated.resources.active_student_keyboard_settings
import readwriteapp.feature.student.generated.resources.active_student_no_studied_letters
import readwriteapp.feature.student.generated.resources.active_student_studied_letters
import readwriteapp.feature.student.generated.resources.active_student_studied_russian_letters
import readwriteapp.feature.student.generated.resources.active_student_deck_progress

@Composable
fun ActiveStudentRoute(
    onOpenDeck: (String) -> Unit,
    onOpenGame: (List<String>) -> Unit,
    onOpenDeckList: (Boolean) -> Unit,
    onOpenChangeStudent: () -> Unit,
    onOpenSessionSettings: (String) -> Unit,
    onOpenKeyboardSettings: (String) -> Unit,
) {
    val viewModel = koinViewModel<ActiveStudentViewModel>()
    val state by viewModel.uiState.collectAsState()
    val effect by viewModel.effects.collectAsState()
    val messenger = rememberPlatformMessenger()
    val importDeckErrorText = stringResource(Res.string.active_student_error_import_deck_failed)
    val archivePickerErrorText = stringResource(Res.string.active_student_error_open_archive_picker_failed)
    val archivePicker = rememberDeckArchivePicker(
        onArchivePicked = { uri ->
            viewModel.onAction(ActiveStudentAction.OnImportDeckFilePicked(uri))
        },
        onError = {
            messenger.showMessage(archivePickerErrorText)
        },
    )

    LaunchedEffect(viewModel) {
        viewModel.onScreenShown()
    }

    LaunchedEffect(effect) {
        when (val current = effect) {
            is ActiveStudentEffect.OpenDeck -> {
                DeckNavigationState.selectDeck(deckId = current.deckId)
                onOpenDeck(current.deckId)
                viewModel.consumeEffect()
            }

            is ActiveStudentEffect.OpenGame -> {
                DeckNavigationState.selectDeck(deckId = current.deckIds.firstOrNull().orEmpty())
                onOpenGame(current.deckIds)
                viewModel.consumeEffect()
            }

            is ActiveStudentEffect.OpenDeckList -> {
                onOpenDeckList(current.openCreateDialog)
                viewModel.consumeEffect()
            }

            ActiveStudentEffect.OpenImportDeckPicker -> {
                archivePicker.openArchivePicker()
                viewModel.consumeEffect()
            }

            ActiveStudentEffect.ShowImportDeckFailed -> {
                messenger.showMessage(importDeckErrorText)
                viewModel.consumeEffect()
            }

            ActiveStudentEffect.OpenChangeStudent -> {
                onOpenChangeStudent()
                viewModel.consumeEffect()
            }

            null -> Unit
        }
    }

    ActiveStudentScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenSessionSettings = onOpenSessionSettings,
        onOpenKeyboardSettings = onOpenKeyboardSettings,
    )
}

@Composable
private fun ActiveStudentScreen(
    state: ActiveStudentUiState,
    onAction: (ActiveStudentAction) -> Unit,
    onOpenSessionSettings: (String) -> Unit,
    onOpenKeyboardSettings: (String) -> Unit,
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
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .statusBarsPadding()
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { onAction(ActiveStudentAction.OnChangeStudentClick) },
            ) {
                Text(text = stringResource(Res.string.active_student_change))
            }

            TextButton(
                onClick = { onAction(ActiveStudentAction.OnImportDeckClick) },
            ) {
                Text(text = stringResource(Res.string.active_student_import))
            }

            TextButton(
                onClick = {
                    state.studentId?.let(onOpenSessionSettings)
                },
                enabled = state.studentId != null,
            ) {
                Text(text = stringResource(Res.string.active_student_learning_settings))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
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

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$displayName 👧",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = { state.studentId?.let(onOpenKeyboardSettings) },
                    enabled = state.studentId != null,
                ) {
                    Text(text = stringResource(Res.string.active_student_keyboard_settings))
                }
            }

            Text(
                text = stringResource(Res.string.active_student_studied_letters),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.activeLetters.isBlank()) {
                Text(
                    text = stringResource(Res.string.active_student_no_studied_letters),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                val studiedSymbols = state.activeLetters
                    .lowercase()
                    .toSet()
                val digits = studiedSymbols
                    .filter { it in DIGIT_ORDER }
                    .sortedBy { DIGIT_ORDER.indexOf(it) }
                val russianLetters = studiedSymbols
                    .filter { it in RUSSIAN_LETTER_ORDER }
                    .sortedBy { RUSSIAN_LETTER_ORDER.indexOf(it) }
                val englishLetters = studiedSymbols
                    .filter { it in ENGLISH_LETTER_ORDER }
                    .sortedBy { ENGLISH_LETTER_ORDER.indexOf(it) }

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (digits.isNotEmpty()) {
                        StudiedSymbolsRow(
                            title = stringResource(Res.string.active_student_studied_digits),
                            symbols = digits,
                        )
                    }
                    if (russianLetters.isNotEmpty()) {
                        StudiedSymbolsRow(
                            title = stringResource(Res.string.active_student_studied_russian_letters),
                            symbols = russianLetters,
                        )
                    }
                    if (englishLetters.isNotEmpty()) {
                        StudiedSymbolsRow(
                            title = stringResource(Res.string.active_student_studied_english_letters),
                            symbols = englishLetters,
                        )
                    }
                }
            }

            Text(
                text = stringResource(Res.string.active_student_active_decks),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.activeDecks.isEmpty()) {
                Text(
                    text = stringResource(Res.string.active_student_no_active_decks),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(align = Alignment.CenterStart),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.activeDecks) { deck ->
                            DeckInlineItem(
                                deck = deck.deck,
                                supportingText = stringResource(
                                    Res.string.active_student_deck_progress,
                                    deck.learnedCards,
                                    deck.totalCards,
                                ),
                                onClick = { onAction(ActiveStudentAction.OnDeckClick(deck.deck.id)) },
                            )
                        }
                    }
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
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.otherDecks.isEmpty()) {
                Text(
                    text = stringResource(Res.string.active_student_no_decks),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = { onAction(ActiveStudentAction.OnCreateDeckClick) }) {
                        Text(text = stringResource(Res.string.active_student_create_in_other))
                    }
                    Button(onClick = { onAction(ActiveStudentAction.OnMoreDecksClick) }) {
                        Text(text = stringResource(Res.string.active_student_all_decks))
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.otherDecks.take(5)) { deck ->
                        DeckInlineItem(
                            deck = deck.deck,
                            supportingText = stringResource(
                                Res.string.active_student_deck_progress,
                                deck.learnedCards,
                                deck.totalCards,
                            ),
                            onClick = { onAction(ActiveStudentAction.OnDeckClick(deck.deck.id)) },
                        )
                    }
                }
                Button(onClick = { onAction(ActiveStudentAction.OnMoreDecksClick) }) {
                    Text(text = stringResource(Res.string.active_student_all_decks))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private val DIGIT_ORDER = ('0'..'9').toList()
private val RUSSIAN_LETTER_ORDER = listOf(
    'а', 'б', 'в', 'г', 'д', 'е', 'ё', 'ж', 'з', 'и', 'й', 'к', 'л', 'м', 'н', 'о',
    'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ъ', 'ы', 'ь', 'э', 'ю', 'я',
)
private val ENGLISH_LETTER_ORDER = ('a'..'z').toList()

@Composable
private fun StudiedSymbolsRow(
    title: String,
    symbols: List<Char>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(symbols) { symbol ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = symbol.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
