package org.mealforge.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toList(value: String): List<String> = runCatching { Json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
}

@Dao
interface RecipeDao {
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY favorite DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observe(id: Long): Flow<RecipeWithIngredients?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun get(id: Long): RecipeWithIngredients?

    @Transaction
    @Query("SELECT * FROM recipes WHERE id IN (:ids)")
    suspend fun getAll(ids: Collection<Long>): List<RecipeWithIngredients>

    @Query("SELECT * FROM recipes")
    suspend fun allRecipes(): List<RecipeEntity>

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int

    @Insert
    suspend fun insert(recipe: RecipeEntity): Long

    @Update
    suspend fun update(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE recipes SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("SELECT * FROM ingredients")
    suspend fun allIngredients(): List<IngredientEntity>

    @Insert
    suspend fun insertIngredients(items: List<IngredientEntity>)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredients(recipeId: Long)
}

@Dao
interface PlanDao {
    @Transaction
    @Query("SELECT * FROM planned_meals WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay, slot")
    fun observeRange(from: Long, to: Long): Flow<List<PlannedMealWithRecipe>>

    @Query("SELECT * FROM planned_meals WHERE epochDay BETWEEN :from AND :to")
    suspend fun range(from: Long, to: Long): List<PlannedMealEntity>

    @Query("SELECT * FROM planned_meals")
    suspend fun all(): List<PlannedMealEntity>

    @Insert
    suspend fun insert(meal: PlannedMealEntity): Long

    @Insert
    suspend fun insertAll(meals: List<PlannedMealEntity>)

    @Update
    suspend fun update(meal: PlannedMealEntity)

    @Query("DELETE FROM planned_meals WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM planned_meals WHERE epochDay BETWEEN :from AND :to")
    suspend fun clearRange(from: Long, to: Long)
}

@Dao
interface GroceryDao {
    @Query("SELECT * FROM grocery_lists ORDER BY createdAt, id")
    fun observeLists(): Flow<List<GroceryListEntity>>

    @Query("SELECT * FROM grocery_lists ORDER BY createdAt, id")
    suspend fun lists(): List<GroceryListEntity>

    @Query("SELECT * FROM grocery_items WHERE listId = :listId ORDER BY checked, department, name COLLATE NOCASE")
    fun observeItems(listId: Long): Flow<List<GroceryItemEntity>>

    @Query("SELECT * FROM grocery_items")
    suspend fun allItems(): List<GroceryItemEntity>

    @Query("SELECT * FROM grocery_items WHERE listId = :listId")
    suspend fun items(listId: Long): List<GroceryItemEntity>

    @Insert
    suspend fun insertList(list: GroceryListEntity): Long

    @Update
    suspend fun updateList(list: GroceryListEntity)

    @Query("DELETE FROM grocery_lists WHERE id = :id")
    suspend fun deleteList(id: Long)

    @Insert
    suspend fun insertItem(item: GroceryItemEntity): Long

    @Insert
    suspend fun insertItems(items: List<GroceryItemEntity>)

    @Update
    suspend fun updateItem(item: GroceryItemEntity)

    @Delete
    suspend fun deleteItem(item: GroceryItemEntity)

    @Query("DELETE FROM grocery_items WHERE listId = :listId AND fromPlan = 1")
    suspend fun deletePlanItems(listId: Long)

    @Query("DELETE FROM grocery_items WHERE listId = :listId AND checked = 1")
    suspend fun deleteChecked(listId: Long)
}

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY CASE WHEN expiresOnEpochDay IS NULL THEN 1 ELSE 0 END, expiresOnEpochDay, name COLLATE NOCASE")
    fun observeAll(): Flow<List<PantryItemEntity>>

    @Query("SELECT * FROM pantry_items")
    suspend fun all(): List<PantryItemEntity>

    @Upsert
    suspend fun upsert(item: PantryItemEntity): Long

    @Delete
    suspend fun delete(item: PantryItemEntity)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM spend_entries ORDER BY epochDay DESC, id DESC")
    fun observeAll(): Flow<List<SpendEntryEntity>>

    @Query("SELECT * FROM spend_entries")
    suspend fun all(): List<SpendEntryEntity>

    @Insert
    suspend fun insert(entry: SpendEntryEntity): Long

    @Delete
    suspend fun delete(entry: SpendEntryEntity)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM plan_templates ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PlanTemplateEntity>>

    @Query("SELECT * FROM plan_templates")
    suspend fun all(): List<PlanTemplateEntity>

    @Query("SELECT * FROM plan_template_meals WHERE templateId = :templateId")
    suspend fun meals(templateId: Long): List<PlanTemplateMealEntity>

    @Query("SELECT * FROM plan_template_meals")
    suspend fun allMeals(): List<PlanTemplateMealEntity>

    @Insert
    suspend fun insert(template: PlanTemplateEntity): Long

    @Insert
    suspend fun insertMeals(meals: List<PlanTemplateMealEntity>)

    @Query("DELETE FROM plan_templates WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface HouseholdDao {
    @Query("SELECT * FROM household_members ORDER BY id")
    fun observeAll(): Flow<List<HouseholdMemberEntity>>

    @Query("SELECT * FROM household_members")
    suspend fun all(): List<HouseholdMemberEntity>

    @Upsert
    suspend fun upsert(member: HouseholdMemberEntity): Long

    @Delete
    suspend fun delete(member: HouseholdMemberEntity)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings")
    fun observeAll(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM settings")
    suspend fun all(): List<SettingEntity>

    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(setting: SettingEntity)
}

@Dao
interface MaintenanceDao {
    @Query("DELETE FROM grocery_lists") suspend fun clearGroceryLists()
    @Query("DELETE FROM recipes") suspend fun clearRecipes()
    @Query("DELETE FROM plan_templates") suspend fun clearTemplates()
    @Query("DELETE FROM pantry_items") suspend fun clearPantry()
    @Query("DELETE FROM spend_entries") suspend fun clearSpending()
    @Query("DELETE FROM household_members") suspend fun clearMembers()
    @Query("DELETE FROM settings") suspend fun clearSettings()
}

@Database(
    entities = [
        RecipeEntity::class, IngredientEntity::class, PlannedMealEntity::class,
        GroceryListEntity::class, GroceryItemEntity::class, PantryItemEntity::class,
        SpendEntryEntity::class, PlanTemplateEntity::class, PlanTemplateMealEntity::class,
        HouseholdMemberEntity::class, SettingEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MealForgeDatabase : RoomDatabase() {
    abstract fun recipes(): RecipeDao
    abstract fun plan(): PlanDao
    abstract fun groceries(): GroceryDao
    abstract fun pantry(): PantryDao
    abstract fun budget(): BudgetDao
    abstract fun templates(): TemplateDao
    abstract fun household(): HouseholdDao
    abstract fun settings(): SettingsDao
    abstract fun maintenance(): MaintenanceDao

    companion object {
        const val NAME = "mealforge.db"

        fun create(context: Context): MealForgeDatabase =
            Room.databaseBuilder(context.applicationContext, MealForgeDatabase::class.java, NAME)
                // Future schema changes must ship explicit Migration objects; never fall back to destructive migration.
                .build()
    }
}
