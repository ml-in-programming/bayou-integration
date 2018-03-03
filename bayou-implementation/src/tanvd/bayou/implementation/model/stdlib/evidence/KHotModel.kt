package tanvd.bayou.implementation.model.stdlib.evidence

import tanvd.bayou.implementation.utils.JsonUtils
import tanvd.bayou.implementation.utils.Resource
import kotlin.reflect.KClass

open class KHotModel(vocabFile: String) {
    private val vocabulary: MutableMap<String, Int> = HashMap()

    init {
        val serializedModel = Resource.getLines("artifacts/model/stdlib/vocab/$vocabFile").joinToString(separator = " ")
        val vocabList = JsonUtils.readValue(serializedModel, List::class as KClass<List<String>>)
        for ((ind, str) in vocabList.withIndex()) {
            vocabulary.put(str, ind)
        }
    }

    fun transform(doc: List<String>): Array<Int> {
        val kHotVector = Array(vocabulary.size, {0})
        for (str in doc) {
            kHotVector[vocabulary[str]!!] = 1
        }
        return kHotVector
    }

}