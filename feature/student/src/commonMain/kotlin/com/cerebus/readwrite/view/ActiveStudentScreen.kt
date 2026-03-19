package com.cerebus.readwrite.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.cerebus.core.utils.GameLaunchMode
import com.cerebus.readwrite.media.rememberCoverImagePicker
import com.cerebus.readwrite.media.rememberDeckArchivePicker
import com.cerebus.readwrite.media.rememberPlatformMessenger
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import readwriteapp.feature.student.generated.resources.Res
import readwriteapp.feature.student.generated.resources.active_student_active_decks
import readwriteapp.feature.student.generated.resources.active_student_all_decks
import readwriteapp.feature.student.generated.resources.active_student_change
import readwriteapp.feature.student.generated.resources.active_student_create_in_other
import readwriteapp.feature.student.generated.resources.active_student_deck_progress
import readwriteapp.feature.student.generated.resources.active_student_error_import_deck_failed
import readwriteapp.feature.student.generated.resources.active_student_error_delete_student_failed
import readwriteapp.feature.student.generated.resources.active_student_error_open_archive_picker_failed
import readwriteapp.feature.student.generated.resources.active_student_error_open_photo_picker_failed
import readwriteapp.feature.student.generated.resources.active_student_error_update_student_failed
import readwriteapp.feature.student.generated.resources.active_student_fallback_name
import readwriteapp.feature.student.generated.resources.active_student_gallery_choose_deck_title
import readwriteapp.feature.student.generated.resources.active_student_edit_student_title
import readwriteapp.feature.student.generated.resources.active_student_import
import readwriteapp.feature.student.generated.resources.active_student_keyboard_settings
import readwriteapp.feature.student.generated.resources.active_student_learning_settings
import readwriteapp.feature.student.generated.resources.active_student_mode_dialog_cancel
import readwriteapp.feature.student.generated.resources.active_student_mode_dialog_gallery_hint
import readwriteapp.feature.student.generated.resources.active_student_mode_dialog_gallery_title
import readwriteapp.feature.student.generated.resources.active_student_mode_dialog_plan_hint
import readwriteapp.feature.student.generated.resources.active_student_mode_dialog_plan_title
import readwriteapp.feature.student.generated.resources.active_student_more
import readwriteapp.feature.student.generated.resources.active_student_mode_dialog_random_hint
import readwriteapp.feature.student.generated.resources.active_student_mode_dialog_random_title
import readwriteapp.feature.student.generated.resources.active_student_mode_dialog_title
import readwriteapp.feature.student.generated.resources.active_student_no_active_decks
import readwriteapp.feature.student.generated.resources.active_student_no_decks
import readwriteapp.feature.student.generated.resources.active_student_no_studied_letters
import readwriteapp.feature.student.generated.resources.active_student_other_decks
import readwriteapp.feature.student.generated.resources.active_student_delete
import readwriteapp.feature.student.generated.resources.active_student_delete_student_message
import readwriteapp.feature.student.generated.resources.active_student_delete_student_secondary_message
import readwriteapp.feature.student.generated.resources.active_student_delete_student_title
import readwriteapp.feature.student.generated.resources.active_student_start
import readwriteapp.feature.student.generated.resources.active_student_studied_digits
import readwriteapp.feature.student.generated.resources.active_student_studied_english_letters
import readwriteapp.feature.student.generated.resources.active_student_studied_letters
import readwriteapp.feature.student.generated.resources.active_student_studied_russian_letters
import readwriteapp.feature.student.generated.resources.add_photo
import readwriteapp.feature.student.generated.resources.cancel
import readwriteapp.feature.student.generated.resources.create_student_name_subtitle
import readwriteapp.feature.student.generated.resources.create_student_avatar_placeholder
import readwriteapp.feature.student.generated.resources.choose_source
import readwriteapp.feature.student.generated.resources.choose_from_gallery
import readwriteapp.feature.student.generated.resources.take_photo
import readwriteapp.feature.student.generated.resources.close
import readwriteapp.feature.student.generated.resources.save

