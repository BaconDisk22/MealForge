package org.mealforge.app.domain

import kotlin.math.abs
import kotlin.math.roundToLong

enum class UnitKind { MASS, VOLUME, COUNT, OTHER }

/** A known unit. [toBase] converts one of this unit into grams (mass), millilitres (volume) or items (count). */
data class UnitDef(val key: String, val kind: UnitKind, val toBase: Double, val aliases: Set<String>)

object Units {
    val known: List<UnitDef> = listOf(
        UnitDef("g", UnitKind.MASS, 1.0, setOf("g", "gram", "grams", "gr")),
        UnitDef("kg", UnitKind.MASS, 1000.0, setOf("kg", "kilogram", "kilograms", "kilo", "kilos")),
        UnitDef("oz", UnitKind.MASS, 28.349523125, setOf("oz", "ounce", "ounces")),
        UnitDef("lb", UnitKind.MASS, 453.59237, setOf("lb", "lbs", "pound", "pounds")),
        UnitDef("ml", UnitKind.VOLUME, 1.0, setOf("ml", "millilitre", "milliliter", "millilitres", "milliliters")),
        UnitDef("l", UnitKind.VOLUME, 1000.0, setOf("l", "litre", "liter", "litres", "liters")),
        UnitDef("tsp", UnitKind.VOLUME, 4.92892159375, setOf("tsp", "teaspoon", "teaspoons", "t")),
        UnitDef("tbsp", UnitKind.VOLUME, 14.78676478125, setOf("tbsp", "tablespoon", "tablespoons", "tbs", "tbl")),
        UnitDef("fl oz", UnitKind.VOLUME, 29.5735295625, setOf("fl oz", "floz", "fluid ounce", "fluid ounces")),
        UnitDef("cup", UnitKind.VOLUME, 236.5882365, setOf("cup", "cups", "c")),
        UnitDef("pt", UnitKind.VOLUME, 473.176473, setOf("pt", "pint", "pints")),
        UnitDef("qt", UnitKind.VOLUME, 946.352946, setOf("qt", "quart", "quarts")),
        UnitDef("gal", UnitKind.VOLUME, 3785.411784, setOf("gal", "gallon", "gallons")),
        UnitDef("", UnitKind.COUNT, 1.0, setOf("", "each", "ea", "pc", "pcs", "piece", "pieces", "whole", "x")),
        UnitDef("dozen", UnitKind.COUNT, 12.0, setOf("dozen", "doz")),
    )

    private val byAlias: Map<String, UnitDef> = known.flatMap { def -> def.aliases.map { it to def } }.toMap()

    /** Common picks for unit dropdowns in the editor. */
    val suggestions: List<String> = listOf("", "g", "kg", "oz", "lb", "ml", "l", "tsp", "tbsp", "cup", "can", "clove", "bunch", "pinch")

    fun find(unit: String): UnitDef? = byAlias[unit.trim().lowercase().removeSuffix(".")]

    fun kindOf(unit: String): UnitKind = find(unit)?.kind ?: UnitKind.OTHER

    /** Units that can be summed together share a group key; unknown units only group with themselves. */
    fun groupKey(unit: String): String {
        val def = find(unit)
        return if (def == null) "other:" + unit.trim().lowercase() else def.kind.name
    }

    fun toBase(quantity: Double, unit: String): Double = quantity * (find(unit)?.toBase ?: 1.0)

    fun fromBase(base: Double, unit: String): Double = base / (find(unit)?.toBase ?: 1.0)

    /** Converts [quantity] of [from] into [to], or null when the units measure different things. */
    fun convert(quantity: Double, from: String, to: String): Double? =
        if (groupKey(from) != groupKey(to)) null else fromBase(toBase(quantity, from), to)

    /** Canonical spelling for a unit typed by the user ("Tablespoons" -> "tbsp"); unknown units are kept. */
    fun canonical(unit: String): String = find(unit)?.key ?: unit.trim()
}

object Quantities {
    private val fractions = listOf(0.25 to "¼", 0.5 to "½", 0.75 to "¾", 1.0 / 3 to "⅓", 2.0 / 3 to "⅔", 0.125 to "⅛")

    /** Human-friendly quantity: whole numbers, common fractions, otherwise up to two decimals. */
    fun format(value: Double): String {
        if (value <= 0.0) return "0"
        val whole = value.toLong()
        val rest = value - whole
        if (rest < 0.01) return whole.toString()
        if (value < 20) {
            fractions.firstOrNull { abs(it.first - rest) < 0.02 }?.let { (_, glyph) ->
                return if (whole == 0L) glyph else "$whole$glyph"
            }
        }
        val rounded = (value * 100).roundToLong() / 100.0
        return rounded.toString().trimEnd('0').trimEnd('.')
    }

    /** Parses "1.5", "1,5", "1/2", "1 1/2", "½". Returns null when blank or unparseable. */
    fun parse(text: String): Double? {
        val t = text.trim().replace(',', '.')
        if (t.isEmpty()) return null
        fractions.forEach { (v, glyph) ->
            if (t.endsWith(glyph)) {
                val head = t.removeSuffix(glyph).trim()
                val whole = if (head.isEmpty()) 0.0 else head.toDoubleOrNull() ?: return null
                return whole + v
            }
        }
        val parts = t.split(Regex("\\s+"))
        return when (parts.size) {
            1 -> parseSimple(parts[0])
            2 -> {
                val whole = parts[0].toDoubleOrNull() ?: return null
                val frac = parseSimple(parts[1]) ?: return null
                whole + frac
            }
            else -> null
        }?.takeIf { it >= 0 }
    }

    private fun parseSimple(s: String): Double? {
        if ('/' in s) {
            val (n, d) = s.split('/', limit = 2)
            val num = n.toDoubleOrNull() ?: return null
            val den = d.toDoubleOrNull() ?: return null
            return if (den == 0.0) null else num / den
        }
        return s.toDoubleOrNull()
    }

    fun describe(quantity: Double?, unit: String): String = when {
        quantity == null -> unit.ifBlank { "" }
        unit.isBlank() -> format(quantity)
        else -> "${format(quantity)} $unit"
    }
}
