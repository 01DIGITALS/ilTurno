package it.sanges.ilturno.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.sanges.ilturno.data.entity.ShiftAssignment
import it.sanges.ilturno.domain.model.Meal
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments WHERE date = :date AND employeeId = :employeeId AND meal = :meal AND departmentId = :departmentId LIMIT 1")
    suspend fun find(date: LocalDate, employeeId: Long, meal: Meal, departmentId: Long = 0): ShiftAssignment?
    @Query("SELECT * FROM assignments WHERE date = :date ORDER BY id")
    fun assignmentsForDate(date: LocalDate): Flow<List<ShiftAssignment>>

    @Query("SELECT * FROM assignments WHERE date BETWEEN :start AND :end ORDER BY date, id")
    fun assignmentsForWeek(start: LocalDate, end: LocalDate): Flow<List<ShiftAssignment>>

    @Query("SELECT * FROM assignments WHERE employeeId = :employeeId AND date BETWEEN :start AND :end ORDER BY date, id")
    fun assignmentsForEmployeeDuringWeek(employeeId: Long, start: LocalDate, end: LocalDate): Flow<List<ShiftAssignment>>

    @Query("SELECT * FROM assignments WHERE date BETWEEN :start AND :end ORDER BY date, id")
    suspend fun getForWeek(start: LocalDate, end: LocalDate): List<ShiftAssignment>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(assignment: ShiftAssignment): Long

    @Query("DELETE FROM assignments WHERE date = :date AND employeeId = :employeeId AND meal = :meal AND departmentId = :departmentId")
    suspend fun remove(date: LocalDate, employeeId: Long, meal: Meal, departmentId: Long = 0)
    @Query("DELETE FROM assignments WHERE date = :date AND employeeId = :employeeId AND meal = :meal")
    suspend fun removeFromAllDepartments(date: LocalDate, employeeId: Long, meal: Meal)
}
