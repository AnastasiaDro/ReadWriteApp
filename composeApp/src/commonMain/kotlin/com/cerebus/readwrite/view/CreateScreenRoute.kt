package com.cerebus.readwrite.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cerebus.create_screen.navigation.CreateNavigationState
import com.cerebus.create_screen.presentation.CreateScreen
import com.cerebus.create_screen.presentation.CreateScreenAction
import com.cerebus.create_screen.presentation.CreateScreenEffect
import com.cerebus.create_screen.presentation.CreateScreenStrings
import com.cerebus.create_screen.presentation.CreateScreenViewModel
import com.cerebus.create_screen.presentation.CreateValidationError
import com.cerebus.readwrite.media.DeckArchiveShareItem
import com.cerebus.readwrite.media.rememberDeckArchivePicker
import com.cerebus.readwrite.media.rememberDeckArchiveShareLauncher
import com.cerebus.readwrite.media.rememberPlatformMessenger
import com.cerebus.readwrite.media.rememberCoverImagePicker
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource
import readwriteapp.composeapp.generated.resources.Res
import readwriteapp.composeapp.generated.resources.add_cards
import readwriteapp.composeapp.generated.resources.add_cover
import readwriteapp.composeapp.generated.resources.back
import readwriteapp.composeapp.generated.resources.cancel
import readwriteapp.composeapp.generated.resources.choose_from_gallery
import readwriteapp.composeapp.generated.resources.choose_source
import readwriteapp.composeapp.generated.resources.close
import readwriteapp.composeapp.generated.resources.confirm_delete_decks_message
import readwriteapp.composeapp.generated.resources.confirm_delete_decks_title
import readwriteapp.composeapp.generated.resources.confirm_import_deck_replacement_message
import readwriteapp.composeapp.generated.resources.confirm_import_deck_replacement_title
import readwriteapp.composeapp.generated.resources.create
import readwriteapp.composeapp.generated.resources.create_deck
import readwriteapp.composeapp.generated.resources.deck_created_message
import readwriteapp.composeapp.generated.resources.deck_name_label
import readwriteapp.composeapp.generated.resources.delete
import readwriteapp.composeapp.generated.resources.edit_cover
import readwriteapp.composeapp.generated.resources.import_deck
import readwriteapp.composeapp.generated.resources.import_deck_add_missing_summary
import readwriteapp.composeapp.generated.resources.import_deck_replace_summary
import readwriteapp.composeapp.generated.resources.error_create_deck_failed
import readwriteapp.composeapp.generated.resources.error_delete_decks_failed
import readwriteapp.composeapp.generated.resources.error_empty_deck_name
import readwriteapp.composeapp.generated.resources.error_export_decks_limit
import readwriteapp.composeapp.generated.resources.error_export_selected_decks_failed
import readwriteapp.composeapp.generated.resources.error_import_deck_failed
import readwriteapp.composeapp.generated.resources.error_open_archive_picker_failed
import readwriteapp.composeapp.generated.resources.error_share_deck_failed
import readwriteapp.composeapp.generated.resources.export_decks_limit_hint
import readwriteapp.composeapp.generated.resources.export_deck
import readwriteapp.composeapp.generated.resources.my_decks
import readwriteapp.composeapp.generated.resources.no_cover
import readwriteapp.composeapp.generated.resources.no_decks_yet
import readwriteapp.composeapp.generated.resources.add_missing_cards
import readwriteapp.composeapp.generated.resources.replace_deck
import readwriteapp.composeapp.generated.resources.selected_count
import readwriteapp.composeapp.generated.resources.take_photo

private enum class CreatePickerRequest {
    GALLERY,
    CAMERA,
}

