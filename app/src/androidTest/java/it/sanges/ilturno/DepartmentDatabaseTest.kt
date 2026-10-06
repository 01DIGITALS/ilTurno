package it.sanges.ilturno

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import it.sanges.ilturno.data.local.AppDatabase
import it.sanges.ilturno.data.repository.ShiftRepository
import it.sanges.ilturno.domain.model.Meal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class DepartmentDatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun migrationPreservesNamesAndEveryOldAssignment(): Unit = runBlocking {
        val name = "migration-${UUID.randomUUID()}.db"
        val day = LocalDate.of(2026, 10, 6)
        context.openOrCreateDatabase(name, 0, null).use { old ->
            old.execSQL("CREATE TABLE employees (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, isActive INTEGER NOT NULL, sortOrder INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE assignments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, employeeId INTEGER NOT NULL, meal TEXT NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(employeeId) REFERENCES employees(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            old.execSQL("CREATE UNIQUE INDEX index_assignments_date_employeeId_meal ON assignments(date, employeeId, meal)")
            old.execSQL("CREATE INDEX index_assignments_employeeId ON assignments(employeeId)")
            old.execSQL("INSERT INTO employees VALUES(7, 'Anna', 1, 0, 123)")
            old.execSQL("INSERT INTO assignments VALUES(9, ${day.toEpochDay()}, 7, 'LUNCH', 456)")
            old.version = 1
        }
        val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_1_2).build()
        try {
            assertEquals("Anna", upgraded.employees().get(7)!!.name)
            val shift = upgraded.assignments().getForWeek(day, day).single()
            assertEquals(9L, shift.id)
            assertEquals(0L, shift.departmentId)
            assertEquals(456L, shift.createdAt)
            assertEquals("Senza reparto", upgraded.departments().get(0)!!.name)
            val repository = ShiftRepository(upgraded)
            val sala = repository.addDepartment("Sala")
            repository.setAssigned(day, 7, Meal.LUNCH, true, sala)
            assertEquals(2, upgraded.assignments().getForWeek(day, day).size)
        } finally { upgraded.close(); context.deleteDatabase(name) }
    }
    @Test fun departmentsCanBeRenamedRemovedAndRestoredWithoutLosingShifts(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = ShiftRepository(db)
            val day = LocalDate.of(2026, 10, 6)
            val person = repo.addEmployee("Anna")
            val sala = repo.addDepartment("  Sala  ")
            val cucina = repo.addDepartment("Cucina")
            repo.setAssigned(day, person, Meal.LUNCH, true, sala)
            repo.setAssigned(day, person, Meal.LUNCH, true, sala)
            repo.setAssigned(day, person, Meal.DINNER, true, cucina)
            assertEquals(2, repo.assignmentsForDate(day).first().size)
            repo.renameDepartment(sala, "Sala principale")
            repo.setDepartmentActive(sala, false)
            assertEquals("Sala principale", db.departments().get(sala)!!.name)
            assertFalse(db.departments().get(sala)!!.isActive)
            assertEquals(2, repo.assignmentsForDate(day).first().size)
            try { repo.setAssigned(day.plusDays(1), person, Meal.LUNCH, true, sala); fail("Removed department assigned") }
            catch (_: IllegalArgumentException) { }
            repo.setAssigned(day, person, Meal.DINNER, false, cucina)
            assertEquals(sala, repo.assignmentsForDate(day).first().single().departmentId)
            repo.setDepartmentActive(sala, true)
            repo.setAssigned(day.plusDays(1), person, Meal.LUNCH, true, sala)
            assertEquals(sala, repo.assignmentsForDate(day.plusDays(1)).first().single().departmentId)
        } finally { db.close() }
    }
}
