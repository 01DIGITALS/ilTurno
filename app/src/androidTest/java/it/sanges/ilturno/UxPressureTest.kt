package it.sanges.ilturno

import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import it.sanges.ilturno.domain.model.Meal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.time.LocalDate
import androidx.test.platform.app.InstrumentationRegistry

class UxPressureTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val ids = mutableListOf<Long>()
    @Before fun seed(): Unit = runBlocking(Dispatchers.IO) {
        val app = compose.activity.application as IlTurnoApplication
        app.database.clearAllTables()
        listOf("Marco", "Anna", "Luca", "Giulia", "Stefano", "Sara", "Paolo", "Elena").forEach { ids += app.repository.addEmployee(it) }
    }
    @Test fun eightPeopleAcrossTwoServicesRequireTwelveTapsAndNoPickerScroll() {
        if (InstrumentationRegistry.getArguments().getString("prepare-only") == "true") return
        val started = SystemClock.elapsedRealtime()
        var taps = 0
        for ((meal, people) in listOf(Meal.LUNCH to ids.take(4), Meal.DINNER to ids.drop(4))) {
            compose.onNodeWithTag("service-${meal.name}").performClick(); taps++
            people.forEach { id -> compose.onNodeWithTag("picker-$id").assertIsDisplayed().performClick().assertIsOn(); taps++ }
            compose.onNodeWithText(compose.activity.getString(R.string.done)).performClick(); taps++
        }
        val elapsed = SystemClock.elapsedRealtime() - started
        assertEquals(12, taps)
        val app = compose.activity.application as IlTurnoApplication
        val stored = runBlocking(Dispatchers.IO) { app.database.assignments().getForWeek(LocalDate.now(), LocalDate.now()) }
        assertEquals(8, stored.size)
        val qa = File(app.filesDir, "qa").apply { mkdirs() }
        File(qa, "ux-pressure.txt").writeText("8 persone, 2 servizi: $taps tocchi, ${elapsed} ms, nessuno scroll nel selettore. Tempo di automazione su emulatore, non cronometraggio umano.\n")
    }
    @Test fun allInactivePeopleOfferAUsefulActionInsteadOfAnEmptyPicker() {
        val app = compose.activity.application as IlTurnoApplication
        runBlocking(Dispatchers.IO) { ids.forEach { app.repository.setEmployeeActive(it, false) } }
        compose.onNodeWithTag("service-LUNCH").performClick()
        compose.onNode(hasText(compose.activity.getString(R.string.no_active_people)) and hasAnyAncestor(hasTestTag("people-picker"))).performClick()
        compose.onAllNodesWithText(compose.activity.getString(R.string.reactivate)).onFirst().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.deactivate)).assertIsDisplayed()
    }
}
