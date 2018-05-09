package tanvd.bayou.plugin.synthesizer

import tanvd.bayou.implementation.BayouClient
import tanvd.bayou.implementation.facade.SynthesisProgress
import tanvd.bayou.implementation.model.configurable.ConfigurableSynthesizingModel
import tanvd.bayou.plugin.EvidencesInput
import tanvd.bayou.plugin.annotations.kotlin.SynthesizerType
import tanvd.bayou.plugin.synthesizer.implementation.BayouTextConverter
import java.io.InputStreamReader

object BayouSynthesizer {
    private val models: Map<SynthesizerType, ConfigurableSynthesizingModel> = HashMap()

    operator fun invoke(type: SynthesizerType, request: BayouRequest, progress: SynthesisProgress): BayouResponse? {
        val model = models[type]
                ?: BayouClient.getConfigurableModel(InputStreamReader(BayouSynthesizer::class.java.classLoader.getResourceAsStream("${type.name.toLowerCase()}/config.json")).readText())
        return BayouTextConverter.fromProgramText(model.synthesize(BayouTextConverter.toProgramText(request), 100, progress).first { it.isNotBlank() })
    }

    fun getEvidencesFromLang(text: String): EvidencesInput {
        val name = text.split("\n").first { it.isNotBlank() }.trim()
        val synthesizerType = when (name) {
            "STDLIB" -> SynthesizerType.StdLib
            "ANDROID" -> SynthesizerType.Android
            else -> error("UNKNOWN TYPE")
        }
        val input = EvidencesInput(synthesizerType)
        Regex("(\\w+)[\\s]*:=[\\s]*(\\w+)").findAll(text).forEach {
            val typeName = it.groupValues[1]
            when (typeName) {
                "API" -> {
                    input.apiCalls.add(it.groupValues[2])
                }
                "TYPE" -> {
                    input.apiTypes.add(it.groupValues[2])
                }
                "CONTEXT" -> {
                    input.contextClasses.add(it.groupValues[2])
                }
                else -> {
                }
            }
        }

        return input
    }
}