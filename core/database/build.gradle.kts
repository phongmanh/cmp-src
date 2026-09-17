plugins {
    id("cmpsrc.kmp.library")
    id("cmpsrc.room")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `RoomTokenStore` implements network's `TokenStore` and is built from security's `TokenCipher`.
            api(projects.core.network)
            api(projects.core.security)
        }
    }
}
