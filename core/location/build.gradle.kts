plugins {
    id("dromos.android.library")
    id("dromos.android.hilt")
}

android {
    namespace = "com.gabrielifrim.dromos.core.location"
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
