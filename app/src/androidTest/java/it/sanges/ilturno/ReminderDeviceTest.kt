package it.sanges.ilturno

import android.app.NotificationManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import it.sanges.ilturno.notifications.ReminderPolicy
import it.sanges.ilturno.notifications.ShiftReminders
import it.sanges.ilturno.domain.model.Meal
import it.sanges.ilturno.util.DateUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime

class ReminderDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val sunday = ZonedDateTime.parse("2026-10-11T19:00:00+02:00[Europe/Rome]")
    private val context get() = compose.activity
    private val app get() = context.application as IlTurnoApplication
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    @Before fun reset() = runBlocking {
        app.database.clearAllTables()
        context.getSharedPreferences("shift_reminders", 0).edit().clear().commit()
        ShiftReminders.setEnabled(context, true)
        ShiftReminders.cancelNotification(context)
        assertTrue("Grant POST_NOTIFICATIONS on test emulator before this suite", ShiftReminders.allowed(context))
    }
    @After fun clean() {
        ShiftReminders.setEnabled(context, false)
        context.getSharedPreferences("shift_reminders", 0).edit().clear().commit()
    }
    @Test fun incompleteWeekNotifiesAndActionOpensTheWeek(): Unit = runBlocking {
        ShiftReminders.check(context, true, sunday)
        compose.waitUntil(5_000) { manager.activeNotifications.size == 1 }
        val notification = manager.activeNotifications.single().notification
        assertTrue(notification.extras.getCharSequence("android.text").toString().contains("14"))
        notification.contentIntent.send()
        compose.waitForIdle()
        compose.onNodeWithText(DateUtils.weekLabel(ReminderPolicy.nextWeek(sunday.toLocalDate()))).assertIsDisplayed()
    }
    @Test fun completeWeekStopsRegularRemindersButKeepsFinalReview() = runBlocking {
        val start = ReminderPolicy.nextWeek(sunday.toLocalDate())
        val id = app.repository.addEmployee("Anna")
        for (offset in 0L..6L) for (meal in Meal.entries) app.repository.setAssigned(start.plusDays(offset), id, meal, true)
        ShiftReminders.check(context, true, sunday)
        assertTrue(manager.activeNotifications.isEmpty())
        ShiftReminders.check(context, true, sunday.withHour(23))
        compose.waitUntil(5_000) { manager.activeNotifications.size == 1 }
        assertEquals(context.getString(R.string.reminder_final), manager.activeNotifications.single().notification.extras.getCharSequence("android.text").toString())
        ShiftReminders.setEnabled(context, false)
        compose.waitUntil(5_000) { manager.activeNotifications.isEmpty() }
        assertTrue(manager.activeNotifications.isEmpty())
    }
}
