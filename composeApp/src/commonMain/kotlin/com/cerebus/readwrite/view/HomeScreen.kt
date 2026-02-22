package com.cerebus.readwrite.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import readwriteapp.composeapp.generated.resources.Res
import readwriteapp.composeapp.generated.resources.go_to_decks
import readwriteapp.composeapp.generated.resources.start_learning

@Composable
fun HomeScreen(
    onNavigateToCreate: () -> Unit,
) {
    MaterialTheme {
        val density = LocalDensity.current
        val widthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
        val isTablet = widthDp >= 840.dp
        val buttonWidthFraction = if (isTablet) 0.54f else 0.72f
        val buttonMinHeight = if (isTablet) (widthDp * 0.085f) else 52.dp

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            Button(
                onClick = onNavigateToCreate,
                modifier = Modifier
                    .fillMaxWidth(buttonWidthFraction)
                    .heightIn(min = buttonMinHeight)
                    .padding(horizontal = 4.dp),
            ) {
                Text(
                    text = stringResource(Res.string.start_learning),
                    textAlign = TextAlign.Center,
                )
            }

            Button(
                onClick = onNavigateToCreate,
                modifier = Modifier
                    .fillMaxWidth(buttonWidthFraction)
                    .heightIn(min = buttonMinHeight)
                    .padding(horizontal = 4.dp),
            ) {
                Text(
                    text = stringResource(Res.string.go_to_decks),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
