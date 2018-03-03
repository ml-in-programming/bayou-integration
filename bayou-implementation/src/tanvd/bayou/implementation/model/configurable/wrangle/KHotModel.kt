package tanvd.bayou.implementation.model.configurable.wrangle

import tanvd.bayou.implementation.utils.JsonUtils
import tanvd.bayou.implementation.utils.Resource
import java.io.File
import kotlin.reflect.KClass

open class KHotModel(vocabFile: File) {
    private val vocabulary: MutableMap<String, Int> = HashMap()

    init {
        val serializedModel = vocabFile.readLines().joinToString(separator = " ")
        val vocabList = JsonUtils.readValue(serializedModel, List::class as KClass<List<String>>)
        for ((ind, str) in vocabList.withIndex()) {
            vocabulary.put(str, ind)
        }
    }

    fun transform(doc: List<String>): Array<Float> {
        val kHotVector = Array(vocabulary.size, {0.0f})
        for (str in doc) {
            kHotVector[vocabulary[str]!!] = 1.0f
        }
        return kHotVector
    }

}