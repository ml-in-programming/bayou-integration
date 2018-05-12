package tanvd.bayou.implementation

enum class EvidenceType {
    ApiCall,
    ApiType,
    ContextType
}

enum class WrangleType {
    LDA,
    KHot
}

enum class SynthesizerMode {
    Combinatorial,
    Conditional
}


data class WrangleConfig(val type: WrangleType, val config: Map<String, String>)

data class EvidenceConfig(val type: EvidenceType, val wrangling: WrangleConfig)

data class ClasspathUnit(val lib_name: String, val lib_url: String)

data class SynthesizerConfig(val psi_size: Int, val model_url: String, val config_url: String)

data class ConcretizationConfig(val mode: SynthesizerMode)

data class Config(
        val name: String,
        val classpath: Set<ClasspathUnit>,
        val evidences: Set<EvidenceConfig>,
        val synthesizer: SynthesizerConfig,
        val concretization: ConcretizationConfig)