@Composable
fun ActiveStudentRoute(
    onOpenDeck: (String) -> Unit,
    onOpenGame: (List<String>, GameLaunchMode) -> Unit,
    onOpenDeckGallery: (String) -> Unit,
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
    val photoPickerErrorText = stringResource(Res.string.active_student_error_open_photo_picker_failed)
    val updateStudentErrorText = stringResource(Res.string.active_student_error_update_student_failed)
    val deleteStudentErrorText = stringResource(Res.string.active_student_error_delete_student_failed)
    val archivePicker = rememberDeckArchivePicker(
        onArchivePicked = { uri ->
            viewModel.onAction(ActiveStudentAction.OnImportDeckFilePicked(uri))
        },
        onError = {
            messenger.showMessage(archivePickerErrorText)
        },
    )
    val photoPicker = rememberCoverImagePicker(
        onImagePicked = { uri ->
            viewModel.onAction(ActiveStudentAction.OnEditStudentPhotoPicked(uri))
        },
        onError = {
            messenger.showMessage(photoPickerErrorText)
        },
    )

    LaunchedEffect(viewModel) {
        viewModel.onScreenShown()
    }

    LaunchedEffect(
        state.pendingPickerRequest,
        state.isEditStudentPhotoSourceDialogVisible,
    ) {
        if (state.isEditStudentPhotoSourceDialogVisible) return@LaunchedEffect
        when (state.pendingPickerRequest) {
            StudentPickerRequest.GALLERY -> {
                photoPicker.openGallery()
                viewModel.onAction(ActiveStudentAction.OnEditStudentPickerRequestConsumed)
            }
            StudentPickerRequest.CAMERA -> {
                photoPicker.openCamera()
                viewModel.onAction(ActiveStudentAction.OnEditStudentPickerRequestConsumed)
            }
            null -> Unit
        }
    }

    LaunchedEffect(effect) {
        when (val current = effect) {
            is ActiveStudentEffect.OpenDeck -> {
                DeckNavigationState.selectDeck(deckId = current.deckId)
                onOpenDeck(current.deckId)
                viewModel.consumeEffect()
            }

            is ActiveStudentEffect.OpenDeckGallery -> {
                DeckNavigationState.selectDeck(deckId = current.deckId)
                onOpenDeckGallery(current.deckId)
                viewModel.consumeEffect()
            }

            is ActiveStudentEffect.OpenGame -> {
                DeckNavigationState.selectDeck(deckId = current.deckIds.firstOrNull().orEmpty())
                onOpenGame(current.deckIds, current.mode)
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

            ActiveStudentEffect.ShowStudentUpdateFailed -> {
                messenger.showMessage(updateStudentErrorText)
                viewModel.consumeEffect()
            }

            ActiveStudentEffect.ShowDeleteStudentFailed -> {
                messenger.showMessage(deleteStudentErrorText)
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
    val studiedSymbols = state.activeLetters.lowercase().toSet()
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
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            BoxWithConstraints {
                val compactHeroSpacing = maxWidth < 600.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StudentAvatar(
                        avatarUri = state.studentAvatarUri,
                        size = 124.dp,
                    )

                    Column(
                        modifier = Modifier.padding(bottom = if (compactHeroSpacing) 0.dp else 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        Text(
                            text = "$displayName 👧",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )

                        TextButton(
                            onClick = { onAction(ActiveStudentAction.OnChangeStudentClick) },
                        ) {
                            Text(text = stringResource(Res.string.active_student_change))
                        }
                    }

                    Button(
                        onClick = { onAction(ActiveStudentAction.OnStartClick) },
                        enabled = state.activeDecks.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(Res.string.active_student_start))
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextButton(
                            onClick = { state.studentId?.let(onOpenSessionSettings) },
                            enabled = state.studentId != null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(Res.string.active_student_learning_settings),
                                textAlign = TextAlign.Center,
                            )
                        }
                        TextButton(
                            onClick = { state.studentId?.let(onOpenKeyboardSettings) },
                            enabled = state.studentId != null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(Res.string.active_student_keyboard_settings),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
        }

        SectionCard(
            title = stringResource(Res.string.active_student_studied_letters),
        ) {
            if (state.activeLetters.isBlank()) {
                Text(
                    text = stringResource(Res.string.active_student_no_studied_letters),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
        }

        SectionCard(
            title = stringResource(Res.string.active_student_active_decks),
        ) {
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
                }
            }
        }

        SectionCard(
            title = stringResource(Res.string.active_student_other_decks),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (state.otherDecks.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.active_student_no_decks),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                }

                Button(
                    onClick = { onAction(ActiveStudentAction.OnMoreDecksClick) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(Res.string.active_student_all_decks))
                }
            }
        }

        SectionCard(
            title = stringResource(Res.string.active_student_more),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { onAction(ActiveStudentAction.OnImportDeckClick) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(Res.string.active_student_import),
                        textAlign = TextAlign.Center,
                    )
                }
                OutlinedButton(
                    onClick = { onAction(ActiveStudentAction.OnCreateDeckClick) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(Res.string.active_student_create_in_other),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (state.isTrainingModeDialogVisible) {
        AlertDialog(
            onDismissRequest = {
                onAction(ActiveStudentAction.OnDismissTrainingModeDialog)
            },
            title = {
                Text(stringResource(Res.string.active_student_mode_dialog_title))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onAction(
                                ActiveStudentAction.OnTrainingModeSelected(GameLaunchMode.Plan)
                            )
                        },
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(stringResource(Res.string.active_student_mode_dialog_plan_title))
                            Text(
                                text = stringResource(Res.string.active_student_mode_dialog_plan_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            onAction(
                                ActiveStudentAction.OnTrainingModeSelected(GameLaunchMode.RandomLearned)
                            )
                        },
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(stringResource(Res.string.active_student_mode_dialog_random_title))
                            Text(
                                text = stringResource(Res.string.active_student_mode_dialog_random_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            onAction(ActiveStudentAction.OnGalleryClick)
                        },
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(stringResource(Res.string.active_student_mode_dialog_gallery_title))
                            Text(
                                text = stringResource(Res.string.active_student_mode_dialog_gallery_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        onAction(ActiveStudentAction.OnDismissTrainingModeDialog)
                    },
                ) {
                    Text(stringResource(Res.string.active_student_mode_dialog_cancel))
                }
            },
        )
    }

    if (state.isGalleryDeckDialogVisible) {
        AlertDialog(
            onDismissRequest = {
                onAction(ActiveStudentAction.OnDismissGalleryDeckDialog)
            },
            title = {
                Text(stringResource(Res.string.active_student_gallery_choose_deck_title))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.activeDecks.forEach { deck ->
                        TextButton(
                            onClick = {
                                onAction(
                                    ActiveStudentAction.OnGalleryDeckSelected(deck.deck.id)
                                )
                            },
                        ) {
                            Text(deck.deck.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        onAction(ActiveStudentAction.OnDismissGalleryDeckDialog)
                    },
                ) {
                    Text(stringResource(Res.string.active_student_mode_dialog_cancel))
                }
            },
        )
    }

    if (state.isEditStudentDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(ActiveStudentAction.OnDismissEditStudentDialog) },
            title = { Text(stringResource(Res.string.active_student_edit_student_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier.clickable {
                                onAction(ActiveStudentAction.OnEditStudentAvatarClick)
                            },
                        ) {
                            StudentAvatar(
                                avatarUri = state.editStudentAvatarUri,
                                size = 112.dp,
                            )
                        }
                    }

                    TextButton(
                        onClick = { onAction(ActiveStudentAction.OnEditStudentAvatarClick) },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(stringResource(Res.string.add_photo))
                    }

                    Text(
                        text = stringResource(Res.string.create_student_name_subtitle),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )

                    OutlinedTextField(
                        value = state.editStudentName,
                        onValueChange = {
                            onAction(ActiveStudentAction.OnEditStudentNameChanged(it))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onAction(ActiveStudentAction.OnSaveStudentChanges) },
                ) {
                    Text(stringResource(Res.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onAction(ActiveStudentAction.OnDismissEditStudentDialog) },
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    if (state.isEditStudentPhotoSourceDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(ActiveStudentAction.OnDismissEditStudentPhotoSourceDialog) },
            title = { Text(stringResource(Res.string.choose_source)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onAction(ActiveStudentAction.OnEditStudentPickFromGalleryClick) }) {
                        Text(stringResource(Res.string.choose_from_gallery))
                    }
                    TextButton(onClick = { onAction(ActiveStudentAction.OnEditStudentTakePhotoClick) }) {
                        Text(stringResource(Res.string.take_photo))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { onAction(ActiveStudentAction.OnDismissEditStudentPhotoSourceDialog) }) {
                    Text(stringResource(Res.string.close))
                }
            },
        )
    }

    if (state.isDeleteStudentDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(ActiveStudentAction.OnDismissDeleteStudentDialog) },
            title = { Text(stringResource(Res.string.active_student_delete_student_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StudentAvatar(
                        avatarUri = state.studentAvatarUri,
                        size = 88.dp,
                    )
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(
                            Res.string.active_student_delete_student_message,
                            displayName,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(
                            Res.string.active_student_delete_student_secondary_message,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onAction(ActiveStudentAction.OnConfirmDeleteStudent) },
                ) {
                    Text(stringResource(Res.string.active_student_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onAction(ActiveStudentAction.OnDismissDeleteStudentDialog) },
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

private val DIGIT_ORDER = ('0'..'9').toList()
private val RUSSIAN_LETTER_ORDER = listOf(
    'а', 'б', 'в', 'г', 'д', 'е', 'ё', 'ж', 'з', 'и', 'й', 'к', 'л', 'м', 'н', 'о',
    'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ъ', 'ы', 'ь', 'э', 'ю', 'я',
)
private val ENGLISH_LETTER_ORDER = ('a'..'z').toList()

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                content()
            },
        )
    }
}

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
                        .background(MaterialTheme.colorScheme.primaryContainer)
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
