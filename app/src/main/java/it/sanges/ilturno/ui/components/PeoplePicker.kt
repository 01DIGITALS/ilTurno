package it.sanges.ilturno.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import it.sanges.ilturno.R
import it.sanges.ilturno.data.entity.Employee
import it.sanges.ilturno.data.entity.Department
import it.sanges.ilturno.data.entity.ShiftAssignment
import it.sanges.ilturno.domain.model.Meal
import it.sanges.ilturno.ui.PickerTarget
import it.sanges.ilturno.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeoplePicker(
    target: PickerTarget, people: List<Employee>, assignments: List<ShiftAssignment>,
    onToggle: (Long) -> Unit, onDismiss: () -> Unit, onPeople: () -> Unit,
    department: Department? = null,
) {
    val selected = assignments.filter { it.date == target.date && it.meal == target.meal && (target.departmentId == -1L || it.departmentId == target.departmentId) }.map { it.employeeId }.toSet()
    val canAdd = target.departmentId <= 0L || department?.isActive == true
    val available = people.filter { (canAdd && it.isActive) || it.id in selected }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).testTag("people-picker")) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(if (target.meal == Meal.LUNCH) R.string.lunch else R.string.dinner), style = MaterialTheme.typography.headlineMedium)
                    Text(DateUtils.fullDate(target.date), style = MaterialTheme.typography.bodyMedium)
                    if (target.departmentId > 0L) Text(department?.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
            }
            Spacer(Modifier.height(8.dp))
            if (!canAdd) Text(stringResource(R.string.department_archived_picker), modifier = Modifier.padding(bottom = 12.dp))
            if (people.none { it.isActive }) {
                Button(onClick = onPeople, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Text(stringResource(R.string.no_active_people))
                }
            }
            LazyColumn(Modifier.weight(1f, fill = false), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(available, key = { it.id }) { person ->
                    Row(
                        Modifier.fillMaxWidth().testTag("picker-${person.id}")
                            .toggleable(value = person.id in selected, role = Role.Checkbox, onValueChange = { onToggle(person.id) })
                            .heightIn(min = 48.dp).padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = person.id in selected, onCheckedChange = null)
                        Column(Modifier.weight(1f).padding(start = 16.dp)) {
                            Text(person.name, style = MaterialTheme.typography.bodyLarge)
                            if (!person.isActive) Text(stringResource(R.string.inactive_short), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
