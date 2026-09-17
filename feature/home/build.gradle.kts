plugins {
    id("cmpsrc.cmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `HomeRoute` saves the signed-in account across process death as the contract's JSON.
            implementation(libs.kotlinx.serializationJson)
        }
    }
}
