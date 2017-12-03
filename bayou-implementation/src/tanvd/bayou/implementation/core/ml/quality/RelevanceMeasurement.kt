package tanvd.bayou.implementation.core.ml.quality

import tanvd.bayou.implementation.core.code.dsl.DSubTree
import tanvd.bayou.implementation.core.ml.Evidences

object RelevanceMeasurement {
    fun dedupAndSort(asts: List<DSubTree>): List<Pair<DSubTree, Int>> {
        return asts.groupBy { it }.map { it.key to it.value.size }.sortedByDescending { it.second }
    }

    fun filterByEvidences(calls: List<String>, evidences: Evidences): Boolean {
        val callsTypes = calls.mapNotNull { Evidences.typeFromCall(it) }
        val callsApis = calls.mapNotNull { Evidences.apicallFromCall(it) }
        val callsContext = calls.flatMap { Evidences.contextFromCall(it) }.mapNotNull { it }
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