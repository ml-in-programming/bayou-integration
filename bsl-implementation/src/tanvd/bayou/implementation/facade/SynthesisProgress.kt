package tanvd.bayou.implementation.facade

enum class SynthesisPhase {
    IDLE,
    Started,
    Parsing,
    Embedding,
    SketchGeneration,
    Concretization,
    Finished
}

interface SynthesisProgress {
    var phase: SynthesisPhase
    var fraction: Double
}

class SimpleSynthesisProgress(override var phase: SynthesisPhase = SynthesisPhase.IDLE, override var fraction: Double = 0.0) : SynthesisProgress