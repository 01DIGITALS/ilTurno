package it.sanges.ilturno.ui.week

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.sanges.ilturno.R
import it.sanges.ilturno.data.entity.Department
import it.sanges.ilturno.domain.model.departmentSections
import it.sanges.ilturno.data.entity.Employee
import it.sanges.ilturno.data.entity.ShiftAssignment
import it.sanges.ilturno.domain.model.Meal
import it.sanges.ilturno.domain.model.WeekSummary
import it.sanges.ilturno.ui.components.DateHeader
import it.sanges.ilturno.ui.components.dateSwipe
import it.sanges.ilturno.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WeekScreen(
    date: LocalDate, people: List<Employee>, assignments: List<ShiftAssignment>, highlightedId: Long?,
    onHighlight: (Long?) -> Unit, onStep: (Int) -> Unit, onDate: () -> Unit, onMeal: (LocalDate, Meal, Long) -> Unit, departments: List<Department> = emptyList(), groupByDepartment: Boolean = true,
) {
    val days = DateUtils.weekDays(date)
    val inWeek = assignments.filter { it.date in days }
    val highlighted = people.find { it.id == highlightedId }
    val listState = rememberLazyListState()
    LaunchedEffect(highlightedId) { if (highlightedId != null) listState.scrollToItem(0) }
    Column(Modifier.fillMaxSize().testTag("week-screen").dateSwipe(onStep).padding(horizontal = 16.dp)) {
        DateHeader(date, true, onStep, onDate)
        LazyColumn(modifier = Modifier.testTag("week-list"), state = listState, contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (highlighted != null) item(key = "summary") { PersonSummary(highlighted, date, inWeek, onClose = { onHighlight(null) }) }
            if (inWeek.isEmpty()) item { Text(stringResource(R.string.empty_week), modifier = Modifier.padding(vertical = 12.dp)) }
            items(days, key = { it.toEpochDay() }) { day ->
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(DateUtils.shortDay(day), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        (if (groupByDepartment) departmentSections(departments, inWeek.filter { it.date == day }) else listOf(Department(0, "Senza reparto"))).forEach { department ->
                            if (groupByDepartment && departments.any { it.id != 0L }) Text(department.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                            if (department.id != 0L && !department.isActive) Text(stringResource(R.string.department_removed), style = MaterialTheme.typography.bodySmall)
                            Meal.entries.forEach { meal ->
                                val label = stringResource(if (meal == Meal.LUNCH) R.string.lunch else R.string.dinner)
                                val edit = stringResource(R.string.edit_service, label, DateUtils.fullDate(day)) + if (department.id != 0L) " · ${department.name}" else ""
                                val ids = inWeek.filter { it.date == day && it.meal == meal && (!groupByDepartment || it.departmentId == department.id) }.map { it.employeeId }.toSet()
                                val assigned = people.filter { it.id in ids }
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                    TextButton(onClick = { onMeal(day, meal, if (groupByDepartment) department.id else -1L) }, modifier = Modifier.testTag(if (department.id == 0L) "week-service-$day-${meal.name}" else "week-service-$day-${meal.name}-${department.id}")
                                        .semantics { contentDescription = edit }) { Text(label) }
                                    if (assigned.isEmpty()) {
                                        Text(stringResource(R.string.no_assignments), style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f).clickable { onMeal(day, meal, if (groupByDepartment) department.id else -1L) }.heightIn(min = 48.dp).padding(vertical = 12.dp))
                                    } else FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        assigned.forEach { employee ->
                                            val isSelected = highlightedId == employee.id
                                            val description = stringResource(R.string.highlight_person, employee.name)
                                            TextButton(onClick = { onHighlight(employee.id) }, modifier = Modifier
                                                .testTag(if (department.id == 0L) "week-name-$day-${meal.name}-${employee.id}" else "week-name-$day-${meal.name}-${employee.id}-${department.id}").semantics {
                                                    selected = isSelected
                                                    contentDescription = description
                                                }) {
                                                Text(if (isSelected) "✓ ${employee.name}" else employee.name,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (highlightedId != null && !isSelected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonSummary(employee: Employee, date: LocalDate, assignments: List<ShiftAssignment>, onClose: () -> Unit) {
    val summary = WeekSummary.forEmployee(employee.id, date, assignments)
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("person-summary")) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(employee.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                val closeDescription = stringResource(R.string.clear_highlight)
                IconButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = closeDescription }) { Text("×") }
            }
            Text(pluralStringResource(R.plurals.days_assigned, summary.daysAssigned, summary.daysAssigned), style = MaterialTheme.typography.bodyMedium)
            Text(pluralStringResource(R.plurals.days_without, summary.daysWithoutAssignments, summary.daysWithoutAssignments), style = MaterialTheme.typography.bodyMedium)
            Text(pluralStringResource(R.plurals.services_assigned, summary.services, summary.services), style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                DateUtils.weekDays(date).forEach { day ->
                    val meals = assignments.filter { it.employeeId == employee.id && it.date == day }.map { it.meal }.toSet()
                    val lunch = Meal.LUNCH in meals
                    val dinner = Meal.DINNER in meals
                    val description = stringResource(R.string.summary_day, DateUtils.fullDate(day),
                        stringResource(if (lunch) R.string.assigned else R.string.not_assigned),
                        stringResource(if (dinner) R.string.assigned else R.string.not_assigned))
                    Column(Modifier.weight(1f).clearAndSetSemantics { contentDescription = description }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day.format(DateTimeFormatter.ofPattern("EEE", DateUtils.locale)), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium)
                        Text("${stringResource(R.string.lunch_short)} ${if (lunch) "✓" else "–"}", style = MaterialTheme.typography.labelSmall)
                        Text("${stringResource(R.string.dinner_short)} ${if (dinner) "✓" else "–"}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text(stringResource(R.string.summary_legend), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
