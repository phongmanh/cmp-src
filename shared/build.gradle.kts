import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("cmpsrc.cmp.library")
    id("cmpsrc.cmp.koin")
}

kotlin {
    // Targets, the Android namespace, host tests and Compose all come from the conventions above.
    // What only this module does is link the framework the iOS app imports.
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    // The app shell: navigation, the Koin graph and the avatar image loader. Every feature and core
    // module it assembles is a dependency; what those modules expose (Ktor, coroutines, Room,
    // api-contract) comes with them.
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.network)
            implementation(projects.core.security)
            implementation(projects.core.database)
            implementation(projects.core.ui)
            implementation(projects.feature.auth)
            implementation(projects.feature.home)
            implementation(projects.feature.profile)

            implementation(libs.navigation3.runtime)
            implementation(libs.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.androidx.savedstate)
            implementation(libs.kotlinx.serializationCore)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }
    }
}

// Both release frameworks link through the one Kotlin/Native compiler JVM, so linking them at the
// same time puts two whole-program LTO passes in the single heap `kotlin.native.jvmArgs` sizes and
// `./gradlew build` dies with an OutOfMemoryError — see gradle.properties, and note that 16 GB
// leaves no headroom to simply raise the cap again. Ordering them costs nothing: the two are
// independent, `build` is the only thing that asks for both, and each still links at full speed.
tasks.matching { it.name == "linkReleaseFrameworkIosSimulatorArm64" }.configureEach {
    mustRunAfter("linkReleaseFrameworkIosArm64")
}
