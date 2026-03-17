package com.cerebus.session_settings.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.material3.FilterChip
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
import com.cerebus.data.preferences.domain.models.KeyboardPressDelay
import com.cerebus.data.preferences.domain.models.NeighborTypoSensitivity
import com.cerebus.session_settings.navigation.SessionSettingsScrollTarget
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import readwriteapp.feature.session_settings.generated.resources.Res
import readwriteapp.feature.session_settings.generated.resources.session_settings_active_decks
import readwriteapp.feature.session_settings.generated.resources.session_settings_allow_near_match
import readwriteapp.feature.session_settings.generated.resources.session_settings_allow_neighbor_typos
import readwriteapp.feature.session_settings.generated.resources.session_settings_back
import readwriteapp.feature.session_settings.generated.resources.session_settings_cancel
import readwriteapp.feature.session_settings.generated.resources.session_settings_guided_hint_threshold
import readwriteapp.feature.session_settings.generated.resources.session_settings_guided_hint_threshold_hint
import readwriteapp.feature.session_settings.generated.resources.session_settings_keyboard_press_delay
import readwriteapp.feature.session_settings.generated.resources.session_settings_keyboard_press_delay_fast
import readwriteapp.feature.session_settings.generated.resources.session_settings_keyboard_press_delay_hint
import readwriteapp.feature.session_settings.generated.resources.session_settings_keyboard_press_delay_normal
import readwriteapp.feature.session_settings.generated.resources.session_settings_keyboard_press_delay_slow
import readwriteapp.feature.session_settings.generated.resources.session_settings_keyboard
import readwriteapp.feature.session_settings.generated.resources.session_settings_learn_more_step
import readwriteapp.feature.session_settings.generated.resources.session_settings_load_error
import readwriteapp.feature.session_settings.generated.resources.session_settings_max_new_per_day
import readwriteapp.feature.session_settings.generated.resources.session_settings_neighbor_typo_sensitivity
import readwriteapp.feature.session_settings.generated.resources.session_settings_neighbor_typo_sensitivity_hint
import readwriteapp.feature.session_settings.generated.resources.session_settings_neighbor_typo_sensitivity_normal
import readwriteapp.feature.session_settings.generated.resources.session_settings_neighbor_typo_sensitivity_soft
import readwriteapp.feature.session_settings.generated.resources.session_settings_free_neighbor_slips
import readwriteapp.feature.session_settings.generated.resources.session_settings_free_neighbor_slips_hint
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
    scrollTarget: SessionSettingsScrollTarget? = null,
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
        scrollTarget = scrollTarget,
        onIntent = viewModel::onIntent,
        onOpenKeyboardSettings = onOpenKeyboardSettings,
        onClose = onClose,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionSettingsScreen(
    state: SessionSettingsState,
    scrollTarget: SessionSettingsScrollTarget? = null,
    onIntent: (SessionSettingsIntent) -> Unit,
    onOpenKeyboardSettings: () -> Unit,
    onClose: () -> Unit,
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

    val typoSettingsBringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(scrollTarget, state.isLoading) {
        if (state.isLoading) return@LaunchedEffect
        if (scrollTarget == SessionSettingsScrollTarget.TypoSettings) {
            typoSettingsBringIntoViewRequester.bringIntoView()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterVertically),
            ) {
                Text(stringResource(Res.string.session_settings_back))
            }

            Text(
                text = stringResource(Res.string.session_settings_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        }

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
            helperText = stringResource(Res.string.session_settings_guided_hint_threshold_hint),
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

        ChoiceChipField(
            label = stringResource(Res.string.session_settings_keyboard_press_delay),
            helperText = stringResource(Res.string.session_settings_keyboard_press_delay_hint),
            selected = state.keyboardPressDelay,
            enabled = true,
            options = listOf(
                KeyboardPressDelay.Fast to stringResource(
                    Res.string.session_settings_keyboard_press_delay_fast
                ),
                KeyboardPressDelay.Normal to stringResource(
                    Res.string.session_settings_keyboard_press_delay_normal
                ),
                KeyboardPressDelay.Slow to stringResource(
                    Res.string.session_settings_keyboard_press_delay_slow
                ),
            ),
            onSelected = {
                onIntent(SessionSettingsIntent.ChangeKeyboardPressDelay(it))
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(typoSettingsBringIntoViewRequester),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(Res.string.session_settings_allow_neighbor_typos))
                Switch(
                    checked = state.allowNeighborTypos,
                    onCheckedChange = { onIntent(SessionSettingsIntent.ChangeAllowNeighborTypos(it)) },
                )
            }

            ChoiceChipField(
                label = stringResource(Res.string.session_settings_neighbor_typo_sensitivity),
                helperText = stringResource(Res.string.session_settings_neighbor_typo_sensitivity_hint),
                selected = state.neighborTypoSensitivity
                    .takeIf { it != NeighborTypoSensitivity.Strict }
                    ?: NeighborTypoSensitivity.Normal,
                enabled = state.allowNeighborTypos,
                options = listOf(
                    NeighborTypoSensitivity.Normal to stringResource(
                        Res.string.session_settings_neighbor_typo_sensitivity_normal
                    ),
                    NeighborTypoSensitivity.Soft to stringResource(
                        Res.string.session_settings_neighbor_typo_sensitivity_soft
                    ),
                ),
                onSelected = {
                    onIntent(SessionSettingsIntent.ChangeNeighborTypoSensitivity(it))
                },
            )

            DiscreteSliderField(
                label = stringResource(Res.string.session_settings_free_neighbor_slips),
                helperText = stringResource(Res.string.session_settings_free_neighbor_slips_hint),
                value = state.freeNeighborSlipPresses,
                valueRange = 0..3,
                enabled = state.allowNeighborTypos,
                onValueChanged = { onIntent(SessionSettingsIntent.ChangeFreeNeighborSlipPresses(it)) },
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
    helperText: String? = null,
    value: Int,
    valueRange: IntRange,
    enabled: Boolean = true,
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
        if (!helperText.isNullOrBlank()) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = safeValue.toFloat(),
            onValueChange = { raw ->
                onValueChanged(raw.roundToInt().coerceIn(min, max))
            },
            valueRange = min.toFloat()..max.toFloat(),
            steps = (max - min - 1).coerceAtLeast(0),
            enabled = enabled,
        )
    }
}

@Composable
private fun <T> ChoiceChipField(
    label: String,
    helperText: String? = null,
    selected: T,
    options: List<Pair<T, String>>,
    enabled: Boolean,
    onSelected: (T) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (!helperText.isNullOrBlank()) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (value, title) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelected(value) },
                    enabled = enabled,
                    label = { Text(title) },
                )
            }
        }
    }
}
