pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
// Normal Kotlin builds never depend on Flutter. Opt in with -PwaFlutterUi=true.
val waFlutterUiEnabled = providers.gradleProperty("waFlutterUi").orNull == "true"

dependencyResolutionManagement {
    repositoriesMode.set(
        if (waFlutterUiEnabled) RepositoriesMode.PREFER_SETTINGS
        else RepositoriesMode.FAIL_ON_PROJECT_REPOS
    )
    repositories {
        google()
        mavenCentral()
        if (waFlutterUiEnabled) maven("https://storage.googleapis.com/download.flutter.io")
    }
}

rootProject.name = "WA Lead Saver"
include(":app")

// The Flutter module is generated locally, not committed to the repository.
if (waFlutterUiEnabled) {
    val script = file("flutter_module/.android/include_flutter.groovy")
    require(script.isFile) {
        "Generate flutter_module before enabling the Flutter UI; see flutter_ui/README.md"
    }
    apply(from = script)
}
