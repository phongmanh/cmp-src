package multipplatform

import libs
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Applies JetBrains Compose Multiplatform + its Kotlin compiler plugin, plus
 * kotlinx.serialization (every Compose module here uses @Serializable Navigation 3
 * keys) — the plugin ids every Compose-enabled Kotlin Multiplatform convention needs
 * (cmp.CmpLibraryConventionPlugin). Apply this common plugin instead of re-applying
 * those ids directly in each one.
 */
class CmpMultConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.findPlugin("composeMultiplatform").get().get().pluginId)
        pluginManager.apply(libs.findPlugin("composeCompiler").get().get().pluginId)
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
    }
}
