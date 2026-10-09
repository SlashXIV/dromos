// Module JVM pur : aucune dépendance au framework Android.
// Tout le calcul (distance, allure, dénivelé, splits) vit ici, testable sans émulateur.
plugins {
    id("dromos.jvm.library")
}

dependencies {
    // api : Flow et @Inject apparaissent dans la signature publique des interfaces.
    api(libs.kotlinx.coroutines.core)
    api(libs.javax.inject)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
