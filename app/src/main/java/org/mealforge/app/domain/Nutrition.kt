package org.mealforge.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Per-serving nutrition for one recipe. Null fields mean "unknown". */
data class NutritionFacts(
    val calories: Int? = null,
    val proteinG: Double? = null,
    val carbsG: Double? = null,
    val fatG: Double? = null,
    val estimated: Boolean = false,
) {
    val isMissing: Boolean get() = calories == null && proteinG == null && carbsG == null && fatG == null
}

data class NutritionTotals(
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val meals: Int = 0,
    val mealsMissingData: Int = 0,
    val mealsEstimated: Int = 0,
)

object NutritionMath {
    /** Totals for one person eating one serving of each listed meal. */
    fun perPerson(meals: List<NutritionFacts>): NutritionTotals = meals.fold(NutritionTotals()) { acc, f ->
        acc.copy(
            calories = acc.calories + (f.calories ?: 0),
            proteinG = acc.proteinG + (f.proteinG ?: 0.0),
            carbsG = acc.carbsG + (f.carbsG ?: 0.0),
            fatG = acc.fatG + (f.fatG ?: 0.0),
            meals = acc.meals + 1,
            mealsMissingData = acc.mealsMissingData + if (f.isMissing) 1 else 0,
            mealsEstimated = acc.mealsEstimated + if (f.estimated && !f.isMissing) 1 else 0,
        )
    }

    /** Divides whole-recipe totals into per-serving facts. */
    fun perServing(totalCalories: Int?, totalProtein: Double?, totalCarbs: Double?, totalFat: Double?, servings: Int): NutritionFacts {
        val s = servings.coerceAtLeast(1)
        return NutritionFacts(totalCalories?.let { it / s }, totalProtein?.div(s), totalCarbs?.div(s), totalFat?.div(s))
    }
}

object Allergens {
    private val keywords: Map<String, List<String>> = linkedMapOf(
        "Milk" to listOf("milk", "cheese", "butter", "cream", "yogurt", "parmesan", "mozzarella", "cheddar", "feta", "whey", "ghee"),
        "Egg" to listOf("egg", "mayonnaise", "mayo"),
        "Peanut" to listOf("peanut"),
        "Tree nuts" to listOf("almond", "walnut", "cashew", "pecan", "pistachio", "hazelnut", "macadamia"),
        "Wheat / gluten" to listOf("flour", "bread", "pasta", "tortilla", "noodle", "wheat", "couscous", "breadcrumb", "pita", "bun", "soy sauce", "barley"),
        "Soy" to listOf("soy", "tofu", "edamame", "tempeh", "miso"),
        "Fish" to listOf("salmon", "tuna", "cod", "fish", "anchovy", "tilapia", "trout"),
        "Shellfish" to listOf("shrimp", "prawn", "crab", "lobster", "scallop", "clam", "mussel", "oyster"),
        "Sesame" to listOf("sesame", "tahini"),
    )

    val names: List<String> get() = keywords.keys.toList()

    /** Allergens whose keywords appear in any ingredient name. Advisory only: always check labels. */
    fun detect(ingredientNames: List<String>): List<String> {
        val lower = ingredientNames.map { it.lowercase() }
        return keywords.filter { (_, words) -> words.any { w -> lower.any { it.contains(w) } } }.keys.toList()
    }
}

object Preferences {
    /** Exclusion terms that match any of the ingredient names. */
    fun conflicts(ingredientNames: List<String>, exclusions: List<String>): List<String> {
        val keys = ingredientNames.map { Names.key(it) }
        return exclusions.map { it.trim() }.filter { it.isNotEmpty() }.filter { term ->
            val t = Names.key(term)
            keys.any { it.contains(t) }
        }
    }

    fun parseList(text: String): List<String> = text.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
}

object PantryMatch {
    /** Fraction (0..1) of a recipe's ingredients that are already in the pantry. */
    fun coverage(ingredientNames: List<String>, pantryNames: List<String>): Double {
        if (ingredientNames.isEmpty()) return 0.0
        val have = pantryNames.map { Names.key(it) }.toSet()
        return ingredientNames.count { Names.key(it) in have }.toDouble() / ingredientNames.size
    }
}

enum class BudgetPeriod { WEEK, MONTH }

object Dates {
    fun weekStart(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun period(today: LocalDate, period: BudgetPeriod): ClosedRange<LocalDate> = when (period) {
        BudgetPeriod.WEEK -> weekStart(today).let { it..it.plusDays(6) }
        BudgetPeriod.MONTH -> today.withDayOfMonth(1).let { it..it.plusMonths(1).minusDays(1) }
    }
}

object Money {
    /** Parses "12", "12.5", "$12.50" into cents. */
    fun parseCents(text: String): Long? {
        val cleaned = text.trim().replace(Regex("[^0-9.,-]"), "").replace(',', '.')
        if (cleaned.isEmpty()) return null
        val value = cleaned.toBigDecimalOrNull() ?: return null
        if (value.signum() < 0) return null
        return value.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
    }

    fun format(cents: Long, symbol: String = "$"): String {
        val sign = if (cents < 0) "-" else ""
        val abs = kotlin.math.abs(cents)
        return "$sign$symbol${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }
}
