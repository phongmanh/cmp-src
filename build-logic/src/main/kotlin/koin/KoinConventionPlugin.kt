package koin

import libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Adds Koin (core + Compose Multiplatform integration) to `commonMain`, version-pinned via
 * the `koin-bom` platform so every Koin artifact stays on one coordinated release. Platform
 * modules that need Koin's Android-specific extras (e.g. `androidContext()`) add
 * `koin-android` themselves — this convention only covers what's usable from common code.
 * Used via `id("cmpsrc.cmp.koin")` alongside `cmpsrc.cmp.library`.
 */
class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(project.dependencies.platform(libs.findLibrary("koin-bom").get()))
                implementation(libs.findLibrary("koin-core").get())
                implementation(libs.findLibrary("koin-compose").get())
                implementation(libs.findLibrary("koin-compose-viewmodel").get())
            }
        }
    }
}
