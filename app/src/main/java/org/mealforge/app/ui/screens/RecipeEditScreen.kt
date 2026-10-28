package org.mealforge.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.first
import org.mealforge.app.data.IngredientEntity
import org.mealforge.app.data.RecipeEntity
import org.mealforge.app.data.sortedIngredients
import org.mealforge.app.domain.Departments
import org.mealforge.app.domain.Quantities
import org.mealforge.app.ui.AppViewModel
import org.mealforge.app.ui.ChoiceField
import org.mealforge.app.ui.MealForgeTopBar
import org.mealforge.app.ui.NumberField
import org.mealforge.app.ui.Routes
import org.mealforge.app.ui.SectionHeader
import org.mealforge.app.ui.SpacerH
import org.mealforge.app.ui.rememberUriImage
import org.mealforge.app.ui.screenPadding

internal data class IngredientRow(val qty: String = "", val unit: String = "", val name: String = "", val department: String = "", val note: String = "")

/** Unsaved editor state; lives as long as the edit screen's back-stack entry, so rotation keeps edits. */
class RecipeDraft : ViewModel() {
    var loaded by mutableStateOf(false)
    var original by mutableStateOf<RecipeEntity?>(null)
    var name by mutableStateOf("")
    var description by mutableStateOf("")
    var category by mutableStateOf("Dinner")
    var tags by mutableStateOf("")
    var servings by mutableStateOf("2")
    var prep by mutableStateOf("")
    var cook by mutableStateOf("")
    var calories by mutableStateOf("")
    var protein by mutableStateOf("")
    var carbs by mutableStateOf("")
    var fat by mutableStateOf("")
    var estimated by mutableStateOf(false)
    var photoUri by mutableStateOf<String?>(null)
    internal val ingredients = mutableStateListOf(IngredientRow())
    val steps = mutableStateListOf("")
}

private val categories = listOf("Breakfast", "Lunch", "Dinner", "Snack", "Dessert", "Side", "Drink")

