package com.cerebus.game_screen.navigation

import com.cerebus.core.utils.GameLaunchMode

object GameSessionNavigationState {
    var selectedDeckIds: List<String> = emptyList()
    var launchMode: GameLaunchMode = GameLaunchMode.Plan
    var returnRoute: String? = null
}
