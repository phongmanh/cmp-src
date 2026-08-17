package android

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Enables Jetpack Compose on top of the android.library or android.application
 * convention. Applies the Compose compiler and kotlinx.serialization plugins (every
 * Compose module here uses @Serializable Navigation 3 keys), turns on the compose
 * build feature, and wires the Compose BOM + baseline UI/tooling dependencies.
 *
 * Apply alongside a base convention, e.g.:
 *   id("cmpsrc.cmp.application.android")
 *   id("cmpsrc.cmp.android.compose")
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.findPlugin("composeCompiler").get().get().pluginId)
            apply("org.jetbrains.kotlin.plugin.serialization")
        }

        // Enable the compose build feature on whichever base convention is present.
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<LibraryExtension> { buildFeatures { compose = true } }
            addComposeDependencies()
        }
        pluginManager.withPlugin("com.android.application") {
            extensions.configure<ApplicationExtension> { buildFeatures { compose = true } }
            addComposeDependencies()
            dependencies {
                add("implementation", libs.findLibrary("androidx-activity-compose").get())
            }
        }
    }

    private fun Project.addComposeDependencies() {
        val bom = libs.findLibrary("androidx-compose-bom").get()
        dependencies {
            add("implementation", platform(bom))
            add("implementation", libs.findLibrary("androidx-compose-ui").get())
            add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
            add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        }
    }
}
