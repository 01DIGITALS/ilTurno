package it.sanges.ilturno

import it.sanges.ilturno.data.entity.ShiftAssignment
import it.sanges.ilturno.domain.model.Meal
import it.sanges.ilturno.domain.model.WeekSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeekSummaryTest {
    private val monday = LocalDate.of(2026, 10, 5)
    private fun assignment(offset: Long, meal: Meal, id: Long = 1) = ShiftAssignment(date = monday.plusDays(offset), employeeId = id, meal = meal)

    @Test fun lunchAndDinnerCountAsOneDayAndTwoServices() {
        assertEquals(WeekSummary(1, 6, 2), WeekSummary.forEmployee(1, monday,
            listOf(assignment(0, Meal.LUNCH), assignment(0, Meal.DINNER))))
    }
    @Test fun emptyWeekHasSevenDaysWithoutAssignments() {
        assertEquals(WeekSummary(0, 7, 0), WeekSummary.forEmployee(1, monday, emptyList()))
    }
    @Test fun countsOnlyThisEmployeeAndThisWeek() {
        val values = listOf(assignment(0, Meal.LUNCH), assignment(2, Meal.DINNER), assignment(2, Meal.DINNER),
            assignment(-1, Meal.LUNCH), assignment(7, Meal.DINNER), assignment(1, Meal.LUNCH, 2))
        assertEquals(WeekSummary(2, 5, 2), WeekSummary.forEmployee(1, monday.plusDays(3), values))
    }
    @Test fun sevenDoubleServicesHaveNoDaysWithoutAssignments() {
        val assignments = (0L..6L).flatMap { listOf(assignment(it, Meal.LUNCH), assignment(it, Meal.DINNER)) }
        assertEquals(WeekSummary(7, 0, 14), WeekSummary.forEmployee(1, monday, assignments))
    }
}
