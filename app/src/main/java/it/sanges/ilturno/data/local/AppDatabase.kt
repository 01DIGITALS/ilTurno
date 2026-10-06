package it.sanges.ilturno.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import it.sanges.ilturno.data.entity.Employee
import it.sanges.ilturno.data.entity.ShiftAssignment
import it.sanges.ilturno.data.entity.Department
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Employee::class, ShiftAssignment::class, Department::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employees(): EmployeeDao
    abstract fun assignments(): AssignmentDao
    abstract fun departments(): DepartmentDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS departments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, isActive INTEGER NOT NULL)")
                db.execSQL("INSERT INTO departments (id, name, isActive) VALUES (0, 'Senza reparto', 0)")
                db.execSQL("CREATE TABLE assignments_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, employeeId INTEGER NOT NULL, meal TEXT NOT NULL, createdAt INTEGER NOT NULL, departmentId INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(employeeId) REFERENCES employees(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(departmentId) REFERENCES departments(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("INSERT INTO assignments_new (id, date, employeeId, meal, createdAt, departmentId) SELECT id, date, employeeId, meal, createdAt, 0 FROM assignments")
                db.execSQL("DROP TABLE assignments")
                db.execSQL("ALTER TABLE assignments_new RENAME TO assignments")
                db.execSQL("CREATE UNIQUE INDEX index_assignments_date_employeeId_meal_departmentId ON assignments(date, employeeId, meal, departmentId)")
                db.execSQL("CREATE INDEX index_assignments_employeeId ON assignments(employeeId)")
                db.execSQL("CREATE INDEX index_assignments_departmentId ON assignments(departmentId)")
            }
        }
    }
}
