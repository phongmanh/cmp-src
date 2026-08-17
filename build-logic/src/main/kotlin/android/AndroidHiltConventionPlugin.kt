package android

import libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Applies Hilt + KSP and their dependencies. Compose-specific Hilt integration
 * (hilt-navigation-compose) stays in the module that needs it. Used via
 * `id("jetpackcompose.android.hilt")`.
 */
class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.google.devtools.ksp")
            apply("com.google.dagger.hilt.android")
        }

        dependencies {
            add("implementation", libs.findLibrary("hilt").get())
            add("ksp", libs.findLibrary("hilt-compiler").get())
        }
    }
}
