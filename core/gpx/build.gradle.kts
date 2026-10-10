// Formats d'échange (GPX, KML) : JVM pur, comme le calcul, pour tester l'aller-retour sans émulateur.
plugins {
    id("dromos.jvm.library")
}

dependencies {
    api(projects.core.domain)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
