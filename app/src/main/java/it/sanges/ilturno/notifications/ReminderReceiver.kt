package it.sanges.ilturno.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import it.sanges.ilturno.util.DebugDiagnostics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ShiftReminders.schedule(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { ShiftReminders.check(context, deliver = intent.action == ShiftReminders.ACTION_REMIND &&
                intent.getStringExtra("slot") == ReminderPolicy.slot(ZonedDateTime.now())) }
            catch (error: Exception) { DebugDiagnostics.failure("reminder.check", error) }
            finally { pending.finish() }
        }
    }
}
