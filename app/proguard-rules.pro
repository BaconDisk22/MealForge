# kotlinx.serialization (backup files)
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class org.mealforge.app.** { *** Companion; }
-keepclasseswithmembers class org.mealforge.app.** { kotlinx.serialization.KSerializer serializer(...); }
