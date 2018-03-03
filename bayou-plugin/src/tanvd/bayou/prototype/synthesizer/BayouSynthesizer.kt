package tanvd.bayou.prototype.synthesizer

import tanvd.bayou.implementation.BayouClient
import tanvd.bayou.prototype.synthesizer.implementation.BayouTextConverter

object BayouSynthesizer {
    private val model = BayouClient.getModel("stdlib")

    operator fun invoke(request: BayouRequest): BayouResponse? {
        return BayouTextConverter.fromProgramText(model.synthesize(BayouTextConverter.toProgramText(request), 100).first { it.isNotBlank() })
    }
}