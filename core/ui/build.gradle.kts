plugins {
    id("dromos.android.library")
    id("dromos.android.compose")
}

android {
    namespace = "com.gabrielifrim.dromos.core.ui"
}

dependencies {
    // Les libellés des énumérations du domaine vivent ici (cf. text/DomainLabels.kt).
    implementation(projects.core.domain)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
