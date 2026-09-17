plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.compose.gradlePlugin)
    implementation(libs.composeCompiler.gradlePlugin)
    implementation(libs.kotlinSerialization.gradlePlugin)
    implementation(libs.ksp.gradlePlugin)
    implementation(libs.androidx.room3.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("cmpAndroidApplication") {
            id = "cmpsrc.cmp.application.android"
            implementationClass = "android.AndroidApplicationConvention"
        }

        register("kmpLibrary") {
            id = "cmpsrc.kmp.library"
            implementationClass = "kmp.KmpLibraryConventionPlugin"
        }
        register("cmpLibrary") {
            id = "cmpsrc.cmp.library"
            implementationClass = "cmp.CmpLibraryConventionPlugin"
        }
        register("cmpFeature") {
            id = "cmpsrc.cmp.feature"
            implementationClass = "cmp.CmpFeatureConventionPlugin"
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
        register("room") {
            id = "cmpsrc.room"
            implementationClass = "room.RoomConventionPlugin"
        }
    }
}
