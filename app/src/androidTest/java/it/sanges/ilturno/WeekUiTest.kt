package it.sanges.ilturno

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import it.sanges.ilturno.util.DateUtils
import it.sanges.ilturno.domain.model.Meal

class WeekUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private var marco = 0L
    private val monday = DateUtils.weekStart(LocalDate.now())
    @Before fun seed(): Unit = runBlocking(Dispatchers.IO) {
        val app = compose.activity.application as IlTurnoApplication
        app.database.clearAllTables()
        marco = app.repository.addEmployee("Marco")
        app.repository.addEmployee("Anna")
        app.repository.setAssigned(monday, marco, Meal.LUNCH, true)
        app.repository.setAssigned(monday, marco, Meal.DINNER, true)
    }
    @Test fun tapHighlightsEveryAssignmentAndCountsDaysAndServices() {
        compose.onNodeWithText(compose.activity.getString(R.string.week_tab)).performClick()
        compose.onNodeWithTag("week-name-$monday-LUNCH-$marco").performClick().assertIsSelected()
        compose.onNodeWithTag("week-name-$monday-DINNER-$marco").assertIsSelected()
        compose.onNodeWithText("1 giorno con almeno un turno").assertIsDisplayed()
        compose.onNodeWithText("6 giorni senza assegnazioni").assertIsDisplayed()
        compose.onNodeWithText("2 servizi assegnati").assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.clear_highlight)).performClick()
        compose.onNodeWithTag("week-service-$monday-DINNER").performClick()
        compose.onNodeWithTag("picker-$marco").assertIsOn().performClick().assertIsOff()
    }
    @Test fun nextWeekChangesRealDatesAndDoesNotCopyAssignments() {
        compose.onNodeWithText(compose.activity.getString(R.string.week_tab)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.next_week)).performClick()
        compose.onNodeWithText(DateUtils.weekLabel(monday.plusWeeks(1))).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.empty_week)).assertIsDisplayed()
    }
}
