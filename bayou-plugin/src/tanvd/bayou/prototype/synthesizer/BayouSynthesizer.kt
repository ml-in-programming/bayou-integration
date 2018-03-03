package tanvd.bayou.prototype.synthesizer

import tanvd.bayou.implementation.BayouClient
import tanvd.bayou.prototype.synthesizer.implementation.BayouTextConverter
import java.io.InputStreamReader

object BayouSynthesizer {
    private val model = BayouClient.getConfigurableModel(InputStreamReader(BayouSynthesizer::class.java.classLoader.getResourceAsStream("stdlib.json")).readText())

    operator fun invoke(request: BayouRequest): BayouResponse? {
        return BayouTextConverter.fromProgramText(model.synthesize(BayouTextConverter.toProgramText(request), 100).first { it.isNotBlank() })
    }
}