package org.mealforge.app.domain

/** One ingredient requirement, already scaled to the servings it is needed for. */
data class IngredientNeed(val name: String, val quantity: Double?, val unit: String, val department: String)

/** Something already on hand. A null quantity means "plenty" and fully covers any need. */
data class PantryStock(val name: String, val quantity: Double?, val unit: String)

data class GroceryLine(val name: String, val quantity: Double?, val unit: String, val department: String)

object Names {
    /** Comparison key for ingredient names: lowercase, single spaces, naive singular form. */
    fun key(name: String): String {
        val words = name.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return ""
        return (words.dropLast(1) + singular(words.last())).joinToString(" ")
    }

    private fun singular(word: String): String = when {
        word.length > 4 && word.endsWith("ies") -> word.dropLast(3) + "y"
        word.length > 4 && (word.endsWith("oes") || word.endsWith("ches") || word.endsWith("shes")) -> word.dropLast(2)
        word.length > 3 && word.endsWith("s") && !word.endsWith("ss") && !word.endsWith("us") -> word.dropLast(1)
        else -> word
    }
}

object GroceryBuilder {
    private const val EPSILON = 1e-6

    fun scale(quantity: Double?, recipeServings: Int, targetServings: Int): Double? =
        quantity?.let { it * targetServings / recipeServings.coerceAtLeast(1) }

    /**
     * Combines needs with the same ingredient and compatible units, then subtracts pantry stock.
     * The first unit seen for an ingredient is used for display. Lines fully covered by the pantry are dropped.
     */
    fun build(needs: List<IngredientNeed>, pantry: List<PantryStock> = emptyList()): List<GroceryLine> {
        data class Acc(val displayName: String, val unit: String, val department: String, var base: Double?, var hasUnknownAmount: Boolean)

        val groups = LinkedHashMap<Pair<String, String>, Acc>()
        for (need in needs) {
            val nameKey = Names.key(need.name)
            if (nameKey.isEmpty()) continue
            val unit = Units.canonical(need.unit)
            val key = nameKey to Units.groupKey(unit)
            val acc = groups.getOrPut(key) { Acc(need.name.trim(), unit, need.department, null, false) }
            if (need.quantity == null) {
                acc.hasUnknownAmount = true
            } else {
                acc.base = (acc.base ?: 0.0) + Units.toBase(need.quantity, unit)
            }
        }

        for (stock in pantry) {
            val nameKey = Names.key(stock.name)
            val matches = groups.filterKeys { it.first == nameKey }
            if (matches.isEmpty()) continue
            if (stock.quantity == null) {
                matches.keys.forEach { groups.remove(it) }
                continue
            }
            val acc = groups[nameKey to Units.groupKey(Units.canonical(stock.unit))] ?: continue
            val base = acc.base ?: continue
            acc.base = base - Units.toBase(stock.quantity, Units.canonical(stock.unit))
        }

        return groups.values.mapNotNull { acc ->
            val base = acc.base
            when {
                base == null -> GroceryLine(acc.displayName, null, acc.unit, acc.department)
                base <= EPSILON && !acc.hasUnknownAmount -> null
                base <= EPSILON -> GroceryLine(acc.displayName, null, acc.unit, acc.department)
                else -> GroceryLine(acc.displayName, Units.fromBase(base, acc.unit), acc.unit, acc.department)
            }
        }
    }
}

object Departments {
    val all = listOf("Produce", "Meat & Seafood", "Dairy & Eggs", "Bakery", "Pantry", "Frozen", "Spices", "Beverages", "Household", "Other")

    private val hints = mapOf(
        "Produce" to listOf("onion", "garlic", "tomato", "potato", "carrot", "lettuce", "spinach", "pepper", "apple", "banana", "lemon", "lime", "broccoli", "cucumber", "avocado", "cilantro", "parsley", "basil", "ginger", "celery", "mushroom", "zucchini", "berry", "kale", "cabbage", "scallion", "squash"),
        "Meat & Seafood" to listOf("chicken", "beef", "pork", "turkey", "salmon", "tuna", "shrimp", "fish", "sausage", "bacon", "ham", "lamb", "steak"),
        "Dairy & Eggs" to listOf("milk", "cheese", "butter", "yogurt", "cream", "egg", "parmesan", "mozzarella", "cheddar", "feta"),
        "Bakery" to listOf("bread", "tortilla", "bun", "roll", "bagel", "pita", "naan"),
        "Frozen" to listOf("frozen", "ice cream", "peas"),
        "Spices" to listOf("salt", "cumin", "paprika", "oregano", "chili powder", "cinnamon", "thyme", "black pepper", "spice", "seasoning"),
        "Beverages" to listOf("juice", "coffee", "tea", "soda", "water"),
        "Pantry" to listOf("rice", "pasta", "flour", "sugar", "oil", "vinegar", "bean", "lentil", "oat", "sauce", "broth", "stock", "honey", "can", "noodle", "quinoa", "peanut butter", "soy"),
    )

    /** Best-guess department for a new ingredient name. */
    fun guess(name: String): String {
        val n = name.lowercase()
        // Longest hint wins so "black pepper" lands in Spices, not Produce.
        return hints.entries
            .flatMap { (dept, words) -> words.map { it to dept } }
            .sortedByDescending { it.first.length }
            .firstOrNull { n.contains(it.first) }?.second ?: "Other"
    }
}
