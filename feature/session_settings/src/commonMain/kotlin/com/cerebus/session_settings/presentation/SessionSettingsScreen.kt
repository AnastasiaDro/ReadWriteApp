package com.cerebus.session_settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import readwriteapp.feature.session_settings.generated.resources.Res
import readwriteapp.feature.session_settings.generated.resources.session_settings_active_decks
import readwriteapp.feature.session_settings.generated.resources.session_settings_allow_near_match
import readwriteapp.feature.session_settings.generated.resources.session_settings_cancel
import readwriteapp.feature.session_settings.generated.resources.session_settings_guided_hint_threshold
import readwriteapp.feature.session_settings.generated.resources.session_settings_keyboard
import readwriteapp.feature.session_settings.generated.resources.session_settings_learn_more_step
import readwriteapp.feature.session_settings.generated.resources.session_settings_load_error
import readwriteapp.feature.session_settings.generated.resources.session_settings_max_new_per_day
import readwriteapp.feature.session_settings.generated.resources.session_settings_prevent_wrong_key_press
import readwriteapp.feature.session_settings.generated.resources.session_settings_new_cards
import readwriteapp.feature.session_settings.generated.resources.session_settings_no_decks
import readwriteapp.feature.session_settings.generated.resources.session_settings_reviews
import readwriteapp.feature.session_settings.generated.resources.session_settings_save
import readwriteapp.feature.session_settings.generated.resources.session_settings_save_error
import readwriteapp.feature.session_settings.generated.resources.session_settings_saving
import readwriteapp.feature.session_settings.generated.resources.session_settings_title
import kotlin.math.roundToInt

@Composable
fun SessionSettingsRoute(
    studentId: String,
    onOpenKeyboardSettings: () -> Unit,
    onClose: () -> Unit,
    onError: (String) -> Unit = {},
) {
    val viewModel = koinViewModel<SessionSettingsViewModel>(
        parameters = { parametersOf(studentId) },
    )
    val state by viewModel.state.collectAsState()
    val loadErrorMessage = stringResource(Res.string.session_settings_load_error)
    val saveErrorMessage = stringResource(Res.string.session_settings_save_error)

    LaunchedEffect(viewModel) {
        viewModel.onIntent(SessionSettingsIntent.Load)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SessionSettingsEffect.CloseScreen -> onClose()
                SessionSettingsEffect.ShowLoadError -> onError(loadErrorMessage)
                SessionSettingsEffect.ShowSaveError -> onError(saveErrorMessage)
            }
        }
    }

    SessionSettingsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onOpenKeyboardSettings = onOpenKeyboardSettings,
    )
}

@Composable
fun SessionSettingsScreen(
    state: SessionSettingsState,
    onIntent: (SessionSettingsIntent) -> Unit,
    onOpenKeyboardSettings: () -> Unit,
) {
    if (state.isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.session_settings_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        TextButton(
            onClick = onOpenKeyboardSettings,
            modifier = Modifier.align(Alignment.Start),
        ) {
            Text(stringResource(Res.string.session_settings_keyboard))
        }

        NumericField(
            label = stringResource(Res.string.session_settings_new_cards),
            value = state.newCardsPerSession,
            onValueChanged = { onIntent(SessionSettingsIntent.ChangeNewCards(it)) },
        )

        NumericField(
            label = stringResource(Res.string.session_settings_reviews),
            value = state.reviewsPerSession,
            onValueChanged = { onIntent(SessionSettingsIntent.ChangeReviews(it)) },
        )

        NumericField(
            label = stringResource(Res.string.session_settings_learn_more_step),
            value = state.learnMoreStep,
            onValueChanged = { onIntent(SessionSettingsIntent.ChangeLearnMoreStep(it)) },
        )

        DiscreteSliderField(
            label = stringResource(Res.string.session_settings_guided_hint_threshold),
            value = state.guidedHintSuccessThreshold,
            valueRange = 0..5,
            onValueChanged = { onIntent(SessionSettingsIntent.ChangeGuidedHintSuccessThreshold(it)) },
        )

        NumericField(
            label = stringResource(Res.string.session_settings_max_new_per_day),
            value = state.maxNewCardsPerDay,
            onValueChanged = { onIntent(SessionSettingsIntent.ChangeMaxNewPerDay(it)) },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(Res.string.session_settings_allow_near_match))
            Switch(
                checked = state.allowNearMatch,
                onCheckedChange = { onIntent(SessionSettingsIntent.ChangeAllowNearMatch(it)) },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(Res.string.session_settings_prevent_wrong_key_press))
            Switch(
                checked = state.preventWrongKeyPress,
                onCheckedChange = { onIntent(SessionSettingsIntent.ChangePreventWrongKeyPress(it)) },
            )
        }

        Text(
            text = stringResource(Res.string.session_settings_active_decks),
            style = MaterialTheme.typography.titleMedium,
        )

        if (state.deckOptions.isEmpty()) {
            Text(
                text = stringResource(Res.string.session_settings_no_decks),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.deckOptions.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = option.deckName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(
                        checked = option.isActive,
                        onCheckedChange = { enabled ->
                            onIntent(
                                SessionSettingsIntent.ToggleDeck(
                                    deckId = option.deckId,
                                    isActive = enabled,
                                )
                            )
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { onIntent(SessionSettingsIntent.SaveClicked) },
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (state.isSaving) {
                        stringResource(Res.string.session_settings_saving)
                    } else {
                        stringResource(Res.string.session_settings_save)
                    },
                )
            }

            TextButton(
                onClick = { onIntent(SessionSettingsIntent.CancelClicked) },
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.session_settings_cancel))
            }
        }
    }
}

@Composable
private fun NumericField(
    label: String,
    value: Int,
    onValueChanged: (Int) -> Unit,
) {
    val valueText = value.toString()
    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = valueText,
                selection = TextRange(valueText.length),
            )
        )
    }

    LaunchedEffect(valueText) {
        if (valueText != fieldValue.text) {
            val selectionIndex = fieldValue.selection.end.coerceIn(0, valueText.length)
            fieldValue = TextFieldValue(
                text = valueText,
                selection = TextRange(selectionIndex),
            )
        }
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { updated ->
            val digitsOnly = updated.text.filter { it.isDigit() }
            val selectionIndex = updated.selection.end.coerceIn(0, digitsOnly.length)
            fieldValue = updated.copy(
                text = digitsOnly,
                selection = TextRange(selectionIndex),
            )
            if (digitsOnly.isEmpty()) {
                onValueChanged(0)
                return@OutlinedTextField
            }
            digitsOnly.toIntOrNull()?.let(onValueChanged)
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
}

@Composable
private fun DiscreteSliderField(
    label: String,
    value: Int,
    valueRange: IntRange,
    onValueChanged: (Int) -> Unit,
) {
    val min = valueRange.first
    val max = valueRange.last
    val safeValue = value.coerceIn(min, max)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "$label: $safeValue",
            style = MaterialTheme.typography.bodyLarge,
        )
        Slider(
            value = safeValue.toFloat(),
            onValueChange = { raw ->
                onValueChanged(raw.roundToInt().coerceIn(min, max))
            },
            valueRange = min.toFloat()..max.toFloat(),
            steps = (max - min - 1).coerceAtLeast(0),
        )
    }
}
