package tanvd.bayou.implementation.core.quality

import tanvd.bayou.implementation.core.synthesizer.DSubTree

interface RelevanceMeasurement<T : DSubTree> {
    fun sortAndDedup(programs: List<T>): List<T>
}