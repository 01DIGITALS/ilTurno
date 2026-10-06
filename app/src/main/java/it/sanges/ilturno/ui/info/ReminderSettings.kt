package it.sanges.ilturno.ui.info

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import it.sanges.ilturno.R
import it.sanges.ilturno.notifications.ShiftReminders

@Composable
fun ReminderSettings() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val reminderLabel = stringResource(R.string.reminders)
    var enabled by remember { mutableStateOf(ShiftReminders.enabled(context)) }
    var allowed by remember { mutableStateOf(ShiftReminders.allowed(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = ShiftReminders.allowed(context)
    }
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allowed = ShiftReminders.allowed(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.reminders), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(top = 12.dp))
            Switch(checked = enabled, modifier = Modifier.semantics { contentDescription = reminderLabel }, onCheckedChange = {
                enabled = it
                ShiftReminders.setEnabled(context, it)
                if (it && Build.VERSION.SDK_INT >= 33 && !allowed) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            })
        }
        Text(stringResource(R.string.reminders_description), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.reminders_timing), style = MaterialTheme.typography.bodySmall)
        if (enabled && !allowed) {
            Text(stringResource(R.string.reminders_permission), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }) { Text(stringResource(R.string.notification_settings)) }
        }
    }
}
