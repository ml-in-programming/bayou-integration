package tanvd.bayou.implementation.core.ast

import tanvd.bayou.implementation.facade.SynthesisProgress


interface AstGeneratorInput

interface AstGenerator<in E : AstGeneratorInput> {
    fun process(input: E, synthesisProgress: SynthesisProgress, maxNumber: Int): List<*>
}