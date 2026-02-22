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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import readwriteapp.composeapp.generated.resources.Res
import readwriteapp.composeapp.generated.resources.add_student_cta
import readwriteapp.composeapp.generated.resources.no_students_subtitle
import readwriteapp.composeapp.generated.resources.no_students_title
import readwriteapp.composeapp.generated.resources.try_demo_cta

@Composable
fun NoStudentsScreen(
    onAddStudentClick: () -> Unit,
    onTryDemoClick: () -> Unit,
) {
    val density = LocalDensity.current
    val widthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val isTablet = widthDp >= 840.dp
    val buttonWidthFraction = if (isTablet) 0.64f else 0.86f
    val buttonMinHeight = if (isTablet) (widthDp * 0.085f) else 54.dp

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.no_students_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.no_students_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
        )

        Button(
            onClick = onAddStudentClick,
            modifier = Modifier
                .fillMaxWidth(buttonWidthFraction)
                .heightIn(min = buttonMinHeight),
        ) {
            Text(
                text = stringResource(Res.string.add_student_cta),
                textAlign = TextAlign.Center,
            )
        }

        OutlinedButton(
            onClick = onTryDemoClick,
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(buttonWidthFraction)
                .heightIn(min = buttonMinHeight),
        ) {
            Text(
                text = stringResource(Res.string.try_demo_cta),
                textAlign = TextAlign.Center,
            )
        }
    }
}
