package tanvd.bayou.implementation.model.stdlib.quality

import tanvd.bayou.implementation.core.quality.AstVerification
import tanvd.bayou.implementation.model.stdlib.evidence.StdlibEvidences
import tanvd.bayou.implementation.model.stdlib.synthesizer.dsl.DSubTree

object StdlibEvidenceAstVerification : AstVerification<StdlibEvidences, DSubTree> {
    override fun verify(tree: DSubTree, evidences: StdlibEvidences): Boolean {
        val calls = tree.bagOfAPICalls().map { it._call }
        val callsTypes = calls.mapNotNull { StdlibEvidences.typeFromCall(it) }
        val callsApis = calls.mapNotNull { StdlibEvidences.apicallFromCall(it) }
        val typesContains = evidences.types.all {
            callsTypes.contains(it)
        }
        val apisContains = evidences.apicalls.all {
            callsApis.contains(it)
        }
        return typesContains && apisContains
    }

}