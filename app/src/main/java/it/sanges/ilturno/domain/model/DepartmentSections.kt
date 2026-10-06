package it.sanges.ilturno.domain.model

import it.sanges.ilturno.data.entity.Department
import it.sanges.ilturno.data.entity.ShiftAssignment
import java.text.Collator
import java.util.Locale

fun List<Department>.alphabeticalDepartments(): List<Department> {
    val collator = Collator.getInstance(Locale.ITALIAN).apply { strength = Collator.PRIMARY }
    return sortedWith { a, b -> collator.compare(a.name, b.name).let { if (it == 0) a.id.compareTo(b.id) else it } }
}

fun departmentSections(departments: List<Department>, assignments: List<ShiftAssignment>): List<Department> {
    val assignedIds = assignments.map { it.departmentId }.toSet()
    val real = departments.filter { it.id != 0L && (it.isActive || it.id in assignedIds) }.alphabeticalDepartments()
    return if (0L in assignedIds || departments.none { it.id != 0L && it.isActive }) {
        real + Department(0, "Senza reparto", false)
    } else real
}