@Composable
fun CreateScreenRoute(
    onBackClick: () -> Unit,
    onNavigateToDeck: (String, Boolean) -> Unit,
) {
    val viewModel = koinViewModel<CreateScreenViewModel>()
    val state by viewModel.uiState.collectAsState()
    val pendingImportDeckArchiveUri by CreateNavigationState.pendingImportDeckArchiveUri.collectAsState()
    var pendingPickerRequest by remember { mutableStateOf<CreatePickerRequest?>(null) }
    val messenger = rememberPlatformMessenger()
    val archivePickerErrorText = stringResource(Res.string.error_open_archive_picker_failed)
    val importDeckErrorText = stringResource(Res.string.error_import_deck_failed)
    val exportSelectedDecksErrorText = stringResource(Res.string.error_export_selected_decks_failed)
    val shareDeckErrorText = stringResource(Res.string.error_share_deck_failed)
    val exportDeckLimitErrorText = stringResource(Res.string.error_export_decks_limit)

    val picker = rememberCoverImagePicker(
        onImagePicked = { uri ->
            viewModel.onAction(CreateScreenAction.OnCoverUriSelected(uri))
        },
        onError = {
            // Placeholder for future snackbar/toast integration.
        },
    )
    val archivePicker = rememberDeckArchivePicker(
        onArchivePicked = { uri ->
            viewModel.onAction(CreateScreenAction.OnImportDeckFilePicked(uri))
        },
        onError = {
            messenger.showMessage(archivePickerErrorText)
        },
    )
    val deckShareLauncher = rememberDeckArchiveShareLauncher(
        onError = {
            messenger.showMessage(shareDeckErrorText)
        },
    )

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CreateScreenEffect.OpenGallery -> pendingPickerRequest = CreatePickerRequest.GALLERY
                CreateScreenEffect.OpenCamera -> pendingPickerRequest = CreatePickerRequest.CAMERA
                CreateScreenEffect.OpenImportDeckPicker -> archivePicker.openArchivePicker()
                CreateScreenEffect.ShowImportDeckFailed -> messenger.showMessage(importDeckErrorText)
                CreateScreenEffect.ShowExportSelectedDecksFailed -> messenger.showMessage(exportSelectedDecksErrorText)
                CreateScreenEffect.ShowExportSelectedDecksLimitExceeded -> messenger.showMessage(exportDeckLimitErrorText)
                is CreateScreenEffect.ShareDeckArchives -> {
                    deckShareLauncher.shareArchives(
                        effect.files.map { file ->
                            DeckArchiveShareItem(
                                filePath = file.path,
                                fileName = file.fileName,
                            )
                        }
                    )
                }
                is CreateScreenEffect.OpenDeck -> {
                    onNavigateToDeck(effect.deckId, effect.openAddCardDialog)
                }
            }
        }
    }

    LaunchedEffect(pendingPickerRequest, state.isCoverSourceDialogVisible) {
        if (state.isCoverSourceDialogVisible) return@LaunchedEffect

        when (pendingPickerRequest) {
            CreatePickerRequest.GALLERY -> {
                picker.openGallery()
                pendingPickerRequest = null
            }
            CreatePickerRequest.CAMERA -> {
                picker.openCamera()
                pendingPickerRequest = null
            }
            null -> Unit
        }
    }

    LaunchedEffect(viewModel) {
        CreateNavigationState.openCreateDialogRequests.collect {
            viewModel.onAction(CreateScreenAction.OnCreateDeckClick)
        }
    }

    LaunchedEffect(pendingImportDeckArchiveUri) {
        val uri = pendingImportDeckArchiveUri ?: return@LaunchedEffect
        viewModel.onAction(CreateScreenAction.OnImportDeckFilePicked(uri))
        CreateNavigationState.consumePendingImportDeckArchive(uri)
    }

    val strings = CreateScreenStrings(
        back = stringResource(Res.string.back),
        importDeck = stringResource(Res.string.import_deck),
        myDecks = stringResource(Res.string.my_decks),
        noDecksYet = stringResource(Res.string.no_decks_yet),
        createDeckTitle = stringResource(Res.string.create_deck),
        addCover = stringResource(Res.string.add_cover),
        editCover = stringResource(Res.string.edit_cover),
        deckNameLabel = stringResource(Res.string.deck_name_label),
        create = stringResource(Res.string.create),
        cancel = stringResource(Res.string.cancel),
        chooseSource = stringResource(Res.string.choose_source),
        chooseFromGallery = stringResource(Res.string.choose_from_gallery),
        takePhoto = stringResource(Res.string.take_photo),
        close = stringResource(Res.string.close),
        delete = stringResource(Res.string.delete),
        export = stringResource(Res.string.export_deck),
        selectedCount = stringResource(Res.string.selected_count),
        exportLimitHint = stringResource(Res.string.export_decks_limit_hint),
        confirmDeleteDecksTitle = stringResource(Res.string.confirm_delete_decks_title),
        confirmDeleteDecksMessage = stringResource(Res.string.confirm_delete_decks_message),
        confirmImportDeckReplacementTitle = stringResource(Res.string.confirm_import_deck_replacement_title),
        confirmImportDeckReplacementMessage = stringResource(Res.string.confirm_import_deck_replacement_message),
        importDeckReplaceSummary = stringResource(Res.string.import_deck_replace_summary),
        importDeckAddMissingSummary = stringResource(Res.string.import_deck_add_missing_summary),
        replaceDeck = stringResource(Res.string.replace_deck),
        addMissingCards = stringResource(Res.string.add_missing_cards),
        deckCreatedTemplate = stringResource(Res.string.deck_created_message),
        addCards = stringResource(Res.string.add_cards),
        noCover = stringResource(Res.string.no_cover),
    )

    val validationErrorText = when (state.validationError) {
        CreateValidationError.EMPTY_DECK_NAME -> stringResource(Res.string.error_empty_deck_name)
        CreateValidationError.CREATE_DECK_FAILED -> stringResource(Res.string.error_create_deck_failed)
        CreateValidationError.DELETE_DECK_FAILED -> stringResource(Res.string.error_delete_decks_failed)
        CreateValidationError.DELETE_DECKS_FAILED -> stringResource(Res.string.error_delete_decks_failed)
        null -> null
    }

    CreateScreen(
        state = state,
        strings = strings,
        validationErrorText = validationErrorText,
        onBackClick = onBackClick,
        onAction = viewModel::onAction,
    )
}
