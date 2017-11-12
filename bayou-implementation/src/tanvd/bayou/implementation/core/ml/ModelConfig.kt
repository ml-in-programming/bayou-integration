package tanvd.bayou.implementation.core.ml

data class DecoderConfig(val units: Long, val vocab: Map<String, Long>, val chars: List<String>)
