package it.sanges.ilturno.data.repository

import androidx.room.withTransaction
import it.sanges.ilturno.data.entity.Employee
import it.sanges.ilturno.data.entity.ShiftAssignment
import it.sanges.ilturno.data.local.AppDatabase
import it.sanges.ilturno.domain.model.Meal
import it.sanges.ilturno.domain.model.WeekSnapshot
import it.sanges.ilturno.util.DateUtils
import java.time.LocalDate
import kotlinx.coroutines.flow.map
import it.sanges.ilturno.util.alphabetically
import it.sanges.ilturno.data.entity.Department
import it.sanges.ilturno.domain.model.alphabeticalDepartments

class ShiftRepository(private val database: AppDatabase) {
    suspend fun snapshot(date: LocalDate, groupDepartments: Boolean = true): WeekSnapshot = database.withTransaction {
        WeekSnapshot.create(date, database.employees().getAll(), database.assignments().getForWeek(
            DateUtils.weekStart(date), DateUtils.weekEnd(date)), database.departments().getAll(), groupDepartments)
    }
    val employees = database.employees().observeAll().map { it.alphabetically() }
    val departments = database.departments().observeAll().map { it.alphabeticalDepartments() }
    suspend fun addDepartment(name: String): Long = database.withTransaction {
        val clean = name.trim()
        require(clean.isNotEmpty() && clean.length <= 80)
        require(database.departments().getAll().none { it.name.equals(clean, ignoreCase = true) })
        database.departments().insert(Department(name = clean))
    }
    suspend fun renameDepartment(id: Long, name: String) = database.withTransaction {
        val clean = name.trim()
        require(id != 0L && clean.isNotEmpty() && clean.length <= 80)
        require(database.departments().getAll().none { it.id != id && it.name.equals(clean, ignoreCase = true) })
        database.departments().rename(id, clean)
    }
    suspend fun setDepartmentActive(id: Long, active: Boolean) {
        require(id != 0L)
        database.departments().setActive(id, active)
    }

    fun assignmentsForDate(date: LocalDate) = database.assignments().assignmentsForDate(date)
    fun assignmentsForWeek(date: LocalDate) = database.assignments().assignmentsForWeek(
        DateUtils.weekStart(date), DateUtils.weekEnd(date),
    )
    fun assignmentsForEmployeeDuringWeek(employeeId: Long, date: LocalDate) =
        database.assignments().assignmentsForEmployeeDuringWeek(
            employeeId, DateUtils.weekStart(date), DateUtils.weekEnd(date),
        )

    suspend fun addEmployee(name: String): Long = database.withTransaction {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty())
        val nextOrder = (database.employees().getAll().maxOfOrNull { it.sortOrder } ?: -1) + 1
        database.employees().insert(Employee(name = cleanName, sortOrder = nextOrder))
    }

    suspend fun renameEmployee(id: Long, name: String) {
        require(name.isNotBlank())
        database.employees().rename(id, name.trim())
    }

    suspend fun setEmployeeActive(id: Long, active: Boolean) = database.employees().setActive(id, active)

    /** Reads inside the transaction, so fast repeated taps toggle rather than duplicate. */
    suspend fun toggleAssigned(date: LocalDate, employeeId: Long, meal: Meal, departmentId: Long = 0) = database.withTransaction {
        if (departmentId == -1L) {
            val assigned = database.assignments().getForWeek(date, date).any { it.employeeId == employeeId && it.meal == meal }
            if (assigned) database.assignments().removeFromAllDepartments(date, employeeId, meal)
            else setAssigned(date, employeeId, meal, true, 0)
            return@withTransaction
        }
        val existing = database.assignments().find(date, employeeId, meal, departmentId)
        setAssigned(date, employeeId, meal, existing == null, departmentId)
    }

    /** Idempotent writes; deactivation never removes assignments, including future ones. */
    suspend fun setAssigned(date: LocalDate, employeeId: Long, meal: Meal, assigned: Boolean, departmentId: Long = 0) =
        database.withTransaction {
            if (assigned) {
                if (departmentId == 0L) database.departments().ensureUnclassified()
                else require(database.departments().get(departmentId)?.isActive == true) { "Department is inactive" }
                val employee = database.employees().get(employeeId)
                require(employee?.isActive == true) { "Employee is not active" }
                database.assignments().insert(ShiftAssignment(date = date, employeeId = employeeId, meal = meal, departmentId = departmentId))
            } else {
                database.assignments().remove(date, employeeId, meal, departmentId)
            }
            Unit
        }
}
