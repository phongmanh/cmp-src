plugins {
    id("cmpsrc.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            // TextFieldState, which the ViewModels under test own their form fields as.
            api(libs.compose.foundation)
        }
    }
}
