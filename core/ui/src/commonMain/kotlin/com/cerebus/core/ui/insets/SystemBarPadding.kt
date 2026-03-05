package com.cerebus.core.ui.insets

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun topSystemBarPadding(
    minPadding: Dp = 0.dp,
    extraPadding: Dp = 0.dp,
): Dp {
    val density = LocalDensity.current
    val topInset = with(density) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    }
    return maxOf(minPadding, topInset + extraPadding)
}

@Composable
fun bottomSystemBarPadding(
    minPadding: Dp = 0.dp,
    extraPadding: Dp = 0.dp,
): Dp {
    val density = LocalDensity.current
    val bottomInset = with(density) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    }
    return maxOf(minPadding, bottomInset + extraPadding)
}
