plugins {
    id("dromos.android.library")
    id("dromos.android.compose")
    id("dromos.android.hilt")
}

android {
    namespace = "com.gabrielifrim.dromos.feature.record"
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.core.domain)
    implementation(projects.core.tracking)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
