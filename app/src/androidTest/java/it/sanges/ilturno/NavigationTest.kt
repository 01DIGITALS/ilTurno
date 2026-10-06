package it.sanges.ilturno

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import it.sanges.ilturno.util.DateUtils
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun clearPeople() = runBlocking {
        (compose.activity.application as IlTurnoApplication).database.clearAllTables()
    }

    @Test fun infoShowsVersionManualUpdatesAndDeveloper() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.info)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.version)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.updates_manual)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.download_updates)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("LouisBigDev").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.back)).performClick()
        compose.onNodeWithText(DateUtils.dayTitle(LocalDate.now())).assertIsDisplayed()
    }

    @Test fun appOpensTodayAndCanSwitchToTheRealWeek() {
        val today = LocalDate.now()
        compose.onNodeWithText(DateUtils.dayTitle(today)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.week_tab)).performClick()
        compose.onNodeWithText(DateUtils.weekLabel(today)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.day_tab)).performClick()
        compose.onNodeWithText(DateUtils.dayTitle(today)).assertIsDisplayed()
    }
}
