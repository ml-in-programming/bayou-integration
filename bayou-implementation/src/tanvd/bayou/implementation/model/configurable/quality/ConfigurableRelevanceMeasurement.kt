package tanvd.bayou.implementation.model.configurable.quality

import tanvd.bayou.implementation.core.quality.RelevanceMeasurement
import tanvd.bayou.implementation.model.configurable.synthesizer.dsl.DSubTree


object ConfigurableRelevanceMeasurement : RelevanceMeasurement<DSubTree> {
    override fun sortAndDedup(programs: List<DSubTree>): List<DSubTree> {
        return programs.groupBy { it }.map { it.key to it.value.size }.sortedByDescending { it.second }.map { it.first }
    }
}