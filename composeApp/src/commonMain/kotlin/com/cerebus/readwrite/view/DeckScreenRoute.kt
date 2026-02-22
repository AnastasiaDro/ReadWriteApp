package com.cerebus.readwrite.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cerebus.create_screen.presentation.DeckPickerRequest
import com.cerebus.create_screen.presentation.DeckScreen
import com.cerebus.create_screen.presentation.DeckScreenAction
import com.cerebus.create_screen.presentation.DeckScreenStrings
import com.cerebus.create_screen.presentation.DeckScreenViewModel
import com.cerebus.create_screen.presentation.DeckValidationError
import com.cerebus.readwrite.media.rememberCoverImagePicker
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource
import readwriteapp.composeapp.generated.resources.Res
import readwriteapp.composeapp.generated.resources.add_card
import readwriteapp.composeapp.generated.resources.add_card_title
import readwriteapp.composeapp.generated.resources.add_cover
import readwriteapp.composeapp.generated.resources.back
import readwriteapp.composeapp.generated.resources.card_name_label
import readwriteapp.composeapp.generated.resources.cancel
import readwriteapp.composeapp.generated.resources.cards
import readwriteapp.composeapp.generated.resources.choose_from_gallery
import readwriteapp.composeapp.generated.resources.choose_source
import readwriteapp.composeapp.generated.resources.close
import readwriteapp.composeapp.generated.resources.create
import readwriteapp.composeapp.generated.resources.confirm_delete_cards_message
import readwriteapp.composeapp.generated.resources.confirm_delete_cards_title
import readwriteapp.composeapp.generated.resources.deck_name_label
import readwriteapp.composeapp.generated.resources.edit_cover
import readwriteapp.composeapp.generated.resources.edit_card_title
import readwriteapp.composeapp.generated.resources.edit_name
import readwriteapp.composeapp.generated.resources.error_add_card_failed
import readwriteapp.composeapp.generated.resources.error_delete_cards_failed
import readwriteapp.composeapp.generated.resources.error_deck_not_found
import readwriteapp.composeapp.generated.resources.error_empty_card_name
import readwriteapp.composeapp.generated.resources.error_empty_deck_name
import readwriteapp.composeapp.generated.resources.error_update_card_failed
import readwriteapp.composeapp.generated.resources.error_update_cover_failed
import readwriteapp.composeapp.generated.resources.error_update_name_failed
import readwriteapp.composeapp.generated.resources.no_cover
import readwriteapp.composeapp.generated.resources.save
import readwriteapp.composeapp.generated.resources.selected_count
import readwriteapp.composeapp.generated.resources.take_photo
import readwriteapp.composeapp.generated.resources.unnamed_deck
import readwriteapp.composeapp.generated.resources.delete

@Composable
fun DeckScreenRoute(
    deckId: String,
    onBackClick: () -> Unit,
) {
    val viewModel = koinViewModel<DeckScreenViewModel>()
    val state by viewModel.uiState.collectAsState()

    val picker = rememberCoverImagePicker(
        onImagePicked = { uri ->
            viewModel.onAction(DeckScreenAction.OnImagePicked(uri))
        },
        onError = {
            // Placeholder for future snackbar/toast integration.
        },
    )

    LaunchedEffect(deckId) {
        viewModel.onAction(DeckScreenAction.Initialize(deckId))
    }

    LaunchedEffect(state.pendingPickerRequest) {
        when (state.pendingPickerRequest) {
            DeckPickerRequest.GALLERY -> {
                picker.openGallery()
                viewModel.onAction(DeckScreenAction.OnPickerRequestConsumed)
            }
            DeckPickerRequest.CAMERA -> {
                picker.openCamera()
                viewModel.onAction(DeckScreenAction.OnPickerRequestConsumed)
            }
            null -> Unit
        }
    }

    val strings = DeckScreenStrings(
        back = stringResource(Res.string.back),
        addCard = stringResource(Res.string.add_card),
        addCardTitle = stringResource(Res.string.add_card_title),
        editCardTitle = stringResource(Res.string.edit_card_title),
        cards = stringResource(Res.string.cards),
        editName = stringResource(Res.string.edit_name),
        editCover = stringResource(Res.string.edit_cover),
        addCover = stringResource(Res.string.add_cover),
        chooseSource = stringResource(Res.string.choose_source),
        chooseFromGallery = stringResource(Res.string.choose_from_gallery),
        takePhoto = stringResource(Res.string.take_photo),
        create = stringResource(Res.string.create),
        save = stringResource(Res.string.save),
        cancel = stringResource(Res.string.cancel),
        close = stringResource(Res.string.close),
        delete = stringResource(Res.string.delete),
        selectedCount = stringResource(Res.string.selected_count),
        confirmDeleteCardsTitle = stringResource(Res.string.confirm_delete_cards_title),
        confirmDeleteCardsMessage = stringResource(Res.string.confirm_delete_cards_message),
        deckNameLabel = stringResource(Res.string.deck_name_label),
        cardNameLabel = stringResource(Res.string.card_name_label),
        noCover = stringResource(Res.string.no_cover),
        unnamedDeck = stringResource(Res.string.unnamed_deck),
    )

    val validationErrorText = when (state.validationError) {
        DeckValidationError.DECK_NOT_FOUND -> stringResource(Res.string.error_deck_not_found)
        DeckValidationError.EMPTY_DECK_NAME -> stringResource(Res.string.error_empty_deck_name)
        DeckValidationError.UPDATE_NAME_FAILED -> stringResource(Res.string.error_update_name_failed)
        DeckValidationError.UPDATE_COVER_FAILED -> stringResource(Res.string.error_update_cover_failed)
        DeckValidationError.EMPTY_CARD_NAME -> stringResource(Res.string.error_empty_card_name)
        DeckValidationError.ADD_CARD_FAILED -> stringResource(Res.string.error_add_card_failed)
        DeckValidationError.UPDATE_CARD_FAILED -> stringResource(Res.string.error_update_card_failed)
        DeckValidationError.DELETE_CARDS_FAILED -> stringResource(Res.string.error_delete_cards_failed)
        null -> null
    }

    DeckScreen(
        state = state,
        strings = strings,
        validationErrorText = validationErrorText,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
    )
}
