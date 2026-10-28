package org.mealforge.app

import android.content.Context
import org.json.JSONArray

/** Small offline-only store for Phase 2. No account, network, or permissions required. */
internal class MealStore(context: Context) {
    private val prefs = context.getSharedPreferences("mealforge_local_v1", Context.MODE_PRIVATE)

    fun loadList(key: String, defaults: List<String>): List<String> {
        val saved = prefs.getString(key, null) ?: return defaults
        return try {
            val array = JSONArray(saved)
            List(array.length()) { index -> array.getString(index) }
        } catch (_: Exception) {
            defaults
        }
    }

    fun saveList(key: String, values: List<String>) {
        val array = JSONArray()
        values.forEach { array.put(it) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    fun loadServings(): Int = prefs.getInt("servings", 2).coerceAtLeast(1)

    fun saveServings(value: Int) {
        prefs.edit().putInt("servings", value.coerceAtLeast(1)).apply()
    }
}
