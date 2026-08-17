plugins {
    id("cmpsrc.cmp.application.web")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
        }
    }
}
