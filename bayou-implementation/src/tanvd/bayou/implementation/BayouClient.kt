package tanvd.bayou.implementation

import tanvd.bayou.implementation.model.SynthesizingModel
import tanvd.bayou.implementation.model.android.AndroidSynthesizingModel
import tanvd.bayou.implementation.model.configurable.ConfigurableSynthesizingModel
import tanvd.bayou.implementation.model.stdlib.StdlibSynthesizingModel
import tanvd.bayou.implementation.utils.JsonUtils
import java.io.File

object BayouClient {
//    private val synthesizers by lazy {
//        mapOf(
//                "android" to AndroidSynthesizingModel())
//                "stdlib" to StdlibSynthesizingModel())
//    }

//    fun getModel(name: String): SynthesizingModel {
//        return synthesizers[name]!!
//    }

    fun getConfigurableModel(config: String): SynthesizingModel {
        return ConfigurableSynthesizingModel(JsonUtils.readValue(config, Config::class))
    }
}