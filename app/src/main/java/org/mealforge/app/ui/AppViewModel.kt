package org.mealforge.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mealforge.app.BuildConfig
import org.mealforge.app.MealForgeApplication
import org.mealforge.app.data.AppSettings
import org.mealforge.app.data.BackupFile
import org.mealforge.app.data.GroceryItemEntity
import org.mealforge.app.data.GroceryListEntity
import org.mealforge.app.data.HouseholdMemberEntity
import org.mealforge.app.data.IngredientEntity
import org.mealforge.app.data.MealSlot
import org.mealforge.app.data.PantryItemEntity
import org.mealforge.app.data.PlannedMealEntity
import org.mealforge.app.data.PlannedMealWithRecipe
import org.mealforge.app.data.RecipeEntity
import org.mealforge.app.data.RecipeWithIngredients
import org.mealforge.app.data.Seeder
import org.mealforge.app.data.SpendEntryEntity
import org.mealforge.app.domain.BudgetPeriod
import org.mealforge.app.domain.Dates
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MealForgeApplication
    val repo = app.repository

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    val today: LocalDate get() = LocalDate.now()

    private val _weekStart = MutableStateFlow(Dates.weekStart(LocalDate.now()))
    val weekStart: StateFlow<LocalDate> = _weekStart.asStateFlow()

    val settings: StateFlow<AppSettings> = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val recipes: StateFlow<List<RecipeWithIngredients>> = repo.allRecipes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val groceryLists: StateFlow<List<GroceryListEntity>> = repo.groceryLists.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val pantry: StateFlow<List<PantryItemEntity>> = repo.pantryItems.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val spending: StateFlow<List<SpendEntryEntity>> = repo.spendEntries.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val templates = repo.planTemplates.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val members: StateFlow<List<HouseholdMemberEntity>> = repo.members.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val weekMeals: StateFlow<List<PlannedMealWithRecipe>> = _weekStart
        .flatMapLatest { repo.mealsBetween(it, it.plusDays(6)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Always the real current week, independent of where the planner is scrolled. */
    val thisWeekMeals: StateFlow<List<PlannedMealWithRecipe>> = Dates.weekStart(LocalDate.now()).let { start ->
        repo.mealsBetween(start, start.plusDays(6)).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    }

    /** Meals in the month shown by the planner's month view. */
    private val _month = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    val month: StateFlow<LocalDate> = _month.asStateFlow()
    val monthMeals: StateFlow<List<PlannedMealWithRecipe>> = _month
        .flatMapLatest { repo.mealsBetween(it, it.plusMonths(1).minusDays(1)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeListId: StateFlow<Long?> = combine(settings, groceryLists) { s, lists ->
        s.activeListId?.takeIf { id -> lists.any { it.id == id } } ?: lists.firstOrNull()?.id
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val groceryItems: StateFlow<List<GroceryItemEntity>> = activeListId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.groceryItems(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            Seeder(app, app.database, repo).run()
            _ready.value = true
        }
    }

    private fun io(block: suspend () -> Unit) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) { block() }
        } catch (e: Exception) {
            _messages.tryEmit(e.message ?: "Something went wrong")
        }
    }

    fun toast(message: String) { _messages.tryEmit(message) }

    // Settings
    fun setSetting(key: String, value: String?) = io { repo.setSetting(key, value) }
    fun setServings(value: Int) = setSetting(AppSettings.SERVINGS, value.coerceIn(1, 50).toString())
    fun setBudget(cents: Long?, period: BudgetPeriod) = io {
        repo.setSetting(AppSettings.BUDGET, cents?.toString())
        repo.setSetting(AppSettings.BUDGET_PERIOD, period.name)
    }
    fun finishOnboarding(servings: Int) = io {
        repo.setSetting(AppSettings.SERVINGS, servings.coerceAtLeast(1).toString())
        repo.setSetting(AppSettings.ONBOARDING, "true")
    }

    // Planner
    fun shiftWeek(weeks: Long) { _weekStart.value = _weekStart.value.plusWeeks(weeks) }
    fun goToWeekOf(day: LocalDate) { _weekStart.value = Dates.weekStart(day) }
    fun shiftMonth(months: Long) { _month.value = _month.value.plusMonths(months) }
    fun addMeal(day: LocalDate, slot: MealSlot, recipeId: Long, servings: Int?, leftover: Boolean = false) =
        io { repo.addMeal(day, slot, recipeId, servings, leftover) }
    fun updateMeal(meal: PlannedMealEntity) = io { repo.updateMeal(meal) }
    fun removeMeal(id: Long) = io { repo.removeMeal(id) }
    fun repeatMeal(meal: PlannedMealEntity, everyDays: Int, times: Int) = io {
        repo.repeatMeal(meal, everyDays, times)
        _messages.tryEmit("Repeated $times more times")
    }
    fun copyPreviousWeek() = io {
        val target = _weekStart.value
        repo.copyWeek(target.minusWeeks(1), target)
        _messages.tryEmit("Copied last week's meals")
    }
    fun clearWeek() = io { repo.clearWeek(_weekStart.value) }
    fun saveWeekAsTemplate(name: String) = io {
        repo.saveWeekAsTemplate(_weekStart.value, name)
        _messages.tryEmit("Template saved")
    }
    fun applyTemplate(id: Long) = io { repo.applyTemplate(id, _weekStart.value) }
    fun deleteTemplate(id: Long) = io { repo.deleteTemplate(id) }

    // Recipes
    fun saveRecipe(recipe: RecipeEntity, ingredients: List<IngredientEntity>, onSaved: (Long) -> Unit) = viewModelScope.launch {
        try {
            val id = withContext(Dispatchers.IO) { repo.saveRecipe(recipe, ingredients) }
            onSaved(id)
        } catch (e: Exception) {
            _messages.tryEmit(e.message ?: "Couldn't save recipe")
        }
    }
    fun duplicateRecipe(id: Long, onDone: (Long) -> Unit) = viewModelScope.launch {
        val newId = withContext(Dispatchers.IO) { repo.duplicateRecipe(id) }
        if (newId != null) onDone(newId)
    }
    fun deleteRecipe(id: Long) = io { repo.deleteRecipe(id) }
    fun setFavorite(id: Long, favorite: Boolean) = io { repo.setFavorite(id, favorite) }

    // Groceries
    fun selectList(id: Long) = setSetting(AppSettings.ACTIVE_LIST, id.toString())
    fun createList(name: String) = io { repo.createList(name) }
    fun renameList(list: GroceryListEntity, name: String) = io { repo.renameList(list, name) }
    fun deleteList(id: Long) = io { repo.deleteList(id) }
    fun addGroceryItem(name: String, quantity: Double?, unit: String) = io {
        val id = activeListId.value ?: repo.ensureActiveList()
        repo.addGroceryItem(id, name, quantity, unit)
    }
    fun updateGroceryItem(item: GroceryItemEntity) = io { repo.updateGroceryItem(item) }
    fun deleteGroceryItem(item: GroceryItemEntity) = io { repo.deleteGroceryItem(item) }
    fun clearChecked() = io { activeListId.value?.let { repo.clearChecked(it) } }
    fun generateGroceries(from: LocalDate, to: LocalDate, usePantry: Boolean) = io {
        val id = activeListId.value ?: repo.ensureActiveList()
        val count = repo.generateFromPlan(id, from, to, usePantry)
        _messages.tryEmit(if (count == 0) "Nothing to buy for those meals" else "Added $count items from your meal plan")
    }
    fun addRecipeToGroceries(recipe: RecipeWithIngredients, servings: Int) = io {
        val id = activeListId.value ?: repo.ensureActiveList()
        recipe.ingredients.forEach { ing ->
            val qty = ing.quantity?.let { it * servings / recipe.recipe.servings.coerceAtLeast(1) }
            repo.addGroceryItem(id, ing.name, qty, ing.unit, ing.department)
        }
        _messages.tryEmit("Added ${recipe.ingredients.size} ingredients to your list")
    }
    fun finishShopping(totalCents: Long?, addToPantry: Boolean) = io {
        activeListId.value?.let { repo.finishShopping(it, totalCents, addToPantry, today) }
        _messages.tryEmit("Shopping trip saved")
    }

    // Pantry, budget, household
    fun savePantryItem(item: PantryItemEntity) = io { repo.savePantryItem(item) }
    fun deletePantryItem(item: PantryItemEntity) = io { repo.deletePantryItem(item) }
    fun addSpend(cents: Long, note: String, day: LocalDate = today) = io { repo.addSpend(day, cents, note) }
    fun deleteSpend(entry: SpendEntryEntity) = io { repo.deleteSpend(entry) }
    fun saveMember(member: HouseholdMemberEntity) = io { repo.saveMember(member) }
    fun deleteMember(member: HouseholdMemberEntity) = io { repo.deleteMember(member) }

    // Backup
    fun exportBackup(uri: Uri) = io {
        val text = BackupFile.encode(repo.snapshot(BuildConfig.VERSION_NAME))
        app.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: error("Couldn't open file")
        _messages.tryEmit("Backup saved")
    }

    fun importBackup(uri: Uri) = io {
        val text = app.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("Couldn't open file")
        val backup = BackupFile.decode(text)
        repo.restore(backup)
        repo.ensureActiveList()
        _messages.tryEmit("Restored ${backup.recipes.size} recipes and ${backup.plannedMeals.size} planned meals")
    }
}
