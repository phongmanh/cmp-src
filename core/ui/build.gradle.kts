plugins {
    id("cmpsrc.cmp.library")
}

compose.resources {
    // Features draw the shared icons and strings (the social logos, error text, form labels) from
    // this module, so its `Res` has to be visible to them.
    publicResClass = true
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The message mappers and `ErrorBanner` take domain types.
            api(projects.core.domain)
            implementation(libs.coil.compose)
        }
    }
}
