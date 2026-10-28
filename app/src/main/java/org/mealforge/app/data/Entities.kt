package org.mealforge.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable
import org.mealforge.app.domain.NutritionFacts

enum class MealSlot(val label: String) { BREAKFAST("Breakfast"), LUNCH("Lunch"), DINNER("Dinner"), SNACK("Snack") }

@Serializable
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: String = "Dinner",
    val tags: List<String> = emptyList(),
    val servings: Int = 2,
    val prepMinutes: Int = 0,
    val cookMinutes: Int = 0,
    val steps: List<String> = emptyList(),
    val favorite: Boolean = false,
    val caloriesPerServing: Int? = null,
    val proteinPerServing: Double? = null,
    val carbsPerServing: Double? = null,
    val fatPerServing: Double? = null,
    val nutritionEstimated: Boolean = false,
    val photoUri: String? = null,
    val createdAt: Long = 0,
)

val RecipeEntity.totalMinutes: Int get() = prepMinutes + cookMinutes
val RecipeEntity.nutrition: NutritionFacts
    get() = NutritionFacts(caloriesPerServing, proteinPerServing, carbsPerServing, fatPerServing, nutritionEstimated)

@Serializable
@Entity(
    tableName = "ingredients",
    foreignKeys = [ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recipeId")],
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val position: Int,
    val name: String,
    val quantity: Double? = null,
    val unit: String = "",
    val department: String = "Other",
    val note: String = "",
)

data class RecipeWithIngredients(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId") val ingredients: List<IngredientEntity>,
)

val RecipeWithIngredients.sortedIngredients: List<IngredientEntity> get() = ingredients.sortedBy { it.position }

@Serializable
@Entity(
    tableName = "planned_meals",
    foreignKeys = [ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recipeId"), Index("epochDay")],
)
data class PlannedMealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val slot: MealSlot,
    val recipeId: Long,
    val servings: Int,
    /** Leftover meals reuse food cooked earlier, so they add nothing to the grocery list. */
    val leftover: Boolean = false,
)

data class PlannedMealWithRecipe(
    @Embedded val meal: PlannedMealEntity,
    @Relation(parentColumn = "recipeId", entityColumn = "id") val recipe: RecipeEntity,
)

@Serializable
@Entity(tableName = "grocery_lists")
data class GroceryListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = 0,
)

@Serializable
@Entity(
    tableName = "grocery_items",
    foreignKeys = [ForeignKey(entity = GroceryListEntity::class, parentColumns = ["id"], childColumns = ["listId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("listId")],
)
data class GroceryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    val quantity: Double? = null,
    val unit: String = "",
    val department: String = "Other",
    val checked: Boolean = false,
    val estimatedPriceCents: Long? = null,
    val actualPriceCents: Long? = null,
    /** True when generated from the meal plan; regenerating replaces these but keeps manual items. */
    val fromPlan: Boolean = false,
)

@Serializable
@Entity(tableName = "pantry_items")
data class PantryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double? = null,
    val unit: String = "",
    val expiresOnEpochDay: Long? = null,
)

@Serializable
@Entity(tableName = "spend_entries", indices = [Index("epochDay")])
data class SpendEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val amountCents: Long,
    val note: String = "",
)

@Serializable
@Entity(tableName = "plan_templates")
data class PlanTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Serializable
@Entity(
    tableName = "plan_template_meals",
    foreignKeys = [
        ForeignKey(entity = PlanTemplateEntity::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("templateId"), Index("recipeId")],
)
data class PlanTemplateMealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    /** 0 = Monday ... 6 = Sunday. */
    val dayOffset: Int,
    val slot: MealSlot,
    val recipeId: Long,
    val servings: Int,
    val leftover: Boolean = false,
)

@Serializable
@Entity(tableName = "household_members")
data class HouseholdMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Servings this person eats per meal, e.g. 0.5 for a small child. */
    val portion: Double = 1.0,
    val exclusions: List<String> = emptyList(),
)

@Serializable
@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)
