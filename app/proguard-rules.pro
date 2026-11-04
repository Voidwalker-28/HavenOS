# HavenOS — minimal keep rules. Data layer classes are kept whole so Room
# codegen and DataStore serialization keep working under R8.
-keep class app.havenos.launcher.data.** { *; }
-keep @androidx.room.Database class * { *; }

# 3.1.1: keep file names and line numbers in release stack traces, so a
# crash report can be retraced to source lines (see README.md).
-keepattributes SourceFile,LineNumberTable
