package it.sanges.ilturno.ui.export

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.sanges.ilturno.R
import it.sanges.ilturno.util.DateUtils
import it.sanges.ilturno.util.ExportFormat
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportPicker(date: LocalDate, onDismiss: () -> Unit, onExport: (ExportFormat) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp).testTag("export-picker"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.export_week), style = MaterialTheme.typography.headlineMedium)
            Text(DateUtils.weekLabel(date), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { onExport(ExportFormat.PDF) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(R.string.pdf))
            }
            FilledTonalButton(onClick = { onExport(ExportFormat.XLSX) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(R.string.excel))
            }
        }
    }
}
