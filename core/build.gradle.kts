plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // api：KungalClient（P2）将公开暴露 OkHttpClient 类型，app 端需按同一版本编译
    api(libs.okhttp)
    // api：core 的公开 API 会用到序列化类型
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver3)
}
