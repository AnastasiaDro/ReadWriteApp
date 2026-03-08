rootProject.name = "ReadWriteApp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
include(":feature:game_screen")
include(":data:flashcards")
include(":data:decks")
include(":data:student")
include(":data:student-deck")
include(":data:preferences")
include(":data:study_progress")
include(":feature:create_screen")
include(":feature:student")
include(":feature:session_settings")
include(":core:utils")
include(":core:game_engine")
include(":core:parser")
include(":data:database")
include(":core:ui")
