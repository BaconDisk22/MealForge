package org.mealforge.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mealforge.app.domain.BudgetPeriod
import org.mealforge.app.domain.Dates
import org.mealforge.app.domain.Departments
import org.mealforge.app.domain.GroceryBuilder
import org.mealforge.app.domain.IngredientNeed
import org.mealforge.app.domain.Names
import org.mealforge.app.domain.PantryStock
import org.mealforge.app.domain.Preferences
import org.mealforge.app.domain.Units
import java.time.LocalDate
import kotlin.math.ceil

data class AppSettings(
    val householdServings: Int = 2,
    val budgetCents: Long? = null,
    val budgetPeriod: BudgetPeriod = BudgetPeriod.WEEK,
    val currencySymbol: String = "$",
    val exclusions: List<String> = emptyList(),
    val onboardingDone: Boolean = false,
    val activeListId: Long? = null,
) {
    companion object {
        const val SERVINGS = "household_servings"
        const val BUDGET = "budget_cents"
        const val BUDGET_PERIOD = "budget_period"
        const val CURRENCY = "currency_symbol"
        const val EXCLUSIONS = "exclusions"
        const val ONBOARDING = "onboarding_done"
        const val ACTIVE_LIST = "active_list_id"
        const val STARTER_SEEDED = "starter_seeded"
        const val LEGACY_IMPORTED = "legacy_imported"

        fun from(map: Map<String, String>) = AppSettings(
            householdServings = map[SERVINGS]?.toIntOrNull()?.coerceAtLeast(1) ?: 2,
            budgetCents = map[BUDGET]?.toLongOrNull(),
            budgetPeriod = map[BUDGET_PERIOD]?.let { runCatching { BudgetPeriod.valueOf(it) }.getOrNull() } ?: BudgetPeriod.WEEK,
            currencySymbol = map[CURRENCY]?.takeIf { it.isNotBlank() } ?: "$",
            exclusions = map[EXCLUSIONS]?.let { Preferences.parseList(it) } ?: emptyList(),
            onboardingDone = map[ONBOARDING] == "true",
            activeListId = map[ACTIVE_LIST]?.toLongOrNull(),
        )
    }
}

/** Single write path for all MealForge data. */
class Repository(private val db: MealForgeDatabase, private val clock: () -> Long = System::currentTimeMillis) {
    private val recipes = db.recipes()
    private val plan = db.plan()
    private val groceries = db.groceries()
    private val pantry = db.pantry()
    private val budget = db.budget()
    private val templates = db.templates()
    private val household = db.household()
    private val settingsDao = db.settings()

    val settings: Flow<AppSettings> = settingsDao.observeAll().map { rows -> AppSettings.from(rows.associate { it.key to it.value }) }
    val allRecipes: Flow<List<RecipeWithIngredients>> = recipes.observeAll()
    val groceryLists: Flow<List<GroceryListEntity>> = groceries.observeLists()
    val pantryItems: Flow<List<PantryItemEntity>> = pantry.observeAll()
    val spendEntries: Flow<List<SpendEntryEntity>> = budget.observeAll()
    val planTemplates: Flow<List<PlanTemplateEntity>> = templates.observeAll()
    val members: Flow<List<HouseholdMemberEntity>> = household.observeAll()

    fun recipe(id: Long): Flow<RecipeWithIngredients?> = recipes.observe(id)
    fun groceryItems(listId: Long): Flow<List<GroceryItemEntity>> = groceries.observeItems(listId)
    fun mealsBetween(from: LocalDate, to: LocalDate): Flow<List<PlannedMealWithRecipe>> =
        plan.observeRange(from.toEpochDay(), to.toEpochDay()).map { list -> list.sortedWith(compareBy({ it.meal.epochDay }, { it.meal.slot.ordinal })) }

    suspend fun setSetting(key: String, value: String?) = settingsDao.put(SettingEntity(key, value ?: ""))

    /** Effective servings: sum of member portions (rounded up) when members exist, else the manual setting. */
    suspend fun defaultServings(): Int {
        val members = household.all()
        if (members.isNotEmpty()) return ceil(members.sumOf { it.portion }).toInt().coerceAtLeast(1)
        return settingsDao.get(AppSettings.SERVINGS)?.toIntOrNull()?.coerceAtLeast(1) ?: 2
    }

    // ---- Recipes ----

