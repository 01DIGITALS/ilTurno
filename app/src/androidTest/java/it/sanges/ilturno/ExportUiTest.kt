package it.sanges.ilturno

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import it.sanges.ilturno.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

class ExportUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun seed(): Unit = runBlocking(Dispatchers.IO) {
        val app = compose.activity.application as IlTurnoApplication
        app.database.clearAllTables()
        app.repository.addEmployee("Marco")
    }
    @After fun dismissSharesheet() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent KEYCODE_BACK").close()
    }
    @Test fun exportingTheNextWeekGeneratesItsPdfAndLaunchesTheChooser() {
        val app = compose.activity.application as IlTurnoApplication
        val existing = app.cacheDir.walkTopDown().filter { it.isFile }.map { it.absolutePath }.toSet()
        val nextWeek = DateUtils.weekStart(LocalDate.now()).plusWeeks(1)
        compose.onNodeWithText(compose.activity.getString(R.string.week_tab)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.next_week)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.export)).performClick()
        compose.onNode(hasText(DateUtils.weekLabel(nextWeek)) and hasAnyAncestor(hasTestTag("export-picker"))).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.pdf)).performClick()
        val fileName = "Turni_${nextWeek}_${nextWeek.plusDays(6)}.pdf"
        compose.waitUntil(10_000) { app.cacheDir.walkTopDown().any { it.name == fileName && it.length() > 0 && it.absolutePath !in existing } }
        val file = app.cacheDir.walkTopDown().first { it.name == fileName && it.absolutePath !in existing }
        assertEquals("%PDF", file.inputStream().use { stream -> val header = ByteArray(4); assertEquals(4, stream.read(header)); String(header, Charsets.US_ASCII) })
    }
}
