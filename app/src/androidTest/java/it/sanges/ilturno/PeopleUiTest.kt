package it.sanges.ilturno

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PeopleUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun clearTestDatabase() = runBlocking(Dispatchers.IO) {
        (compose.activity.application as IlTurnoApplication).database.clearAllTables()
    }

    @Test fun addRenameDeactivateReactivate() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.people)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.add_person_cta)).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Marco")
        compose.onNodeWithText(compose.activity.getString(R.string.add)).performClick()
        compose.onNodeWithText("Marco").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Marco Rossi")
        compose.onNodeWithText(compose.activity.getString(R.string.confirm)).performClick()
        compose.onNodeWithText("Marco Rossi").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.deactivate)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.reactivate)).assertIsDisplayed().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.deactivate)).assertIsDisplayed()
    }
}
