package com.cerebus.game_screen.domain.models

/**
 * [StudentDeckProgress] - прогресс уяеника по конкретной колоде
 *
 * @param cardStats - id карточки и её статистика
 *
 * @author cerebus
 * @since 13.02.2026
 */
data class StudentDeckProgress(
    val studentId: String,
    val deckId: String,
    val cardStats: Map<String, CardStat>,
)
