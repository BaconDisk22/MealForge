package org.mealforge.app.data

import android.content.Context
import org.json.JSONArray
import org.mealforge.app.domain.Dates
import java.time.LocalDate

/** First-launch setup: seeds starter recipes and imports data saved by MealForge 0.2's SharedPreferences store. */
class Seeder(private val context: Context, private val db: MealForgeDatabase, private val repo: Repository) {

    suspend fun run(today: LocalDate = LocalDate.now()) {
        val settings = db.settings()
        val starterIds = mutableMapOf<String, Long>()
        if (settings.get(AppSettings.STARTER_SEEDED) != "true") {
            for (starter in StarterRecipes.all) {
                starterIds[starter.recipe.name] = repo.saveRecipe(starter.recipe, starter.ingredients)
            }
            repo.setSetting(AppSettings.STARTER_SEEDED, "true")
        }
        val listId = repo.ensureActiveList()
        if (settings.get(AppSettings.LEGACY_IMPORTED) != "true") {
            importLegacy(listId, starterIds, today)
            repo.setSetting(AppSettings.LEGACY_IMPORTED, "true")
        }
    }

    private suspend fun importLegacy(listId: Long, starterIds: Map<String, Long>, today: LocalDate) {
        val prefs = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        if (prefs.all.isEmpty()) return
        if (prefs.contains("servings")) {
            repo.setSetting(AppSettings.SERVINGS, prefs.getInt("servings", 2).coerceAtLeast(1).toString())
        }
        readList(prefs.getString("groceries", null)).forEach { repo.addGroceryItem(listId, it) }
        val week = Dates.weekStart(today)
        readList(prefs.getString("meal_plan", null)).take(7).forEachIndexed { day, name ->
            starterIds[name]?.let { repo.addMeal(week.plusDays(day.toLong()), MealSlot.DINNER, it) }
        }
    }

    private fun readList(json: String?): List<String> = try {
        val array = JSONArray(json ?: return emptyList())
        List(array.length()) { array.getString(it) }.filter { it.isNotBlank() }
    } catch (_: Exception) {
        emptyList()
    }

    companion object {
        const val LEGACY_PREFS = "mealforge_local_v1"
    }
}
