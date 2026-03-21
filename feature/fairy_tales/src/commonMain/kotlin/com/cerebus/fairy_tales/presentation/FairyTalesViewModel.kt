package com.cerebus.fairy_tales.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class FairyTalesViewModel(
    fairyTaleId: String,
) : ViewModel() {

    var state by mutableStateOf(
        FairyTalesCatalog.findById(fairyTaleId)?.toUiState()
            ?: FairyTalesCatalog.items.first().toUiState()
    )
        private set
}

private fun FairyTaleContent.toUiState(): FairyTalesUiState =
    FairyTalesUiState(
        fairyTaleId = id,
        title = title,
        description = description,
        coverColor = coverColor,
        coverRes = coverRes,
        animationAssetPath = animationAssetPath,
        storyText = storyText,
    )