@Composable
fun RecipeEditScreen(vm: AppViewModel, nav: NavHostController, id: Long) {
    val context = LocalContext.current
    val d: RecipeDraft = viewModel(key = "recipe-draft-$id")
    var showErrors by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != 0L) {
            val r = vm.repo.recipe(id).first() ?: return@LaunchedEffect
            d.original = r.recipe
            if (!d.loaded) {
                d.name = r.recipe.name; d.description = r.recipe.description; d.category = r.recipe.category
                d.tags = r.recipe.tags.joinToString(", "); d.servings = r.recipe.servings.toString()
                d.prep = r.recipe.prepMinutes.takeIf { it > 0 }?.toString() ?: ""
                d.cook = r.recipe.cookMinutes.takeIf { it > 0 }?.toString() ?: ""
                d.calories = r.recipe.caloriesPerServing?.toString() ?: ""
                d.protein = r.recipe.proteinPerServing?.let { Quantities.format(it) } ?: ""
                d.carbs = r.recipe.carbsPerServing?.let { Quantities.format(it) } ?: ""
                d.fat = r.recipe.fatPerServing?.let { Quantities.format(it) } ?: ""
                d.estimated = r.recipe.nutritionEstimated
                d.photoUri = r.recipe.photoUri
                d.ingredients.clear()
                d.ingredients.addAll(r.sortedIngredients.map { IngredientRow(it.quantity?.let(Quantities::format) ?: "", it.unit, it.name, it.department, it.note) })
                if (d.ingredients.isEmpty()) d.ingredients.add(IngredientRow())
                d.steps.clear(); d.steps.addAll(r.recipe.steps.ifEmpty { listOf("") })
                d.loaded = true
            }
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            d.photoUri = uri.toString()
        }
    }
    val photo = rememberUriImage(d.photoUri)
    val nameError = showErrors && d.name.isBlank()
    val servingsValue = d.servings.toIntOrNull()
    val servingsError = showErrors && (servingsValue == null || servingsValue < 1)

    fun save() {
        showErrors = true
        if (d.name.isBlank() || servingsValue == null || servingsValue < 1) return
        val recipe = (d.original ?: RecipeEntity(name = "")).copy(
            id = id,
            name = d.name,
            description = d.description.trim(),
            category = d.category,
            tags = d.tags.split(',').map { it.trim() }.filter { it.isNotEmpty() },
            servings = servingsValue,
            prepMinutes = d.prep.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            cookMinutes = d.cook.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            steps = d.steps.toList(),
            caloriesPerServing = d.calories.toIntOrNull(),
            proteinPerServing = Quantities.parse(d.protein),
            carbsPerServing = Quantities.parse(d.carbs),
            fatPerServing = Quantities.parse(d.fat),
            nutritionEstimated = d.estimated,
            photoUri = d.photoUri,
        )
        val rows = d.ingredients.filter { it.name.isNotBlank() }.mapIndexed { i, row ->
            IngredientEntity(
                recipeId = id, position = i, name = row.name, quantity = Quantities.parse(row.qty), unit = row.unit.trim(),
                department = row.department.takeIf { it.isNotBlank() && it != "Other" } ?: Departments.guess(row.name), note = row.note.trim(),
            )
        }
        vm.saveRecipe(recipe, rows) { savedId ->
            nav.popBackStack()
            if (id == 0L) nav.navigate(Routes.recipe(savedId))
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        MealForgeTopBar(if (id == 0L) "New recipe" else "Edit recipe", onBack = { nav.popBackStack() }, actions = {
            TextButton(onClick = { save() }) { Text("Save") }
        })
        if (id != 0L && !d.loaded) return@Column
        LazyColumn(Modifier.screenPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                OutlinedTextField(
                    value = d.name, onValueChange = { d.name = it }, label = { Text("Recipe name *") }, singleLine = true,
                    isError = nameError, supportingText = if (nameError) ({ Text("Give your recipe a name") }) else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(value = d.description, onValueChange = { d.description = it }, label = { Text("Short description") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceField("Category", d.category, categories, { d.category = it }, Modifier.weight(1f))
                    NumberField("Serves *", d.servings, { d.servings = it.filter(Char::isDigit).take(2) }, Modifier.weight(0.6f), decimal = false)
                }
                if (servingsError) Text("Servings must be at least 1", color = MaterialTheme.colorScheme.error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Prep (min)", d.prep, { d.prep = it.filter(Char::isDigit).take(4) }, Modifier.weight(1f), decimal = false)
                    NumberField("Cook (min)", d.cook, { d.cook = it.filter(Char::isDigit).take(4) }, Modifier.weight(1f), decimal = false)
                }
                OutlinedTextField(value = d.tags, onValueChange = { d.tags = it }, label = { Text("Tags (comma separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                SpacerH(4)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickPhoto.launch(arrayOf("image/*")) }) { Text(if (d.photoUri == null) "Add photo" else "Change photo") }
                    if (d.photoUri != null) TextButton(onClick = { d.photoUri = null }) { Text("Remove photo") }
                }
                if (photo != null) {
                    Image(photo, contentDescription = "Recipe photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)))
                }
                SectionHeader("Ingredients")
                Text("Amount, unit and name. Leave amount blank for \"to taste\".", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            itemsIndexed(d.ingredients) { index, row ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    NumberField("Qty", row.qty, { d.ingredients[index] = row.copy(qty = it) }, Modifier.weight(0.22f))
                    OutlinedTextField(value = row.unit, onValueChange = { d.ingredients[index] = row.copy(unit = it) }, label = { Text("Unit") }, singleLine = true, modifier = Modifier.weight(0.22f))
                    OutlinedTextField(
                        value = row.name, onValueChange = { d.ingredients[index] = row.copy(name = it) }, label = { Text("Ingredient") }, singleLine = true,
                        modifier = Modifier.weight(0.46f),
                    )
                    IconButton(onClick = { if (d.ingredients.size > 1) d.ingredients.removeAt(index) else d.ingredients[0] = IngredientRow() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove ingredient ${index + 1}")
                    }
                }
            }
            item {
                TextButton(onClick = { d.ingredients.add(IngredientRow()) }) { Text("+ Add ingredient") }
                SectionHeader("Steps")
            }
            itemsIndexed(d.steps) { index, step ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = step, onValueChange = { d.steps[index] = it.replace('\n', ' ') }, label = { Text("Step ${index + 1}") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { if (d.steps.size > 1) d.steps.removeAt(index) else d.steps[0] = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove step ${index + 1}")
                    }
                }
            }
            item {
                TextButton(onClick = { d.steps.add("") }) { Text("+ Add step") }
                SectionHeader("Nutrition per serving (optional)")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumberField("kcal", d.calories, { d.calories = it.filter(Char::isDigit).take(5) }, Modifier.weight(1f), decimal = false)
                    NumberField("Protein g", d.protein, { d.protein = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumberField("Carbs g", d.carbs, { d.carbs = it }, Modifier.weight(1f))
                    NumberField("Fat g", d.fat, { d.fat = it }, Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = d.estimated, onCheckedChange = { d.estimated = it })
                    Text("These values are estimates")
                }
                SpacerH(8)
                Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) { Text("Save recipe") }
                SpacerH(32)
            }
        }
    }
}
