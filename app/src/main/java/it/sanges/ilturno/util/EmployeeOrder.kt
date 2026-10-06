package it.sanges.ilturno.util

import it.sanges.ilturno.data.entity.Employee
import java.text.Collator
import java.util.Locale

fun List<Employee>.alphabetically(): List<Employee> {
    val collator = Collator.getInstance(Locale.ITALIAN).apply { strength = Collator.PRIMARY }
    return sortedWith { a, b ->
        val comparison = collator.compare(a.name, b.name)
        if (comparison != 0) comparison else a.id.compareTo(b.id)
    }
}
