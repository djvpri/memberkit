package com.memberkit.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MemberLogicTest {
    private fun m(start: String, end: String) = Member(1, "Budi Santoso", "Netflix", start, end, "")

    @Test
    fun statusTracksToday() {
        val today = LocalDate.of(2026, 10, 25)
        assertEquals(Status.ACTIVE, m("2026-10-25", "2026-11-24").status(today))
        assertEquals(Status.SOON, m("2026-10-01", "2026-10-31").status(today))
        assertEquals(Status.SOON, m("2026-10-01", "2026-10-25").status(today))
        assertEquals(Status.EXPIRED, m("2026-09-01", "2026-10-19").status(today))
    }

    @Test
    fun daysLeftAndInitials() {
        val today = LocalDate.of(2026, 10, 25)
        assertEquals(30L, m("2026-10-25", "2026-11-24").daysLeft(today))
        assertEquals("BS", m("2026-10-25", "2026-11-24").initials())
    }

    @Test
    fun isoRoundTrip() {
        val d = LocalDate.of(2026, 10, 25)
        assertEquals(d, Dates.parse(Dates.iso(d)))
    }
}
