import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("cmpsrc.cmp.application.desktop")
}

dependencies {
    implementation(projects.shared)

    // Only so `main` can hand FileKit an application id before the window opens — see main.kt.
    // Every other target initialises itself.
    implementation(libs.filekit.dialogs.compose)
}

compose.desktop {
    application {
        mainClass = "com.liam.cmp_src.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.liam.cmp_src"
            packageVersion = "1.0.0"
        }
    }
}
