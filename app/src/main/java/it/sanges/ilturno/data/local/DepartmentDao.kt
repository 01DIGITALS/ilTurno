package it.sanges.ilturno.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import it.sanges.ilturno.data.entity.Department
import kotlinx.coroutines.flow.Flow

@Dao
interface DepartmentDao {
    @Query("SELECT * FROM departments ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<Department>>
    @Query("SELECT * FROM departments ORDER BY name COLLATE NOCASE, id")
    suspend fun getAll(): List<Department>
    @Query("SELECT * FROM departments WHERE id = :id")
    suspend fun get(id: Long): Department?
    @Insert suspend fun insert(department: Department): Long
    @Query("INSERT OR IGNORE INTO departments (id, name, isActive) VALUES (0, 'Senza reparto', 0)")
    suspend fun ensureUnclassified()
    @Query("UPDATE departments SET name = :name WHERE id = :id AND id != 0")
    suspend fun rename(id: Long, name: String)
    @Query("UPDATE departments SET isActive = :active WHERE id = :id AND id != 0")
    suspend fun setActive(id: Long, active: Boolean)
}
