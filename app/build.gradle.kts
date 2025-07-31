plugins {
    id("android-application-convention")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android.gradle.plugin)
}

kotlin {
    jvmToolchain(JavaVersion.VERSION_21.toString().toInt())
}

dependencies {

    // Common
    implementation(libs.ax.core.ktx)
    implementation(libs.com.google.android.material)

    //Common Testing
    testImplementation(libs.junit)
    testImplementation(libs.ax.test.ext.junit.ktx)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.ax.test.ext.junit.ktx)

    //Compose
    implementation(platform(libs.ax.compose.bom))
    implementation(libs.ax.compose.ui)
    implementation(libs.ax.compose.ui.graphics)
    implementation(libs.ax.compose.ui.tooling)
    implementation(libs.ax.compose.ui.tooling.preview)
    implementation(libs.ax.compose.ui.viewbinding)
    implementation(libs.ax.compose.runtime.livedata)
    implementation(libs.ax.compose.runtime.rxjava3)
    implementation(libs.ax.compose.material3)
    implementation(libs.ax.constraintlayout.compose)
    implementation(libs.ax.compose.material.icons.core)
    implementation(libs.ax.compose.material.icons.extended)
    implementation(libs.ax.activity.compose)
    implementation(libs.ax.lifecycle.viewmodel.compose)
    implementation(libs.ax.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    //Compose Testing
    androidTestImplementation(platform(libs.ax.compose.bom))
    androidTestImplementation(libs.ax.compose.ui.test)
    androidTestImplementation(libs.ax.compose.ui.test.junit4)
    androidTestImplementation(libs.org.mockito.android)
    debugImplementation(libs.ax.compose.ui.test.manifest)
    debugImplementation(libs.ax.compose.ui.tooling)

    //Compose Navigation 3
    implementation(libs.ax.navigation3.runtime)
    implementation(libs.ax.navigation3.ui)
    implementation(libs.ax.navigation3.viewmodel)
    //implementation(libs.ax.navigation3.adaptive)

    //Exoplayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.dash)
    implementation(libs.androidx.media3.hls)

    //Work Runtime
    implementation(libs.ax.work.runtime.ktx)

    //Timber
    implementation(libs.com.jakewharton.timber)

    //Dagger & Hilt
    implementation(libs.com.google.dagger.hilt.android)
    ksp(libs.com.google.dagger.hilt.android.compiler)
    implementation(libs.ax.hilt.common)
    ksp(libs.ax.hilt.compiler)
    implementation(libs.ax.hilt.navigation.compose)
    implementation(libs.ax.hilt.work)
    //Dagger & Hilt Testing
    testImplementation(libs.com.google.dagger.hilt.android.testing)
    kspTest(libs.com.google.dagger.hilt.android.compiler)
    androidTestImplementation(libs.com.google.dagger.hilt.android.testing)
    kspAndroidTest(libs.com.google.dagger.hilt.android.compiler)

    //OkHttp
    implementation(platform(libs.com.squareup.okhttp3.bom))
    implementation(libs.com.squareup.okhttp3)
    implementation(libs.com.squareup.okhttp3.logging.interceptor)

    //Retrofit 2
    implementation(libs.com.squareup.retrofit2.retrofit)
    implementation(libs.com.squareup.retrofit2.kotlinx.serialization.converter)

    // YouTube Extractor
    implementation(libs.com.github.teamnewpipe.newpipeextractor)
}
