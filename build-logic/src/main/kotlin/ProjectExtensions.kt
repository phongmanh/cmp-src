import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** The shared `libs` version catalog (declared in build-logic/settings.gradle.kts). */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Java toolchain baseline every module compiles against (see CLAUDE.md). */
internal val JAVA_VERSION = JavaVersion.VERSION_11

/** compileSdk/minSdk, single-sourced from the `libs` version catalog. */
internal val Project.compileSdkVersion: Int
    get() = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()

internal val Project.minSdkVersion: Int
    get() = libs.findVersion("android-minSdk").get().requiredVersion.toInt()

/** The package every module's Android namespace hangs off. */
private const val BASE_PACKAGE = "com.liam.cmp_src"

/** The module's Gradle path as package segments: `:feature:auth` → `feature.auth`. */
internal val Project.pathSegments: String
    get() = path.removePrefix(":").replace(':', '.')

/**
 * The module's Android namespace, `com.liam.cmp_src.<path>` — derived rather than declared, so
 * a new module needs no `androidLibrary { }` block and no two modules can end up sharing one.
 */
internal val Project.modulePackage: String
    get() = "$BASE_PACKAGE.$pathSegments"

/**
 * The KMP Android library target. AGP registers it as an extension of `kotlin { }` rather than
 * as a member, so a plain `.kt` convention has to look it up by name.
 */
internal fun KotlinMultiplatformExtension.androidLibraryTarget(): KotlinMultiplatformAndroidLibraryTarget =
    (this as ExtensionAware).extensions.getByName("androidLibrary") as KotlinMultiplatformAndroidLibraryTarget

/**
 * Baseline compileSdk/minSdk/instrumentation-runner/Java-compatibility settings shared
 * by every `com.android.*` convention (application and library alike).
 */
internal fun Project.configureAndroidBaseline(extension: CommonExtension) {
    extension.compileSdk = compileSdkVersion

    extension.defaultConfig.apply {
        minSdk = minSdkVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    extension.compileOptions.apply {
        sourceCompatibility = JAVA_VERSION
        targetCompatibility = JAVA_VERSION
    }
}

/** Common JUnit/Espresso test dependencies applied to every Android module. */
internal fun Project.configureCommonTestDependencies() {
    dependencies {
        add("testImplementation", libs.findLibrary("junit").get())
        add("androidTestImplementation", libs.findLibrary("androidx-espresso-core").get())
        add("androidTestImplementation", libs.findLibrary("androidx-testExt-junit").get())
    }
}
