package it.sanges.ilturno.ui.departments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.sanges.ilturno.R
import it.sanges.ilturno.data.entity.Department

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentsScreen(departments: List<Department>, onBack: () -> Unit,
    onSave: (Long?, String) -> Unit, onActive: (Department, Boolean) -> Unit,
    grouped: Boolean, onGrouped: (Boolean) -> Unit) {
    var showName by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.departments)) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } })
    }, bottomBar = {
        Button(onClick = { editingId = null; name = ""; showName = true },
            modifier = Modifier.navigationBarsPadding().fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.add_department))
        }
    }) { insets ->
        LazyColumn(Modifier.fillMaxSize().padding(insets), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                val label = stringResource(R.string.group_departments)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, modifier = Modifier.weight(1f).padding(top = 12.dp))
                    Switch(checked = grouped, onCheckedChange = onGrouped,
                        modifier = Modifier.testTag("group-departments").semantics { contentDescription = label })
                }
                Text(stringResource(R.string.group_departments_hint), style = MaterialTheme.typography.bodySmall)
            }
            item { Text(stringResource(R.string.departments_hint), style = MaterialTheme.typography.bodyMedium) }
            items(departments.filter { it.id != 0L }, key = { it.id }) { department ->
                ElevatedCard(Modifier.fillMaxWidth().testTag("department-${department.id}")) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(department.name, style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.fillMaxWidth().clickable {
                                editingId = department.id; name = department.name; showName = true
                            }.padding(vertical = 12.dp))
                        if (!department.isActive) Text(stringResource(R.string.department_removed))
                        TextButton(onClick = { onActive(department, !department.isActive) }) {
                            Text(stringResource(if (department.isActive) R.string.remove_department else R.string.restore_department))
                        }
                    }
                }
            }
        }
    }
    if (showName) AlertDialog(onDismissRequest = { showName = false },
        title = { Text(stringResource(if (editingId == null) R.string.add_department else R.string.rename_department)) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, singleLine = true,
            label = { Text(stringResource(R.string.department_name)) }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(editingId, name.trim()); showName = false }) {
            Text(stringResource(R.string.confirm))
        } }, dismissButton = { TextButton(onClick = { showName = false }) { Text(stringResource(R.string.cancel)) } })
}
