# HavenOS — minimal keep rules. Data layer classes are kept whole so Room
# codegen and DataStore serialization keep working under R8.
-keep class app.havenos.launcher.data.** { *; }
-keep @androidx.room.Database class * { *; }
