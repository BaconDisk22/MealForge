# Changelog

All notable changes to MealForge. Versions follow [Semantic Versioning](https://semver.org); each release section is copied into its GitHub Release notes.

## 0.5.0 - 2026-10-29

First public test release.

### Added
- Recipes with structured ingredients, units, steps, cook mode, times, photos, categories, tags and favorites; search, filter, sort and servings scaling; 12 original starter recipes
- Meal planner with breakfast, lunch, dinner and snack slots on real dates; week and month views; move, repeat, leftovers, copy last week and templates
- Grocery lists generated from the plan, combining duplicates, converting units and subtracting pantry stock; department grouping, multiple lists, prices, share and copy
- Pantry with expiry dates and recipe ideas from what you have
- Weekly or monthly grocery budget with estimated vs actual spending
- Per-serving nutrition with daily and weekly summaries; allergen flags; household members, portions and food exclusions
- Backup to and restore from a JSON file
- App icon, splash screen and onboarding
- Signed release builds published to GitHub Releases

### Changed
- Data now lives in an on-device database; lists, plans and servings from 0.2 are imported automatically
- Test builds are now installed as a separate "MealForge Debug" app

### License
- MealForge is licensed under GPL-3.0-or-later
