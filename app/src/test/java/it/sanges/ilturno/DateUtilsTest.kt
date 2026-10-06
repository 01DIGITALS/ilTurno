package it.sanges.ilturno

import it.sanges.ilturno.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {
    private fun date(value: String) = LocalDate.parse(value)

    @Test fun tuesdayStartsOnMondayAndEndsOnSunday() {
        assertEquals(date("2026-10-05"), DateUtils.weekStart(date("2026-10-06")))
        assertEquals(date("2026-10-11"), DateUtils.weekEnd(date("2026-10-06")))
    }
    @Test fun mondayDoesNotMoveAndSundayBelongsToSameWeek() {
        assertEquals(date("2026-10-05"), DateUtils.weekStart(date("2026-10-05")))
        assertEquals(date("2026-10-05"), DateUtils.weekStart(date("2026-10-11")))
    }
    @Test fun monthBoundary() {
        assertEquals(date("2026-03-30"), DateUtils.weekStart(date("2026-04-01")))
        assertEquals(date("2026-04-05"), DateUtils.weekEnd(date("2026-04-01")))
        assertEquals("30 marzo – 5 aprile 2026", DateUtils.weekLabel(date("2026-04-01")))
    }
    @Test fun yearBoundary() {
        assertEquals(date("2026-12-28"), DateUtils.weekStart(date("2027-01-01")))
        assertEquals(date("2027-01-03"), DateUtils.weekEnd(date("2026-12-31")))
        assertEquals("28 dicembre 2026 – 3 gennaio 2027", DateUtils.weekLabel(date("2027-01-01")))
    }
    @Test fun leapDayAndFebruary() {
        assertEquals(date("2024-02-26"), DateUtils.weekStart(date("2024-02-29")))
        assertEquals(date("2024-03-03"), DateUtils.weekEnd(date("2024-02-29")))
        assertEquals(date("2025-02-24"), DateUtils.weekStart(date("2025-02-28")))
        assertEquals(date("2025-03-02"), DateUtils.weekEnd(date("2025-02-28")))
    }
    @Test fun weekContainsExactlySevenRealDates() {
        assertEquals((5L..11L).map { date("2026-10-01").plusDays(it - 1) }, DateUtils.weekDays(date("2026-10-06")))
        assertEquals("5 – 11 ottobre 2026", DateUtils.weekLabel(date("2026-10-06")))
    }
    @Test fun datePickerConversionIsUtcAndKeepsTheCalendarDay() {
        listOf("2026-12-25", "2024-02-29", "2026-03-29", "2026-10-25").forEach { value ->
            assertEquals(date(value), DateUtils.pickerDate(DateUtils.pickerMillis(date(value))))
        }
    }
}
