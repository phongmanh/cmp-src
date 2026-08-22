import org.jetbrains.kotlin.gradle.targets.js.EnvSpec

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}

/**
 * Hand the JS/Wasm toolchain downloads over to the repositories declared in `settings.gradle.kts`.
 *
 * Each Kotlin `EnvSpec` (Node and Yarn, once for JS and again for Wasm, plus Binaryen) otherwise
 * registers an ivy repository on *this* project the moment its setup task resolves, which
 * `RepositoriesMode.FAIL_ON_PROJECT_REPOS` then refuses — `:kotlinNodeJsSetup` fails before any JS
 * or Wasm task can run. Clearing `downloadBaseUrl` is the plugin's own switch for this: with no
 * base URL, it skips adding the repository and resolves the distribution from the settings
 * repositories instead. The downloads still happen; only where they are declared changes.
 *
 * Driven off `plugins.all` rather than a fixed list because these specs are registered lazily, by
 * different plugins, at different points — Binaryen only appears once something needs a Wasm
 * production build. Applied to every project, not just this one: a module with a JS or Wasm target
 * gets its own spec alongside the root project's.
 */
allprojects {
    val target = this
    plugins.all {
        target.extensions.extensionsSchema
            .mapNotNull { target.extensions.findByName(it.name) as? EnvSpec<*> }
            .forEach { spec -> spec.downloadBaseUrl.set(null as String?) }
    }
}
