@file:Suppress("UnstableApiUsage")

include(":tflite")


include(":kotlin")


include(":calculator")


pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MLDemo"

include(":app")
// include(":app:ui:interaction")
// include(":guesser")
include(":asr")
include(":audio")
include(":audio-classification")
include(":guesser")
//project(":google-services-s4y").projectDir = File("./google-services-s4y")
include(":firebase")
