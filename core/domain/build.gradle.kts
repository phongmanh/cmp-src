plugins {
    id("cmpsrc.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `AuthResult` carries the contract's `UserResponse`, so every consumer compiles against it.
            api(libs.api.contract)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
