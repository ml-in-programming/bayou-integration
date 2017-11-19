package tanvd.bayou.implementation

import tanvd.bayou.implementation.core.code.synthesizer.ApiSynthesizerRemoteTensorFlowAsts

object BayouAPI {
    private val tensorFlowAPI = ApiSynthesizerRemoteTensorFlowAsts("localhost", 8084,
            Configuration.SynthesizeTimeoutMs,
            Configuration.EvidenceClasspath,
            Configuration.AndroidJarPath)

    fun synthesize(code: String): List<String>? {
        return tensorFlowAPI.synthesise(code, Integer.MAX_VALUE).toList()
    }
}