plugins {
    id("cmpsrc.cmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.network)
            implementation(libs.filekit.dialogs.compose)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.serializationJson)
        }
    }
}
