package com.higumasoft.mneme.sr

import com.higumasoft.mneme.domain.model.CardSchedule
import com.higumasoft.mneme.domain.model.Rating
import kotlin.math.max

/**
 * SuperMemo-2 (SM-2) spaced repetition algorithm.
 *
 * Pure function. No I/O, no clock — caller passes [todayEpochDay] so it's
 * trivially testable and determinable across timezones.
 *
 * Original paper: https://www.supermemo.com/en/archives1990-2015/english/ol/sm2
 *
 * Differences from textbook SM-2:
 *  - "Again" (quality < 3) resets repetitions to 0 but keeps the EF reduced
 *    by the standard formula, not floored to 1.3 immediately.
 *  - Minimum EF is clamped to 1.3 as in Anki.
 *  - Intervals are integer days; sub-day "learning steps" are out of scope
 *    in v1.0 (deferred to a future micro-scheduler).
 */
object Sm2Engine {

    private const val MIN_EASE = 1.3

    fun schedule(
        previous: CardSchedule,
        rating: Rating,
        todayEpochDay: Long
    ): CardSchedule {
        val q = rating.quality

        // 1. Update ease factor (EF). Same formula whether pass or fail.
        val newEase = max(
            MIN_EASE,
            previous.easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        )

        // 2. Compute repetitions and interval based on quality.
        val (newReps, newInterval) = if (q < 3) {
            // Lapse: schedule for tomorrow, reset rep count.
            0 to 1
        } else {
            when (previous.repetitions) {
                0 -> 1 to 1
                1 -> 2 to 6
                else -> {
                    val nextReps = previous.repetitions + 1
                    val nextInterval = (previous.intervalDays * newEase).toInt().coerceAtLeast(1)
                    nextReps to nextInterval
                }
            }
        }

        return CardSchedule(
            cardId = previous.cardId,
            easeFactor = newEase,
            intervalDays = newInterval,
            repetitions = newReps,
            dueEpochDay = todayEpochDay + newInterval
        )
    }
}
