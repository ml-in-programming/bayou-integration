package tanvd.bayou.implementation.core.ml

import tanvd.bayou.implementation.utils.dropLastWhileIncluding
import tanvd.bayou.implementation.utils.dropWhileIncluding

data class Evidences (val apicalls: List<String>, val types: List<String>, val context: List<String>) {
//    def from_call(call):
//    split = list(reversed([q for q in call.split('(')[0].split('.')[:-1] if q[0].isupper()]))
//    types = [split[1], split[0]] if len(split) > 1 else [split[0]]
//    return [re.sub('<.*', r'', t) for t in types]  # ignore generic types in evidence
    companion object {
    fun typeFromCall(call: String) : String? = call.takeWhile { it != '(' }.split(".").reversed()[1]

    fun apicallFromCall(call: String): String? = call.takeWhile { it != '(' }.split(".").reversed()[0]
    }
}