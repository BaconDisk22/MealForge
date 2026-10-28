package org.mealforge.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import org.mealforge.app.data.AppSettings
import org.mealforge.app.data.HouseholdMemberEntity
import org.mealforge.app.domain.Allergens
import org.mealforge.app.domain.Preferences
import org.mealforge.app.domain.Quantities
import org.mealforge.app.ui.AppViewModel
import org.mealforge.app.ui.InfoCard
import org.mealforge.app.ui.MealForgeTopBar
import org.mealforge.app.ui.SectionHeader
import org.mealforge.app.ui.SpacerH
import org.mealforge.app.ui.Stepper
import org.mealforge.app.ui.screenPadding
import kotlin.math.ceil

private val portions = listOf(0.5, 0.75, 1.0, 1.5, 2.0)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun HouseholdScreen(vm: AppViewModel, nav: NavHostController) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val members by vm.members.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<HouseholdMemberEntity?>(null) }
    var exclusions by remember(settings.exclusions) { mutableStateOf(settings.exclusions.joinToString(", ")) }

    Column(Modifier.fillMaxSize()) {
        MealForgeTopBar("Household", onBack = { nav.popBackStack() })
        LazyColumn(Modifier.screenPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                InfoCard {
                    if (members.isEmpty()) {
                        Text("Default servings per meal")
                        Stepper(settings.householdServings, { vm.setServings(it) }, "Servings")
                        Text("Add household members for per-person portions.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    } else {
                        val total = members.sumOf { it.portion }
                        Text("New meals default to ${ceil(total).toInt()} servings", style = MaterialTheme.typography.titleMedium)
                        Text("Total of everyone's portions (${Quantities.format(total)})", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { SectionHeader("Members") { TextButton(onClick = { editing = HouseholdMemberEntity(name = "") }) { Text("+ Add") } } }
            items(members, key = { it.id }) { m ->
                Row(Modifier.fillMaxWidth().clickable { editing = m }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.name)
                        if (m.exclusions.isNotEmpty()) Text("Avoids: ${m.exclusions.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Text("${Quantities.format(m.portion)} serving", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                SectionHeader("Household food exclusions")
                Text("Recipes containing these are flagged and can be filtered out. Separate with commas.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = exclusions, onValueChange = { exclusions = it }, label = { Text("e.g. peanut, shrimp, pork") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.setSetting(AppSettings.EXCLUSIONS, Preferences.parseList(exclusions).joinToString(",")); vm.toast("Exclusions saved") }) { Text("Save exclusions") }
                }
                SpacerH(8)
                Text("Quick add an allergen:", style = MaterialTheme.typography.labelLarge)
                FlowRow {
                    listOf("peanut", "almond", "milk", "egg", "wheat", "soy", "shrimp", "fish", "sesame", "pork", "beef").forEach { term ->
                        FilterChip(
                            selected = Preferences.parseList(exclusions).any { it.equals(term, true) },
                            onClick = {
                                val list = Preferences.parseList(exclusions).toMutableList()
                                if (!list.removeAll { it.equals(term, true) }) list.add(term)
                                exclusions = list.joinToString(", ")
                            },
                            label = { Text(term) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
                Text("Allergen flags cover: ${Allergens.names.joinToString()}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                SectionHeader("Sharing")
                Text(
                    "MealForge currently keeps everything on this phone with no account. To move data between phones, use Settings → Back up, then Restore on the other device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SpacerH(24)
            }
        }
    }

    editing?.let { m ->
        var name by remember { mutableStateOf(m.name) }
        var portion by remember { mutableStateOf(m.portion) }
        var excl by remember { mutableStateOf(m.exclusions.joinToString(", ")) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (m.id == 0L) "Add member" else "Edit member") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                    Text("Portion size", style = MaterialTheme.typography.labelLarge)
                    FlowRow {
                        portions.forEach { p -> FilterChip(selected = portion == p, onClick = { portion = p }, label = { Text(Quantities.format(p)) }, modifier = Modifier.padding(end = 4.dp)) }
                    }
                    OutlinedTextField(value = excl, onValueChange = { excl = it }, label = { Text("Avoids (comma separated)") })
                    if (m.id != 0L) TextButton(onClick = { vm.deleteMember(m); editing = null }) { Text("Remove member", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = { vm.saveMember(m.copy(name = name, portion = portion, exclusions = Preferences.parseList(excl))); editing = null }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}
