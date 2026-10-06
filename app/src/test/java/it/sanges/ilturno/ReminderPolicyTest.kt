package it.sanges.ilturno

import it.sanges.ilturno.notifications.ReminderPolicy
import it.sanges.ilturno.data.entity.ShiftAssignment
import it.sanges.ilturno.domain.model.Meal
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class ReminderPolicyTest {
    private fun time(value: String) = ZonedDateTime.parse("${value}:00+02:00[Europe/Rome]")
    @Test fun schedulesTwoDailySlotsAndSundayFinalReview() {
        assertEquals(time("2026-10-08T12:00"), ReminderPolicy.nextAlarm(time("2026-10-06T09:00")))
        assertEquals(time("2026-10-08T19:00"), ReminderPolicy.nextAlarm(time("2026-10-08T12:00")))
        assertEquals(time("2026-10-09T12:00"), ReminderPolicy.nextAlarm(time("2026-10-08T19:00")))
        assertEquals(time("2026-10-11T23:00"), ReminderPolicy.nextAlarm(time("2026-10-11T19:00")))
        assertEquals(time("2026-10-15T12:00"), ReminderPolicy.nextAlarm(time("2026-10-11T23:00")))
    }
    @Test fun requiresEveryServiceAndIgnoresDuplicatesAndOtherWeeks() {
        val today = LocalDate.of(2026, 10, 8)
        val start = ReminderPolicy.nextWeek(today)
        assertEquals(LocalDate.of(2026, 10, 12), start)
        val assignments = (0L..6L).flatMap { offset -> Meal.entries.map {
            ShiftAssignment(date = start.plusDays(offset), employeeId = 1, meal = it)
        } }
        assertEquals(14, ReminderPolicy.missingServices(today, emptyList()))
        assertEquals(0, ReminderPolicy.missingServices(today, assignments))
        assertEquals(1, ReminderPolicy.missingServices(today, assignments.dropLast(1) + assignments.first()))
        assertEquals(14, ReminderPolicy.missingServices(today, assignments.map { it.copy(date = it.date.minusWeeks(1)) }))
    }
    @Test fun sundayReviewIsIndependentOfCoverageAndSlotsAreDistinct() {
        assertTrue(ReminderPolicy.isFinalReview(time("2026-10-11T23:00")))
        assertFalse(ReminderPolicy.isFinalReview(time("2026-10-11T19:00")))
        assertNull(ReminderPolicy.slot(time("2026-10-12T12:00")))
        assertNotEquals(ReminderPolicy.slot(time("2026-10-11T19:00")), ReminderPolicy.slot(time("2026-10-11T23:00")))
    }
    @Test fun schedulingFollowsLocalTimeAfterDaylightSavingAndYearBoundary() {
        val autumn = ZonedDateTime.parse("2026-10-25T23:00:00+01:00[Europe/Rome]")
        assertEquals(ZonedDateTime.parse("2026-10-29T12:00:00+01:00[Europe/Rome]"), ReminderPolicy.nextAlarm(autumn))
        assertEquals(LocalDate.of(2027, 1, 4), ReminderPolicy.nextWeek(LocalDate.of(2026, 12, 31)))
    }
}
