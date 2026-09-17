import androidx.room3.gradle.RoomExtension

plugins {
    id("cmpsrc.cmp.library")
    id("cmpsrc.cmp.koin")
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room3)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    androidLibrary {
       namespace = "com.liam.cmp_src.shared"

       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.sqlite.bundled)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.androidx.sqlite.bundled)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
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
            implementation(libs.androidx.room3.runtime)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.filekit.dialogs.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
            implementation(libs.ktor.client.mock)
        }
    }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
    resolutionStrategy.cacheDynamicVersionsFor(0, "seconds")
}

// The Room plugin registers its extension only once it has found a KSP-processed target, so the
// generated `room { }` accessor doesn't exist at script-compile time — configure it by type.
extensions.configure<RoomExtension> {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)

    // KSP has no cross-target configuration: the Room compiler is added per target, and the
    // list has to track `kotlin { }` above — a target added there without a line here compiles
    // its @Database annotations to nothing.
    listOf(
        "kspAndroid",
        "kspIosArm64",
        "kspIosSimulatorArm64",
    ).forEach { configuration ->
        add(configuration, libs.androidx.room3.compiler)
    }
}

// AGP's lint tasks read the Android compilations' KSP output directories without declaring the KSP
// tasks that write them, which Gradle rejects as an undeclared dependency — `./gradlew build` fails
// on the validation before lint runs at all. Lint is meant to see generated sources, so declaring
// the dependency is the fix rather than switching the validation off. Drop this once AGP's
// `com.android.kotlin.multiplatform.library` plugin wires it itself.
mapOf(
    "AndroidMain" to "kspAndroidMain",
    "AndroidHostTest" to "kspAndroidHostTest",
).forEach { (compilation, kspTask) ->
    tasks.matching { it.name == "generate${compilation}LintModel" || it.name == "lintAnalyze$compilation" }
        .configureEach { dependsOn(kspTask) }
}

// Both release frameworks link through the one Kotlin/Native compiler JVM, so linking them at the
// same time puts two whole-program LTO passes in the single heap `kotlin.native.jvmArgs` sizes and
// `./gradlew build` dies with an OutOfMemoryError — see gradle.properties, and note that 16 GB
// leaves no headroom to simply raise the cap again. Ordering them costs nothing: the two are
// independent, `build` is the only thing that asks for both, and each still links at full speed.
tasks.matching { it.name == "linkReleaseFrameworkIosSimulatorArm64" }.configureEach {
    mustRunAfter("linkReleaseFrameworkIosArm64")
}
