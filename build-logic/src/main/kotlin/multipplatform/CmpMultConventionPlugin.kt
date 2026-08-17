package multipplatform

import libs
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Applies JetBrains Compose Multiplatform + its Kotlin compiler plugin — the pair of
 * plugin ids every Compose-enabled Kotlin Multiplatform/JVM convention needs
 * (cmp.CmpLibraryConventionPlugin, desktop.DesktopApplicationConventionPlugin,
 * web.WebApplicationConventionPlugin). Apply this common plugin instead of re-applying
 * those ids directly in each one.
 */
class CmpMultConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.findPlugin("composeMultiplatform").get().get().pluginId)
        pluginManager.apply(libs.findPlugin("composeCompiler").get().get().pluginId)
    }
}
