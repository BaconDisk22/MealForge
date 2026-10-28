package org.mealforge.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.mealforge.app.data.MealSlot
import org.mealforge.app.data.PlannedMealEntity
import org.mealforge.app.data.PlannedMealWithRecipe
import org.mealforge.app.data.RecipeWithIngredients
import org.mealforge.app.ui.AppViewModel
import org.mealforge.app.ui.ConfirmDialog
import org.mealforge.app.ui.EmptyState
import org.mealforge.app.ui.InfoCard
import org.mealforge.app.ui.MealForgeTopBar
import org.mealforge.app.ui.Routes
import org.mealforge.app.ui.SpacerH
import org.mealforge.app.ui.Stepper
import org.mealforge.app.ui.TextPromptDialog
import org.mealforge.app.ui.goToTab
import org.mealforge.app.ui.screenPadding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayHeader = DateTimeFormatter.ofPattern("EEEE d MMM")
private val shortDay = DateTimeFormatter.ofPattern("d MMM")
private val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(vm: AppViewModel, nav: NavHostController) {
    val weekStart by vm.weekStart.collectAsStateWithLifecycle()
    val meals by vm.weekMeals.collectAsStateWithLifecycle()
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    val templates by vm.templates.collectAsStateWithLifecycle()
    var monthMode by rememberSaveable { mutableStateOf(false) }
    var addFor by remember { mutableStateOf<LocalDate?>(null) }
    var editing by remember { mutableStateOf<PlannedMealWithRecipe?>(null) }
    var menu by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var saveTemplate by remember { mutableStateOf(false) }
    var pickTemplate by remember { mutableStateOf(false) }
    var buildList by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        MealForgeTopBar("Meal planner", actions = {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Planner options") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Build grocery list for this week") }, onClick = { menu = false; buildList = true })
                DropdownMenuItem(text = { Text("Copy last week") }, onClick = { menu = false; vm.copyPreviousWeek() })
                DropdownMenuItem(text = { Text("Save week as template") }, onClick = { menu = false; saveTemplate = true })
                DropdownMenuItem(text = { Text("Apply a template") }, onClick = { menu = false; pickTemplate = true })
                DropdownMenuItem(text = { Text("Clear this week") }, onClick = { menu = false; confirmClear = true })
            }
        })
        SingleChoiceSegmentedButtonRow(Modifier.screenPadding.fillMaxWidth()) {
            SegmentedButton(selected = !monthMode, onClick = { monthMode = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Week") }
            SegmentedButton(selected = monthMode, onClick = { monthMode = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Month") }
        }
        if (monthMode) {
            MonthView(vm, onPick = { vm.goToWeekOf(it); monthMode = false })
        } else {
            Row(Modifier.screenPadding.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.shiftWeek(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week") }
                Text(
                    "${weekStart.format(shortDay)} – ${weekStart.plusDays(6).format(shortDay)}",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { vm.shiftWeek(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week") }
                TextButton(onClick = { vm.goToWeekOf(vm.today) }) { Text("Today") }
            }
            if (recipes.isEmpty()) {
                EmptyState("No recipes yet", "Add a recipe first, then plan it here.") {
                    TextButton(onClick = { nav.navigate(Routes.editRecipe()) }) { Text("Create a recipe") }
                }
            }
            LazyColumn(Modifier.screenPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items((0..6).map { weekStart.plusDays(it.toLong()) }, key = { it.toEpochDay() }) { day ->
                    val dayMeals = meals.filter { it.meal.epochDay == day.toEpochDay() }
                    DayCard(day, day == vm.today, dayMeals, onAdd = { addFor = day }, onMeal = { editing = it })
                }
                item { SpacerH(16) }
            }
        }
    }

    addFor?.let { day ->
        AddMealDialog(day, recipes, vm.settings.value.householdServings, onDismiss = { addFor = null }) { slot, recipeId, servings, leftover ->
            vm.addMeal(day, slot, recipeId, servings, leftover)
        }
    }
    editing?.let { m ->
        MealDialog(
            m, weekStart,
            onDismiss = { editing = null },
            onSave = { vm.updateMeal(it) },
            onRemove = { vm.removeMeal(m.meal.id) },
            onRepeat = { vm.repeatMeal(m.meal, 7, 3) },
            onOpen = { nav.navigate(Routes.recipe(m.recipe.id)) },
        )
    }
    if (confirmClear) {
        ConfirmDialog("Clear this week?", "Removes all ${meals.size} planned meals for this week.", "Clear", onConfirm = { vm.clearWeek() }, onDismiss = { confirmClear = false })
    }
    if (saveTemplate) {
        TextPromptDialog("Save week as template", label = "Template name", onConfirm = { vm.saveWeekAsTemplate(it) }, onDismiss = { saveTemplate = false })
    }
    if (pickTemplate) {
        AlertDialog(
            onDismissRequest = { pickTemplate = false },
            title = { Text("Apply a template") },
            text = {
                if (templates.isEmpty()) {
                    Text("No templates yet. Plan a week you like, then use \"Save week as template\".")
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(templates, key = { it.id }) { t ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { vm.applyTemplate(t.id); pickTemplate = false }, modifier = Modifier.weight(1f)) { Text(t.name, modifier = Modifier.fillMaxWidth()) }
                                TextButton(onClick = { vm.deleteTemplate(t.id) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickTemplate = false }) { Text("Close") } },
        )
    }
    if (buildList) {
        var usePantry by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { buildList = false },
            title = { Text("Build grocery list") },
            text = {
                Column {
                    Text("Adds every ingredient for ${meals.count { !it.meal.leftover }} planned meals this week to your active list, combining duplicates. Earlier plan-generated items are replaced; items you added yourself stay.")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = usePantry, onCheckedChange = { usePantry = it })
                        Text("Skip what's already in my pantry")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.generateGroceries(weekStart, weekStart.plusDays(6), usePantry)
                    buildList = false
                    nav.goToTab(Routes.GROCERIES)
                }) { Text("Build list") }
            },
            dismissButton = { TextButton(onClick = { buildList = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DayCard(day: LocalDate, isToday: Boolean, meals: List<PlannedMealWithRecipe>, onAdd: () -> Unit, onMeal: (PlannedMealWithRecipe) -> Unit) {
    InfoCard(container = if (isToday) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                day.format(dayHeader) + if (isToday) " · Today" else "",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Add meal on ${day.format(dayHeader)}") }
        }
        if (meals.isEmpty()) {
            Text("No meals planned", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        meals.forEach { m ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().clickable { onMeal(m) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(m.meal.slot.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(0.3f))
                Column(Modifier.weight(0.7f)) {
                    Text(m.recipe.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${m.meal.servings} servings" + if (m.meal.leftover) " · leftovers" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddMealDialog(
    day: LocalDate,
    recipes: List<RecipeWithIngredients>,
    defaultServings: Int,
    onDismiss: () -> Unit,
    onAdd: (MealSlot, Long, Int, Boolean) -> Unit,
) {
    var slot by remember { mutableStateOf(MealSlot.DINNER) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Long?>(null) }
    var servings by remember { mutableIntStateOf(defaultServings) }
    var leftover by remember { mutableStateOf(false) }
    val filtered = recipes.filter { query.isBlank() || it.recipe.name.contains(query, ignoreCase = true) || it.recipe.tags.any { t -> t.contains(query, true) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add meal · ${day.format(shortDay)}") },
        text = {
            Column {
                FlowRow {
                    MealSlot.entries.forEach { s ->
                        FilterChip(selected = slot == s, onClick = { slot = s }, label = { Text(s.label) }, modifier = Modifier.padding(end = 6.dp))
                    }
                }
                OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search recipes") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = 220.dp)) {
                    items(filtered, key = { it.recipe.id }) { r ->
                        val isSel = selected == r.recipe.id
                        Surface(
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { selected = r.recipe.id },
                        ) {
                            Text(r.recipe.name + if (r.recipe.favorite) " ★" else "", modifier = Modifier.padding(12.dp))
                        }
                    }
                }
                SpacerH(8)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Servings", modifier = Modifier.weight(1f))
                    Stepper(servings, { servings = it }, "Servings")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = leftover, onCheckedChange = { leftover = it })
                    Text("Leftovers (don't add to grocery list)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { selected?.let { onAdd(slot, it, servings, leftover) }; onDismiss() }, enabled = selected != null) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun MealDialog(
    m: PlannedMealWithRecipe,
    weekStart: LocalDate,
    onDismiss: () -> Unit,
    onSave: (PlannedMealEntity) -> Unit,
    onRemove: () -> Unit,
    onRepeat: () -> Unit,
    onOpen: () -> Unit,
) {
    var meal by remember { mutableStateOf(m.meal) }
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(m.recipe.name) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Servings", modifier = Modifier.weight(1f))
                    Stepper(meal.servings, { meal = meal.copy(servings = it) }, "Servings")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = meal.leftover, onCheckedChange = { meal = meal.copy(leftover = it) })
                    Text("Leftovers")
                }
                Text("Move to", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                FlowRow {
                    days.forEach { d ->
                        FilterChip(
                            selected = meal.epochDay == d.toEpochDay(),
                            onClick = { meal = meal.copy(epochDay = d.toEpochDay()) },
                            label = { Text(d.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
                FlowRow {
                    MealSlot.entries.forEach { s ->
                        FilterChip(selected = meal.slot == s, onClick = { meal = meal.copy(slot = s) }, label = { Text(s.label) }, modifier = Modifier.padding(end = 4.dp))
                    }
                }
                Row {
                    TextButton(onClick = { onOpen(); onDismiss() }) { Text("View recipe") }
                    TextButton(onClick = { onRepeat(); onDismiss() }) { Text("Repeat 3 more weeks") }
                }
                TextButton(onClick = { onRemove(); onDismiss() }) { Text("Remove from plan", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(meal); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MonthView(vm: AppViewModel, onPick: (LocalDate) -> Unit) {
    val month by vm.month.collectAsStateWithLifecycle()
    val meals by vm.monthMeals.collectAsStateWithLifecycle()
    val counts = meals.groupingBy { it.meal.epochDay }.eachCount()
    val first = month.withDayOfMonth(1)
    val leading = first.dayOfWeek.value - 1
    val daysInMonth = month.lengthOfMonth()
    val cells: List<LocalDate?> = List(leading) { null } + (1..daysInMonth).map { first.withDayOfMonth(it) }
    Column(Modifier.screenPadding) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.shiftMonth(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month") }
            Text(month.format(monthTitle), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.shiftMonth(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month") }
        }
        Row {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        cells.chunked(7).forEach { week ->
            Row {
                (0 until 7).forEach { i ->
                    val day = week.getOrNull(i)
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp)) {
                        if (day != null) {
                            val count = counts[day.toEpochDay()] ?: 0
                            val isToday = day == vm.today
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                    count > 0 -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                modifier = Modifier.fillMaxSize().clickable { onPick(day) }.semantics {
                                    contentDescription = "${day.format(dayHeader)}, $count meals planned"
                                },
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text("${day.dayOfMonth}")
                                    if (count > 0) Text("$count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
        SpacerH(8)
        Text("Tap a day to plan that week.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
