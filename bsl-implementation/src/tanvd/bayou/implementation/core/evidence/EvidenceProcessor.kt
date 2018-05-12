package tanvd.bayou.implementation.core.evidence

interface EvidenceProcessor<out E : Any> {
    fun wrangle(evidences: List<String>): E
}