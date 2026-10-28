package org.mealforge.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.mealforge.app.domain.BudgetPeriod
import org.mealforge.app.domain.Money
import org.mealforge.app.ui.AppViewModel
import org.mealforge.app.ui.InfoCard
import org.mealforge.app.ui.MealForgeTopBar
import org.mealforge.app.ui.NumberField
import org.mealforge.app.ui.SectionHeader
import org.mealforge.app.ui.SpacerH
import org.mealforge.app.ui.screenPadding
import org.mealforge.app.ui.theme.Brand
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BudgetScreen(vm: AppViewModel, nav: NavHostController) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val spending by vm.spending.collectAsStateWithLifecycle()
    val items by vm.groceryItems.collectAsStateWithLifecycle()
    var editBudget by remember { mutableStateOf(false) }
    var addSpend by remember { mutableStateOf(false) }
    val cur = settings.currencySymbol
    val range = vm.repo.periodRange(vm.today, settings.budgetPeriod)
    val inPeriod = spending.filter { it.epochDay in range.start.toEpochDay()..range.endInclusive.toEpochDay() }
    val actual = inPeriod.sumOf { it.amountCents }
    val estimate = items.filter { !it.checked }.sumOf { it.estimatedPriceCents ?: 0L }
    val unpriced = items.count { !it.checked && it.estimatedPriceCents == null }
    val fmt = DateTimeFormatter.ofPattern("EEE d MMM")

    Column(Modifier.fillMaxSize()) {
        MealForgeTopBar("Budget", onBack = { nav.popBackStack() })
        LazyColumn(Modifier.screenPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                InfoCard {
                    Text("This ${settings.budgetPeriod.name.lowercase()} · ${range.start.format(fmt)} – ${range.endInclusive.format(fmt)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SpacerH(4)
                    val budget = settings.budgetCents
                    if (budget != null) {
                        val over = actual > budget
                        Text("${Money.format(actual, cur)} of ${Money.format(budget, cur)}", style = MaterialTheme.typography.titleLarge)
                        SpacerH(6)
                        LinearProgressIndicator(
                            progress = { if (budget == 0L) 1f else (actual.toFloat() / budget).coerceIn(0f, 1f) },
                            color = if (over) Brand.Red else Brand.Gold,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SpacerH(6)
                        Text(if (over) "Over budget by ${Money.format(actual - budget, cur)}" else "${Money.format(budget - actual, cur)} left", color = if (over) Brand.Red else Brand.Success)
                        if (estimate > 0) {
                            val projected = actual + estimate
                            Text(
                                "Current list estimate: ${Money.format(estimate, cur)} → projected ${Money.format(projected, cur)}" + if (projected > budget) " (over)" else "",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Text("Spent ${Money.format(actual, cur)}", style = MaterialTheme.typography.titleLarge)
                        Text("No budget set yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (unpriced > 0) Text("$unpriced list items have no estimated price.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SpacerH(8)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { addSpend = true }) { Text("Log spending") }
                        OutlinedButton(onClick = { editBudget = true }) { Text(if (budget == null) "Set budget" else "Change budget") }
                    }
                }
            }
            item { SectionHeader("Estimated vs actual") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("Estimated (list)", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(Money.format(estimate, cur), style = MaterialTheme.typography.titleMedium) }
                    Column(horizontalAlignment = Alignment.End) { Text("Actual (period)", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(Money.format(actual, cur), style = MaterialTheme.typography.titleMedium) }
                }
            }
            item { SectionHeader("Spending history") }
            if (spending.isEmpty()) item { Text("Nothing logged yet. Use Finish shopping on your grocery list, or log spending here.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(spending, key = { it.id }) { e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.note.ifBlank { "Groceries" })
                        Text(LocalDate.ofEpochDay(e.epochDay).format(fmt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(Money.format(e.amountCents, cur), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { vm.deleteSpend(e) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
            }
            item { SpacerH(24) }
        }
    }

    if (editBudget) {
        var amount by remember { mutableStateOf(settings.budgetCents?.let { Money.format(it, "") } ?: "") }
        var period by remember { mutableStateOf(settings.budgetPeriod) }
        AlertDialog(
            onDismissRequest = { editBudget = false },
            title = { Text("Grocery budget") },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BudgetPeriod.entries.forEach { p -> FilterChip(selected = p == period, onClick = { period = p }, label = { Text(if (p == BudgetPeriod.WEEK) "Weekly" else "Monthly") }) }
                    }
                    NumberField("Amount ($cur), blank for none", amount, { amount = it })
                }
            },
            confirmButton = { TextButton(onClick = { vm.setBudget(Money.parseCents(amount), period); editBudget = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editBudget = false }) { Text("Cancel") } },
        )
    }
    if (addSpend) {
        var amount by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        val cents = Money.parseCents(amount)
        AlertDialog(
            onDismissRequest = { addSpend = false },
            title = { Text("Log spending") },
            text = {
                Column {
                    NumberField("Amount ($cur)", amount, { amount = it })
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Store or note") }, singleLine = true)
                }
            },
            confirmButton = { TextButton(enabled = cents != null && cents > 0, onClick = { vm.addSpend(cents ?: 0, note); addSpend = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { addSpend = false }) { Text("Cancel") } },
        )
    }
}
