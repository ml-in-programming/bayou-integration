package tanvd.bayou.implementation.model.android.evidence

import tanvd.bayou.implementation.core.evidence.Evidences
import tanvd.bayou.implementation.core.evidence.EvidencesExtractorResult
import tanvd.bayou.implementation.model.android.ast.AndroidAstGeneratorInput

data class AndroidEvidences(val apicalls: MutableList<String> = ArrayList(),
                            val types: MutableList<String> = ArrayList(),
                            val context: MutableList<String> = ArrayList()) : Evidences {

    constructor(result: EvidencesExtractorResult) : this(result.apicalls, result.types, result.context)

    override fun toAstGeneratorInput(): AndroidAstGeneratorInput {
        return AndroidAstGeneratorInput(ApiCallProcessor.wrangle(apicalls), ApiTypeProcessor.wrangle(types), ContextTypeProcessor.wrangle(context))
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