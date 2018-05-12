package tanvd.bayou.implementation.model.stdlib.evidence

import tanvd.bayou.implementation.core.evidence.EvidenceProcessor

object ApiCallProcessor : EvidenceProcessor<Array<Int>> {

    private val kHotModel = KHotModel("api_calls.json")

    override fun wrangle(evidences: List<String>): Array<Int> {
        return kHotModel.transform(evidences)
    }
}