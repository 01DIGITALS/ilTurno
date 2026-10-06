package it.sanges.ilturno.notifications

import it.sanges.ilturno.data.entity.ShiftAssignment
import it.sanges.ilturno.domain.model.Meal
import it.sanges.ilturno.util.DateUtils
import java.time.LocalDate
import java.time.ZonedDateTime

object ReminderPolicy {
    fun nextWeek(today: LocalDate): LocalDate = DateUtils.weekStart(today).plusWeeks(1)
    fun isReminderDay(today: LocalDate): Boolean = today.dayOfWeek.value >= 4
    fun missingServices(today: LocalDate, assignments: List<ShiftAssignment>, departmentIds: List<Long> = listOf(0L), combineDepartments: Boolean = false): Int {
        val start = nextWeek(today)
        val assigned = assignments.filter { it.date >= start && it.date < start.plusWeeks(1) }
            .map { Triple(it.date, it.meal, if (combineDepartments) 0L else it.departmentId) }.toSet()
        return departmentIds.distinct().sumOf { department ->
            (0L..6L).sumOf { offset -> Meal.entries.count { Triple(start.plusDays(offset), it, department) !in assigned } }
        }
    }
    fun nextAlarm(now: ZonedDateTime): ZonedDateTime {
        for (offset in 0L..7L) {
            val date = now.toLocalDate().plusDays(offset)
            if (!isReminderDay(date)) continue
            for (hour in if (date.dayOfWeek.value == 7) listOf(12, 19, 23) else listOf(12, 19)) {
                val candidate = date.atTime(hour, 0).atZone(now.zone)
                if (candidate.isAfter(now)) return candidate
            }
        }
        error("No reminder slot found")
    }
    // An alarm delayed past its slot is discarded rather than sending an extra reminder.
    fun slot(now: ZonedDateTime): String? {
        if (!isReminderDay(now.toLocalDate())) return null
        val hour = when {
            now.dayOfWeek.value == 7 && now.hour == 23 -> 23
            now.hour in 12..18 -> 12
            now.hour in 19..23 -> 19
            else -> return null
        }
        return "${now.toLocalDate()}|$hour"
    }
    fun isFinalReview(now: ZonedDateTime): Boolean = now.dayOfWeek.value == 7 && now.hour == 23
}
