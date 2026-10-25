pluginManagement {
    repositories {
        maven(url = "file:///home/hatch/local-maven")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven(url = "file:///home/hatch/local-maven")
        google()
        mavenCentral()
    }
}
rootProject.name = "havenos"
include(":app")
