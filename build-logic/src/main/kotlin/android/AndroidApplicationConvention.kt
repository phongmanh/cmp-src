package android

import com.android.build.api.dsl.ApplicationExtension
import configureAndroidBaseline
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 *  Applies `com.android.application` and the shared Android baseline. App-specific
 *  identity (applicationId, targetSdk, versionCode/Name, buildTypes) stays
 */
class AndroidApplicationConvention : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.android.application")

        target.extensions.configure<ApplicationExtension> {
            target.configureAndroidBaseline(this)
        }
    }
}