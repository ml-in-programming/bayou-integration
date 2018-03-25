package tanvd.bayou.prototype.synthesizer

import tanvd.bayou.implementation.BayouClient
import tanvd.bayou.implementation.facade.SynthesisProgress
import tanvd.bayou.implementation.model.configurable.ConfigurableSynthesizingModel
import tanvd.bayou.prototype.annotations.kotlin.SynthesizerType
import tanvd.bayou.prototype.synthesizer.implementation.BayouTextConverter
import java.io.InputStreamReader

object BayouSynthesizer {
    private val models: Map<SynthesizerType, ConfigurableSynthesizingModel> = HashMap()

    operator fun invoke(type: SynthesizerType, request: BayouRequest, progress: SynthesisProgress): BayouResponse? {
        val model = models[type] ?: BayouClient.getConfigurableModel(InputStreamReader(BayouSynthesizer::class.java.classLoader.getResourceAsStream("${type.name.toLowerCase()}/config.json")).readText())
        return BayouTextConverter.fromProgramText(model.synthesize(BayouTextConverter.toProgramText(request), 100, progress).first { it.isNotBlank() })
    }
}