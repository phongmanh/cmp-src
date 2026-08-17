plugins {
    id("cmpsrc.cmp.application.android")
    id("cmpsrc.cmp.android.compose")
}

dependencies {
    implementation(projects.shared)
}

android {
    namespace = "com.liam.cmp_src"

    defaultConfig {
        applicationId = "com.liam.cmp_src"
        versionCode = 1
        versionName = "1.0"
    }
}
