package it.sanges.ilturno.ui.info

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.sanges.ilturno.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.info)) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } })
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            InfoItem(stringResource(R.string.version), version)
            InfoItem(stringResource(R.string.updates), stringResource(R.string.updates_manual))
            InfoItem(stringResource(R.string.developer), "LouisBigDev")
            Text(stringResource(R.string.special_thanks), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.personal_use), style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()
            ReminderSettings()
        }
    }
}

@Composable
private fun InfoItem(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
