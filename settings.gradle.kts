pluginManagement {
    // Le build composite build-logic fournit les plugins de convention « dromos.* ».
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Aucun module ne déclare ses propres dépôts : tout passe par ici.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Dromos"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

include(":core:domain")
include(":core:data")
include(":core:ui")
include(":core:location")
include(":core:tracking")
include(":core:map")

include(":feature:history")
include(":feature:settings")
include(":feature:record")
