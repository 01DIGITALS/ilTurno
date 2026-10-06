package it.sanges.ilturno

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import it.sanges.ilturno.ui.NotebookViewModel
import it.sanges.ilturno.domain.model.Meal
import it.sanges.ilturno.data.entity.Department
import it.sanges.ilturno.data.entity.Employee
import it.sanges.ilturno.data.entity.ShiftAssignment
import it.sanges.ilturno.domain.model.WeekSnapshot
import it.sanges.ilturno.util.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.io.File

class DepartmentsUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application as IlTurnoApplication
    @Before fun reset() = runBlocking {
        app.database.clearAllTables()
        ViewModelProvider(compose.activity)[NotebookViewModel::class.java].setGroupByDepartment(true)
    }
    @After fun restoreGrouping() {
        ViewModelProvider(compose.activity)[NotebookViewModel::class.java].setGroupByDepartment(true)
    }
    @Test fun manageDepartmentsAndSwitchBackToSimpleLunchAndDinner() {
        compose.onNodeWithText(compose.activity.getString(R.string.departments)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.add_department)).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Sala")
        compose.onNodeWithText(compose.activity.getString(R.string.confirm)).performClick()
        compose.onNodeWithText("Sala").assertIsDisplayed().performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Sala principale")
        compose.onNodeWithText(compose.activity.getString(R.string.confirm)).performClick()
        compose.onNodeWithText("Sala principale").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.remove_department)).performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.restore_department)).performScrollTo().performClick()
        compose.onNodeWithTag("group-departments").performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.back)).performClick()
        compose.onNodeWithTag("service-LUNCH").assertExists()
        compose.onNodeWithTag("service-DINNER").assertExists()
        compose.onNodeWithText("Sala principale").assertDoesNotExist()
    }
    @Test fun assignmentsRemainSeparateAndSimpleViewKeepsThemVisible(): Unit = runBlocking {
        val person = app.repository.addEmployee("Anna")
        val sala = app.repository.addDepartment("Sala")
        val cucina = app.repository.addDepartment("Cucina")
        val today = LocalDate.now()
        compose.onNodeWithTag("service-LUNCH-$sala").performScrollTo().performClick()
        compose.onNodeWithTag("picker-$person").performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.done)).performClick()
        compose.onNodeWithTag("day-LUNCH-$person-$sala", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("service-DINNER-$cucina").performScrollTo().performClick()
        compose.onNodeWithTag("picker-$person").assertIsOff().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.done)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.week_tab)).performClick()
        compose.onNodeWithTag("week-list").performScrollToNode(hasTestTag("week-name-$today-LUNCH-$person-$sala"))
        compose.onNodeWithTag("week-name-$today-LUNCH-$person-$sala").assertIsDisplayed()
        compose.onNodeWithTag("week-list").performScrollToNode(hasTestTag("week-name-$today-DINNER-$person-$cucina"))
        compose.onNodeWithTag("week-name-$today-DINNER-$person-$cucina").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.departments)).performClick()
        compose.onNodeWithTag("group-departments").performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.back)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.day_tab)).performClick()
        compose.onNodeWithTag("day-LUNCH-$person", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("day-DINNER-$person", useUnmergedTree = true).assertExists()
        assertEquals(2, app.database.assignments().getForWeek(today, today).size)
    }
    @Test fun departmentExportsAreReadableAndPreserveLabels() {
        val day = LocalDate.of(2026, 10, 6)
        val snapshot = WeekSnapshot.create(day, listOf(Employee(1, "Anna")), listOf(
            ShiftAssignment(date = day, employeeId = 1, meal = Meal.LUNCH, departmentId = 1),
            ShiftAssignment(date = day, employeeId = 1, meal = Meal.DINNER, departmentId = 2)),
            listOf(Department(1, "Sala"), Department(2, "Cucina")))
        val pdf = ShareExporter.generate(compose.activity, snapshot, ExportFormat.PDF)
        PdfRenderer(ParcelFileDescriptor.open(pdf.file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            assertEquals(1, renderer.pageCount)
            renderer.openPage(0).use { assertEquals(842, it.width) }
        }
        val qa = File(compose.activity.filesDir, "qa").apply { mkdirs() }
        pdf.file.copyTo(File(qa, "departments.pdf"), overwrite = true)
        ShareExporter.generate(compose.activity, snapshot, ExportFormat.XLSX).file.copyTo(File(qa, "departments.xlsx"), overwrite = true)
    }
}
