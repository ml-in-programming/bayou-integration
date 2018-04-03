package tanvd.bayou.implementation.core.synthesizer

import tanvd.bayou.implementation.model.configurable.synthesizer.Parser
import tanvd.bayou.implementation.model.configurable.synthesizer.dsl.DSubTree

interface Synthesizer {
    fun synthesize(parser: Parser, ast: DSubTree): List<String>
}