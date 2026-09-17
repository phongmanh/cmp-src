package kmp

import androidLibraryTarget
import compileSdkVersion
import libs
import minSdkVersion
import modulePackage
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * The baseline every module in this build shares, Compose or not: Kotlin Multiplatform on the
 * app's targets — the KMP Android library and both iOS ARM targets — with host tests and the
 * common test dependencies.
 *
 * The Android namespace is derived from the module's path ([modulePackage]). No framework is
 * declared here: only `:shared` links one, and every other module reaches Swift through it.
 *
 * Compose modules get this through `cmpsrc.cmp.library`, which applies Compose first so the
 * host-test setup below can tell the two kinds of module apart.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.findPlugin("kotlinMultiplatform").get().get().pluginId)
            apply(libs.findPlugin("androidMultiplatformLibrary").get().get().pluginId)
        }
        val hasComposeResources = pluginManager.hasPlugin("org.jetbrains.compose")

        extensions.configure<KotlinMultiplatformExtension> {
            iosArm64()
            iosSimulatorArm64()

            androidLibraryTarget().apply {
                namespace = modulePackage
                compileSdk = compileSdkVersion
                minSdk = minSdkVersion
                compilerOptions { jvmTarget = JvmTarget.JVM_11 }

                // AGP throws on a second `withHostTest`, so this is the only place that enables
                // it — which is why the resource switch is decided here rather than by the
                // Compose convention.
                withHostTest { isIncludeAndroidResources = hasComposeResources }
            }

            sourceSets.getByName("commonTest").dependencies {
                implementation(libs.findLibrary("kotlin-test").get())
                implementation(libs.findLibrary("kotlinx-coroutinesTest").get())
            }
        }

        // api-contract is a SNAPSHOT: resolve changing and dynamic versions afresh on every build
        // instead of from Gradle's 24-hour cache, so a republished contract is picked up at once.
        configurations.configureEach {
            resolutionStrategy.cacheChangingModulesFor(0, "seconds")
            resolutionStrategy.cacheDynamicVersionsFor(0, "seconds")
        }
    }
}
