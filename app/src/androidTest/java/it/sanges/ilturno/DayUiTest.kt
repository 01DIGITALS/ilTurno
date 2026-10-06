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

class DayUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private var marco = 0L

    @Before fun preparePeople() = runBlocking(Dispatchers.IO) {
        val application = compose.activity.application as IlTurnoApplication
        application.database.clearAllTables()
        marco = application.repository.addEmployee("Marco")
        application.repository.addEmployee("Anna")
        Unit
    }

    @Test fun lunchAndDinnerPersistWithoutClosingThePickerAfterEachTap() {
        compose.onNodeWithTag("service-LUNCH").performClick()
        compose.onNodeWithTag("picker-$marco").performClick().assertIsOn()
        compose.onNodeWithText("Anna").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.done)).performClick()
        compose.onNodeWithTag("day-LUNCH-$marco", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("service-DINNER").performClick()
        compose.onNodeWithTag("picker-$marco").performClick().assertIsOn()
        compose.onNodeWithText(compose.activity.getString(R.string.done)).performClick()
        compose.onNodeWithTag("day-DINNER-$marco", useUnmergedTree = true).assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("day-LUNCH-$marco", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("day-DINNER-$marco", useUnmergedTree = true).assertIsDisplayed()
    }
    @Test fun arrowsTodayAndDatePickerWork() {
        val today = LocalDate.now()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.next_day)).performClick()
        compose.onNodeWithText(DateUtils.dayTitle(today.plusDays(1))).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.today)).performClick()
        compose.onNodeWithText(DateUtils.dayTitle(today)).assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.choose_date)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.confirm)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.cancel)).performClick()
        compose.onNodeWithText(DateUtils.dayTitle(today)).assertIsDisplayed()
    }
    @Test fun swipeChangesTheRealDate() {
        val today = LocalDate.now()
        compose.onNodeWithTag("day-screen").performTouchInput { swipeLeft() }
        compose.onNodeWithText(DateUtils.dayTitle(today.minusDays(1))).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.today)).performClick()
        compose.onNodeWithText(DateUtils.dayTitle(today)).assertIsDisplayed()
    }
}
