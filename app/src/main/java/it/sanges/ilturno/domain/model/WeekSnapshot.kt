package it.sanges.ilturno.domain.model

import it.sanges.ilturno.data.entity.Employee
import it.sanges.ilturno.data.entity.ShiftAssignment
import it.sanges.ilturno.util.DateUtils
import java.time.LocalDate
import it.sanges.ilturno.util.alphabetically
import it.sanges.ilturno.data.entity.Department

data class WeekRow(val date: LocalDate, val lunch: List<String>, val dinner: List<String>, val departmentName: String? = null)
data class WeekSnapshot(val start: LocalDate, val end: LocalDate, val rows: List<WeekRow>) {
    val label: String get() = DateUtils.weekLabel(start)
    fun fileName(extension: String): String = "Turni_${start}_${end}.$extension"

    companion object {
        fun create(date: LocalDate, employees: List<Employee>, assignments: List<ShiftAssignment>, departments: List<Department> = emptyList(), groupDepartments: Boolean = true): WeekSnapshot {
            val ordered = employees.alphabetically()
            val days = DateUtils.weekDays(date)
            val inWeek = assignments.filter { it.date in days }
            val sections = if (groupDepartments) departmentSections(departments, inWeek) else listOf(Department(0, "Senza reparto"))
            val showDepartment = groupDepartments && departments.any { it.id != 0L }
            val rows = sections.flatMap { department -> days.map { day ->
                fun names(meal: Meal): List<String> {
                    val ids = inWeek.filter { it.date == day && it.meal == meal && (!groupDepartments || it.departmentId == department.id) }.map { it.employeeId }.toSet()
                    return ordered.filter { it.id in ids }.map { it.name }
                }
                WeekRow(day, names(Meal.LUNCH), names(Meal.DINNER), if (showDepartment) department.name else null)
            } }
            return WeekSnapshot(days.first(), days.last(), rows)
        }
    }
}
