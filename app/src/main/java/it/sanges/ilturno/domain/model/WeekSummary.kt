package it.sanges.ilturno.domain.model

import it.sanges.ilturno.data.entity.ShiftAssignment
import it.sanges.ilturno.util.DateUtils
import java.time.LocalDate

data class WeekSummary(val daysAssigned: Int, val daysWithoutAssignments: Int, val services: Int) {
    companion object {
        fun forEmployee(employeeId: Long, week: LocalDate, assignments: List<ShiftAssignment>): WeekSummary {
            val start = DateUtils.weekStart(week)
            val end = DateUtils.weekEnd(week)
            val relevant = assignments.filter { it.employeeId == employeeId && it.date >= start && it.date <= end }
                .distinctBy { it.date to it.meal }
            val days = relevant.map { it.date }.distinct().size
            return WeekSummary(days, 7 - days, relevant.size)
        }
    }
}
