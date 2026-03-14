package com.cerebus.customkeyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.data.student.domain.repositories.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import readwriteapp.feature.customkeyboard.generated.resources.Res
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_close
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_empty
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_english
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_russian
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_selected
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_subtitle
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_title

private val keyboardSettingsRussianRows = listOf(
    listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х"),
    listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"),
    listOf("я", "ч", "с", "м", "и", "т", "ь", "б", "ю", "ъ", "ё"),
)

private val keyboardSettingsEnglishRows = listOf(
    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
    listOf("z", "x", "c", "v", "b", "n", "m"),
)

data class KeyboardSettingsUiState(
    val isLoading: Boolean = true,
    val selectedLetters: Set<Char> = emptySet(),
)

class KeyboardSettingsViewModel(
    private val studentId: String,
    private val studentRepository: StudentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(KeyboardSettingsUiState())
    val uiState: StateFlow<KeyboardSettingsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val letters = studentRepository.getActiveLettersById(studentId)
                .orEmpty()
                .lowercase()
                .filter { it.isLetter() }
                .toSet()
            _uiState.value = KeyboardSettingsUiState(
                isLoading = false,
                selectedLetters = letters,
            )
        }
    }

    fun onLetterClicked(letter: Char) {
        val normalized = letter.lowercaseChar()
        viewModelScope.launch {
            val updated = if (normalized in _uiState.value.selectedLetters) {
                if (studentRepository.removeLetter(studentId, normalized)) {
                    _uiState.value.selectedLetters - normalized
                } else {
                    _uiState.value.selectedLetters
                }
            } else {
                if (studentRepository.addLetter(studentId, normalized)) {
                    _uiState.value.selectedLetters + normalized
                } else {
                    _uiState.value.selectedLetters
                }
            }
            _uiState.update { it.copy(selectedLetters = updated) }
        }
    }
}

@Composable
fun KeyboardSettingsRoute(
    studentId: String,
    onClose: () -> Unit,
) {
    val viewModel = koinViewModel<KeyboardSettingsViewModel>(
        parameters = { parametersOf(studentId) },
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.load()
    }

    KeyboardSettingsScreen(
        state = state,
        onLetterClicked = viewModel::onLetterClicked,
        onClose = onClose,
    )
}

@Composable
fun KeyboardSettingsScreen(
    state: KeyboardSettingsUiState,
    onLetterClicked: (Char) -> Unit,
    onClose: () -> Unit,
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.keyboard_settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onClose) {
                Text(stringResource(Res.string.keyboard_settings_close))
            }
        }

        Text(
            text = stringResource(Res.string.keyboard_settings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SelectedLettersBlock(selectedLetters = state.selectedLetters)

        KeyboardLettersSection(
            title = stringResource(Res.string.keyboard_settings_russian),
            rows = keyboardSettingsRussianRows,
            selectedLetters = state.selectedLetters,
            onLetterClicked = onLetterClicked,
        )

        KeyboardLettersSection(
            title = stringResource(Res.string.keyboard_settings_english),
            rows = keyboardSettingsEnglishRows,
            selectedLetters = state.selectedLetters,
            onLetterClicked = onLetterClicked,
        )
    }
}

@Composable
private fun SelectedLettersBlock(selectedLetters: Set<Char>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.keyboard_settings_selected),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Text(
                text = selectedLetters.sorted().joinToString(" ").ifBlank {
                    stringResource(Res.string.keyboard_settings_empty)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun KeyboardLettersSection(
    title: String,
    rows: List<List<String>>,
    selectedLetters: Set<Char>,
    onLetterClicked: (Char) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        rows.forEach { row ->
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { letter ->
                    val char = letter.single()
                    LetterChip(
                        label = letter,
                        isSelected = char in selectedLetters,
                        onClick = { onLetterClicked(char) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LetterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val content = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
