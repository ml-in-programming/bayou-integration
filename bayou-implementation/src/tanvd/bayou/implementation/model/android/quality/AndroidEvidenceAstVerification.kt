package tanvd.bayou.implementation.model.android.quality

import tanvd.bayou.implementation.core.quality.AstVerification
import tanvd.bayou.implementation.model.android.evidence.AndroidEvidences
import tanvd.bayou.implementation.model.android.synthesizer.dsl.DSubTree

object AndroidEvidenceAstVerification : AstVerification<AndroidEvidences, DSubTree> {
    override fun verify(tree: DSubTree, evidences: AndroidEvidences): Boolean {
        val calls = tree.bagOfAPICalls().map { it._call }
        val callsTypes = calls.mapNotNull { AndroidEvidences.typeFromCall(it) }
        val callsApis = calls.mapNotNull { AndroidEvidences.apicallFromCall(it) }
        val callsContext = calls.flatMap { AndroidEvidences.contextFromCall(it) }.map { it }
        val typesContains = evidences.types.all {
            callsTypes.contains(it)
        }
        val apisContains = evidences.apicalls.all {
            callsApis.contains(it)
        }
        val contextContains = evidences.context.all {
            callsContext.contains(it)
        }
        return typesContains && apisContains && contextContains
    }

}