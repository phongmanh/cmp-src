plugins {
    // Compose because `rememberPermissionController` is a composable: an Android permission
    // request needs the hosting Activity's result registry, which only composition can hand over.
    id("cmpsrc.cmp.library")
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            // `rememberLauncherForActivityResult` and `LocalActivity`.
            implementation(libs.androidx.activity.compose)
        }
    }
}
