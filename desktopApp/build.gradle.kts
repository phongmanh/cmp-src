import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("cmpsrc.cmp.application.desktop")
}

dependencies {
    implementation(projects.shared)
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
