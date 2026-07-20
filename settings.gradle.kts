pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("${rootDir}/gradle/offline-repo") }
        maven { url = uri("${rootDir}/gradle/plugin-repo") }
        // Online mirrors for newly added CameraX / ML Kit / PdfBox artifacts.
        // Prefer offline-repo when populated; keep google/mavenCentral as fallback.
        google()
        mavenCentral()
    }
}

rootProject.name = "DocuFind"
include(":app")
