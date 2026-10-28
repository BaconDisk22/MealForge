# MealForge

Free, offline Android meal planning, recipes, grocery lists and budgeting. No ads, accounts or tracking.

Built with Kotlin, Jetpack Compose and Room. Original design in black, gold, purple, pink, red and maroon.

## Features (0.5)

- **Recipes** – create, edit, duplicate, delete; ingredients with amounts and units; step-by-step cook mode; prep/cook time; photos, categories, tags, favorites; search, filter and sort; scale to any number of servings; 12 original starter recipes
- **Meal planner** – breakfast, lunch, dinner and snack slots on real dates; week and month views; move, swap, remove, repeat meals; leftovers; copy last week; reusable templates
- **Grocery lists** – built from your plan with duplicates combined and units converted; pantry stock subtracted; grouped by department; multiple lists; estimated and paid prices; share or copy
- **Pantry** – quantities and expiry dates, "use soon" alerts, recipe ideas from what you have
- **Budget** – weekly or monthly budget, estimated vs actual spending, shopping-trip log
- **Nutrition** – per-serving calories and macros, daily/weekly summaries, missing or estimated data clearly labelled
- **Household** – members with portion sizes, food exclusions, allergen flags
- **Backup** – export everything to a JSON file and restore it on any phone

Data from MealForge 0.2 (grocery list, weekly plan, servings) is imported automatically on first launch.

## Building

```
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

GitHub Actions runs tests and lint and uploads a debug APK on every pull request and push to `main`.

## Data and schema

The Room schema is exported to `app/schemas/`. Any schema change must bump the database version and ship a `Migration`; destructive migration is never used.

See [PRIVACY.md](PRIVACY.md).
