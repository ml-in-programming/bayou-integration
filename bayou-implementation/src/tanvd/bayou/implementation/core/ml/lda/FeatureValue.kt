package tanvd.bayou.implementation.core.ml.lda


import com.google.common.base.Preconditions


class FeatureValue {

    private/* final */ var feature: Any? = null // possible types: String, Text, Integer, Long
    private/* final */ var value: Double = 0.toDouble()

    val featureAsInt: Int
        get() {
            Preconditions.checkNotNull<Any>(feature)
            Preconditions.checkArgument(feature is Int)
            return (feature as Int).toInt()
        }

    val featureAsString: String
        get() = feature!!.toString()

    val valueAsFloat: Float
        get() = value.toFloat()

    constructor() {}// used for Probe

    constructor(f: Any, v: Float) {
        this.feature = f
        this.value = v.toDouble()
    }

    constructor(f: Any, v: Double) {
        this.feature = f
        this.value = v
    }

    fun <T> getFeature(): T? {
        return feature as T?
    }

    fun getValue(): Double {
        return value
    }

    fun setValue(value: Float) {
        this.value = value.toDouble()
    }

    fun setValue(value: Double) {
        this.value = value
    }

    companion object {

        @Throws(IllegalArgumentException::class)
        fun parseFeatureAsString( s: String): FeatureValue {
            val pos = s.indexOf(':')
            if (pos == 0) {
                throw IllegalArgumentException("Invalid feature value representation: " + s)
            }

            val feature: String
            val weight: Double
            if (pos > 0) {
                feature = s.substring(0, pos)
                val s2 = s.substring(pos + 1)
                try {
                    weight = java.lang.Double.parseDouble(s2)
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException("Failed to parse a feature value: " + s, e)
                }

            } else {
                feature = s
                weight = 1.0
            }
            return FeatureValue(feature, weight)
        }

        @Throws(IllegalArgumentException::class)
        fun parseFeatureAsString(s: String, probe: FeatureValue) {
            val pos = s.indexOf(':')
            if (pos == 0) {
                throw IllegalArgumentException("Invalid feature value representation: " + s)
            }
            if (pos > 0) {
                probe.feature = s.substring(0, pos)
                val s2 = s.substring(pos + 1)
                try {
                    probe.value = java.lang.Double.parseDouble(s2)
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException("Failed to parse a feature value: " + s, e)
                }

            } else {
                probe.feature = s
                probe.value = 1.0
            }
        }
    }

}