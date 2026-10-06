package it.sanges.ilturno.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import it.sanges.ilturno.data.entity.Employee
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<Employee>>

    @Query("SELECT * FROM employees ORDER BY sortOrder, id")
    suspend fun getAll(): List<Employee>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun get(id: Long): Employee?

    @Insert suspend fun insert(employee: Employee): Long

    @Query("UPDATE employees SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE employees SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

}
