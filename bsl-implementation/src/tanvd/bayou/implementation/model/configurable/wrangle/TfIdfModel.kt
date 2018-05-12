package tanvd.bayou.implementation.model.configurable.wrangle

import tanvd.bayou.implementation.utils.JsonUtils
import tanvd.bayou.implementation.utils.equals
import java.io.File

/**
 * TfIdf implementation.
 * No learning process, uses preprocessed data
 * Reimplementation of sklearn TfIdfVectorizer
 */
open class TfIdfModel(modelFile: File, vocabFile: File) {
    private val idfVector: DoubleArray
    private val vocabulary: Map<String, Int>

    init {
        val serializedModel = modelFile.readLines().joinToString(separator = " ")
        idfVector = JsonUtils.readValue(serializedModel, DoubleArray::class)
        val serializedVocab = vocabFile.readLines().joinToString(separator = " ")
        vocabulary = JsonUtils.readValue(serializedVocab, Map::class) as Map<String, Int>
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