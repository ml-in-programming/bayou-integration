package tanvd.bayou.implementation.model.stdlib.ast

import tanvd.bayou.implementation.core.ast.AstGeneratorInput

data class StdlibAstGeneratorInput(val apiCalls: Array<Int>, val apiTypes: Array<Int>): AstGeneratorInput