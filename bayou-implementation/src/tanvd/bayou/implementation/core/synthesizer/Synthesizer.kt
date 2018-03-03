package tanvd.bayou.implementation.core.synthesizer

import tanvd.bayou.implementation.model.android.synthesizer.dsl.DSubTree
import tanvd.bayou.implementation.model.android.synthesizer.Parser

interface Synthesizer {
    fun synthesize(parser: Parser, ast: DSubTree): List<String>
}