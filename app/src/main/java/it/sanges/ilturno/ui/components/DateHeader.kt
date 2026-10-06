package it.sanges.ilturno.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.sanges.ilturno.R
import it.sanges.ilturno.util.DateUtils
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun DateHeader(date: LocalDate, week: Boolean, onStep: (Int) -> Unit, onPick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val previous = stringResource(if (week) R.string.previous_week else R.string.previous_day)
        val next = stringResource(if (week) R.string.next_week else R.string.next_day)
        IconButton(onClick = { onStep(-1) }, modifier = Modifier.semantics { contentDescription = previous }) {
            Text("‹", fontSize = 36.sp)
        }
        val pickDescription = stringResource(R.string.choose_date)
        Column(
            Modifier.weight(1f).clickable(onClick = onPick).semantics { contentDescription = pickDescription }.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (week) DateUtils.weekLabel(date) else DateUtils.dayTitle(date),
                style = if (week) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium)
            if (!week) Text(DateUtils.monthYear(date), style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = { onStep(1) }, modifier = Modifier.semantics { contentDescription = next }) {
            Text("›", fontSize = 36.sp)
        }
    }
}

/** Direction follows the blueprint: left = earlier, right = later. */
@Composable
fun Modifier.dateSwipe(onStep: (Int) -> Unit): Modifier {
    val currentStep by rememberUpdatedState(onStep)
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    return pointerInput(threshold) {
        var distance = 0f
        detectHorizontalDragGestures(
            onDragStart = { distance = 0f },
            onHorizontalDrag = { change, delta -> change.consume(); distance += delta },
            onDragEnd = { if (abs(distance) >= threshold) currentStep(if (distance < 0) -1 else 1) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JumpDateDialog(date: LocalDate, onDismiss: () -> Unit, onSelect: (LocalDate) -> Unit) {
    val density = LocalDensity.current
    val windowHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val compact = windowHeight < 480.dp || density.fontScale >= 1.5f
    val picker = rememberDatePickerState(initialSelectedDateMillis = DateUtils.pickerMillis(date),
        initialDisplayMode = if (compact) DisplayMode.Input else DisplayMode.Picker)
    LaunchedEffect(compact) { if (compact) picker.displayMode = DisplayMode.Input }
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { picker.selectedDateMillis?.let { onSelect(DateUtils.pickerDate(it)) } }, enabled = picker.selectedDateMillis != null) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) {
        if (compact) DatePicker(state = picker, title = null, headline = null, showModeToggle = false,
            modifier = Modifier.verticalScroll(rememberScrollState()))
        else DatePicker(state = picker, modifier = Modifier.verticalScroll(rememberScrollState()))
    }
}
