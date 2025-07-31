apply(from = "./config/keystore.gradle.kts")

@Suppress("UNCHECKED_CAST")
val verifyKeystore = extra["verifyKeystore"] as () -> Unit

buildscript {
    dependencies {
    }
}

plugins {
    alias(libs.plugins.com.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.com.google.devtools.ksp) apply false
    alias(libs.plugins.hilt.android.gradle.plugin) apply false
}

verifyKeystore()
