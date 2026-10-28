package org.mealforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Gold = Color(0xFFFFD700)
private val Purple = Color(0xFF800080)
private val Pink = Color(0xFFFF69B4)
private val Red = Color(0xFFFF0000)
private val Maroon = Color(0xFF800000)
private val Dark = Color(0xFF000000)
private val tabs = listOf("Home", "Planner", "Recipes", "Groceries", "Settings")
private val recipes = listOf("Garlic chicken & rice", "Veggie pasta", "Breakfast burrito", "Baked salmon", "Bean chili")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MealForgeApp() }
    }
}

@Composable
private fun MealForgeApp() {
    var tab by remember { mutableStateOf(0) }
    val store = remember(LocalContext.current) { MealStore(LocalContext.current) }
    val groceries = remember { mutableStateListOf<String>().apply { addAll(store.loadList("groceries", listOf("Rice", "Chicken", "Tomatoes", "Eggs"))) } }
    var newItem by remember { mutableStateOf("") }
    val mealPlan = remember { mutableStateListOf<String>().apply { addAll(store.loadList("meal_plan", listOf("Garlic chicken & rice", "Veggie pasta", "Bean chili", "Baked salmon", "Breakfast burrito", "Veggie pasta", "Bean chili"))) } }
    var servings by remember { mutableIntStateOf(store.loadServings()) }
    LaunchedEffect(store) {
        snapshotFlow { groceries.toList() }.distinctUntilChanged().collect { store.saveList("groceries", it) }
    }
    LaunchedEffect(store) {
        snapshotFlow { mealPlan.toList() }.distinctUntilChanged().collect { store.saveList("meal_plan", it) }
    }
    LaunchedEffect(store) {
        snapshotFlow { servings }.distinctUntilChanged().collect { store.saveServings(it) }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, secondary = Pink, tertiary = Purple, background = Dark, surface = Color(0xFF21101C), onBackground = Color.White, onSurface = Color.White)) {
        Scaffold(
            containerColor = Dark,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF21001D)) {
                    tabs.forEachIndexed { index, name ->
                        NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Text(listOf("⌂", "▦", "♨", "☑", "⚙")[index], fontSize = 20.sp) }, label = { Text(name, fontSize = 10.sp) })
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(18.dp))
                Text("MealForge", color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("Plan smart. Eat better. Spend less.", color = Pink, fontSize = 12.sp)
                Spacer(Modifier.height(24.dp))
                when (tab) {
                    0 -> {
                        FeatureCard("Welcome to MealForge", "Your free meal planning companion.", Purple)
                        Spacer(Modifier.height(14.dp))
                        FeatureCard("This week", "7 meals planned • ${groceries.size} grocery items", Maroon)
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = { tab = 1 }) { Text("Open meal planner") }
                    }
                    1 -> {
                        Text("Weekly meal planner", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Tap a day to rotate its recipe.", color = Pink)
                        Spacer(Modifier.height(12.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(7) { day ->
                                Card(onClick = { mealPlan[day] = recipes[(recipes.indexOf(mealPlan[day]) + 1) % recipes.size] }, colors = CardDefaults.cardColors(containerColor = Color(0xFF241020))) {
                                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                        Text(listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")[day], color = Gold)
                                        Text(mealPlan[day])
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        Text("Recipe library", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Starter recipes • more coming soon", color = Pink)
                        Spacer(Modifier.height(12.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(recipes) { recipe -> FeatureCard(recipe, "Add ingredients to your shopping plan in a future update.", Purple) }
                        }
                    }
                    3 -> {
                        Text("Grocery list", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Row {
                            OutlinedTextField(value = newItem, onValueChange = { newItem = it }, label = { Text("Add an item") }, modifier = Modifier.weight(1f), singleLine = true)
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { if (newItem.isNotBlank()) { groceries.add(newItem.trim()); newItem = "" } }, modifier = Modifier.padding(top = 8.dp)) { Text("+") }
                        }
                        Spacer(Modifier.height(12.dp))
                        LazyColumn {
                            items(groceries.toList()) { item ->
                                var checked by remember(item) { mutableStateOf(false) }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row { Checkbox(checked = checked, onCheckedChange = { checked = it }); Text(item, modifier = Modifier.padding(top = 14.dp)) }
                                    TextButton(onClick = { groceries.remove(item) }) { Text("Remove", color = Pink) }
                                }
                            }
                        }
                    }
                    else -> {
                        Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Household servings: $servings", color = Gold)
                        Row {
                            Button(onClick = { if (servings > 1) servings-- }) { Text("−") }
                            Spacer(Modifier.width(12.dp))
                            Button(onClick = { servings++ }) { Text("+") }
                        }
                        Spacer(Modifier.height(20.dp))
                        FeatureCard("Free forever", "No ads, subscriptions, or paywalls.", Maroon)
                        Text("MealForge 0.2.0 • Offline save enabled", modifier = Modifier.padding(top = 16.dp), color = Pink)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(title: String, subtitle: String, accent: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF21101C)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = accent.takeIf { it != Purple && it != Maroon } ?: Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp)
        }
    }
}
