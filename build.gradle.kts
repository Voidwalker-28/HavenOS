// Version catalog is used for library versions (gradle/libs.versions.toml).
// Plugin classpaths stay in buildscript: proven offline-safe in this environment.
buildscript {
    repositories {
        maven(url = "file:///home/hatch/local-maven")
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.12.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.1.20")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.1.20-2.0.1")
    }
}
