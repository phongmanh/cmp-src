import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

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
