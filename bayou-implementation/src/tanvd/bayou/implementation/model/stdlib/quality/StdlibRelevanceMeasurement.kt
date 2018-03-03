package tanvd.bayou.implementation.model.stdlib.quality

import tanvd.bayou.implementation.core.quality.RelevanceMeasurement
import tanvd.bayou.implementation.model.stdlib.synthesizer.dsl.DSubTree


object StdlibRelevanceMeasurement : RelevanceMeasurement<DSubTree> {
    override fun sortAndDedup(programs: List<DSubTree>): List<DSubTree> {
        return programs.groupBy { it }.map { it.key to it.value.size }.sortedByDescending { it.second }.map { it.first }
    }
}