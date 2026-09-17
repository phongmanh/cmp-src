package room

import androidx.room3.gradle.RoomExtension
import libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/** Source sets that get the bundled SQLite driver. It publishes no web variants, so not `commonMain`. */
private val SQLITE_DRIVER_SOURCE_SETS = setOf("androidMain", "iosMain")

/**
 * Room 3 for a Kotlin Multiplatform module: the KSP and Room Gradle plugins, the runtime, the
 * bundled SQLite driver, the Room compiler on every target, and the exported schema in
 * `<module>/schemas` (checked in, so a migration can be written against the previous version).
 *
 * Apply alongside `cmpsrc.kmp.library` or `cmpsrc.cmp.library`, in either order: everything
 * below waits for the Kotlin plugin and reacts to targets as they are added.
 */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.findPlugin("ksp").get().get().pluginId)
            apply(libs.findPlugin("androidx-room3").get().get().pluginId)
        }

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            extensions.configure<KotlinMultiplatformExtension> {
                // `api`: the module's `RoomDatabase` subclass is its public surface.
                sourceSets.getByName("commonMain").dependencies {
                    api(libs.findLibrary("androidx-room3-runtime").get())
                }
                sourceSets.matching { it.name in SQLITE_DRIVER_SOURCE_SETS }.configureEach {
                    dependencies { implementation(libs.findLibrary("androidx-sqlite-bundled").get()) }
                }

                // KSP has no cross-target configuration, so the compiler goes on each target's
                // own. Following `targets` means a target added later still gets it, instead of
                // compiling its @Database annotations to nothing.
                targets.matching { it.platformType != KotlinPlatformType.common }.configureEach {
                    val kspConfiguration = "ksp" + name.replaceFirstChar(Char::uppercase)
                    dependencies.add(kspConfiguration, libs.findLibrary("androidx-room3-compiler").get())
                }
            }
        }

        // AGP's lint tasks read the Android compilations' KSP output directories without declaring
        // the KSP tasks that write them, which Gradle rejects as an undeclared dependency —
        // `./gradlew build` fails on the validation before lint runs at all. Lint is meant to see
        // generated sources, so declaring the dependency is the fix rather than switching the
        // validation off. Drop this once AGP's `com.android.kotlin.multiplatform.library` plugin
        // wires it itself.
        mapOf(
            "AndroidMain" to "kspAndroidMain",
            "AndroidHostTest" to "kspAndroidHostTest",
        ).forEach { (compilation, kspTask) ->
            tasks.matching { it.name == "generate${compilation}LintModel" || it.name == "lintAnalyze$compilation" }
                .configureEach { dependsOn(kspTask) }
        }
    }
}
