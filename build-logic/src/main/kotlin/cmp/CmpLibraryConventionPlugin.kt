package cmp

import androidLibraryTarget
import libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import pathSegments

/** The Compose artifacts every UI module compiles against. */
private val COMPOSE_LIBRARIES = listOf(
    "compose-runtime",
    "compose-foundation",
    "compose-material3",
    "compose-ui",
    "compose-components-resources",
    "compose-uiToolingPreview",
)

/**
 * A Compose Multiplatform library: `cmpsrc.kmp.library` plus Compose, its compiler plugin and
 * kotlinx.serialization (`cmpsrc.cmp.multiplatform`), the Compose artifacts, and Compose
 * resources packaged for Android.
 *
 * Each module's generated `Res` lives in `cmpsrc.<module path>.generated.resources` — `:shared`
 * keeps the package it always had, and `:feature:auth` gets `cmpsrc.feature.auth.generated.resources`
 * — so resources from two modules never collide. `Res` stays internal unless a module whose
 * resources others read sets `publicResClass` itself.
 */
class CmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            // Compose first: the base convention checks for it when it sets up host tests.
            apply("cmpsrc.cmp.multiplatform")
            apply("cmpsrc.kmp.library")
        }

        extensions.configure<KotlinMultiplatformExtension> {
            androidLibraryTarget().androidResources { enable = true }

            sourceSets.getByName("commonMain").dependencies {
                COMPOSE_LIBRARIES.forEach { implementation(libs.findLibrary(it).get()) }
            }
        }

        extensions.configure<ComposeExtension> {
            (this as ExtensionAware).extensions.configure<ResourcesExtension> {
                packageOfResClass = "cmpsrc.$pathSegments.generated.resources"
            }
        }

        // Android Studio renders previews through ui-tooling, which only the runtime needs.
        dependencies {
            add("androidRuntimeClasspath", libs.findLibrary("compose-uiTooling").get())
        }
    }
}
