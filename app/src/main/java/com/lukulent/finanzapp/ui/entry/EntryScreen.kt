package com.lukulent.finanzapp.ui.entry

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lukulent.finanzapp.FinanzApp
import com.lukulent.finanzapp.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    transactionId: Long?,
    onNavigateToStatistics: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as FinanzApp
    val viewModel: EntryViewModel = viewModel(
        factory = EntryViewModel.Factory(app.repository, app.settingsDataStore)
    )

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            viewModel.load(transactionId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is EntryViewModel.UiEvent.NavigateBack -> onNavigateBack()
                is EntryViewModel.UiEvent.CloseApp -> (context as Activity).finish()
            }
        }
    }

    val amount by viewModel.amount.collectAsState()
    val subject by viewModel.subject.collectAsState()
    val date by viewModel.date.collectAsState()
    val amountError by viewModel.amountError.collectAsState()
    val closeOnEntry by viewModel.closeOnEntry.collectAsState()
    val isDone by viewModel.isDone.collectAsState()
    val isExpense by viewModel.isExpense.collectAsState()
    val editingId by viewModel.editingId.collectAsState()

    val isEditing = editingId != null
    var showDatePicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Bearbeiten" else "Budget") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = { viewModel.setAmount(it) },
                label = { Text("Betrag") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = amountError != null,
                supportingText = amountError?.let { error -> { Text(error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp)
            )

            OutlinedTextField(
                value = subject,
                onValueChange = { viewModel.setSubject(it) },
                label = { Text("Betreff (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = date.format(dateFormatter),
                onValueChange = {},
                label = { Text("Datum") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Datum wählen")
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isEditing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isDone,
                        onCheckedChange = { viewModel.setIsDone(it) }
                    )
                    Text("Erledigt")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activeColors = ButtonDefaults.buttonColors()
                    val inactiveColors = ButtonDefaults.outlinedButtonColors()
                    OutlinedButton(
                        onClick = { viewModel.setIsExpense(false) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = if (!isExpense) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("+", fontSize = 24.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.setIsExpense(true) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = if (isExpense) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("−", fontSize = 24.sp)
                    }
                }
                Button(
                    onClick = { viewModel.save(isExpense = isExpense) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Speichern", fontSize = 18.sp)
                }
            } else {
                Button(
                    onClick = { viewModel.save(isExpense = false) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("+", fontSize = 24.sp)
                }

                Button(
                    onClick = { viewModel.save(isExpense = true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("−", fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onNavigateToStatistics,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bar_chart),
                    contentDescription = "Statistik"
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        viewModel.setDate(selected)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Einstellungen") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("App bei Eingabe schließen", modifier = Modifier.weight(1f))
                    Switch(
                        checked = closeOnEntry,
                        onCheckedChange = { viewModel.setCloseOnEntry(it) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Fertig")
                }
            }
        )
    }
}
