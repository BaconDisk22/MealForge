package org.mealforge.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.mealforge.app.ui.screens.BudgetScreen
import org.mealforge.app.ui.screens.GroceriesScreen
import org.mealforge.app.ui.screens.HomeScreen
import org.mealforge.app.ui.screens.HouseholdScreen
import org.mealforge.app.ui.screens.MoreScreen
import org.mealforge.app.ui.screens.NutritionScreen
import org.mealforge.app.ui.screens.PantryScreen
import org.mealforge.app.ui.screens.PlannerScreen
import org.mealforge.app.ui.screens.RecipeDetailScreen
import org.mealforge.app.ui.screens.RecipeEditScreen
import org.mealforge.app.ui.screens.RecipesScreen
import org.mealforge.app.ui.screens.SettingsScreen
import org.mealforge.app.ui.theme.MealForgeTheme

object Routes {
    const val HOME = "home"
    const val PLANNER = "planner"
    const val RECIPES = "recipes"
    const val GROCERIES = "groceries"
    const val MORE = "more"
    const val PANTRY = "pantry"
    const val BUDGET = "budget"
    const val NUTRITION = "nutrition"
    const val HOUSEHOLD = "household"
    const val SETTINGS = "settings"
    const val RECIPE = "recipe/{id}"
    const val RECIPE_EDIT = "recipe/{id}/edit"

    fun recipe(id: Long) = "recipe/$id"
    fun editRecipe(id: Long = 0) = "recipe/$id/edit"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.PLANNER, "Planner", Icons.Filled.CalendarMonth),
    Tab(Routes.RECIPES, "Recipes", Icons.AutoMirrored.Filled.MenuBook),
    Tab(Routes.GROCERIES, "Groceries", Icons.Filled.ShoppingCart),
    Tab(Routes.MORE, "More", Icons.Filled.MoreHoriz),
)

@Composable
fun MealForgeApp(vm: AppViewModel = viewModel()) {
    MealForgeTheme {
        val nav = rememberNavController()
        val snackbar = remember { SnackbarHostState() }
        val ready by vm.ready.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
        val backStack by nav.currentBackStackEntryAsState()
        val route = backStack?.destination?.route
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (tabs.any { it.route == route } || route == null) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = route == tab.route,
                                onClick = { nav.goToTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            if (!ready) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Scaffold
            }
            NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
                composable(Routes.HOME) { HomeScreen(vm, nav) }
                composable(Routes.PLANNER) { PlannerScreen(vm, nav) }
                composable(Routes.RECIPES) { RecipesScreen(vm, nav) }
                composable(Routes.GROCERIES) { GroceriesScreen(vm) }
                composable(Routes.MORE) { MoreScreen(nav) }
                composable(Routes.PANTRY) { PantryScreen(vm, nav) }
                composable(Routes.BUDGET) { BudgetScreen(vm, nav) }
                composable(Routes.NUTRITION) { NutritionScreen(vm, nav) }
                composable(Routes.HOUSEHOLD) { HouseholdScreen(vm, nav) }
                composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
                composable(Routes.RECIPE, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                    RecipeDetailScreen(vm, nav, it.arguments?.getLong("id") ?: 0)
                }
                composable(Routes.RECIPE_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                    RecipeEditScreen(vm, nav, it.arguments?.getLong("id") ?: 0)
                }
            }
        }
    }
}

fun NavHostController.goToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
