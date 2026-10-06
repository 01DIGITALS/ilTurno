package it.sanges.ilturno.ui.day

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import it.sanges.ilturno.R
import it.sanges.ilturno.data.entity.Department
import it.sanges.ilturno.domain.model.departmentSections
import it.sanges.ilturno.data.entity.Employee
import it.sanges.ilturno.data.entity.ShiftAssignment
import it.sanges.ilturno.domain.model.Meal
import it.sanges.ilturno.ui.components.DateHeader
import it.sanges.ilturno.ui.components.dateSwipe
import it.sanges.ilturno.util.DateUtils
import java.time.LocalDate

@Composable
fun DayScreen(
    date: LocalDate, people: List<Employee>, assignments: List<ShiftAssignment>,
    onStep: (Int) -> Unit, onDate: () -> Unit, onMeal: (LocalDate, Meal, Long) -> Unit, onPeople: () -> Unit, departments: List<Department> = emptyList(), groupByDepartment: Boolean = true,
) {
    Column(Modifier.fillMaxSize().testTag("day-screen").dateSwipe(onStep).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DateHeader(date, false, onStep, onDate)
        (if (groupByDepartment) departmentSections(departments, assignments.filter { it.date == date }) else listOf(Department(0, "Senza reparto"))).forEach { department ->
            if (groupByDepartment && departments.any { it.id != 0L }) Text(department.name, style = MaterialTheme.typography.titleLarge)
            if (department.id != 0L && !department.isActive) Text(stringResource(R.string.department_removed), style = MaterialTheme.typography.bodySmall)
            Meal.entries.forEach { meal ->
                val assignedIds = assignments.filter { it.date == date && it.meal == meal && (!groupByDepartment || it.departmentId == department.id) }.map { it.employeeId }.toSet()
                val assigned = people.filter { it.id in assignedIds }
                val label = stringResource(if (meal == Meal.LUNCH) R.string.lunch else R.string.dinner)
                val description = stringResource(R.string.edit_service, label, DateUtils.fullDate(date)) + if (department.id != 0L) " · ${department.name}" else ""
                Card(
                    onClick = { onMeal(date, meal, if (groupByDepartment) department.id else -1L) }, shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = if (meal == Meal.LUNCH) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth().testTag(if (department.id == 0L) "service-${meal.name}" else "service-${meal.name}-${department.id}").semantics { contentDescription = description },
                ) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(if (meal == Meal.LUNCH) R.string.lunch_symbol else R.string.dinner_symbol), style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.clearAndSetSemantics {})
                            Text(label, style = MaterialTheme.typography.headlineSmall)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (assigned.isEmpty()) Text(stringResource(R.string.no_assignments), style = MaterialTheme.typography.bodyLarge)
                            assigned.forEach { person -> Text(person.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag(if (department.id == 0L) "day-${meal.name}-${person.id}" else "day-${meal.name}-${person.id}-${department.id}")) }
                        }
                        Text(stringResource(if (department.id == 0L || department.isActive) R.string.add_service else R.string.edit_assignments), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        if (people.none { it.isActive }) TextButton(onClick = onPeople, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.no_active_people))
        }
        Spacer(Modifier.height(8.dp))
    }
}
