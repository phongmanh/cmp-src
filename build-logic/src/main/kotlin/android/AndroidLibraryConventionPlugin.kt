package android

import com.android.build.api.dsl.LibraryExtension
import configureAndroidBaseline
import configureCommonTestDependencies
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Applies `com.android.library` and the shared Android baseline (compileSdk, minSdk,
 * Java 11, instrumentation runner + common test dependencies). Used by every library
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            configureAndroidBaseline(this)
        }

        configureCommonTestDependencies()
    }
}