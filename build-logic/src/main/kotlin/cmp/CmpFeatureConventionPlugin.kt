package cmp

import libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A feature module: a Compose library with Koin, the core modules every feature builds on —
 * `core:domain` for the account types, `core:ui` for the design system and shared resources, and
 * `core:utils` for formatting and runtime permissions — the lifecycle-aware ViewModel APIs its
 * routes use, and `core:testing` for its tests.
 *
 * Features never depend on one another. Anything two of them need moves down into `core`, and the
 * app module (`:shared`) is the only place they meet.
 */
class CmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("cmpsrc.cmp.library")
            apply("cmpsrc.cmp.koin")
        }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(project(":core:domain"))
                implementation(project(":core:ui"))
                implementation(project(":core:utils"))
                implementation(libs.findLibrary("kotlinx-coroutinesCore").get())
                implementation(libs.findLibrary("androidx-lifecycle-viewmodelCompose").get())
                implementation(libs.findLibrary("androidx-lifecycle-runtimeCompose").get())
            }
            sourceSets.getByName("commonTest").dependencies {
                implementation(project(":core:testing"))
            }
        }
    }
}
