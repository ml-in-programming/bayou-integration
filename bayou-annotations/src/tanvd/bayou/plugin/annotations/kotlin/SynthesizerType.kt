package tanvd.bayou.plugin.annotations.kotlin

enum class SynthesizerType {
    Android,
    StdLib
}

annotation class BayouSynthesizer(val type: SynthesizerType)