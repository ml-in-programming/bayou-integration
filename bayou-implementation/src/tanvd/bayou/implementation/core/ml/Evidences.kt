package tanvd.bayou.implementation.core.ml

data class Evidences(val apicalls: MutableList<String> = ArrayList(),
                     val types: MutableList<String> = ArrayList(),
                     val context: MutableList<String> = ArrayList()) {
    companion object {
        //Think about arrays and generics
        fun typeFromCall(call: String): String? = call.takeWhile { it != '(' }.split(".").reversed()[1]

        fun apicallFromCall(call: String): String? = call.takeWhile { it != '(' }.split(".").reversed()[0]

        fun contextFromCall(call: String): List<String> {
            val args = call.split('(')[1].split(')')[0].split(',')
            return args.map { it.split(".").reversed()[0] }
        }
    }
}