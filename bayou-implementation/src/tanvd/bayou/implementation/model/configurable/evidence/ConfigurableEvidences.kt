package tanvd.bayou.implementation.model.configurable.evidence

import tanvd.bayou.implementation.EvidenceType
import tanvd.bayou.implementation.core.ast.AstGeneratorInput
import tanvd.bayou.implementation.core.evidence.Evidences
import tanvd.bayou.implementation.core.evidence.EvidencesExtractorResult

data class ConfigurableEvidences(val types: List<EvidenceType>, val evidences: MutableMap<EvidenceType, List<String>> = HashMap()) : Evidences {
    override fun toAstGeneratorInput(): AstGeneratorInput {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    constructor(types: List<EvidenceType>, result: EvidencesExtractorResult) : this(types) {
        if (types.contains(EvidenceType.ApiType)) {
            this.evidences[EvidenceType.ApiType] = result.types
        }
        if (types.contains(EvidenceType.ApiCall)) {
            this.evidences[EvidenceType.ApiCall] = result.apicalls
        }
        if (types.contains(EvidenceType.ContextType)) {
            this.evidences[EvidenceType.ContextType] = result.context
        }
    }

    companion object {
        //Think about arrays and generics
        fun typeFromCall(call: String): String? = call.takeWhile { it != '(' }.split(".").reversed()[1]

        fun apicallFromCall(call: String): String? = call.takeWhile { it != '(' }.split(".").reversed()[0]

        fun contextFromCall(call: String): List<String> {
            val args = call.split('(')[1].split(')')[0].split(',')
            return args.map { it.split(".").reversed()[0] }
        }
    }
}