package it.sanges.ilturno

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import it.sanges.ilturno.data.local.AppDatabase
import it.sanges.ilturno.data.repository.ShiftRepository
import it.sanges.ilturno.domain.model.Meal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: ShiftRepository
    private val date = LocalDate.of(2026, 10, 6)

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = ShiftRepository(database)
    }
    @After fun tearDown() { database.close() }

    @Test fun employeeInsertionTrimsNameAndSortsAlphabetically() = runBlocking {
        val marco = repository.addEmployee("  Marco  ")
        val anna = repository.addEmployee("Anna")
        val people = repository.employees.first()
        assertEquals(listOf(anna, marco), people.map { it.id })
        assertEquals(listOf("Anna", "Marco"), people.map { it.name })
        assertTrue(people.all { it.isActive })
    }
    @Test fun blankNameIsRejected() = runBlocking {
        try {
            repository.addEmployee("   ")
            fail("Blank employee name accepted")
        } catch (_: IllegalArgumentException) {
            assertTrue(repository.employees.first().isEmpty())
        }
    }
    @Test fun lunchAndDinnerAreDistinctAndDuplicatesArePrevented() = runBlocking {
        val id = repository.addEmployee("Marco")
        repository.setAssigned(date, id, Meal.LUNCH, true)
        repository.setAssigned(date, id, Meal.LUNCH, true)
        repository.setAssigned(date, id, Meal.DINNER, true)
        val assignments = repository.assignmentsForDate(date).first()
        assertEquals(2, assignments.size)
        assertEquals(setOf(Meal.LUNCH, Meal.DINNER), assignments.map { it.meal }.toSet())
    }
    @Test fun removingLunchKeepsDinner() = runBlocking {
        val id = repository.addEmployee("Marco")
        repository.setAssigned(date, id, Meal.LUNCH, true)
        repository.setAssigned(date, id, Meal.DINNER, true)
        repository.setAssigned(date, id, Meal.LUNCH, false)
        assertEquals(listOf(Meal.DINNER), repository.assignmentsForDate(date).first().map { it.meal })
    }
    @Test fun deactivationPreservesHistoryAndReactivationWorks() = runBlocking {
        val id = repository.addEmployee("Marco")
        repository.setAssigned(date, id, Meal.LUNCH, true)
        repository.setEmployeeActive(id, false)
        assertFalse(repository.employees.first().single().isActive)
        assertEquals(id, repository.assignmentsForDate(date).first().single().employeeId)
        repository.setEmployeeActive(id, true)
        assertTrue(repository.employees.first().single().isActive)
    }
    @Test fun inactivePersonCannotReceiveNewAssignments() = runBlocking {
        val id = repository.addEmployee("Marco")
        repository.setEmployeeActive(id, false)
        try {
            repository.setAssigned(date, id, Meal.DINNER, true)
            fail("Inactive employee assigned")
        } catch (_: IllegalArgumentException) {
            assertTrue(repository.assignmentsForDate(date).first().isEmpty())
        }
    }
    @Test fun weekQueryUsesRealDateBoundaries() = runBlocking {
        val id = repository.addEmployee("Marco")
        listOf(date.minusDays(2), date.minusDays(1), date.plusDays(5), date.plusDays(6)).forEach {
            repository.setAssigned(it, id, Meal.LUNCH, true)
        }
        assertEquals(listOf(date.minusDays(1), date.plusDays(5)), repository.assignmentsForWeek(date).first().map { it.date })
        assertTrue(repository.assignmentsForWeek(date.plusWeeks(2)).first().isEmpty())
        assertEquals(2, repository.assignmentsForEmployeeDuringWeek(id, date).first().size)
    }
    @Test fun namesAutomaticallyReorderAfterRename() = runBlocking {
        val marco = repository.addEmployee("Marco")
        val anna = repository.addEmployee("anna")
        val giulia = repository.addEmployee("Giulia")
        assertEquals(listOf(anna, giulia, marco), repository.employees.first().map { it.id })
        repository.renameEmployee(marco, "  Alberto  ")
        assertEquals(listOf(marco, anna, giulia), repository.employees.first().map { it.id })
        assertEquals("Alberto", repository.employees.first().first().name)
    }
    @Test fun fastRepeatedTapsToggleTheStoredAssignment() = runBlocking {
        val id = repository.addEmployee("Marco")
        repository.toggleAssigned(date, id, Meal.LUNCH)
        repository.toggleAssigned(date, id, Meal.LUNCH)
        assertTrue(repository.assignmentsForDate(date).first().isEmpty())
    }
    @Test fun committedAssignmentsSurviveClosingAndReopeningTheDatabase(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "durability-${java.util.UUID.randomUUID()}.db"
        var persisted = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val first = ShiftRepository(persisted)
            val id = first.addEmployee("Marco")
            first.setAssigned(date, id, Meal.DINNER, true)
            persisted.close()
            persisted = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            assertEquals(id, ShiftRepository(persisted).assignmentsForDate(date).first().single().employeeId)
        } finally { persisted.close(); context.deleteDatabase(name) }
    }
}
