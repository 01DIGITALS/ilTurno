package it.sanges.ilturno.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import it.sanges.ilturno.domain.model.Meal
import java.time.LocalDate

@Entity(
    tableName = "assignments",
    foreignKeys = [ForeignKey(
        entity = Employee::class,
        parentColumns = ["id"], childColumns = ["employeeId"],
        onDelete = ForeignKey.RESTRICT,
    ), ForeignKey(
        entity = Department::class,
        parentColumns = ["id"], childColumns = ["departmentId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [
        Index(value = ["date", "employeeId", "meal", "departmentId"], unique = true),
        Index(value = ["employeeId"]),
        Index(value = ["departmentId"]),
    ],
)
data class ShiftAssignment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val employeeId: Long,
    val meal: Meal,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val departmentId: Long = 0,
)
