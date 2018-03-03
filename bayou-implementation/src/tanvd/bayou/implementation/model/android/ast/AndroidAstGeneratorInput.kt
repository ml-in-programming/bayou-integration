package tanvd.bayou.implementation.model.android.ast

import tanvd.bayou.implementation.core.ast.AstGeneratorInput

data class AndroidAstGeneratorInput(val apiCalls: Array<Float>, val apiTypes: Array<Float>, val contextClasses: Array<Float>): AstGeneratorInput