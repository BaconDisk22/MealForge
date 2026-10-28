package org.mealforge.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import org.mealforge.app.data.PantryItemEntity
import org.mealforge.app.domain.PantryMatch
import org.mealforge.app.domain.Quantities
import org.mealforge.app.ui.AppViewModel
import org.mealforge.app.ui.EmptyState
import org.mealforge.app.ui.InfoCard
import org.mealforge.app.ui.MealForgeTopBar
import org.mealforge.app.ui.NumberField
import org.mealforge.app.ui.Routes
import org.mealforge.app.ui.SectionHeader
import org.mealforge.app.ui.SpacerH
import org.mealforge.app.ui.screenPadding
import org.mealforge.app.ui.theme.Brand
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val expiryFmt = DateTimeFormatter.ISO_LOCAL_DATE

@Composable
fun PantryScreen(vm: AppViewModel, nav: NavHostController) {
    val pantry by vm.pantry.collectAsStateWithLifecycle()
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<PantryItemEntity?>(null) }
    val today = vm.today.toEpochDay()
    val names = pantry.map { it.name }
    val ideas = recipes
        .map { it to PantryMatch.coverage(it.ingredients.map { i -> i.name }, names) }
        .filter { it.second >= 0.4 }
        .sortedByDescending { it.second }
        .take(5)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            MealForgeTopBar("Pantry", onBack = { nav.popBackStack() })
            LazyColumn(Modifier.screenPadding, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (ideas.isNotEmpty()) {
                    item { SectionHeader("Cook with what you have") }
                    items(ideas, key = { "idea" + it.first.recipe.id }) { (r, cov) ->
                        InfoCard(onClick = { nav.navigate(Routes.recipe(r.recipe.id)) }) {
                            Text(r.recipe.name, style = MaterialTheme.typography.titleMedium)
                            Text("You have ${(cov * 100).toInt()}% of the ingredients", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { SectionHeader("On hand (${pantry.size})") }
                if (pantry.isEmpty()) {
                    item { EmptyState("Pantry is empty", "Add staples you keep at home. They're skipped when building grocery lists from your plan.") }
                }
                items(pantry, key = { it.id }) { p ->
                    val daysLeft = p.expiresOnEpochDay?.let { it - today }
                    Row(Modifier.fillMaxWidth().clickable { editing = p }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name)
                            Text(
                                if (p.quantity == null) "Plenty" else Quantities.describe(p.quantity, p.unit),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (daysLeft != null) {
                            Text(
                                when {
                                    daysLeft < 0 -> "Expired"
                                    daysLeft == 0L -> "Expires today"
                                    daysLeft == 1L -> "1 day left"
                                    else -> "$daysLeft days left"
                                },
                                color = when {
                                    daysLeft < 0 -> Brand.Red
                                    daysLeft <= 3 -> Brand.Pink
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
                item { SpacerH(88) }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { editing = PantryItemEntity(name = "") },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add to pantry") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    editing?.let { item ->
        var name by remember { mutableStateOf(item.name) }
        var qty by remember { mutableStateOf(item.quantity?.let(Quantities::format) ?: "") }
        var unit by remember { mutableStateOf(item.unit) }
        var expires by remember { mutableStateOf(item.expiresOnEpochDay?.let { LocalDate.ofEpochDay(it).format(expiryFmt) } ?: "") }
        val parsedExpiry = try { expires.trim().takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it, expiryFmt) } } catch (_: DateTimeParseException) { null }
        val expiryError = expires.isNotBlank() && parsedExpiry == null
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (item.id == 0L) "Add to pantry" else "Edit pantry item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NumberField("Qty (blank = plenty)", qty, { qty = it }, Modifier.weight(1f))
                        OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, singleLine = true, modifier = Modifier.weight(0.7f))
                    }
                    OutlinedTextField(
                        value = expires, onValueChange = { expires = it }, label = { Text("Expires (YYYY-MM-DD, optional)") }, singleLine = true,
                        isError = expiryError, supportingText = if (expiryError) ({ Text("Use a date like ${vm.today.plusDays(7).format(expiryFmt)}") }) else null,
                    )
                    if (item.id != 0L) TextButton(onClick = { vm.deletePantryItem(item); editing = null }) { Text("Remove from pantry", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank() && !expiryError, onClick = {
                    vm.savePantryItem(item.copy(name = name, quantity = Quantities.parse(qty), unit = unit, expiresOnEpochDay = parsedExpiry?.toEpochDay()))
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}
