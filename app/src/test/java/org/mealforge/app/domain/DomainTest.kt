package org.mealforge.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UnitsTest {
    @Test fun `aliases resolve to canonical units`() {
        assertEquals("tbsp", Units.canonical("Tablespoons"))
        assertEquals("lb", Units.canonical("lbs"))
        assertEquals("", Units.canonical("each"))
        assertEquals("clove", Units.canonical(" clove "))
    }

    @Test fun `converts between compatible units`() {
        assertEquals(3.0, Units.convert(1.0, "tbsp", "tsp")!!, 1e-6)
        assertEquals(16.0, Units.convert(1.0, "lb", "oz")!!, 1e-6)
        assertEquals(1000.0, Units.convert(1.0, "kg", "g")!!, 1e-6)
        assertNull(Units.convert(1.0, "cup", "g"))
        assertNull(Units.convert(1.0, "clove", "can"))
    }
}

class QuantitiesTest {
    @Test fun `parses fractions and mixed numbers`() {
        assertEquals(0.5, Quantities.parse("1/2")!!, 1e-9)
        assertEquals(1.5, Quantities.parse("1 1/2")!!, 1e-9)
        assertEquals(1.5, Quantities.parse("1½")!!, 1e-9)
        assertEquals(2.25, Quantities.parse("2,25")!!, 1e-9)
        assertNull(Quantities.parse(""))
        assertNull(Quantities.parse("abc"))
        assertNull(Quantities.parse("1/0"))
    }

    @Test fun `formats friendly quantities`() {
        assertEquals("2", Quantities.format(2.0))
        assertEquals("½", Quantities.format(0.5))
        assertEquals("1¼", Quantities.format(1.25))
        assertEquals("⅓", Quantities.format(1.0 / 3))
        assertEquals("2.4", Quantities.format(2.4))
        assertEquals("1½ cup", Quantities.describe(1.5, "cup"))
        assertEquals("to taste", Quantities.describe(null, "to taste"))
    }
}

class GroceryBuilderTest {
    private fun need(name: String, q: Double?, unit: String, dept: String = "Other") = IngredientNeed(name, q, unit, dept)

    @Test fun `combines same ingredient across compatible units`() {
        val lines = GroceryBuilder.build(listOf(need("Olive oil", 2.0, "tbsp"), need("olive oil", 3.0, "tsp")))
        assertEquals(1, lines.size)
        assertEquals("tbsp", lines[0].unit)
        assertEquals(3.0, lines[0].quantity!!, 1e-6)
    }

    @Test fun `keeps incompatible units separate`() {
        val lines = GroceryBuilder.build(listOf(need("garlic", 2.0, "clove"), need("garlic", 1.0, "tsp")))
        assertEquals(2, lines.size)
    }

    @Test fun `plural and singular names merge`() {
        val lines = GroceryBuilder.build(listOf(need("Tomatoes", 2.0, ""), need("tomato", 1.0, "")))
        assertEquals(1, lines.size)
        assertEquals(3.0, lines[0].quantity!!, 1e-6)
    }

    @Test fun `pantry stock is subtracted and covered items dropped`() {
        val lines = GroceryBuilder.build(
            listOf(need("rice", 2.0, "cup"), need("salt", null, ""), need("eggs", 8.0, "")),
            listOf(PantryStock("rice", 0.5, "cup"), PantryStock("salt", null, ""), PantryStock("egg", 12.0, "")),
        )
        assertEquals(listOf("rice"), lines.map { it.name })
        assertEquals(1.5, lines[0].quantity!!, 1e-6)
    }

    @Test fun `unknown amounts stay on the list`() {
        val lines = GroceryBuilder.build(listOf(need("basil", null, ""), need("basil", null, "")))
        assertEquals(1, lines.size)
        assertNull(lines[0].quantity)
    }

    @Test fun `scales by servings`() {
        assertEquals(3.0, GroceryBuilder.scale(1.5, 2, 4)!!, 1e-9)
        assertNull(GroceryBuilder.scale(null, 2, 4))
        assertEquals(4.0, GroceryBuilder.scale(1.0, 0, 4)!!, 1e-9)
    }

    @Test fun `department guess prefers longest hint`() {
        assertEquals("Spices", Departments.guess("black pepper"))
        assertEquals("Produce", Departments.guess("red bell pepper"))
        assertEquals("Meat & Seafood", Departments.guess("Chicken thighs"))
        assertEquals("Other", Departments.guess("xylophone"))
    }
}

class NutritionAndPrefsTest {
    @Test fun `per person totals track missing and estimated`() {
        val t = NutritionMath.perPerson(listOf(NutritionFacts(500, 30.0, 50.0, 10.0, estimated = true), NutritionFacts(), NutritionFacts(200)))
        assertEquals(700, t.calories)
        assertEquals(30.0, t.proteinG, 1e-9)
        assertEquals(3, t.meals)
        assertEquals(1, t.mealsMissingData)
        assertEquals(1, t.mealsEstimated)
    }

    @Test fun `allergens detected from ingredient names`() {
        val found = Allergens.detect(listOf("Shredded cheddar", "flour tortillas", "Peanut butter"))
        assertTrue("Milk" in found)
        assertTrue("Wheat / gluten" in found)
        assertTrue("Peanut" in found)
        assertTrue("Fish" !in found)
    }

    @Test fun `exclusions match plural forms`() {
        assertEquals(listOf("peanut"), Preferences.conflicts(listOf("roasted peanuts", "rice"), listOf("peanut", "pork")))
        assertEquals(listOf("a", "B"), Preferences.parseList(" a, B ,, b\n"))
    }

    @Test fun `pantry coverage`() {
        assertEquals(0.5, PantryMatch.coverage(listOf("rice", "Eggs"), listOf("egg")), 1e-9)
        assertEquals(0.0, PantryMatch.coverage(emptyList(), listOf("egg")), 1e-9)
    }
}

class MoneyAndDatesTest {
    @Test fun `money parses to cents without float drift`() {
        assertEquals(1999L, Money.parseCents("19.99"))
        assertEquals(1050L, Money.parseCents("$10.5"))
        assertEquals(700L, Money.parseCents("7"))
        assertNull(Money.parseCents(""))
        assertNull(Money.parseCents("-3"))
        assertEquals("$12.05", Money.format(1205))
        assertEquals("-€0.50", Money.format(-50, "€"))
    }

    @Test fun `periods start on monday and month start`() {
        val wed = LocalDate.of(2026, 10, 28)
        val week = Dates.period(wed, BudgetPeriod.WEEK)
        assertEquals(LocalDate.of(2026, 10, 26), week.start)
        assertEquals(LocalDate.of(2026, 11, 1), week.endInclusive)
        val month = Dates.period(wed, BudgetPeriod.MONTH)
        assertEquals(LocalDate.of(2026, 10, 1), month.start)
        assertEquals(LocalDate.of(2026, 10, 31), month.endInclusive)
    }
}
