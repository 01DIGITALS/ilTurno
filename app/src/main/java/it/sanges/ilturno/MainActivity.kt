package it.sanges.ilturno

import android.os.Bundle
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import it.sanges.ilturno.notifications.ShiftReminders
import java.time.LocalDate
import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import it.sanges.ilturno.ui.NotebookScreen
import it.sanges.ilturno.ui.NotebookViewModel
import it.sanges.ilturno.ui.theme.IlTurnoTheme
import it.sanges.ilturno.util.DebugDiagnostics

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration).apply { setLocale(Locale.ITALIAN) }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DebugDiagnostics.event("activity.create sdk=${android.os.Build.VERSION.SDK_INT} restored=${savedInstanceState != null}")
        enableEdgeToEdge()
        val viewModel = ViewModelProvider(this)[NotebookViewModel::class.java]
        openReminderWeek(intent, viewModel)
        setContent {
            IlTurnoTheme { NotebookScreen(viewModel) }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openReminderWeek(intent, ViewModelProvider(this)[NotebookViewModel::class.java])
    }
    private fun openReminderWeek(intent: Intent, viewModel: NotebookViewModel) {
        val date = intent.getStringExtra(ShiftReminders.OPEN_WEEK) ?: return
        intent.removeExtra(ShiftReminders.OPEN_WEEK)
        runCatching { LocalDate.parse(date) }.getOrNull()?.let {
            viewModel.selectDate(it)
            viewModel.showWeek(true)
        }
    }
    override fun onResume() {
        super.onResume()
        ShiftReminders.schedule(this)
        lifecycleScope.launch {
            runCatching { ShiftReminders.check(this@MainActivity, deliver = false) }
                .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it; if (it is Exception) DebugDiagnostics.failure("reminder.refresh", it) }
        }
    }
}
