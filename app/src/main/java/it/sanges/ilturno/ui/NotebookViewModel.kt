package it.sanges.ilturno.ui

import android.app.Application
import it.sanges.ilturno.notifications.ShiftReminders
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import it.sanges.ilturno.data.entity.Employee
import it.sanges.ilturno.data.entity.Department
import it.sanges.ilturno.data.DepartmentPreferences
import it.sanges.ilturno.data.entity.ShiftAssignment
import it.sanges.ilturno.domain.model.Meal
import it.sanges.ilturno.util.DateUtils
import it.sanges.ilturno.IlTurnoApplication
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import it.sanges.ilturno.R
import it.sanges.ilturno.util.ExportedFile
import it.sanges.ilturno.util.ExportFormat
import it.sanges.ilturno.util.ShareExporter
import it.sanges.ilturno.util.DebugDiagnostics
import java.time.LocalDate

data class NotebookUiState(val selectedDate: LocalDate = LocalDate.now(), val weekView: Boolean = false)
data class PickerTarget(val date: LocalDate, val meal: Meal, val departmentId: Long = 0)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NotebookViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    // A new process starts Today. Rotation keeps this ViewModel and its current selection.
    init {
        savedState["selection"] = "${LocalDate.now()}|false"
        savedState["picker"] = ""
        savedState["highlighted"] = null
    }
    val repository = (application as IlTurnoApplication).repository
    val highlightedEmployeeId = savedState.getStateFlow<Long?>("highlighted", null)
    fun highlightEmployee(id: Long?) { savedState["highlighted"] = if (highlightedEmployeeId.value == id) null else id }
    val employees: StateFlow<List<Employee>?> = repository.employees.map<List<Employee>, List<Employee>?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val departments: StateFlow<List<Department>> = repository.departments.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val groupedState = MutableStateFlow(DepartmentPreferences.enabled(application))
    val groupByDepartment: StateFlow<Boolean> = groupedState
    fun setGroupByDepartment(enabled: Boolean) {
        DepartmentPreferences.setEnabled(getApplication(), enabled)
        groupedState.value = enabled
        write { refreshReminders() }
    }
    val errors = Channel<Int>(Channel.CONFLATED)
    val exportedFiles = Channel<ExportedFile>(Channel.BUFFERED)
    private val exportingState = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = exportingState
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            for (write in writes) {
                try { write() } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) { errors.send(R.string.write_failed) }
            }
        }
    }

    private fun write(operation: String? = null, action: suspend () -> Unit) {
        writes.trySend { if (operation == null) action() else DebugDiagnostics.trace(operation, action) }
    }
    fun addEmployee(name: String) = write("person.add") { repository.addEmployee(name); Unit }
    fun renameEmployee(id: Long, name: String) = write("person.rename") { repository.renameEmployee(id, name) }
    fun setEmployeeActive(id: Long, active: Boolean) = write("person.active value=$active") { repository.setEmployeeActive(id, active) }
    fun saveDepartment(id: Long?, name: String) = write("department.save") {
        if (id == null) repository.addDepartment(name) else repository.renameDepartment(id, name)
        refreshReminders()
    }
    fun setDepartmentActive(id: Long, active: Boolean) = write("department.active") {
        repository.setDepartmentActive(id, active)
        refreshReminders()
    }
    private suspend fun refreshReminders() {
        runCatching { ShiftReminders.check(getApplication(), deliver = false) }
            .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it; if (it is Exception) DebugDiagnostics.failure("reminder.refresh", it) }
    }
    fun export(date: LocalDate, format: ExportFormat) {
        if (exportingState.value) return
        exportingState.value = true
        write {
            try {
                val file = DebugDiagnostics.trace("export format=${format.name} week=${DateUtils.weekStart(date)}") {
                    val snapshot = repository.snapshot(date, groupDepartments = groupedState.value && departments.value.any { it.id != 0L && it.isActive })
                    withContext(Dispatchers.IO) { ShareExporter.generate(getApplication(), snapshot, format) }
                }
                exportedFiles.send(file)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (_: Exception) { errors.send(R.string.export_failed)
            } finally { exportingState.value = false }
        }
    }
    private val selection = savedState.getStateFlow("selection", "${LocalDate.now()}|false")
    private fun decode(encoded: String): NotebookUiState {
        val parts = encoded.split('|')
        return NotebookUiState(LocalDate.parse(parts[0]), parts[1].toBoolean())
    }
    val state: StateFlow<NotebookUiState> = selection.map { encoded ->
        decode(encoded)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, decode(selection.value))

    val assignments: StateFlow<List<ShiftAssignment>> = state.map { DateUtils.weekStart(it.selectedDate) }
        .distinctUntilChanged().flatMapLatest { repository.assignmentsForWeek(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val pickerSelection = savedState.getStateFlow("picker", "")
    private fun decodePicker(value: String): PickerTarget? {
        if (value.isEmpty()) return null
        val parts = value.split('|')
        return PickerTarget(LocalDate.parse(parts[0]), Meal.valueOf(parts[1]), parts.getOrNull(2)?.toLongOrNull() ?: 0)
    }
    val picker: StateFlow<PickerTarget?> = pickerSelection.map(::decodePicker)
        .stateIn(viewModelScope, SharingStarted.Eagerly, decodePicker(pickerSelection.value))
    val pickerAssignments: StateFlow<List<ShiftAssignment>> = picker.flatMapLatest { target ->
        if (target == null) flowOf(emptyList()) else repository.assignmentsForDate(target.date)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun openPicker(date: LocalDate, meal: Meal, departmentId: Long = 0) {
        DebugDiagnostics.event("picker.open date=$date meal=${meal.name}")
        savedState["picker"] = "$date|${meal.name}|$departmentId"
    }
    fun closePicker() { DebugDiagnostics.event("picker.close"); savedState["picker"] = "" }
    fun toggleAssigned(date: LocalDate, employeeId: Long, meal: Meal, departmentId: Long = 0) = write("assignment.toggle date=$date meal=${meal.name}") {
        repository.toggleAssigned(date, employeeId, meal, departmentId)
        refreshReminders()
    }
    fun selectDate(date: LocalDate) {
        DebugDiagnostics.event("navigation.date value=$date")
        savedState["selection"] = "$date|${decode(selection.value).weekView}"
    }
    fun step(direction: Int) {
        val current = decode(selection.value)
        selectDate(current.selectedDate.plusDays(direction.toLong() * if (current.weekView) 7 else 1))
    }
    fun today() = selectDate(LocalDate.now())

    fun showWeek(week: Boolean) {
        DebugDiagnostics.event("navigation.view value=${if (week) "week" else "day"}")
        savedState["selection"] = "${decode(selection.value).selectedDate}|$week"
    }
}
