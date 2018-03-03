package tanvd.bayou.implementation.model

interface SynthesizingModel {
    /**
     * Synthesize up to maxPrograms number of programs using this model from provided code
     */
    fun synthesize(code: String, maxPrograms: Int): Iterable<String>
}

/**
 * Thrown to indicate a problem arose during synthesis.
 */
class SynthesizeException(cause: Throwable) : Exception(cause)