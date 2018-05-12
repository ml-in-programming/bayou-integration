package tanvd.bayou.implementation.model

import tanvd.bayou.implementation.facade.SynthesisProgress

interface SynthesizingModel {
    /**
     * Synthesize up to maxPrograms number of programs using this model from provided code
     */
    fun synthesize(code: String, maxPrograms: Int, synthesisProgress: SynthesisProgress): Iterable<String>
}

/**
 * Thrown to indicate a problem arose during synthesis.
 */
class SynthesizeException(cause: Throwable) : Exception(cause)