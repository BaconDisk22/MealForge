package org.mealforge.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.mealforge.app.data.RecipeWithIngredients
import org.mealforge.app.data.totalMinutes
import org.mealforge.app.domain.PantryMatch
import org.mealforge.app.domain.Preferences
import org.mealforge.app.ui.AppViewModel
import org.mealforge.app.ui.EmptyState
import org.mealforge.app.ui.InfoCard
import org.mealforge.app.ui.MealForgeTopBar
import org.mealforge.app.ui.Routes
import org.mealforge.app.ui.SpacerH
import org.mealforge.app.ui.screenPadding
import org.mealforge.app.ui.theme.Brand

private enum class RecipeSort(val label: String) {
    NAME("Name"), QUICKEST("Quickest"), CALORIES("Fewest calories"), PROTEIN("Most protein"), NEWEST("Newest"), PANTRY("Best pantry match")
}

@Composable
fun RecipesScreen(vm: AppViewModel, nav: NavHostController) {
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val members by vm.members.collectAsStateWithLifecycle()
    val pantry by vm.pantry.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var fitsPrefs by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf(RecipeSort.NAME) }
    var sortMenu by remember { mutableStateOf(false) }

    val exclusions = settings.exclusions + members.flatMap { it.exclusions }
    val pantryNames = pantry.map { it.name }
    val categories = recipes.map { it.recipe.category }.filter { it.isNotBlank() }.distinct().sorted()
    val shown = recipes
        .filter { r ->
            val q = query.trim()
            q.isEmpty() || r.recipe.name.contains(q, true) || r.recipe.tags.any { it.contains(q, true) } ||
                r.ingredients.any { it.name.contains(q, true) }
        }
        .filter { category == null || it.recipe.category == category }
        .filter { !favoritesOnly || it.recipe.favorite }
        .filter { !fitsPrefs || Preferences.conflicts(it.ingredients.map { i -> i.name }, exclusions).isEmpty() }
        .let { list ->
            when (sort) {
                RecipeSort.NAME -> list.sortedBy { it.recipe.name.lowercase() }
                RecipeSort.QUICKEST -> list.sortedBy { it.recipe.totalMinutes }
                RecipeSort.CALORIES -> list.sortedBy { it.recipe.caloriesPerServing ?: Int.MAX_VALUE }
                RecipeSort.PROTEIN -> list.sortedByDescending { it.recipe.proteinPerServing ?: -1.0 }
                RecipeSort.NEWEST -> list.sortedByDescending { it.recipe.createdAt }
                RecipeSort.PANTRY -> list.sortedByDescending { PantryMatch.coverage(it.ingredients.map { i -> i.name }, pantryNames) }
            }
        }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            MealForgeTopBar("Recipes", actions = {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort recipes") }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    RecipeSort.entries.forEach { s ->
                        DropdownMenuItem(text = { Text(s.label + if (s == sort) "  ✓" else "") }, onClick = { sort = s; sortMenu = false })
                    }
                }
            })
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search name, tag or ingredient") },
                singleLine = true,
                modifier = Modifier.screenPadding.fillMaxWidth(),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }, label = { Text("★ Favorites") })
                if (exclusions.isNotEmpty()) FilterChip(selected = fitsPrefs, onClick = { fitsPrefs = !fitsPrefs }, label = { Text("Fits my diet") })
                categories.forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = if (category == c) null else c }, label = { Text(c) })
                }
            }
            if (shown.isEmpty()) {
                EmptyState(
                    if (recipes.isEmpty()) "Your recipe box is empty" else "No matches",
                    if (recipes.isEmpty()) "Tap New recipe to add your first one." else "Try a different search or clear filters.",
                )
            }
            LazyColumn(Modifier.screenPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(shown, key = { it.recipe.id }) { r ->
                    RecipeRow(r, exclusions, onClick = { nav.navigate(Routes.recipe(r.recipe.id)) })
                }
                item { SpacerH(88) }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { nav.navigate(Routes.editRecipe()) },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("New recipe") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Composable
private fun RecipeRow(r: RecipeWithIngredients, exclusions: List<String>, onClick: () -> Unit) {
    val conflicts = Preferences.conflicts(r.ingredients.map { it.name }, exclusions)
    InfoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(r.recipe.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (r.recipe.favorite) Icon(Icons.Filled.Star, contentDescription = "Favorite", tint = Brand.Gold)
        }
        val facts = buildList {
            add(r.recipe.category)
            if (r.recipe.totalMinutes > 0) add("${r.recipe.totalMinutes} min")
            add("serves ${r.recipe.servings}")
            r.recipe.caloriesPerServing?.let { add("$it kcal") }
        }
        Text(facts.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (r.recipe.tags.isNotEmpty()) Text(r.recipe.tags.joinToString(" · "), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
        if (conflicts.isNotEmpty()) Text("Contains: ${conflicts.joinToString()}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
