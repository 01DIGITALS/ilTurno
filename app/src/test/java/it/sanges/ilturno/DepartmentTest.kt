package it.sanges.ilturno

import it.sanges.ilturno.data.entity.Department
import it.sanges.ilturno.data.entity.Employee
import it.sanges.ilturno.data.entity.ShiftAssignment
import it.sanges.ilturno.domain.model.*
import it.sanges.ilturno.notifications.ReminderPolicy
import it.sanges.ilturno.util.ExcelExporter
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class DepartmentTest {
    private val today = LocalDate.of(2026, 10, 8)
    private val monday = ReminderPolicy.nextWeek(today)
    private val departments = listOf(Department(1, "Sala"), Department(2, "Cucina"))
    @Test fun reminderChecksEachDepartmentSeparately() {
        val shifts = (0L..6L).flatMap { offset -> Meal.entries.map {
            ShiftAssignment(date = monday.plusDays(offset), employeeId = 1, meal = it, departmentId = 1)
        } }
        assertEquals(14, ReminderPolicy.missingServices(today, shifts, listOf(1, 2)))
        assertEquals(0, ReminderPolicy.missingServices(today, shifts, listOf(1)))
        assertEquals(0, ReminderPolicy.missingServices(today, shifts, emptyList()))
        assertEquals(0, ReminderPolicy.missingServices(today, shifts + shifts.map { it.copy(departmentId = 2) }, listOf(1, 2)))
    }
    @Test fun exportsKeepDepartmentsAndArchivedAssignmentsSeparate() {
        val archived = Department(3, "Bar", false)
        val shifts = listOf(
            ShiftAssignment(date = monday, employeeId = 1, meal = Meal.LUNCH, departmentId = 1),
            ShiftAssignment(date = monday, employeeId = 2, meal = Meal.DINNER, departmentId = 3))
        val snapshot = WeekSnapshot.create(monday, listOf(Employee(1, "Anna"), Employee(2, "Marco")), shifts, departments + archived)
        assertEquals(21, snapshot.rows.size)
        assertEquals(listOf("Bar", "Cucina", "Sala"), snapshot.rows.map { it.departmentName }.distinct())
        assertEquals(listOf("Anna"), snapshot.rows.single { it.date == monday && it.departmentName == "Sala" }.lunch)
        assertEquals(listOf("Marco"), snapshot.rows.single { it.date == monday && it.departmentName == "Bar" }.dinner)
        val output = ByteArrayOutputStream()
        ExcelExporter.write(snapshot, output)
        var worksheet = ""
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "xl/worksheets/sheet1.xml") worksheet = zip.readBytes().toString(Charsets.UTF_8)
                entry = zip.nextEntry
            }
        }
        assertTrue(worksheet.contains("Reparto"))
        assertTrue(worksheet.contains("ref=\"A1:E23\""))
        assertTrue(worksheet.contains("ref=\"A2:E23\""))
        assertTrue(worksheet.contains("Bar"))
    }
    @Test fun oldAssignmentsRemainUnclassifiedAndGlobalSummaryDoesNotDoubleCount() {
        val shifts = listOf(
            ShiftAssignment(date = monday, employeeId = 1, meal = Meal.LUNCH),
            ShiftAssignment(date = monday, employeeId = 1, meal = Meal.LUNCH, departmentId = 1))
        assertEquals(listOf("Cucina", "Sala", "Senza reparto"), departmentSections(departments, shifts).map { it.name })
        val summary = WeekSummary.forEmployee(1, monday, shifts)
        assertEquals(1, summary.daysAssigned)
        assertEquals(1, summary.services)
        val simple = WeekSnapshot.create(monday, listOf(Employee(1, "Anna")), shifts, departments, groupDepartments = false)
        assertEquals(7, simple.rows.size)
        assertTrue(simple.rows.all { it.departmentName == null })
        assertEquals(listOf("Anna"), simple.rows.first().lunch)
        assertEquals(13, ReminderPolicy.missingServices(today, shifts, combineDepartments = true))
    }
}
