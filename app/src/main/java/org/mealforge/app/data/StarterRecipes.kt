package org.mealforge.app.data

import org.mealforge.app.domain.Departments

/** Original starter collection seeded on first launch. Nutrition values are rough per-serving estimates. */
object StarterRecipes {
    data class Starter(val recipe: RecipeEntity, val ingredients: List<IngredientEntity>)

    private fun i(qty: Double?, unit: String, name: String, dept: String = Departments.guess(name)) =
        IngredientEntity(recipeId = 0, position = 0, name = name, quantity = qty, unit = unit, department = dept)

    private fun r(
        name: String, category: String, servings: Int, prep: Int, cook: Int, tags: List<String>,
        nutrition: List<Number>, description: String, ingredients: List<IngredientEntity>, steps: List<String>,
    ) = Starter(
        RecipeEntity(
            name = name, description = description, category = category, tags = tags, servings = servings,
            prepMinutes = prep, cookMinutes = cook, steps = steps,
            caloriesPerServing = nutrition[0].toInt(), proteinPerServing = nutrition[1].toDouble(),
            carbsPerServing = nutrition[2].toDouble(), fatPerServing = nutrition[3].toDouble(), nutritionEstimated = true,
        ),
        ingredients,
    )

    val all: List<Starter> = listOf(
        r(
            "Garlic chicken & rice", "Dinner", 4, 10, 30, listOf("High protein", "One pan"), listOf(520, 38, 55, 14),
            "Golden garlic chicken thighs simmered with rice in one pan.",
            listOf(i(1.5, "lb", "chicken thighs"), i(1.5, "cup", "rice"), i(6.0, "", "garlic cloves", "Produce"), i(3.0, "cup", "chicken broth", "Pantry"), i(2.0, "tbsp", "olive oil", "Pantry"), i(1.0, "", "lemon"), i(null, "", "salt", "Spices")),
            listOf("Season chicken with salt and brown in oil in a deep pan, 5 minutes per side. Set aside.", "Add sliced garlic and rice; stir 1 minute.", "Pour in broth, nestle chicken on top, cover and simmer 18 minutes.", "Rest 5 minutes, squeeze lemon over and serve."),
        ),
        r(
            "Veggie pasta", "Dinner", 4, 10, 15, listOf("Vegetarian", "Quick"), listOf(450, 15, 70, 12),
            "Pasta tossed with sautéed zucchini, tomatoes and parmesan.",
            listOf(i(1.0, "lb", "pasta"), i(2.0, "", "zucchini"), i(2.0, "cup", "cherry tomatoes", "Produce"), i(3.0, "", "garlic cloves", "Produce"), i(0.5, "cup", "parmesan"), i(3.0, "tbsp", "olive oil", "Pantry"), i(null, "", "fresh basil", "Produce")),
            listOf("Cook pasta in salted water; reserve 1 cup water.", "Sauté sliced zucchini and garlic in oil 5 minutes.", "Add tomatoes and cook until they burst.", "Toss with pasta, a splash of pasta water, parmesan and basil."),
        ),
        r(
            "Breakfast burrito", "Breakfast", 4, 10, 10, listOf("Make ahead"), listOf(430, 22, 35, 22),
            "Scrambled eggs, black beans and cheese wrapped in a warm tortilla.",
            listOf(i(8.0, "", "eggs"), i(4.0, "", "large flour tortillas", "Bakery"), i(15.0, "oz", "black beans", "Pantry"), i(1.0, "cup", "shredded cheddar"), i(0.5, "cup", "salsa", "Pantry"), i(1.0, "tbsp", "butter")),
            listOf("Warm beans in a small pot.", "Scramble eggs in butter until just set.", "Fill tortillas with eggs, beans, cheese and salsa; roll tightly.", "Toast seam-side down in a dry pan 1 minute."),
        ),
        r(
            "Baked salmon", "Dinner", 4, 5, 15, listOf("High protein", "Gluten free"), listOf(410, 34, 12, 24),
            "Lemon-herb salmon baked on a sheet pan with green beans.",
            listOf(i(1.5, "lb", "salmon fillets"), i(1.0, "lb", "green beans", "Produce"), i(1.0, "", "lemon"), i(2.0, "tbsp", "olive oil", "Pantry"), i(1.0, "tsp", "dried dill", "Spices"), i(null, "", "salt", "Spices")),
            listOf("Heat oven to 425°F (220°C).", "Toss green beans with half the oil and spread on a sheet pan.", "Place salmon in the center, brush with remaining oil, season with dill, salt and lemon slices.", "Bake 12–15 minutes until salmon flakes."),
        ),
        r(
            "Bean chili", "Dinner", 6, 15, 35, listOf("Vegan", "Freezer friendly"), listOf(380, 18, 58, 8),
            "Hearty three-bean chili with warming spices.",
            listOf(i(1.0, "", "onion"), i(1.0, "", "bell pepper"), i(3.0, "", "garlic cloves", "Produce"), i(30.0, "oz", "kidney beans", "Pantry"), i(15.0, "oz", "black beans", "Pantry"), i(28.0, "oz", "crushed tomatoes", "Pantry"), i(2.0, "tbsp", "chili powder", "Spices"), i(1.0, "tsp", "ground cumin", "Spices"), i(1.0, "tbsp", "olive oil", "Pantry")),
            listOf("Sauté diced onion and pepper in oil 6 minutes; add garlic and spices for 1 minute.", "Add drained beans and tomatoes.", "Simmer 25–30 minutes, stirring now and then.", "Season to taste and serve with your favorite toppings."),
        ),
        r(
            "Overnight oats", "Breakfast", 2, 5, 0, listOf("Vegetarian", "Make ahead", "No cook"), listOf(340, 12, 52, 9),
            "Creamy oats soaked overnight with berries.",
            listOf(i(1.0, "cup", "rolled oats", "Pantry"), i(1.0, "cup", "milk"), i(0.5, "cup", "plain yogurt"), i(1.0, "cup", "mixed berries", "Produce"), i(1.0, "tbsp", "honey", "Pantry")),
            listOf("Stir oats, milk, yogurt and honey in a jar.", "Top with berries, cover and chill overnight.", "Stir and eat cold, or warm for 1 minute."),
        ),
        r(
            "Turkey taco bowls", "Dinner", 4, 10, 15, listOf("High protein", "Quick"), listOf(510, 32, 48, 18),
            "Seasoned ground turkey over rice with corn, beans and salsa.",
            listOf(i(1.0, "lb", "ground turkey"), i(1.0, "cup", "rice", "Pantry"), i(1.0, "cup", "corn", "Frozen"), i(15.0, "oz", "black beans", "Pantry"), i(1.0, "", "avocado"), i(0.5, "cup", "salsa", "Pantry"), i(1.0, "tbsp", "taco seasoning", "Spices")),
            listOf("Cook rice.", "Brown turkey with taco seasoning and a splash of water.", "Warm corn and beans together.", "Build bowls with rice, turkey, beans, corn, sliced avocado and salsa."),
        ),
        r(
            "Chickpea salad wraps", "Lunch", 4, 15, 0, listOf("Vegetarian", "No cook"), listOf(390, 13, 50, 15),
            "Smashed chickpea salad with crunchy celery in soft wraps.",
            listOf(i(30.0, "oz", "chickpeas", "Pantry"), i(2.0, "", "celery stalks", "Produce"), i(3.0, "tbsp", "mayonnaise", "Pantry"), i(1.0, "tbsp", "dijon mustard", "Pantry"), i(4.0, "", "large flour tortillas", "Bakery"), i(2.0, "cup", "spinach", "Produce")),
            listOf("Mash drained chickpeas with a fork, leaving some texture.", "Stir in diced celery, mayonnaise and mustard; season.", "Divide over tortillas with spinach and roll up."),
        ),
        r(
            "Sheet-pan sausage & vegetables", "Dinner", 4, 10, 25, listOf("One pan"), listOf(470, 20, 30, 30),
            "Smoked sausage roasted with potatoes, peppers and onion.",
            listOf(i(14.0, "oz", "smoked sausage"), i(1.5, "lb", "baby potatoes", "Produce"), i(2.0, "", "bell pepper"), i(1.0, "", "red onion", "Produce"), i(2.0, "tbsp", "olive oil", "Pantry"), i(1.0, "tsp", "paprika", "Spices")),
            listOf("Heat oven to 425°F (220°C).", "Halve potatoes; chop peppers and onion; slice sausage.", "Toss everything with oil and paprika on a sheet pan.", "Roast 25 minutes, stirring halfway."),
        ),
        r(
            "Tomato lentil soup", "Lunch", 6, 10, 30, listOf("Vegan", "Freezer friendly"), listOf(290, 16, 45, 5),
            "Thick red lentil soup with tomato and a hint of cumin.",
            listOf(i(1.5, "cup", "red lentils", "Pantry"), i(1.0, "", "onion"), i(2.0, "", "carrots", "Produce"), i(28.0, "oz", "crushed tomatoes", "Pantry"), i(6.0, "cup", "vegetable broth", "Pantry"), i(1.0, "tsp", "ground cumin", "Spices"), i(1.0, "tbsp", "olive oil", "Pantry")),
            listOf("Sauté diced onion and carrot in oil 5 minutes.", "Add cumin, lentils, tomatoes and broth.", "Simmer 25 minutes until lentils are soft.", "Blend partly for a creamy texture and season."),
        ),
        r(
            "Peanut noodle stir-fry", "Dinner", 4, 15, 10, listOf("Vegetarian", "Quick"), listOf(520, 18, 64, 22),
            "Noodles and crisp vegetables in a savory peanut sauce.",
            listOf(i(12.0, "oz", "noodles", "Pantry"), i(0.33, "cup", "peanut butter", "Pantry"), i(3.0, "tbsp", "soy sauce", "Pantry"), i(1.0, "tbsp", "rice vinegar", "Pantry"), i(1.0, "", "bell pepper"), i(2.0, "cup", "broccoli florets", "Produce"), i(1.0, "", "carrots", "Produce"), i(1.0, "tbsp", "ginger", "Produce")),
            listOf("Cook noodles; rinse under cold water.", "Whisk peanut butter, soy sauce, vinegar, grated ginger and 1/4 cup hot water.", "Stir-fry vegetables over high heat 4 minutes.", "Toss in noodles and sauce until glossy."),
        ),
        r(
            "Yogurt fruit parfait", "Snack", 2, 5, 0, listOf("Vegetarian", "No cook"), listOf(260, 12, 38, 7),
            "Layers of yogurt, fruit and crunchy granola.",
            listOf(i(1.5, "cup", "plain yogurt"), i(1.0, "cup", "mixed berries", "Produce"), i(0.5, "cup", "granola", "Pantry"), i(2.0, "tsp", "honey", "Pantry")),
            listOf("Layer yogurt, berries and granola in two glasses.", "Drizzle with honey and serve right away."),
        ),
    )
}
