package org.mealforge.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.mealforge.app.data.nutrition
import org.mealforge.app.domain.NutritionMath
import org.mealforge.app.domain.NutritionTotals
import org.mealforge.app.ui.AppViewModel
import org.mealforge.app.ui.InfoCard
import org.mealforge.app.ui.MealForgeTopBar
import org.mealforge.app.ui.SectionHeader
import org.mealforge.app.ui.SpacerH
import org.mealforge.app.ui.screenPadding
import org.mealforge.app.ui.theme.Brand
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun NutritionScreen(vm: AppViewModel, nav: NavHostController) {
    val weekStart by vm.weekStart.collectAsStateWithLifecycle()
    val meals by vm.weekMeals.collectAsStateWithLifecycle()
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    val perDay = days.map { d -> d to NutritionMath.perPerson(meals.filter { it.meal.epochDay == d.toEpochDay() }.map { it.recipe.nutrition }) }
    val week = NutritionMath.perPerson(meals.map { it.recipe.nutrition })
    val daysWithMeals = perDay.count { it.second.meals > 0 }.coerceAtLeast(1)
    val fmt = DateTimeFormatter.ofPattern("EEE d")
    val range = DateTimeFormatter.ofPattern("d MMM")

    Column(Modifier.fillMaxSize()) {
        MealForgeTopBar("Nutrition", onBack = { nav.popBackStack() })
        Row(Modifier.screenPadding, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.shiftWeek(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week") }
            Text("${weekStart.format(range)} – ${weekStart.plusDays(6).format(range)}", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { vm.shiftWeek(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week") }
        }
        LazyColumn(Modifier.screenPadding, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                InfoCard {
                    Text("Per person, one serving of each planned meal", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    SpacerH(6)
                    Text("Daily average · ${week.calories / daysWithMeals} kcal", style = MaterialTheme.typography.titleLarge, color = Brand.Gold)
                    Text(
                        "Protein ${(week.proteinG / daysWithMeals).roundToInt()} g · Carbs ${(week.carbsG / daysWithMeals).roundToInt()} g · Fat ${(week.fatG / daysWithMeals).roundToInt()} g",
                    )
                    SpacerH(4)
                    Text("Week total · ${week.calories} kcal from ${week.meals} meals")
                    Caveats(week)
                }
            }
            item { SectionHeader("By day") }
            item {
                Row(Modifier.fillMaxWidth()) {
                    listOf("Day", "kcal", "Protein", "Carbs", "Fat").forEachIndexed { i, h ->
                        Text(h, modifier = Modifier.weight(if (i == 0) 1.2f else 1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            items(perDay, key = { it.first.toEpochDay() }) { (d, t) ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(d.format(fmt), modifier = Modifier.weight(1.2f), color = MaterialTheme.colorScheme.primary)
                    if (t.meals == 0) {
                        Text("No meals", modifier = Modifier.weight(4f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("${t.calories}" + if (t.mealsMissingData > 0) "*" else "", modifier = Modifier.weight(1f))
                        Text("${t.proteinG.roundToInt()} g", modifier = Modifier.weight(1f))
                        Text("${t.carbsG.roundToInt()} g", modifier = Modifier.weight(1f))
                        Text("${t.fatG.roundToInt()} g", modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                SpacerH(8)
                Text("* Some meals that day have no nutrition data, so totals are incomplete.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("MealForge isn't a medical tool. Values are approximate.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SpacerH(24)
            }
        }
    }
}

@Composable
private fun Caveats(t: NutritionTotals) {
    if (t.mealsMissingData > 0) Text("${t.mealsMissingData} meals have no nutrition data and count as zero.", color = Brand.Pink, style = MaterialTheme.typography.bodySmall)
    if (t.mealsEstimated > 0) Text("${t.mealsEstimated} meals use estimated values.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}
