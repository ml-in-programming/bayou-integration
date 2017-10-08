package tanvd.bayou.implementation.application.server

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

/**
 * Configuration options for the Api Synthesis Server application.
 */
internal object Configuration {
    val RequestProcessingThreadPoolSize: Int

    val ListenPort: Int

    val SynthesizeTimeoutMs: Int

    val UseSynthesizeEchoMode: Boolean

    val EchoModeDelayMs: Long

    val SynthesisLogBucketName: String

    val SynthesisQualityFeedbackLogBucketName: String

    val EvidenceClasspath: String

    val AndroidJarPath: File

    private val MEGA_BYTES_IN_BYTES = 1000000

    var CodeCompletionRequestBodyMaxBytesCount = MEGA_BYTES_IN_BYTES

    val CorsAllowedOrigins: Array<String>

    init {
        val properties = Properties()
        run {
            val propertyKey = "configurationFile"

            val configPathStr = if (System.getProperty(propertyKey) != null)
                System.getProperty(propertyKey)
            else
                "apiSynthesisServerConfig.properties"
            val configPath: File
            try {
                // do getCanonicalFile to resolve path entries like ../
                // this makes for better error messages in the RuntimeException created if .load(...) exceptions.
                configPath = File(configPathStr).canonicalFile
            } catch (e: IOException) {
                throw RuntimeException("Could not load configuration file: " + configPathStr, e)
            }

            try {
                properties.load(FileInputStream(configPath))
            } catch (e: IOException) {
                throw RuntimeException("Could not load configuration file: " + configPath.absolutePath, e)
            }
        }

        RequestProcessingThreadPoolSize = Integer.parseInt(properties.getProperty("RequestProcessingThreadPoolSize"))
        ListenPort = Integer.parseInt(properties.getProperty("ListenPort"))
        SynthesizeTimeoutMs = Integer.parseInt(properties.getProperty("SynthesizeTimeoutMs"))
        UseSynthesizeEchoMode = java.lang.Boolean.parseBoolean(properties.getProperty("UseSynthesizeEchoMode"))
        EchoModeDelayMs = java.lang.Long.parseLong(properties.getProperty("EchoModeDelayMs"))
        SynthesisLogBucketName = properties.getProperty("SynthesisLogBucketName")
        SynthesisQualityFeedbackLogBucketName = properties.getProperty("SynthesisQualityFeedbackLogBucketName")
        EvidenceClasspath = properties.getProperty("EvidenceClasspath")
        AndroidJarPath = File(properties.getProperty("AndroidJarPath"))
        CorsAllowedOrigins = properties.getProperty("CorsAllowedOrigins").split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() // split by whitespace


    }
}
