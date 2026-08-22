rootProject.name = "CMPsrc"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (providers.gradleProperty("useMavenLocal").isPresent) {
            mavenLocal()
        }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()

        // Toolchains the JS and Wasm targets download for themselves: Node, Yarn and the Binaryen
        // wasm optimiser. The Kotlin Gradle plugin normally registers these ivy repositories on
        // the root project, which FAIL_ON_PROJECT_REPOS above rejects — without them declared
        // here, `:kotlinNodeJsSetup` fails before any JS or Wasm task can run.
        //
        // The layouts are not ours to choose: each one mirrors the release-file naming of the
        // project it downloads from, and must match what the Kotlin plugin asks for. `exclusiveContent`
        // keeps each repository to its one artifact, so nothing else is ever looked up here and these
        // coordinates are never looked for anywhere else.
        exclusiveContent {
            forRepository {
                ivy {
                    name = "Node.js Distributions"
                    setUrl("https://nodejs.org/dist")
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
                    metadataSources { artifact() }
                }
            }
            filter { includeModule("org.nodejs", "node") }
        }

        exclusiveContent {
            forRepository {
                ivy {
                    name = "Yarn Distributions"
                    setUrl("https://github.com/yarnpkg/yarn/releases/download")
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
                    metadataSources { artifact() }
                }
            }
            filter { includeModule("com.yarnpkg", "yarn") }
        }

        exclusiveContent {
            forRepository {
                ivy {
                    name = "Binaryen Distributions"
                    setUrl("https://github.com/WebAssembly/binaryen/releases/download")
                    patternLayout {
                        artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]")
                    }
                    metadataSources { artifact() }
                }
            }
            filter { includeModule("com.github.webassembly", "binaryen") }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":androidApp")
include(":desktopApp")
include(":shared")
include(":webApp")