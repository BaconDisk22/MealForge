package org.mealforge.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.mealforge.app.ui.InfoCard
import org.mealforge.app.ui.MealForgeTopBar
import org.mealforge.app.ui.Routes
import org.mealforge.app.ui.SpacerH
import org.mealforge.app.ui.screenPadding

@Composable
fun MoreScreen(nav: NavHostController) {
    val entries = listOf(
        Triple("Pantry", "What you have on hand, expiry dates and meal ideas", Routes.PANTRY),
        Triple("Budget", "Set a grocery budget and track spending", Routes.BUDGET),
        Triple("Nutrition", "Daily and weekly nutrition from your plan", Routes.NUTRITION),
        Triple("Household", "People you cook for, portions and food exclusions", Routes.HOUSEHOLD),
        Triple("Settings & backup", "Currency, backup, restore and about", Routes.SETTINGS),
    )
    Column(Modifier.fillMaxSize()) {
        MealForgeTopBar("More")
        Column(Modifier.screenPadding.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            entries.forEach { (title, body, route) ->
                InfoCard(onClick = { nav.navigate(route) }) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SpacerH(16)
        }
    }
}
