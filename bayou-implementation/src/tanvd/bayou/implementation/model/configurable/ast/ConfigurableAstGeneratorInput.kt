package tanvd.bayou.implementation.model.configurable.ast

import tanvd.bayou.implementation.EvidenceType
import tanvd.bayou.implementation.core.ast.AstGeneratorInput

data class ConfigurableAstGeneratorInput(val types: List<EvidenceType>, val evidences: Map<EvidenceType, Array<Float>>): AstGeneratorInput