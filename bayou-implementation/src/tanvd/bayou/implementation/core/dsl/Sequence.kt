package tanvd.bayou.implementation.core.dsl

import kotlin.collections.ArrayList

data class Sequence(val calls: MutableList<String> = ArrayList()) {
    fun addCall(apiCall: String) {
        calls.add(apiCall)
    }

    /* check if this is a subsequence of "seq" from index 0 */
    fun isSubsequenceOf(seq: Sequence): Boolean {
        if (calls.size > seq.calls.size)
            return false
        return calls.indices.none { calls[it] != seq.calls[it] }
    }
}
