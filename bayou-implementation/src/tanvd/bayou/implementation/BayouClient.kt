package tanvd.bayou.implementation

import tanvd.bayou.implementation.model.SynthesizingModel
import tanvd.bayou.implementation.model.android.AndroidSynthesizingModel
import tanvd.bayou.implementation.model.stdlib.StdlibSynthesizingModel

object BayouClient {
    private val synthesizers = mapOf(
            "android" to AndroidSynthesizingModel(),
            "stdlib" to StdlibSynthesizingModel())

    fun getModel(name: String): SynthesizingModel {
        return synthesizers[name]!!
    }
}