package web

import libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Applies Kotlin Multiplatform + Compose for the browser targets (JS and Wasm),
 * both built as executables, and wires the baseline Compose UI dependency into
 * `commonMain`. Per-module source sets/dependencies stay in the consuming module.
 */
class WebApplicationConventionPlugin : Plugin<Project> {
    @OptIn(ExperimentalWasmDsl::class, ExperimentalKotlinGradlePluginApi::class)
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
            apply("cmpsrc.cmp.multiplatform")
        }

        extensions.configure<KotlinMultiplatformExtension> {
            js {
                browser()
                binaries.executable()
            }

            wasmJs {
                browser()
                binaries.executable()
            }

            sourceSets.commonMain.dependencies {
                implementation(libs.findLibrary("compose-ui").get())
            }
        }
    }
}
