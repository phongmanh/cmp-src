package cmp

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import compileSdkVersion
import minSdkVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Applies Kotlin Multiplatform + the KMP Android library target + Compose, and
 * configures the shared Android baseline (compileSdk, minSdk, JVM target) on the
 * `androidLibrary` target. Per-module targets/source sets/dependencies stay in
 * the consuming module (see `shared/build.gradle.kts`).
 */
class CmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
            apply("com.android.kotlin.multiplatform.library")
            apply("cmpsrc.cmp.multiplatform")
        }

        extensions.configure<KotlinMultiplatformExtension> {
            val androidLibrary = (this as ExtensionAware).extensions
                .getByName("androidLibrary") as KotlinMultiplatformAndroidLibraryTarget

            androidLibrary.apply {
                compileSdk = target.compileSdkVersion
                minSdk = target.minSdkVersion
                compilerOptions { jvmTarget = JvmTarget.JVM_11 }
            }
        }
    }
}
