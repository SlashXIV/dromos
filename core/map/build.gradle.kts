plugins {
    id("dromos.android.library")
    id("dromos.android.compose")
}

android {
    namespace = "com.gabrielifrim.dromos.core.map"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.maplibre.android)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
