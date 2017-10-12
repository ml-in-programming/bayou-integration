package tanvd.bayou.implementation

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*

/**
 * Configuration options for the Api Synthesis Server application.
 */
internal object Configuration {
    val SynthesizeTimeoutMs: Int = 30000

    val EvidenceClasspath: String = "resources/artifacts/classes"

    val AndroidJarPath: File = File("resources/artifacts/jar/android.jar")
}
