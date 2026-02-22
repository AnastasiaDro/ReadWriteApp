package com.cerebus.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AppEntityEditorMode {
    CREATE,
    EDIT,
}

@Composable
fun AppEntityEditorDialog(
    visible: Boolean,
    mode: AppEntityEditorMode,
    createTitle: String,
    editTitle: String,
    createConfirmText: String,
    editConfirmText: String,
    value: String,
    onValueChange: (String) -> Unit,
    fieldLabel: String,
    coverButtonText: String,
    onCoverButtonClick: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    cancelText: String,
    maxLength: Int,
    minDialogHeight: Dp,
    validationErrorText: String?,
    isSaving: Boolean,
    coverContent: @Composable () -> Unit,
) {
    AppAnimatedDialog(visible = visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(if (mode == AppEntityEditorMode.CREATE) createTitle else editTitle)
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(min = minDialogHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        coverContent()
                        TextButton(onClick = onCoverButtonClick) {
                            Text(coverButtonText)
                        }
                    }

                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        label = { Text(fieldLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    )
                    Text(
                        text = "${value.length}/$maxLength",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                    if (!validationErrorText.isNullOrBlank()) {
                        Text(
                            text = validationErrorText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    enabled = !isSaving,
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text(if (mode == AppEntityEditorMode.CREATE) createConfirmText else editConfirmText)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                ) {
                    Text(cancelText)
                }
            },
        )
    }
}
