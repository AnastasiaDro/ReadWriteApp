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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
import com.cerebus.data.student.domain.repositories.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import readwriteapp.feature.customkeyboard.generated.resources.Res
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_close
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_disable_all
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_empty
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_enable_all
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_english
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_language
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_language_english
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_language_russian
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_numbers
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_prevent_wrong_key_press
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_russian
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_select_language
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_selected
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_subtitle
import readwriteapp.feature.customkeyboard.generated.resources.keyboard_settings_title

private val keyboardSettingsNumberRows = listOf(
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
)

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

private val numberSymbols = keyboardSettingsNumberRows.flatten().map { it.single().lowercaseChar() }
private val russianSymbols = keyboardSettingsRussianRows.flatten().map { it.single().lowercaseChar() }
private val englishSymbols = keyboardSettingsEnglishRows.flatten().map { it.single().lowercaseChar() }
private val allKeyboardSymbols = numberSymbols + russianSymbols + englishSymbols

enum class KeyboardSettingsLanguage(
    val code: String,
    val fullNameRes: StringResource,
    val sectionTitleRes: StringResource,
) {
    Russian(
        code = "ru",
        fullNameRes = Res.string.keyboard_settings_language_russian,
        sectionTitleRes = Res.string.keyboard_settings_russian,
    ),
    English(
        code = "en",
        fullNameRes = Res.string.keyboard_settings_language_english,
        sectionTitleRes = Res.string.keyboard_settings_english,
    );

    companion object {
        fun fromCode(code: String?): KeyboardSettingsLanguage? {
            return entries.firstOrNull { it.code == code?.trim()?.lowercase() }
        }

        fun resolveDefault(languageCode: String): KeyboardSettingsLanguage {
            return if (languageCode == Russian.code) Russian else English
        }
    }
}

data class KeyboardSettingsUiState(
    val isLoading: Boolean = true,
    val selectedLetters: Set<Char> = emptySet(),
    val currentLanguage: KeyboardSettingsLanguage = KeyboardSettingsLanguage.resolveDefault(
        currentSystemLanguageCode(),
    ),
    val preventWrongKeyPress: Boolean = true,
)

class KeyboardSettingsViewModel(
    private val studentId: String,
    private val studentRepository: StudentRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(KeyboardSettingsUiState())
    val uiState: StateFlow<KeyboardSettingsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val fallbackLanguage = KeyboardSettingsLanguage.resolveDefault(currentSystemLanguageCode())
            val fallbackState = KeyboardSettingsUiState(
                isLoading = false,
                selectedLetters = emptySet(),
                currentLanguage = fallbackLanguage,
                preventWrongKeyPress = true,
            )
            val loadedState = runCatching {
                val letters = studentRepository.getActiveLettersById(studentId)
                    .orEmpty()
                    .lowercase()
                    .filter { it.isLetterOrDigit() }
                    .toSet()
                val currentLanguage = resolveCurrentLanguage()
                val preventWrongKeyPress = preferencesRepository
                    .getPreventWrongKeyPressEnabled(studentId)
                    ?: true
                KeyboardSettingsUiState(
                    isLoading = false,
                    selectedLetters = letters,
                    currentLanguage = currentLanguage,
                    preventWrongKeyPress = preventWrongKeyPress,
                )
            }.getOrElse { fallbackState }
            _uiState.value = loadedState
        }
    }

    fun onLetterClicked(letter: Char) {
        val normalized = letter.lowercaseChar()
        viewModelScope.launch {
            val currentSelectedLetters = _uiState.value.selectedLetters
            val updated = runCatching {
                if (normalized in currentSelectedLetters) {
                    if (studentRepository.removeLetter(studentId, normalized)) {
                        currentSelectedLetters - normalized
                    } else {
                        currentSelectedLetters
                    }
                } else {
                    if (studentRepository.addLetter(studentId, normalized)) {
                        currentSelectedLetters + normalized
                    } else {
                        currentSelectedLetters
                    }
                }
            }.getOrElse { currentSelectedLetters }
            _uiState.update { it.copy(selectedLetters = updated) }
        }
    }

    fun onLanguageSelected(language: KeyboardSettingsLanguage) {
        if (_uiState.value.currentLanguage == language) return
        preferencesRepository.setKeyboardLanguage(studentId, language.code)
        _uiState.update { it.copy(currentLanguage = language) }
    }

    fun onPreventWrongKeyPressChanged(isEnabled: Boolean) {
        preferencesRepository.setPreventWrongKeyPressEnabled(
            studentId = studentId,
            isEnabled = isEnabled,
        )
        _uiState.update { it.copy(preventWrongKeyPress = isEnabled) }
    }

    fun enableAllCurrentLanguage() {
        val currentSymbols = currentLanguageSymbols(_uiState.value.currentLanguage)
        updateSelectedLetters((_uiState.value.selectedLetters + currentSymbols).toSet())
    }

    fun disableAllCurrentLanguage() {
        val currentSymbols = currentLanguageSymbols(_uiState.value.currentLanguage).toSet()
        updateSelectedLetters(_uiState.value.selectedLetters - currentSymbols)
    }

    private fun updateSelectedLetters(selectedLetters: Set<Char>) {
        viewModelScope.launch {
            runCatching {
                val orderedLetters = allKeyboardSymbols.filter { it in selectedLetters }.joinToString(separator = "")
                if (!studentRepository.updateActiveLetters(studentId, orderedLetters)) return@launch
                _uiState.update { it.copy(selectedLetters = selectedLetters) }
            }
        }
    }

    private fun resolveCurrentLanguage(): KeyboardSettingsLanguage {
        val savedLanguage = KeyboardSettingsLanguage.fromCode(
            preferencesRepository.getKeyboardLanguage(studentId),
        )
        if (savedLanguage != null) return savedLanguage

        val defaultLanguage = KeyboardSettingsLanguage.resolveDefault(currentSystemLanguageCode())
        preferencesRepository.setKeyboardLanguage(studentId, defaultLanguage.code)
        return defaultLanguage
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
        onLanguageSelected = viewModel::onLanguageSelected,
        onPreventWrongKeyPressChanged = viewModel::onPreventWrongKeyPressChanged,
        onEnableAllClick = viewModel::enableAllCurrentLanguage,
        onDisableAllClick = viewModel::disableAllCurrentLanguage,
        onClose = onClose,
    )
}

