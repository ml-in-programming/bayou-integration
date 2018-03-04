package tanvd.bayou.prototype.annotations.kotlin

enum class SynthesizerType {
    Android,
    StdLib
}

annotation class BayouSynthesizer(val type: SynthesizerType)