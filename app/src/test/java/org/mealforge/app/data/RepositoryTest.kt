package org.mealforge.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class RepositoryTest {
    private lateinit var context: Context
    private lateinit var db: MealForgeDatabase
    private lateinit var repo: Repository
    private val monday = LocalDate.of(2026, 10, 26)

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MealForgeDatabase::class.java).allowMainThreadQueries().build()
        repo = Repository(db) { 1_000L }
    }

    @After fun tearDown() = db.close()

    private suspend fun recipe(name: String, servings: Int, vararg ings: Triple<String, Double?, String>): Long =
        repo.saveRecipe(
            RecipeEntity(name = name, servings = servings, steps = listOf("Cook", " ", "Eat")),
            ings.map { IngredientEntity(recipeId = 0, position = 0, name = it.first, quantity = it.second, unit = it.third) },
        )

    @Test fun `save recipe trims steps and canonicalises units`() = runTest {
        val id = recipe("Toast", 1, Triple("bread", 2.0, "pieces"), Triple("", 1.0, "g"), Triple("butter", 1.0, "Tablespoon"))
        val saved = repo.recipe(id).first()!!
        assertEquals(listOf("Cook", "Eat"), saved.recipe.steps)
        assertEquals(listOf("bread", "butter"), saved.sortedIngredients.map { it.name })
        assertEquals(listOf("", "tbsp"), saved.sortedIngredients.map { it.unit })
        assertEquals(1_000L, saved.recipe.createdAt)
    }

    @Test fun `editing a recipe replaces its ingredients`() = runTest {
        val id = recipe("Soup", 2, Triple("carrot", 2.0, ""), Triple("onion", 1.0, ""))
        val r = repo.recipe(id).first()!!
        repo.saveRecipe(r.recipe.copy(name = "Better soup"), listOf(IngredientEntity(recipeId = id, position = 0, name = "leek", quantity = 1.0)))
        val updated = repo.recipe(id).first()!!
        assertEquals("Better soup", updated.recipe.name)
        assertEquals(listOf("leek"), updated.ingredients.map { it.name })
    }

    @Test fun `grocery list is generated from plan, scaled, merged and pantry-aware`() = runTest {
        val a = recipe("A", 2, Triple("rice", 1.0, "cup"), Triple("chicken", 1.0, "lb"))
        val b = recipe("B", 4, Triple("rice", 2.0, "cup"), Triple("salt", null, ""))
        val c = recipe("C", 2, Triple("caviar", 1.0, "oz"))
        repo.addMeal(monday, MealSlot.DINNER, a, servings = 4)          // rice 2 cup, chicken 2 lb
        repo.addMeal(monday.plusDays(1), MealSlot.LUNCH, b, servings = 2) // rice 1 cup, salt
        repo.addMeal(monday.plusDays(2), MealSlot.DINNER, c, servings = 2, leftover = true) // ignored
        repo.addMeal(monday.plusDays(8), MealSlot.DINNER, c, servings = 2) // next week, ignored
        repo.savePantryItem(PantryItemEntity(name = "Salt"))
        repo.savePantryItem(PantryItemEntity(name = "rice", quantity = 0.5, unit = "cup"))

        val listId = repo.ensureActiveList()
        repo.addGroceryItem(listId, "Dish soap")
        assertEquals(2, repo.generateFromPlan(listId, monday, monday.plusDays(6)))
        val items = db.groceries().items(listId).associateBy { it.name }
        assertEquals(setOf("rice", "chicken", "Dish soap"), items.keys)
        assertEquals(2.5, items.getValue("rice").quantity!!, 1e-6)
        assertEquals(2.0, items.getValue("chicken").quantity!!, 1e-6)
        assertFalse(items.getValue("Dish soap").fromPlan)

        // Regenerating replaces plan items but keeps manual ones.
        repo.generateFromPlan(listId, monday, monday.plusDays(6))
        assertEquals(3, db.groceries().items(listId).size)
    }

    @Test fun `finishing shopping logs spend, stocks pantry and clears checked`() = runTest {
        val listId = repo.ensureActiveList()
        repo.addGroceryItem(listId, "milk", 1.0, "l", priceCents = 150)
        repo.addGroceryItem(listId, "bread")
        repo.savePantryItem(PantryItemEntity(name = "Milk", quantity = 500.0, unit = "ml"))
        val milk = db.groceries().items(listId).first { it.name == "milk" }
        repo.updateGroceryItem(milk.copy(checked = true))

        repo.finishShopping(listId, totalCents = null, addToPantry = true, today = monday)

        assertEquals(listOf("bread"), db.groceries().items(listId).map { it.name })
        assertEquals(150L, db.budget().all().single().amountCents)
        val pantryMilk = db.pantry().all().single()
        assertEquals(1500.0, pantryMilk.quantity!!, 1e-6)
        assertEquals("ml", pantryMilk.unit)
    }

    @Test fun `copy week, repeat and templates`() = runTest {
        val a = recipe("A", 2)
        val id = repo.addMeal(monday, MealSlot.DINNER, a, 2)
        repo.copyWeek(monday, monday.plusWeeks(1))
        assertEquals(1, db.plan().range(monday.plusWeeks(1).toEpochDay(), monday.plusWeeks(1).toEpochDay() + 6).size)

        val meal = db.plan().all().first { it.id == id }
        repo.repeatMeal(meal, 7, 2)
        assertEquals(4, db.plan().all().size)

        repo.saveWeekAsTemplate(monday, "Usual")
        val template = db.templates().all().single()
        repo.clearWeek(monday.plusWeeks(5))
        repo.applyTemplate(template.id, monday.plusWeeks(5))
        assertEquals(1, db.plan().range(monday.plusWeeks(5).toEpochDay(), monday.plusWeeks(5).toEpochDay() + 6).size)

        repo.deleteRecipe(a)
        assertTrue("cascade removes planned meals", db.plan().all().isEmpty())
        assertTrue("cascade removes template meals", db.templates().allMeals().isEmpty())
    }

    @Test fun `household members drive default servings`() = runTest {
        repo.setSetting(AppSettings.SERVINGS, "3")
        assertEquals(3, repo.defaultServings())
        repo.saveMember(HouseholdMemberEntity(name = "Sam"))
        repo.saveMember(HouseholdMemberEntity(name = "Kid", portion = 0.5))
        assertEquals(2, repo.defaultServings())
        val a = recipe("A", 2)
        repo.addMeal(monday, MealSlot.BREAKFAST, a)
        assertEquals(2, db.plan().all().single().servings)
    }

    @Test fun `backup round trip restores everything`() = runTest {
        val a = recipe("A", 2, Triple("rice", 1.0, "cup"))
        repo.addMeal(monday, MealSlot.DINNER, a, 2)
        repo.savePantryItem(PantryItemEntity(name = "oil", expiresOnEpochDay = monday.toEpochDay()))
        repo.addSpend(monday, 999, "Market")
        repo.saveMember(HouseholdMemberEntity(name = "Alex", exclusions = listOf("peanut")))
        repo.saveWeekAsTemplate(monday, "T")
        val listId = repo.ensureActiveList()
        repo.addGroceryItem(listId, "apples", 3.0, "")

        val text = BackupFile.encode(repo.snapshot("test"))
        val before = repo.snapshot("test")

        repo.deleteRecipe(a)
        repo.addSpend(monday, 1, "noise")
        repo.restore(BackupFile.decode(text))

        val after = repo.snapshot("test")
        assertEquals(before.copy(exportedAtMillis = 0), after.copy(exportedAtMillis = 0))
        assertEquals(listOf("peanut"), db.household().all().single().exclusions)
    }

    @Test fun `backup decode rejects bad files`() {
        assertThrows(IllegalArgumentException::class.java) { BackupFile.decode("not json") }
        val newer = BackupFile.encode(BackupFile(formatVersion = 99, appVersion = "9", exportedAtMillis = 0))
        assertThrows(IllegalArgumentException::class.java) { BackupFile.decode(newer) }
        val orphan = BackupFile.encode(
            BackupFile(formatVersion = 1, appVersion = "1", exportedAtMillis = 0, ingredients = listOf(IngredientEntity(id = 1, recipeId = 42, position = 0, name = "x"))),
        )
        assertThrows(IllegalArgumentException::class.java) { BackupFile.decode(orphan) }
    }

    @Test fun `seeder adds starter recipes once and imports legacy prefs`() = runTest {
        context.getSharedPreferences(Seeder.LEGACY_PREFS, Context.MODE_PRIVATE).edit()
            .putString("groceries", """["Rice","Eggs"]""")
            .putString("meal_plan", """["Bean chili","Veggie pasta","Unknown","Baked salmon","Breakfast burrito","Veggie pasta","Bean chili"]""")
            .putInt("servings", 5)
            .commit()
        val seeder = Seeder(context, db, repo)
        seeder.run(today = monday.plusDays(2))
        seeder.run(today = monday.plusDays(2))

        assertEquals(StarterRecipes.all.size, db.recipes().count())
        assertEquals("5", db.settings().get(AppSettings.SERVINGS))
        val listId = repo.ensureActiveList()
        assertEquals(listOf("Eggs", "Rice"), db.groceries().items(listId).map { it.name }.sorted())
        val plan = db.plan().range(monday.toEpochDay(), monday.toEpochDay() + 6)
        assertEquals(6, plan.size) // "Unknown" has no matching starter recipe
        assertTrue(plan.all { it.slot == MealSlot.DINNER && it.servings == 5 })
    }

    @Test fun `starter recipes are complete`() {
        StarterRecipes.all.forEach { s ->
            assertTrue(s.recipe.name, s.ingredients.isNotEmpty())
            assertTrue(s.recipe.name, s.recipe.steps.isNotEmpty())
            assertNotNull(s.recipe.caloriesPerServing)
        }
        assertNull(StarterRecipes.all.firstOrNull { it.recipe.name.isBlank() })
    }
}
