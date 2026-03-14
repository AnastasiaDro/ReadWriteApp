package com.cerebus.session_settings.presentation

data class SessionSettingsState(
    val newCardsPerSession: Int = 5,
    val reviewsPerSession: Int = 15,
    val learnMoreStep: Int = 5,
    val guidedHintSuccessThreshold: Int = 2,
    val maxNewCardsPerDay: Int = 15,
    val allowNearMatch: Boolean = true,
    val preventWrongKeyPress: Boolean = true,
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
    data class ToggleDeck(val deckId: String, val isActive: Boolean) : SessionSettingsIntent
    data object SaveClicked : SessionSettingsIntent
    data object CancelClicked : SessionSettingsIntent
}

sealed interface SessionSettingsEffect {
    data object CloseScreen : SessionSettingsEffect
    data object ShowLoadError : SessionSettingsEffect
    data object ShowSaveError : SessionSettingsEffect
}
