package com.cerebus.session_settings.presentation

import com.cerebus.data.preferences.domain.models.KeyboardPressDelay
import com.cerebus.data.preferences.domain.models.NeighborTypoSensitivity

data class SessionSettingsState(
    val newCardsPerSession: Int = 5,
    val reviewsPerSession: Int = 15,
    val learnMoreStep: Int = 5,
    val guidedHintSuccessThreshold: Int = 2,
    val maxNewCardsPerDay: Int = 15,
    val allowNearMatch: Boolean = true,
    val preventWrongKeyPress: Boolean = true,
    val keyboardPressDelay: KeyboardPressDelay = KeyboardPressDelay.Normal,
    val allowNeighborTypos: Boolean = true,
    val neighborTypoSensitivity: NeighborTypoSensitivity = NeighborTypoSensitivity.Normal,
    val freeNeighborSlipPresses: Int = 1,
    val deckOptions: List<SessionDeckOptionUi> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
)

data class SessionDeckOptionUi(
    val deckId: String,
    val deckName: String,
    val isActive: Boolean,
)

sealed interface SessionSettingsIntent {
    data object Load : SessionSettingsIntent
    data class ChangeNewCards(val value: Int) : SessionSettingsIntent
    data class ChangeReviews(val value: Int) : SessionSettingsIntent
    data class ChangeLearnMoreStep(val value: Int) : SessionSettingsIntent
    data class ChangeGuidedHintSuccessThreshold(val value: Int) : SessionSettingsIntent
    data class ChangeMaxNewPerDay(val value: Int) : SessionSettingsIntent
    data class ChangeAllowNearMatch(val value: Boolean) : SessionSettingsIntent
    data class ChangePreventWrongKeyPress(val value: Boolean) : SessionSettingsIntent
    data class ChangeKeyboardPressDelay(val value: KeyboardPressDelay) : SessionSettingsIntent
    data class ChangeAllowNeighborTypos(val value: Boolean) : SessionSettingsIntent
    data class ChangeNeighborTypoSensitivity(val value: NeighborTypoSensitivity) : SessionSettingsIntent
    data class ChangeFreeNeighborSlipPresses(val value: Int) : SessionSettingsIntent
    data class ToggleDeck(val deckId: String, val isActive: Boolean) : SessionSettingsIntent
    data object SaveClicked : SessionSettingsIntent
    data object CancelClicked : SessionSettingsIntent
}

sealed interface SessionSettingsEffect {
    data object CloseScreen : SessionSettingsEffect
    data object ShowLoadError : SessionSettingsEffect
    data object ShowSaveError : SessionSettingsEffect
}
