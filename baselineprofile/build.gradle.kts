// Génération du baseline profile et mesure du démarrage de :app.
plugins {
    id("dromos.android.test")
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.gabrielifrim.dromos.baselineprofile"
    targetProjectPath = ":app"
}

baselineProfile {
    // Sur l'émulateur branché (ANDROID_SERIAL=emulator-5554), pas sur un appareil géré par Gradle.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
