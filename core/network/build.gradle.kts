plugins {
    id("cmpsrc.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Part of this module's API: `HttpClient` and `HttpResponse` in every call, `Logger` and
            // `LogLevel` in `createHttpClient`, `Flow` in `TokenStore`, and the `body<T>()` and
            // `SerializationException` that the inline `apiCall` compiles into its callers.
            api(libs.ktor.client.core)
            api(libs.ktor.client.logging)
            api(libs.kotlinx.coroutinesCore)
            api(libs.kotlinx.serializationCore)

            implementation(libs.api.contract)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.auth)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
