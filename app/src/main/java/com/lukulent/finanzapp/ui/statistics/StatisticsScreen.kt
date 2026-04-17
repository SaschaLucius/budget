package com.lukulent.finanzapp.ui.statistics

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lukulent.finanzapp.FinanzApp
import com.lukulent.finanzapp.data.model.Transaction
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    onEditTransaction: (Long) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as FinanzApp
    val viewModel: StatisticsViewModel = viewModel(
        factory = StatisticsViewModel.Factory(app.repository)
    )

    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val selectedMonths by viewModel.selectedMonths.collectAsState()
    val selectedMonthsBalance by viewModel.selectedMonthsBalance.collectAsState()

    var showFilterMenu by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showMarkSelectedDoneDialog by remember { mutableStateOf(false) }

    val hasSelection = selectedMonths.isNotEmpty()

    val filters = listOf(Filter.ThisMonth, Filter.LastMonth, Filter.LastThreeMonths, Filter.LastSixMonths, Filter.LastTwelveMonths, Filter.All)
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT) }

    val grouped = remember(transactions) {
        transactions.groupBy { YearMonth.from(it.date) }
            .toSortedMap(compareByDescending { it })
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Statistik") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearSelection()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                if (hasSelection) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selectedMonths.size} Monat(e) ausgewählt",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Summe: $selectedMonthsBalance",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Row {
                            TextButton(onClick = { viewModel.clearSelection() }) {
                                Text("Abbrechen")
                            }
                            Button(onClick = { showMarkSelectedDoneDialog = true }) {
                                Text("Erledigen")
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Gesamt: $balance",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                FilterChip(
                    selected = true,
                    onClick = { showFilterMenu = true },
                    label = { Text(selectedFilter.label) }
                )
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    filters.forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(filter.label) },
                            onClick = {
                                viewModel.setFilter(filter)
                                showFilterMenu = false
                            }
                        )
                    }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                grouped.forEach { (yearMonth, monthTransactions) ->
                    val monthBalance = monthTransactions
                        .filter { !it.isDone }
                        .sumOf { if (it.isExpense) -it.amount else it.amount }
                    val monthAllDone = monthTransactions.all { it.isDone }
                    val monthSelected = yearMonth in selectedMonths
                    item(key = "header_$yearMonth") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (monthSelected) MaterialTheme.colorScheme.surfaceVariant
                                    else Color.Transparent
                                )
                                .combinedClickable(
                                    onClick = { if (!monthAllDone) viewModel.toggleMonthSelection(yearMonth) },
                                    onLongClick = { if (!monthAllDone) viewModel.toggleMonthSelection(yearMonth) }
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)} ${yearMonth.year}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (monthAllDone) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Alle erledigt"
                                    )
                                }
                            }
                            if (!monthAllDone) {
                                Text(
                                    text = if (monthBalance >= 0) "+$monthBalance" else "$monthBalance",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    items(
                        items = monthTransactions,
                        key = { it.id }
                    ) { transaction ->
                        val textDecoration = if (transaction.isDone) TextDecoration.LineThrough else null
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onEditTransaction(transaction.id) },
                                    onLongClick = { transactionToDelete = transaction }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(32.dp))
                                    Text(
                                        text = transaction.date.format(dateFormatter),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textDecoration = textDecoration
                                    )
                                }
                                Text(
                                    text = "${if (transaction.isExpense) "−" else "+"}${transaction.amount}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textDecoration = textDecoration
                                )
                            }
                            if (!transaction.subject.isNullOrBlank()) {
                                Text(
                                    text = transaction.subject,
                                    style = MaterialTheme.typography.bodySmall,
                                    textDecoration = textDecoration,
                                    modifier = Modifier.padding(start = 32.dp)
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    transactionToDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Löschen") },
            text = { Text("Transaktion wirklich löschen?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(transaction)
                    transactionToDelete = null
                }) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showMarkSelectedDoneDialog) {
        AlertDialog(
            onDismissRequest = { showMarkSelectedDoneDialog = false },
            title = { Text("Monate erledigen") },
            text = {
                val names = selectedMonths
                    .sortedDescending()
                    .joinToString(", ") {
                        "${it.month.getDisplayName(TextStyle.SHORT, Locale.GERMAN)} ${it.year}"
                    }
                Text("Alle Einträge in $names als erledigt markieren?")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markSelectedMonthsDone()
                    showMarkSelectedDoneDialog = false
                }) {
                    Text("Erledigen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkSelectedDoneDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}
