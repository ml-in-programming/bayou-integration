package tanvd.bayou.prototype.synthesizer

data class BayouRequest(val inputParams: MutableList<InputParameter> = ArrayList(), val apiCalls: MutableList<String> = ArrayList(),
                        val apiTypes: MutableList<String> = ArrayList(), val contextClasses: MutableList<String> = ArrayList())

data class InputParameter(val name: String, val klass: String)