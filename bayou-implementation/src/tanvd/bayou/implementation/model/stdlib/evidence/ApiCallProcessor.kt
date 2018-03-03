package tanvd.bayou.implementation.model.stdlib.evidence

import tanvd.bayou.implementation.core.evidence.EvidenceProcessor
import tanvd.bayou.implementation.model.stdlib.evidence.KHotModel

object ApiCallProcessor: EvidenceProcessor<Array<Int>> {

    private val kHotModel = KHotModel("api_calls.json")

    override fun wrangle(evidences: List<String>): Array<Int> {
        return kHotModel.transform(evidences)
    }
}