    suspend fun saveRecipe(recipe: RecipeEntity, ingredients: List<IngredientEntity>): Long = db.withTransaction {
        val clean = recipe.copy(
            name = recipe.name.trim(),
            servings = recipe.servings.coerceAtLeast(1),
            steps = recipe.steps.map { it.trim() }.filter { it.isNotEmpty() },
            tags = recipe.tags.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() },
        )
        val id = if (clean.id == 0L) recipes.insert(clean.copy(createdAt = clock())) else clean.id.also { recipes.update(clean) }
        recipes.deleteIngredients(id)
        recipes.insertIngredients(
            ingredients.filter { it.name.isNotBlank() }.mapIndexed { index, ing ->
                ing.copy(id = 0, recipeId = id, position = index, name = ing.name.trim(), unit = Units.canonical(ing.unit))
            },
        )
        id
    }

    suspend fun duplicateRecipe(id: Long): Long? {
        val source = recipes.get(id) ?: return null
        return saveRecipe(source.recipe.copy(id = 0, name = source.recipe.name + " (copy)", favorite = false), source.ingredients)
    }

    suspend fun deleteRecipe(id: Long) = recipes.delete(id)
    suspend fun setFavorite(id: Long, favorite: Boolean) = recipes.setFavorite(id, favorite)

    // ---- Meal plan ----

    suspend fun addMeal(day: LocalDate, slot: MealSlot, recipeId: Long, servings: Int? = null, leftover: Boolean = false): Long =
        plan.insert(PlannedMealEntity(epochDay = day.toEpochDay(), slot = slot, recipeId = recipeId, servings = servings ?: defaultServings(), leftover = leftover))

    suspend fun updateMeal(meal: PlannedMealEntity) = plan.update(meal.copy(servings = meal.servings.coerceAtLeast(1)))
    suspend fun removeMeal(id: Long) = plan.delete(id)

    /** Copies every meal from the week starting [fromWeek] into the week starting [toWeek], keeping existing meals. */
    suspend fun copyWeek(fromWeek: LocalDate, toWeek: LocalDate) = db.withTransaction {
        val shift = toWeek.toEpochDay() - fromWeek.toEpochDay()
        val source = plan.range(fromWeek.toEpochDay(), fromWeek.toEpochDay() + 6)
        plan.insertAll(source.map { it.copy(id = 0, epochDay = it.epochDay + shift) })
    }

    suspend fun clearWeek(week: LocalDate) = plan.clearRange(week.toEpochDay(), week.toEpochDay() + 6)

    /** Repeats one meal every [everyDays] days, [times] more times. */
    suspend fun repeatMeal(meal: PlannedMealEntity, everyDays: Int, times: Int) {
        plan.insertAll((1..times).map { meal.copy(id = 0, epochDay = meal.epochDay + everyDays.toLong() * it) })
    }

    suspend fun saveWeekAsTemplate(week: LocalDate, name: String) = db.withTransaction {
        val meals = plan.range(week.toEpochDay(), week.toEpochDay() + 6)
        val id = templates.insert(PlanTemplateEntity(name = name.trim().ifEmpty { "My week" }))
        templates.insertMeals(
            meals.map { PlanTemplateMealEntity(templateId = id, dayOffset = (it.epochDay - week.toEpochDay()).toInt(), slot = it.slot, recipeId = it.recipeId, servings = it.servings, leftover = it.leftover) },
        )
    }

    suspend fun applyTemplate(templateId: Long, week: LocalDate) {
        plan.insertAll(
            templates.meals(templateId).map {
                PlannedMealEntity(epochDay = week.toEpochDay() + it.dayOffset, slot = it.slot, recipeId = it.recipeId, servings = it.servings, leftover = it.leftover)
            },
        )
    }

    suspend fun deleteTemplate(id: Long) = templates.delete(id)

    // ---- Groceries ----

    suspend fun ensureActiveList(): Long {
        val lists = groceries.lists()
        val active = settingsDao.get(AppSettings.ACTIVE_LIST)?.toLongOrNull()
        if (active != null && lists.any { it.id == active }) return active
        val id = lists.firstOrNull()?.id ?: groceries.insertList(GroceryListEntity(name = "Groceries", createdAt = clock()))
        setSetting(AppSettings.ACTIVE_LIST, id.toString())
        return id
    }

    suspend fun createList(name: String): Long {
        val id = groceries.insertList(GroceryListEntity(name = name.trim().ifEmpty { "New list" }, createdAt = clock()))
        setSetting(AppSettings.ACTIVE_LIST, id.toString())
        return id
    }

    suspend fun renameList(list: GroceryListEntity, name: String) = groceries.updateList(list.copy(name = name.trim().ifEmpty { list.name }))

    suspend fun deleteList(id: Long) {
        groceries.deleteList(id)
        setSetting(AppSettings.ACTIVE_LIST, null)
        ensureActiveList()
    }

    suspend fun addGroceryItem(listId: Long, name: String, quantity: Double? = null, unit: String = "", department: String? = null, priceCents: Long? = null) {
        if (name.isBlank()) return
        groceries.insertItem(
            GroceryItemEntity(listId = listId, name = name.trim(), quantity = quantity, unit = Units.canonical(unit), department = department ?: Departments.guess(name), estimatedPriceCents = priceCents),
        )
    }

    suspend fun updateGroceryItem(item: GroceryItemEntity) = groceries.updateItem(item)
    suspend fun deleteGroceryItem(item: GroceryItemEntity) = groceries.deleteItem(item)
    suspend fun clearChecked(listId: Long) = groceries.deleteChecked(listId)

    /**
     * Rebuilds the plan-generated part of [listId] from meals between [from] and [to].
     * Manual items are kept; pantry stock is subtracted; leftovers add nothing.
     * Returns the number of generated lines.
     */
    suspend fun generateFromPlan(listId: Long, from: LocalDate, to: LocalDate, usePantry: Boolean = true): Int = db.withTransaction {
        val meals = plan.range(from.toEpochDay(), to.toEpochDay()).filterNot { it.leftover }
        val recipeMap = recipes.getAll(meals.map { it.recipeId }.toSet()).associateBy { it.recipe.id }
        val needs = meals.flatMap { meal ->
            val r = recipeMap[meal.recipeId] ?: return@flatMap emptyList()
            r.ingredients.map { ing ->
                IngredientNeed(ing.name, GroceryBuilder.scale(ing.quantity, r.recipe.servings, meal.servings), ing.unit, ing.department)
            }
        }
        val stock = if (usePantry) pantry.all().map { PantryStock(it.name, it.quantity, it.unit) } else emptyList()
        val lines = GroceryBuilder.build(needs, stock)
        groceries.deletePlanItems(listId)
        groceries.insertItems(lines.map { GroceryItemEntity(listId = listId, name = it.name, quantity = it.quantity, unit = it.unit, department = it.department, fromPlan = true) })
        lines.size
    }

    /** Records a shopping trip: logs the spend and moves checked items into the pantry. */
    suspend fun finishShopping(listId: Long, totalCents: Long?, addToPantry: Boolean, today: LocalDate) = db.withTransaction {
        val items = groceries.items(listId).filter { it.checked }
        val spent = totalCents ?: items.sumOf { it.actualPriceCents ?: it.estimatedPriceCents ?: 0L }
        if (spent > 0) budget.insert(SpendEntryEntity(epochDay = today.toEpochDay(), amountCents = spent, note = "Shopping trip"))
        if (addToPantry) {
            val existing = pantry.all()
            for (item in items) {
                val match = existing.firstOrNull { Names.key(it.name) == Names.key(item.name) }
                val merged = when {
                    match == null -> PantryItemEntity(name = item.name, quantity = item.quantity, unit = item.unit)
                    match.quantity != null && item.quantity != null -> {
                        val added = Units.convert(item.quantity, item.unit, match.unit)
                        if (added != null) match.copy(quantity = match.quantity + added) else match
                    }
                    else -> match.copy(quantity = null)
                }
                pantry.upsert(merged)
            }
        }
        groceries.deleteChecked(listId)
    }

    // ---- Pantry ----

    suspend fun savePantryItem(item: PantryItemEntity) { if (item.name.isNotBlank()) pantry.upsert(item.copy(name = item.name.trim(), unit = Units.canonical(item.unit))) }
    suspend fun deletePantryItem(item: PantryItemEntity) = pantry.delete(item)

    // ---- Budget ----

    suspend fun addSpend(day: LocalDate, cents: Long, note: String) = budget.insert(SpendEntryEntity(epochDay = day.toEpochDay(), amountCents = cents, note = note.trim()))
    suspend fun deleteSpend(entry: SpendEntryEntity) = budget.delete(entry)

    fun periodRange(today: LocalDate, period: BudgetPeriod) = Dates.period(today, period)

    // ---- Household ----

    suspend fun saveMember(member: HouseholdMemberEntity) { if (member.name.isNotBlank()) household.upsert(member.copy(name = member.name.trim(), portion = member.portion.coerceIn(0.25, 4.0))) }
    suspend fun deleteMember(member: HouseholdMemberEntity) = household.delete(member)

    // ---- Backup ----

    suspend fun snapshot(appVersion: String): BackupFile = BackupFile(
        formatVersion = BackupFile.CURRENT_FORMAT,
        appVersion = appVersion,
        exportedAtMillis = clock(),
        recipes = recipes.allRecipes(),
        ingredients = recipes.allIngredients(),
        plannedMeals = plan.all(),
        groceryLists = groceries.lists(),
        groceryItems = groceries.allItems(),
        pantry = pantry.all(),
        spending = budget.all(),
        templates = templates.all(),
        templateMeals = templates.allMeals(),
        members = household.all(),
        settings = settingsDao.all(),
    )

    /** Replaces all data with [backup] in one transaction; nothing changes if any insert fails. */
    suspend fun restore(backup: BackupFile) = db.withTransaction {
        db.maintenance().clearAll()
        backup.recipes.forEach { recipes.insert(it) }
        recipes.insertIngredients(backup.ingredients)
        plan.insertAll(backup.plannedMeals)
        backup.groceryLists.forEach { groceries.insertList(it) }
        groceries.insertItems(backup.groceryItems)
        backup.pantry.forEach { pantry.upsert(it) }
        backup.spending.forEach { budget.insert(it) }
        backup.templates.forEach { templates.insert(it) }
        templates.insertMeals(backup.templateMeals)
        backup.members.forEach { household.upsert(it) }
        backup.settings.forEach { settingsDao.put(it) }
    }
}
