package it.sanges.ilturno.ui.employees

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import it.sanges.ilturno.R
import it.sanges.ilturno.data.entity.Employee

@Composable
fun PersonNameDialog(initialName: String?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialName.orEmpty()) }
    val focus = remember { FocusRequester() }
    fun submit() { if (name.isNotBlank()) onConfirm(name.trim()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initialName == null) R.string.add_person else R.string.rename_person)) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
        },
        confirmButton = {
            TextButton(onClick = { submit() }, enabled = name.isNotBlank()) {
                Text(stringResource(if (initialName == null) R.string.add else R.string.confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    people: List<Employee>, onBack: () -> Unit, onAdd: () -> Unit,
    onRename: (Employee) -> Unit, onActive: (Employee) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.people)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
            )
        },
        bottomBar = {
            Button(onClick = onAdd, modifier = Modifier.navigationBarsPadding().imePadding().fillMaxWidth().padding(16.dp)) {
                Text(stringResource(R.string.add_person_cta))
            }
        },
    ) { insets ->
        LazyColumn(Modifier.fillMaxSize().padding(insets), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(stringResource(R.string.people_hint), style = MaterialTheme.typography.bodyMedium) }
            items(people, key = { person -> person.id }) { person ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val renameDescription = stringResource(R.string.rename, person.name)
                        Text(
                            person.name, style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.fillMaxWidth().clickable { onRename(person) }
                                .semantics { contentDescription = renameDescription }.padding(vertical = 12.dp),
                        )
                        if (!person.isActive) Text(stringResource(R.string.inactive), style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { onActive(person) }) {
                                Text(stringResource(if (person.isActive) R.string.deactivate else R.string.reactivate))
                            }
                        }
                    }
                }
            }
        }
    }
}
