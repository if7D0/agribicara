package com.agribicara.app.presentation.home

import com.agribicara.app.R
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelTest {

    private fun clockAt(hour: Int, minute: Int = 0): Clock {
        val zone = ZoneId.of("Asia/Jakarta")
        val instant = java.time.LocalDate.of(2026, 8, 29)
            .atTime(hour, minute)
            .atZone(zone)
            .toInstant()
        return Clock.fixed(instant, zone)
    }

    @Test
    fun `sapaan mengikuti batas jam yang benar`() {
        assertEquals(R.string.home_greeting_morning, HomeViewModel.greetingFor(LocalTime.of(4, 0)))
        assertEquals(R.string.home_greeting_morning, HomeViewModel.greetingFor(LocalTime.of(10, 59)))
        assertEquals(R.string.home_greeting_afternoon, HomeViewModel.greetingFor(LocalTime.of(11, 0)))
        assertEquals(R.string.home_greeting_afternoon, HomeViewModel.greetingFor(LocalTime.of(14, 59)))
        assertEquals(R.string.home_greeting_evening, HomeViewModel.greetingFor(LocalTime.of(15, 0)))
        assertEquals(R.string.home_greeting_evening, HomeViewModel.greetingFor(LocalTime.of(18, 59)))
        assertEquals(R.string.home_greeting_night, HomeViewModel.greetingFor(LocalTime.of(19, 0)))
        assertEquals(R.string.home_greeting_night, HomeViewModel.greetingFor(LocalTime.of(3, 59)))
    }

    @Test
    fun `state awal memakai jam yang disuntikkan`() {
        val viewModel = HomeViewModel(clockAt(16))
        assertEquals(R.string.home_greeting_evening, viewModel.uiState.value.greetingRes)
    }

    @Test
    fun `refreshGreeting memperbarui sapaan yang sudah basi`() {
        // Waktu maju dari pagi ke malam selagi ViewModel tetap hidup.
        var now = Instant.parse("2026-08-29T02:00:00Z") // 09:00 WIB
        val zone = ZoneId.of("Asia/Jakarta")
        val movingClock = object : Clock() {
            override fun getZone(): ZoneId = zone
            override fun withZone(z: ZoneId): Clock = this
            override fun instant(): Instant = now
        }

        val viewModel = HomeViewModel(movingClock)
        assertEquals(R.string.home_greeting_morning, viewModel.uiState.value.greetingRes)

        now = Instant.parse("2026-08-29T12:00:00Z") // 19:00 WIB
        viewModel.refreshGreeting()

        assertEquals(R.string.home_greeting_night, viewModel.uiState.value.greetingRes)
    }
}
