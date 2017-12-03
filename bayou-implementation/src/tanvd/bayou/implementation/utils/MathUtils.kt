package tanvd.bayou.implementation.utils

import org.apache.commons.math3.special.Gamma

fun Double.equals(value: Double, epsilon: Double): Boolean {
    return Math.abs(this - value) < epsilon
}

fun Float.equals(value: Float, epsilon: Float): Boolean {
    return Math.abs(this - value) < epsilon
}

object MathUtils {
    fun equals(value: Float, expected: Float, delta: Float): Boolean {
        if (java.lang.Double.isNaN(value.toDouble())) {
            return false
        }
        return Math.abs(expected - value) <= delta
    }

    fun equals(value: Double, expected: Double, delta: Double): Boolean {
        if (java.lang.Double.isNaN(value)) {
            return false
        }
        return Math.abs(expected - value) <= delta
    }


    fun sum(arr: FloatArray?): Double {
        if (arr == null) {
            return 0.0
        }

        return arr.sumByDouble { it.toDouble() }
    }

    fun add(src: FloatArray, dst: FloatArray, size: Int) {
        for (i in 0 until size) {
            dst[i] += src[i]
        }
    }

    fun add(src: FloatArray, dst: DoubleArray, size: Int) {
        for (i in 0 until size) {
            dst[i] += src[i].toDouble()
        }
    }

    fun digamma(arr: FloatArray): FloatArray {
        val k = arr.size
        val ret = FloatArray(k)
        for (i in 0 until k) {
            ret[i] = Gamma.digamma(arr[i].toDouble()).toFloat()
        }
        return ret
    }

    fun digamma(arr: DoubleArray): DoubleArray {
        val k = arr.size
        val ret = DoubleArray(k)
        for (i in 0 until k) {
            ret[i] = Gamma.digamma(arr[i])
        }
        return ret
    }

}