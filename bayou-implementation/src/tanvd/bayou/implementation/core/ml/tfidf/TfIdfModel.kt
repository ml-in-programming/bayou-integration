package tanvd.bayou.implementation.core.ml.tfidf

import tanvd.bayou.implementation.utils.JsonUtil
import tanvd.bayou.implementation.utils.equals
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader

open class TfIdfModel(val  modelFile: String, val vocabFile: String) {
    val prefix = "/home/tanvd/Diploma/bayou-integration/bayou-implementation/resources/artifacts/model/tfidf/"

    val idfVector: DoubleArray
    val vocabulary: Map<String, Int>

    init {
        val serializedModel = BufferedReader(InputStreamReader(FileInputStream(prefix + modelFile))).readLine()
        idfVector = JsonUtil.readValue(serializedModel, DoubleArray::class)
        val serializedVocab = BufferedReader(InputStreamReader(FileInputStream(prefix + vocabFile))).readLine()
        vocabulary = JsonUtil.readValue(serializedVocab, Map::class) as Map<String, Int>
    }

    fun transform(doc: List<String>): List<Float> {
        val vector = DoubleArray(vocabulary.size)
        for (word in doc) {
            vector[vocabulary[word]!!]++
        }
        for ((ind, value) in idfVector.withIndex()) {
            vector[ind] *= value
        }

        val normValue = Math.sqrt(vector.map { it * it }.sum())
        var result = vector
        if (!normValue.equals(0.0, 0.000001)) {
            result = vector.map { it / normValue }.toDoubleArray()
        }

        return result.map { it.toFloat() }
    }

}