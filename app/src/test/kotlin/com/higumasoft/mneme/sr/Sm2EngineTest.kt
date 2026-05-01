package com.higumasoft.mneme.sr

import com.google.common.truth.Truth.assertThat
import com.higumasoft.mneme.domain.model.CardSchedule
import com.higumasoft.mneme.domain.model.Rating
import org.junit.Test

class Sm2EngineTest {

    private val today = 20_000L  // arbitrary epoch day
    private val freshCard = CardSchedule(cardId = "c1")

    @Test
    fun `first time good schedules card 1 day later`() {
        val out = Sm2Engine.schedule(freshCard, Rating.Good, today)
        assertThat(out.repetitions).isEqualTo(1)
        assertThat(out.intervalDays).isEqualTo(1)
        assertThat(out.dueEpochDay).isEqualTo(today + 1)
    }

    @Test
    fun `second good schedules 6 days later`() {
        val first = Sm2Engine.schedule(freshCard, Rating.Good, today)
        val second = Sm2Engine.schedule(first, Rating.Good, today + 1)
        assertThat(second.repetitions).isEqualTo(2)
        assertThat(second.intervalDays).isEqualTo(6)
        assertThat(second.dueEpochDay).isEqualTo(today + 1 + 6)
    }

    @Test
    fun `third good uses ease factor multiplier`() {
        var s = freshCard
        s = Sm2Engine.schedule(s, Rating.Good, today)        // interval 1
        s = Sm2Engine.schedule(s, Rating.Good, today + 1)    // interval 6
        s = Sm2Engine.schedule(s, Rating.Good, today + 7)    // interval ≈ 6 * 2.5 = 15
        assertThat(s.repetitions).isEqualTo(3)
        assertThat(s.intervalDays).isEqualTo(15)
    }

    @Test
    fun `again resets repetitions and schedules tomorrow`() {
        var s = freshCard
        s = Sm2Engine.schedule(s, Rating.Good, today)
        s = Sm2Engine.schedule(s, Rating.Good, today + 1)
        assertThat(s.repetitions).isEqualTo(2)

        val lapsed = Sm2Engine.schedule(s, Rating.Again, today + 7)
        assertThat(lapsed.repetitions).isEqualTo(0)
        assertThat(lapsed.intervalDays).isEqualTo(1)
        assertThat(lapsed.dueEpochDay).isEqualTo(today + 7 + 1)
    }

    @Test
    fun `easy boosts ease factor more than good`() {
        val good = Sm2Engine.schedule(freshCard, Rating.Good, today)
        val easy = Sm2Engine.schedule(freshCard, Rating.Easy, today)
        assertThat(easy.easeFactor).isGreaterThan(good.easeFactor)
    }

    @Test
    fun `hard reduces ease factor compared to good`() {
        val good = Sm2Engine.schedule(freshCard, Rating.Good, today)
        val hard = Sm2Engine.schedule(freshCard, Rating.Hard, today)
        assertThat(hard.easeFactor).isLessThan(good.easeFactor)
    }

    @Test
    fun `ease factor clamped at 1_3`() {
        // Hammer with Again repeatedly; EF must not go below 1.3
        var s = freshCard
        repeat(20) {
            s = Sm2Engine.schedule(s, Rating.Again, today + it)
        }
        assertThat(s.easeFactor).isAtLeast(1.3)
    }

    @Test
    fun `interval is at least 1 day even with low ease`() {
        var s = freshCard.copy(easeFactor = 1.3, intervalDays = 0, repetitions = 5)
        s = Sm2Engine.schedule(s, Rating.Good, today)
        assertThat(s.intervalDays).isAtLeast(1)
    }
}
