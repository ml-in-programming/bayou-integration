package tanvd.bayou.implementation.model.android.quality

import tanvd.bayou.implementation.core.quality.RelevanceMeasurement
import tanvd.bayou.implementation.model.android.synthesizer.dsl.DSubTree

object AndroidRelevanceMeasurement : RelevanceMeasurement<DSubTree> {
    override fun sortAndDedup(programs: List<DSubTree>): List<DSubTree> {
        return programs.groupBy { it }.map { it.key to it.value.size }.sortedByDescending { it.second }.map { it.first }
    }
}