package org.mealforge.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.mealforge.app.domain.Money
import org.mealforge.app.ui.AppViewModel
import org.mealforge.app.ui.InfoCard
import org.mealforge.app.ui.Routes
import org.mealforge.app.ui.SectionHeader
import org.mealforge.app.ui.SpacerH
import org.mealforge.app.ui.Stepper
import org.mealforge.app.ui.goToTab
import org.mealforge.app.ui.screenPadding
import org.mealforge.app.ui.theme.Brand
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(vm: AppViewModel, nav: NavHostController) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val week by vm.thisWeekMeals.collectAsStateWithLifecycle()
    val items by vm.groceryItems.collectAsStateWithLifecycle()
    val pantry by vm.pantry.collectAsStateWithLifecycle()
    val spending by vm.spending.collectAsStateWithLifecycle()
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    val today = vm.today
    val todays = week.filter { it.meal.epochDay == today.toEpochDay() }
    val upcoming = week.filter { it.meal.epochDay > today.toEpochDay() }.take(4)
    val expiring = pantry.filter { p -> p.expiresOnEpochDay?.let { it - today.toEpochDay() <= 3 } == true }
    val range = vm.repo.periodRange(today, settings.budgetPeriod)
    val spent = spending.filter { it.epochDay in range.start.toEpochDay()..range.endInclusive.toEpochDay() }.sumOf { it.amountCents }
    val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM")

    LazyColumn(Modifier.fillMaxSize().screenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SpacerH(16)
            Text("MealForge", style = MaterialTheme.typography.headlineMedium)
            Text("Plan smart. Eat better. Spend less.", color = MaterialTheme.colorScheme.secondary)
        }
        if (!settings.onboardingDone) {
            item {
                var servings by remember { mutableIntStateOf(settings.householdServings) }
                InfoCard(container = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Welcome! Let's get set up", style = MaterialTheme.typography.titleMedium, color = Brand.Gold)
                    SpacerH(4)
                    Text("MealForge is free, works offline and keeps your data on this phone. How many people do you usually cook for?")
                    SpacerH(8)
                    Stepper(servings, { servings = it }, "People")
                    SpacerH(8)
                    Button(onClick = { vm.finishOnboarding(servings) }) { Text("Get started") }
                }
            }
        }
        item {
            SectionHeader("Today · ${today.format(dayFmt)}")
            if (todays.isEmpty()) {
                InfoCard(onClick = { nav.goToTab(Routes.PLANNER) }) {
                    Text("Nothing planned for today")
                    Text("Tap to open the planner", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    todays.forEach { m ->
                        InfoCard(onClick = { nav.navigate(Routes.recipe(m.recipe.id)) }) {
                            Text(m.meal.slot.label, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            Text(m.recipe.name, style = MaterialTheme.typography.titleMedium)
                            Text("${m.meal.servings} servings" + if (m.meal.leftover) " · leftovers" else "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoCard(Modifier.weight(1f), onClick = { nav.goToTab(Routes.PLANNER) }) {
                    Text("${week.size}", style = MaterialTheme.typography.headlineMedium)
                    Text("meals this week")
                }
                InfoCard(Modifier.weight(1f), onClick = { nav.goToTab(Routes.GROCERIES) }) {
                    Text("${items.count { !it.checked }}", style = MaterialTheme.typography.headlineMedium)
                    Text("items to buy")
                }
            }
        }
        item {
            InfoCard(onClick = { nav.navigate(Routes.BUDGET) }) {
                val budget = settings.budgetCents
                Text("Budget", style = MaterialTheme.typography.titleMedium)
                if (budget == null) {
                    Text("Spent ${Money.format(spent, settings.currencySymbol)} this ${settings.budgetPeriod.name.lowercase()}. Tap to set a budget.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val fraction = if (budget == 0L) 1f else (spent.toFloat() / budget).coerceIn(0f, 1f)
                    SpacerH(6)
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth(), color = if (spent > budget) Brand.Red else Brand.Gold)
                    SpacerH(6)
                    Text("${Money.format(spent, settings.currencySymbol)} of ${Money.format(budget, settings.currencySymbol)} " + if (spent > budget) "· over budget" else "· ${Money.format(budget - spent, settings.currencySymbol)} left")
                }
            }
        }
        if (expiring.isNotEmpty()) {
            item {
                InfoCard(container = MaterialTheme.colorScheme.secondaryContainer, onClick = { nav.navigate(Routes.PANTRY) }) {
                    Text("Use soon", style = MaterialTheme.typography.titleMedium)
                    Text(expiring.joinToString { it.name })
                }
            }
        }
        if (upcoming.isNotEmpty()) {
            item { SectionHeader("Coming up") }
            items(upcoming, key = { it.meal.id }) { m ->
                InfoCard(onClick = { nav.navigate(Routes.recipe(m.recipe.id)) }) {
                    Text("${java.time.LocalDate.ofEpochDay(m.meal.epochDay).format(dayFmt)} · ${m.meal.slot.label}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Text(m.recipe.name)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { nav.navigate(Routes.editRecipe()) }) { Text("New recipe") }
                OutlinedButton(onClick = { nav.navigate(Routes.NUTRITION) }) { Text("Nutrition") }
            }
            Text("${recipes.size} recipes in your library", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
            SpacerH(24)
        }
    }
}
