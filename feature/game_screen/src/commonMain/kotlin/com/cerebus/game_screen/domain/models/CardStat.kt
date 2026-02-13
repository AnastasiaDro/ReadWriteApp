package com.cerebus.game_screen.domain.models

import kotlin.time.Instant

/**
 * [CardStat] - статистика по конкретной карточке
 * для конкретного ученика
 * @param correctStreak - серия правильных ответов
 * @param totalAttempts -  всего попыток
 * @param correctCount - сколько раз овтетил верно
 *
 * @author cerebus
 * @since 13.02.2026
 */
data class CardStat(
    val lastAttempt: Instant,
    val isCorrect: Boolean,
    val correctStreak: Int,
    val totalAttempts: Int,
    val correctCount: Int,
)
