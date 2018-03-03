package tanvd.bayou.implementation.core.evidence

import tanvd.bayou.implementation.core.ast.AstGeneratorInput

interface Evidences {
    fun toAstGeneratorInput(): AstGeneratorInput
}