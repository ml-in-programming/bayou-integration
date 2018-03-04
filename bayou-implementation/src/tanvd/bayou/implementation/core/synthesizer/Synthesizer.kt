package tanvd.bayou.implementation.core.synthesizer

import tanvd.bayou.implementation.model.configurable.synthesizer.dsl.DSubTree
import tanvd.bayou.implementation.model.configurable.synthesizer.Parser

interface Synthesizer {
    fun synthesize(parser: Parser, ast: DSubTree): List<String>
}