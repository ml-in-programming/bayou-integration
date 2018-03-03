package tanvd.bayou.implementation.core.evidence

data class EvidencesExtractorResult(val apicalls: MutableList<String> = ArrayList(),
                                    val types: MutableList<String> = ArrayList(),
                                    val context: MutableList<String> = ArrayList())