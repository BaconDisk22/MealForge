package org.mealforge.app

import android.app.Application
import org.mealforge.app.data.MealForgeDatabase
import org.mealforge.app.data.Repository

class MealForgeApplication : Application() {
    val database: MealForgeDatabase by lazy { MealForgeDatabase.create(this) }
    val repository: Repository by lazy { Repository(database) }
}
