/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package tanvd.bayou.implementation.model.stdlib.synthesizer.dsl

import java.util.*

class Sequence {
    internal val calls: MutableList<String>

    constructor() {
        this.calls = ArrayList()
    }

    constructor(calls: List<String>) {
        this.calls = ArrayList(calls)
    }

    fun getCalls(): List<String> {
        return calls
    }

    fun addCall(apiCall: String) {
        calls.add(apiCall)
    }

    /* check if this is a subsequence of "seq" from index 0 */
    fun isSubsequenceOf(seq: Sequence): Boolean {
        if (calls.size > seq.calls.size)
            return false
        for (i in calls.indices)
            if (calls[i] != seq.calls[i])
                return false
        return true
    }

    override fun equals(o: Any?): Boolean {
        if (o == null || o !is Sequence)
            return false
        val seq = o as Sequence?
        return calls == seq!!.calls
    }

    override fun hashCode(): Int {
        var code = 17
        for (call in calls)
            code = 31 * code + call.hashCode()
        return code
    }
}
