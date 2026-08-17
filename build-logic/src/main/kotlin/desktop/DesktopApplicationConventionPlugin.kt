package desktop

import libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.compose.ComposePlugin

/**
 * Applies the Kotlin/JVM + Compose Desktop plugins and the baseline desktop
 * dependencies (currentOs, coroutines-swing, ui-tooling-preview). App-specific
 * config (mainClass, nativeDistributions) stays in the consuming module.
 */
class DesktopApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.jvm")
            apply("cmpsrc.cmp.multiplatform")
        }

        addDesktopDependencies()
    }

    private fun Project.addDesktopDependencies() {
        val compose = (dependencies as ExtensionAware).extensions
            .getByType(ComposePlugin.Dependencies::class.java)

        dependencies {
            add("implementation", compose.desktop.currentOs)
            add("implementation", libs.findLibrary("kotlinx-coroutinesSwing").get())
            add("implementation", libs.findLibrary("compose-uiToolingPreview").get())
        }
    }
}
