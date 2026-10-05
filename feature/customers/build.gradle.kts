plugins {
    id("cmpsrc.cmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.network)
            // The customer table is the list's source of truth; the server is synced into it.
            implementation(projects.core.database)
        }
        androidMain.dependencies {
            implementation(libs.androidx.work.runtime)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.serializationJson)
        }
    }
}
