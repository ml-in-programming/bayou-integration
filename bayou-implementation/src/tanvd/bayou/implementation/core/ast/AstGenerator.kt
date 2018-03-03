package tanvd.bayou.implementation.core.ast

import tanvd.bayou.implementation.core.synthesizer.DSubTree


interface AstGeneratorInput

interface AstGenerator<in E: AstGeneratorInput> {
    fun process(input: E): List<*>
}