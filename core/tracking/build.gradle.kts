plugins {
    id("dromos.android.library")
    id("dromos.android.hilt")
}

android {
    namespace = "com.gabrielifrim.dromos.core.tracking"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
