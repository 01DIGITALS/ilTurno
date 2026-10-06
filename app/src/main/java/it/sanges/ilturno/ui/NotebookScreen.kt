package it.sanges.ilturno.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.sanges.ilturno.R
import it.sanges.ilturno.data.entity.Employee
import it.sanges.ilturno.ui.components.JumpDateDialog
import it.sanges.ilturno.ui.components.PeoplePicker
import it.sanges.ilturno.ui.day.DayScreen
import it.sanges.ilturno.ui.week.WeekScreen
import it.sanges.ilturno.ui.export.ExportPicker
import it.sanges.ilturno.util.ShareExporter
import it.sanges.ilturno.util.DebugDiagnostics
import android.content.Intent
import it.sanges.ilturno.ui.employees.PeopleScreen
import it.sanges.ilturno.ui.employees.PersonNameDialog
import java.time.LocalDate

@Composable
fun NotebookScreen(viewModel: NotebookViewModel) {
    val people by viewModel.employees.collectAsStateWithLifecycle()
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val grouped by viewModel.groupByDepartment.collectAsStateWithLifecycle()
    val target by viewModel.picker.collectAsStateWithLifecycle()
    val pickerAssignments by viewModel.pickerAssignments.collectAsStateWithLifecycle()
    var showInfo by rememberSaveable { mutableStateOf(false) }
    var showPeople by rememberSaveable { mutableStateOf(false) }
    var showDepartments by rememberSaveable { mutableStateOf(false) }
    var showName by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current
    LaunchedEffect(viewModel, resources) { for (message in viewModel.errors) snackbar.showSnackbar(resources.getString(message)) }
    LaunchedEffect(viewModel, context, resources) {
        for (file in viewModel.exportedFiles) {
            try {
                context.startActivity(Intent.createChooser(ShareExporter.intent(context, file), resources.getString(R.string.share_shifts)))
                DebugDiagnostics.event("share.chooser opened format=${file.format.name}")
            } catch (error: Exception) {
                DebugDiagnostics.failure("share.chooser", error)
                snackbar.showSnackbar(resources.getString(R.string.export_failed))
            }
        }
    }
    BackHandler(showInfo) { showInfo = false }
    BackHandler(showDepartments) { showDepartments = false }
    BackHandler(showPeople && !showName) { showPeople = false }
    Box(Modifier.fillMaxSize()) {
        val loadedPeople = people
        when {
            loadedPeople == null -> Surface(Modifier.fillMaxSize()) {}
            showInfo -> it.sanges.ilturno.ui.info.InfoScreen(onBack = { showInfo = false })
            showDepartments -> it.sanges.ilturno.ui.departments.DepartmentsScreen(departments,
                onBack = { showDepartments = false }, onSave = viewModel::saveDepartment,
                onActive = { department, active -> viewModel.setDepartmentActive(department.id, active) },
                grouped = grouped, onGrouped = viewModel::setGroupByDepartment)
            showPeople -> PeopleScreen(
                people = loadedPeople, onBack = { showPeople = false },
                onAdd = { editingId = null; showName = true },
                onRename = { editingId = it.id; showName = true },
                onActive = { viewModel.setEmployeeActive(it.id, !it.isActive) },
            )
            else -> NotebookShell(viewModel, loadedPeople, onPeople = { showPeople = true }, onInfo = { showInfo = true }, onDepartments = { showDepartments = true })
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }
    if (showName) {
        PersonNameDialog(
            initialName = people?.find { it.id == editingId }?.name,
            onDismiss = { showName = false; editingId = null },
            onConfirm = { name ->
                val id = editingId
                if (id == null) viewModel.addEmployee(name) else viewModel.renameEmployee(id, name)
                showName = false; editingId = null
            },
        )
    }
    target?.let { selected ->
        PeoplePicker(selected, people.orEmpty(), pickerAssignments,
            onToggle = { viewModel.toggleAssigned(selected.date, it, selected.meal, selected.departmentId) },
            department = departments.find { it.id == selected.departmentId },
            onDismiss = viewModel::closePicker,
            onPeople = { viewModel.closePicker(); showPeople = true })
    }
}

@Composable
private fun NotebookShell(viewModel: NotebookViewModel, people: List<Employee>, onPeople: () -> Unit, onInfo: () -> Unit, onDepartments: () -> Unit) {
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val grouped by viewModel.groupByDepartment.collectAsStateWithLifecycle()
    val useDepartments = grouped && departments.any { it.id != 0L && it.isActive }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    val highlightedId by viewModel.highlightedEmployeeId.collectAsStateWithLifecycle()
    val exporting by viewModel.exporting.collectAsStateWithLifecycle()
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showExport by rememberSaveable { mutableStateOf(false) }
    var exportDate by rememberSaveable { mutableLongStateOf(state.selectedDate.toEpochDay()) }
    Scaffold(
        topBar = {
            Row(Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Row {
                    IconButton(onClick = onInfo) { Icon(painterResource(R.drawable.ic_info), stringResource(R.string.info)) }
                    if (state.selectedDate != LocalDate.now()) TextButton(onClick = viewModel::today) { Text(stringResource(R.string.today)) }
                    IconButton(onClick = onPeople) { Icon(painterResource(R.drawable.ic_people), stringResource(R.string.people)) }
                    if (state.weekView) IconButton(enabled = !exporting, onClick = { exportDate = state.selectedDate.toEpochDay(); showExport = true }) {
                        Icon(painterResource(R.drawable.ic_share), stringResource(R.string.export))
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = !state.weekView, onClick = { viewModel.showWeek(false) },
                    icon = { Icon(painterResource(R.drawable.ic_day), null) }, label = { Text(stringResource(R.string.day_tab)) })
                NavigationBarItem(selected = state.weekView, onClick = { viewModel.showWeek(true) },
                    icon = { Icon(painterResource(R.drawable.ic_week), null) }, label = { Text(stringResource(R.string.week_tab)) })
                NavigationBarItem(selected = false, onClick = onDepartments,
                    icon = { Icon(painterResource(R.drawable.ic_departments), null) }, label = { Text(stringResource(R.string.departments)) })
            }
        },
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            if (!state.weekView) {
                DayScreen(state.selectedDate, people, assignments, viewModel::step, { showDate = true }, viewModel::openPicker, onPeople, departments, useDepartments)
            } else {
                WeekScreen(state.selectedDate, people, assignments, highlightedId, viewModel::highlightEmployee,
                    viewModel::step, { showDate = true }, viewModel::openPicker, departments, useDepartments)
            }
        }
    }
    if (showDate) JumpDateDialog(state.selectedDate, onDismiss = { showDate = false },
        onSelect = { viewModel.selectDate(it); showDate = false })
    if (showExport) ExportPicker(LocalDate.ofEpochDay(exportDate), onDismiss = { showExport = false }, onExport = {
        showExport = false; viewModel.export(LocalDate.ofEpochDay(exportDate), it)
    })
}
