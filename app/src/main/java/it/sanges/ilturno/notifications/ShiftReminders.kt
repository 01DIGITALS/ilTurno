package it.sanges.ilturno.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import it.sanges.ilturno.IlTurnoApplication
import it.sanges.ilturno.MainActivity
import it.sanges.ilturno.R
import it.sanges.ilturno.data.DepartmentPreferences
import it.sanges.ilturno.util.DateUtils
import java.time.ZonedDateTime

object ShiftReminders {
    const val OPEN_WEEK = "reminder_week"
    const val ACTION_REMIND = "it.sanges.ilturno.REMIND"
    private const val CHANNEL = "next_week_shifts"
    private const val NOTIFICATION = 410
    private fun prefs(context: Context) = context.getSharedPreferences("shift_reminders", Context.MODE_PRIVATE)
    fun enabled(context: Context): Boolean = prefs(context).getBoolean("enabled", false)
    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("enabled", enabled).apply()
        schedule(context)
        if (!enabled) cancelNotification(context)
    }
    fun allowed(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    fun schedule(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val next = ReminderPolicy.nextAlarm(ZonedDateTime.now())
        val pending = PendingIntent.getBroadcast(context, 410,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND)
                .putExtra("slot", ReminderPolicy.slot(next)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (!enabled(context)) { alarm.cancel(pending); return }
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), pending)
    }
    fun cancelNotification(context: Context) = context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION)

    suspend fun check(context: Context, deliver: Boolean, now: ZonedDateTime = ZonedDateTime.now()) {
        val today = now.toLocalDate()
        val start = ReminderPolicy.nextWeek(today)
        val application = context.applicationContext as IlTurnoApplication
        val assignments = application.database.assignments().getForWeek(start, start.plusDays(6))
        val departments = application.database.departments().getAll().filter { it.id != 0L }
        val grouped = DepartmentPreferences.enabled(context) && departments.any { it.isActive }
        val requiredDepartments = if (grouped) departments.filter { it.isActive }.map { it.id } else listOf(0L)
        val missing = ReminderPolicy.missingServices(today, assignments, requiredDepartments, combineDepartments = !grouped)
        val finalReview = ReminderPolicy.isFinalReview(now)
        if (!enabled(context) || !ReminderPolicy.isReminderDay(today) || (missing == 0 && !finalReview)) {
            cancelNotification(context)
            return
        }
        val slot = ReminderPolicy.slot(now) ?: return
        if (!deliver || !allowed(context) || prefs(context).getString("last_slot", null) == slot) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.reminder_channel_description)
        })
        val messageId = when (today.dayOfWeek.value) {
            4 -> R.string.reminder_thursday
            5 -> R.string.reminder_friday
            6 -> R.string.reminder_saturday
            else -> R.string.reminder_sunday
        }
        val message = if (finalReview) {
            context.getString(R.string.reminder_final) +
                (if (missing > 0) " " + context.getString(R.string.reminder_final_missing, missing) else "")
        } else {
            (if (now.hour >= 19) context.getString(R.string.reminder_evening) else "") + context.getString(messageId, missing)
        }
        val open = PendingIntent.getActivity(context, 411,
            Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .putExtra(OPEN_WEEK, start.toString())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title, DateUtils.weekLabel(start)))
            .setContentText(message).setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(open).addAction(0, context.getString(R.string.reminder_action), open)
            .setAutoCancel(true).setOnlyAlertOnce(false)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
        try {
            manager.notify(NOTIFICATION, notification)
            prefs(context).edit().putString("last_slot", slot).apply()
        } catch (_: SecurityException) { /* Permission may change during the database read. */ }
    }
}
