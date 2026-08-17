plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.compose.gradlePlugin)
    implementation(libs.composeCompiler.gradlePlugin)
    implementation(libs.kotlinSerialization.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("cmpAndroidApplication") {
            id = "cmpsrc.cmp.application.android"
            implementationClass = "android.AndroidApplicationConvention"
        }

        register("cmpApplicationDesktop") {
            id = "cmpsrc.cmp.application.desktop"
            implementationClass = "desktop.DesktopApplicationConventionPlugin"
        }
        register("cmpApplicationWeb") {
            id = "cmpsrc.cmp.application.web"
            implementationClass = "web.WebApplicationConventionPlugin"
        }
        register("cmpLibrary") {
            id = "cmpsrc.cmp.library"
            implementationClass = "cmp.CmpLibraryConventionPlugin"
        }
        register("cmpMultiplatform") {
            id = "cmpsrc.cmp.multiplatform"
            implementationClass = "multipplatform.CmpMultConventionPlugin"
        }
        register("cmpAndroidCompose") {
            id = "cmpsrc.cmp.android.compose"
            implementationClass = "android.AndroidComposeConventionPlugin"
        }
        register("cmpKoin") {
            id = "cmpsrc.cmp.koin"
            implementationClass = "koin.KoinConventionPlugin"
        }
    }
}
