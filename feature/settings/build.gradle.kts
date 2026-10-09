plugins {
    id("dromos.android.library")
    id("dromos.android.compose")
    id("dromos.android.hilt")
}

android {
    namespace = "com.gabrielifrim.dromos.feature.settings"
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.core.domain)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
