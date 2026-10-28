package org.mealforge.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.mealforge.app.data.MealSlot
import org.mealforge.app.data.sortedIngredients
import org.mealforge.app.domain.Allergens
import org.mealforge.app.domain.GroceryBuilder
import org.mealforge.app.domain.Preferences
import org.mealforge.app.domain.Quantities
import org.mealforge.app.ui.AppViewModel
import org.mealforge.app.ui.ConfirmDialog
import org.mealforge.app.ui.InfoCard
import org.mealforge.app.ui.MealForgeTopBar
import org.mealforge.app.ui.Pill
import org.mealforge.app.ui.Routes
import org.mealforge.app.ui.SectionHeader
import org.mealforge.app.ui.SpacerH
import org.mealforge.app.ui.Stepper
import org.mealforge.app.ui.rememberUriImage
import org.mealforge.app.ui.screenPadding
import org.mealforge.app.ui.theme.Brand
import java.time.format.DateTimeFormatter

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailScreen(vm: AppViewModel, nav: NavHostController, id: Long) {
    val data by remember(id) { vm.repo.recipe(id) }.collectAsState(initial = null)
    val settings by vm.settings.collectAsStateWithLifecycle()
    val members by vm.members.collectAsStateWithLifecycle()
    val r = data
    var servings by rememberSaveable(id) { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    var planDialog by remember { mutableStateOf(false) }
    val doneSteps = remember(id) { mutableStateListOf<Int>() }

    Column(Modifier.fillMaxSize()) {
        MealForgeTopBar(r?.recipe?.name ?: "Recipe", onBack = { nav.popBackStack() }, actions = {
            if (r != null) {
                IconButton(onClick = { vm.setFavorite(id, !r.recipe.favorite) }) {
                    Icon(
                        if (r.recipe.favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = if (r.recipe.favorite) "Remove from favorites" else "Add to favorites",
                        tint = Brand.Gold,
                    )
                }
                IconButton(onClick = { nav.navigate(Routes.editRecipe(id)) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit recipe") }
                IconButton(onClick = { vm.duplicateRecipe(id) { nav.navigate(Routes.editRecipe(it)) } }) { Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate recipe") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete recipe") }
            }
        })
        if (r == null) return@Column
        if (servings == 0) servings = r.recipe.servings
        val ingredients = r.sortedIngredients
        val allergens = Allergens.detect(ingredients.map { it.name })
        val conflicts = Preferences.conflicts(ingredients.map { it.name }, settings.exclusions + members.flatMap { it.exclusions })
        val photo = rememberUriImage(r.recipe.photoUri)

        LazyColumn(Modifier.screenPadding) {
            if (photo != null) {
                item {
                    Image(photo, contentDescription = "Photo of ${r.recipe.name}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)))
                    SpacerH(8)
                }
            }
            item {
                if (r.recipe.description.isNotBlank()) Text(r.recipe.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SpacerH(8)
                FlowRow {
                    Pill(r.recipe.category)
                    if (r.recipe.prepMinutes > 0) Pill("Prep ${r.recipe.prepMinutes} min")
                    if (r.recipe.cookMinutes > 0) Pill("Cook ${r.recipe.cookMinutes} min")
                    if (r.recipe.prepMinutes + r.recipe.cookMinutes > 0) Pill("Total ${r.recipe.prepMinutes + r.recipe.cookMinutes} min")
                    r.recipe.tags.forEach { Pill(it, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer) }
                }
                if (conflicts.isNotEmpty()) {
                    InfoCard(container = MaterialTheme.colorScheme.errorContainer) {
                        Text("Conflicts with your preferences: ${conflicts.joinToString()}", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    SpacerH(8)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { planDialog = true }) { Text("Add to plan") }
                    OutlinedButton(onClick = { vm.addRecipeToGroceries(r, servings) }) { Text("Add to groceries") }
                }
            }
            item {
                SectionHeader("Ingredients") {
                    Stepper(servings, { servings = it }, "Servings")
                }
                Text("For $servings servings" + if (servings != r.recipe.servings) " (recipe makes ${r.recipe.servings})" else "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                SpacerH(4)
            }
            itemsIndexed(ingredients, key = { _, ing -> "i" + ing.id }) { _, ing ->
                val qty = GroceryBuilder.scale(ing.quantity, r.recipe.servings, servings)
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(Quantities.describe(qty, ing.unit).ifEmpty { "•" }, color = Brand.Gold, modifier = Modifier.weight(0.3f))
                    Text(ing.name + if (ing.note.isNotBlank()) ", ${ing.note}" else "", modifier = Modifier.weight(0.7f))
                }
            }
            if (allergens.isNotEmpty()) {
                item {
                    SpacerH(6)
                    Text("May contain: ${allergens.joinToString()}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                    Text("Detected from ingredient names. Always check product labels.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            item { SectionHeader("Steps") }
            if (r.recipe.steps.isEmpty()) item { Text("No steps yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            itemsIndexed(r.recipe.steps, key = { i, _ -> "s$i" }) { index, step ->
                val done = index in doneSteps
                Row(
                    Modifier.fillMaxWidth().clickable { if (done) doneSteps.remove(index) else doneSteps.add(index) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Checkbox(checked = done, onCheckedChange = { if (it) doneSteps.add(index) else doneSteps.remove(index) })
                    Column(Modifier.padding(top = 12.dp)) {
                        Text("Step ${index + 1}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        Text(
                            step,
                            textDecoration = if (done) TextDecoration.LineThrough else null,
                            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            item {
                SectionHeader("Nutrition per serving")
                val n = r.recipe
                if (n.caloriesPerServing == null && n.proteinPerServing == null && n.carbsPerServing == null && n.fatPerServing == null) {
                    Text("Not provided. Edit the recipe to add nutrition values.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NutrientCell("Calories", n.caloriesPerServing?.toString())
                        NutrientCell("Protein", n.proteinPerServing?.let { "${Quantities.format(it)} g" })
                        NutrientCell("Carbs", n.carbsPerServing?.let { "${Quantities.format(it)} g" })
                        NutrientCell("Fat", n.fatPerServing?.let { "${Quantities.format(it)} g" })
                    }
                    if (n.nutritionEstimated) Text("Estimated values", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                SpacerH(32)
            }
        }
    }

    if (confirmDelete && r != null) {
        ConfirmDialog(
            "Delete ${r.recipe.name}?",
            "This also removes it from your meal plan and templates. This can't be undone.",
            "Delete",
            onConfirm = { vm.deleteRecipe(id); nav.popBackStack() },
            onDismiss = { confirmDelete = false },
        )
    }
    if (planDialog && r != null) {
        PlanRecipeDialog(vm, servings, onDismiss = { planDialog = false }) { day, slot -> vm.addMeal(day, slot, id, servings) }
    }
}

@Composable
private fun NutrientCell(label: String, value: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value ?: "—", style = MaterialTheme.typography.titleMedium, color = Brand.Gold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PlanRecipeDialog(vm: AppViewModel, servings: Int, onDismiss: () -> Unit, onAdd: (java.time.LocalDate, MealSlot) -> Unit) {
    val today = vm.today
    var day by remember { mutableStateOf(today) }
    var slot by remember { mutableStateOf(MealSlot.DINNER) }
    val fmt = DateTimeFormatter.ofPattern("EEE d")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to plan · $servings servings") },
        text = {
            Column {
                Text("Day", style = MaterialTheme.typography.labelLarge)
                FlowRow {
                    (0..13).map { today.plusDays(it.toLong()) }.forEach { d ->
                        FilterChip(selected = d == day, onClick = { day = d }, label = { Text(d.format(fmt)) }, modifier = Modifier.padding(end = 4.dp))
                    }
                }
                Text("Meal", style = MaterialTheme.typography.labelLarge)
                FlowRow {
                    MealSlot.entries.forEach { s ->
                        FilterChip(selected = s == slot, onClick = { slot = s }, label = { Text(s.label) }, modifier = Modifier.padding(end = 4.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(day, slot); vm.toast("Added to ${day.format(fmt)} ${slot.label.lowercase()}"); onDismiss() }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
