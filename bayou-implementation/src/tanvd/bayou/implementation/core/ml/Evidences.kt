package tanvd.bayou.implementation.core.ml

data class Evidences(val apicalls: List<String>, val types: List<String>, val context: List<String>) {
    companion object {
        fun typeFromCall(call: String): String? = call.takeWhile { it != '(' }.split(".").reversed()[1]

        fun apicallFromCall(call: String): String? = call.takeWhile { it != '(' }.split(".").reversed()[0]
    }
}