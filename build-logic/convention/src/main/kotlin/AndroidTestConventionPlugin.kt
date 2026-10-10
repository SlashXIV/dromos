import com.android.build.api.dsl.TestExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

/**
 * Module de test autonome (`com.android.test`) qui instrumente l'application depuis
 * l'extérieur : génération du baseline profile et mesure du démarrage. Même socle que
 * les autres modules, pour ne pas mesurer contre une autre plateforme.
 */
class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.test")

            val extension = extensions.getByType<TestExtension>()
            configureKotlinAndroid(extension)

            extension.apply {
                defaultConfig.apply {
                    targetSdk = libs.versionInt("targetSdk")
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
            }
        }
    }
}
