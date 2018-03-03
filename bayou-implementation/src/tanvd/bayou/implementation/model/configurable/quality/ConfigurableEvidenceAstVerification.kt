package tanvd.bayou.implementation.model.configurable.quality

import tanvd.bayou.implementation.EvidenceType
import tanvd.bayou.implementation.core.quality.AstVerification
import tanvd.bayou.implementation.model.configurable.evidence.ConfigurableEvidences
import tanvd.bayou.implementation.model.configurable.synthesizer.dsl.DSubTree

object ConfigurableEvidenceAstVerification : AstVerification<ConfigurableEvidences, DSubTree> {
    override fun verify(tree: DSubTree, evidences: ConfigurableEvidences): Boolean {
        val calls = tree.bagOfAPICalls().map { it._call }
        val callsTypes = calls.mapNotNull { ConfigurableEvidences.typeFromCall(it) }
        val callsApis = calls.mapNotNull { ConfigurableEvidences.apicallFromCall(it) }
        val callsContext = calls.flatMap { ConfigurableEvidences.contextFromCall(it) }
        val typesContains = evidences.evidences[EvidenceType.ApiType]?.all {
            callsTypes.contains(it)
        }  ?: true
        val apisContains = evidences.evidences[EvidenceType.ApiCall]?.all {
            callsApis.contains(it)
        }  ?: true
        val contextsContains = evidences.evidences[EvidenceType.ContextType]?.all {
            callsContext.contains(it)
        }  ?: true
        return typesContains && apisContains && contextsContains
    }

}