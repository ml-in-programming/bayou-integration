package tanvd.bayou.implementation

import java.io.File

/**
 * Configuration options for the Api Synthesis Server application.
 */
internal object Configuration {
    val SynthesizeTimeoutMs: Int = 30000

    val EvidenceClasspath: String = "resources/artifacts/classes"

    val AndroidJarPath: File = File("resources/artifacts/jar/android.jar")
}