@Composable
fun KeyboardSettingsScreen(
    state: KeyboardSettingsUiState,
    onLetterClicked: (Char) -> Unit,
    onLanguageSelected: (KeyboardSettingsLanguage) -> Unit,
    onPreventWrongKeyPressChanged: (Boolean) -> Unit,
    onEnableAllClick: () -> Unit,
    onDisableAllClick: () -> Unit,
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

    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentLanguageSymbols = remember(state.currentLanguage) {
        currentLanguageSymbols(state.currentLanguage)
    }
    val hasAnyCurrentSelected = currentLanguageSymbols.any { it in state.selectedLetters }
    val hasAllCurrentSelected = currentLanguageSymbols.all { it in state.selectedLetters }

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

        LanguageSelectorRow(
            currentLanguage = state.currentLanguage,
            onClick = { showLanguageDialog = true },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.keyboard_settings_prevent_wrong_key_press),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = state.preventWrongKeyPress,
                onCheckedChange = onPreventWrongKeyPressChanged,
            )
        }

        BulkActionsRow(
            hasAnySelected = hasAnyCurrentSelected,
            hasAllSelected = hasAllCurrentSelected,
            onEnableAllClick = onEnableAllClick,
            onDisableAllClick = onDisableAllClick,
        )

        KeyboardLettersSection(
            title = stringResource(Res.string.keyboard_settings_numbers),
            rows = keyboardSettingsNumberRows,
            selectedLetters = state.selectedLetters,
            onLetterClicked = onLetterClicked,
        )

        KeyboardLettersSection(
            title = stringResource(state.currentLanguage.sectionTitleRes),
            rows = currentLanguageRows(state.currentLanguage),
            selectedLetters = state.selectedLetters,
            onLetterClicked = onLetterClicked,
        )
    }

    if (showLanguageDialog) {
        SelectLanguageDialog(
            selectedLanguage = state.currentLanguage,
            onLanguageSelected = {
                onLanguageSelected(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
}

@Composable
private fun LanguageSelectorRow(
    currentLanguage: KeyboardSettingsLanguage,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.keyboard_settings_language),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
        ) {
            Text(
                text = currentLanguage.code,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectLanguageDialog(
    selectedLanguage: KeyboardSettingsLanguage,
    onLanguageSelected: (KeyboardSettingsLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.keyboard_settings_select_language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyboardSettingsLanguage.entries.forEach { language ->
                    val isSelected = language == selectedLanguage
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onLanguageSelected(language) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ) {
                        Text(
                            text = stringResource(language.fullNameRes),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.keyboard_settings_close))
            }
        },
    )
}

@Composable
private fun BulkActionsRow(
    hasAnySelected: Boolean,
    hasAllSelected: Boolean,
    onEnableAllClick: () -> Unit,
    onDisableAllClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = !hasAllSelected, onClick = onEnableAllClick),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
        ) {
            Box(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.keyboard_settings_enable_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (hasAllSelected) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = hasAnySelected, onClick = onDisableAllClick),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
        ) {
            Box(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.keyboard_settings_disable_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (hasAnySelected) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    },
                )
            }
        }
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

private fun currentLanguageRows(language: KeyboardSettingsLanguage): List<List<String>> {
    return when (language) {
        KeyboardSettingsLanguage.Russian -> keyboardSettingsRussianRows
        KeyboardSettingsLanguage.English -> keyboardSettingsEnglishRows
    }
}

private fun currentLanguageSymbols(language: KeyboardSettingsLanguage): List<Char> {
    return when (language) {
        KeyboardSettingsLanguage.Russian -> russianSymbols
        KeyboardSettingsLanguage.English -> englishSymbols
    }
}
