package org.mealforge.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Exact, machine-readable copy of everything MealForge stores. Restoring replaces all current data. */
@Serializable
data class BackupFile(
    val formatVersion: Int,
    val appVersion: String,
    val exportedAtMillis: Long,
    val recipes: List<RecipeEntity> = emptyList(),
    val ingredients: List<IngredientEntity> = emptyList(),
    val plannedMeals: List<PlannedMealEntity> = emptyList(),
    val groceryLists: List<GroceryListEntity> = emptyList(),
    val groceryItems: List<GroceryItemEntity> = emptyList(),
    val pantry: List<PantryItemEntity> = emptyList(),
    val spending: List<SpendEntryEntity> = emptyList(),
    val templates: List<PlanTemplateEntity> = emptyList(),
    val templateMeals: List<PlanTemplateMealEntity> = emptyList(),
    val members: List<HouseholdMemberEntity> = emptyList(),
    val settings: List<SettingEntity> = emptyList(),
) {
    companion object {
        const val CURRENT_FORMAT = 1
        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

        fun encode(backup: BackupFile): String = json.encodeToString(serializer(), backup)

        /** Parses and validates a backup, throwing [IllegalArgumentException] with a user-readable message. */
        fun decode(text: String): BackupFile {
            val backup = try {
                json.decodeFromString(serializer(), text)
            } catch (e: Exception) {
                throw IllegalArgumentException("This file isn't a MealForge backup.", e)
            }
            require(backup.formatVersion in 1..CURRENT_FORMAT) { "This backup was made by a newer MealForge. Update the app, then try again." }
            val recipeIds = backup.recipes.map { it.id }.toSet()
            require(backup.ingredients.all { it.recipeId in recipeIds }) { "Backup is damaged: ingredients reference missing recipes." }
            require(backup.plannedMeals.all { it.recipeId in recipeIds }) { "Backup is damaged: meal plan references missing recipes." }
            val listIds = backup.groceryLists.map { it.id }.toSet()
            require(backup.groceryItems.all { it.listId in listIds }) { "Backup is damaged: grocery items reference missing lists." }
            val templateIds = backup.templates.map { it.id }.toSet()
            require(backup.templateMeals.all { it.templateId in templateIds && it.recipeId in recipeIds }) { "Backup is damaged: templates reference missing data." }
            return backup
        }
    }
}
