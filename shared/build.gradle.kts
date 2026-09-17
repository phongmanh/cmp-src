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

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.network)
            implementation(projects.core.security)
            implementation(projects.core.database)
            implementation(projects.core.ui)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.androidx.savedstate)
            implementation(libs.navigation3.runtime)
            implementation(libs.navigation3.ui)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.serializationCore)
            implementation(libs.api.contract)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.filekit.dialogs.compose)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
            implementation(libs.ktor.client.mock)
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
