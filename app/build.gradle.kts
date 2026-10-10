plugins {
    id("dromos.android.application")
    id("dromos.android.compose")
    id("dromos.android.hilt")
    // Routes de navigation typées : Navigation Compose sérialise les objets de route.
    alias(libs.plugins.kotlin.serialization)
}

// Version publiée : la SEULE ligne à modifier lors d'une release.
// Règle : correctif = patch, nouveau jalon = mineure, 1.0.0 = cahier des charges complet.
val dromosVersion = "0.8.0"

android {
    namespace = "com.gabrielifrim.dromos"

    defaultConfig {
        applicationId = "com.gabrielifrim.dromos"
        val (major, minor, patch) = dromosVersion.split(".").map { it.toInt() }
        versionCode = major * 10000 + minor * 100 + patch
        versionName = dromosVersion
    }

    // Signature de publication : jamais dans le dépôt. En local, propriétés Gradle
    // dromos.signing.* (~/.gradle/gradle.properties) ; en CI, variables d'environnement.
    // Absentes, le release reste non signé : un clone du dépôt se construit toujours.
    fun signing(key: String): String? =
        providers.gradleProperty("dromos.signing.$key").orNull
            ?: providers.environmentVariable("DROMOS_SIGNING_${key.uppercase()}").orNull
    val signingValues = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
        .associateWith { signing(it) }
    val canSign = signingValues.values.all { it != null }
    if (canSign) {
        signingConfigs.create("release") {
            storeFile = file(signingValues.getValue("storeFile")!!)
            storePassword = signingValues.getValue("storePassword")
            keyAlias = signingValues.getValue("keyAlias")
            keyPassword = signingValues.getValue("keyPassword")
        }
    }

    buildTypes {
        release {
            if (canSign) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    // MapLibre embarque son moteur natif pour quatre architectures. La publication
    // ne garde que celles des téléphones (ARM) ; le debug garde x86 pour l'émulateur.
    androidComponents {
        onVariants(selector().withBuildType("release")) { variant ->
            variant.packaging.jniLibs.excludes.addAll(listOf("lib/x86/**", "lib/x86_64/**"))
        }
    }

    // Langues proposées dans les réglages Android (13+) : français par défaut, anglais.
    androidResources {
        generateLocaleConfig = true
    }
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.ui)
    implementation(projects.core.location)
    implementation(projects.core.tracking)
    implementation(projects.feature.history)
    implementation(projects.feature.settings)
    implementation(projects.feature.record)
    implementation(projects.feature.activity)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
