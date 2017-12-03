package tanvd.bayou.implementation

import tanvd.bayou.implementation.core.code.synthesizer.BayouSynthesizer

object BayouAPI {
    private val tensorFlowAPI = BayouSynthesizer()

    fun synthesize(code: String, maxPrograms: Int): List<String>? {
        return tensorFlowAPI.synthesise(code, maxPrograms).toList()
    }
}