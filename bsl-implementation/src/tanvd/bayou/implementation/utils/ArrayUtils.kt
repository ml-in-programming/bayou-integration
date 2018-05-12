package tanvd.bayou.implementation.utils

import org.apache.commons.math3.distribution.GammaDistribution
import java.util.*
import javax.annotation.Nonnegative

object ArrayUtils {

    operator fun set(src: DoubleArray, index: Int, value: Double): DoubleArray {
        var src = src
        if (index >= src.size) {
            src = Arrays.copyOf(src, src.size * 2)
        }
        src[index] = value
        return src
    }

    operator fun <T> set(src: Array<T>, index: Int, value: T): Array<T> {
        var src = src
        if (index >= src.size) {
            src = Arrays.copyOf(src, src.size * 2)
        }
        src[index] = value
        return src
    }


    fun copy(src: IntArray, dest: IntArray) {
        if (src.size != dest.size) {
            throw IllegalArgumentException("src.legnth '" + src.size + "' != dest.length '"
                    + dest.size + "'")
        }
        System.arraycopy(src, 0, dest, 0, src.size)
    }


    fun equals(array: FloatArray, value: Float): Boolean {
        var i = 0
        val size = array.size
        while (i < size) {
            if (array[i] != value) {
                return false
            }
            i++
        }
        return true
    }


    fun equals(array: FloatArray, expected: Float,
               delta: Float): Boolean {
        var i = 0
        val size = array.size
        while (i < size) {
            val actual = array[i]
            if (Math.abs(expected - actual) > delta) {
                return false
            }
            i++
        }
        return true
    }

    fun copy(src: FloatArray, dst: DoubleArray) {
        val size = Math.min(src.size, dst.size)
        for (i in 0 until size) {
            dst[i] = src[i].toDouble()
        }
    }


    fun newFloatArray(@Nonnegative size: Int, filledValue: Float): FloatArray {
        val a = FloatArray(size)
        Arrays.fill(a, filledValue)
        return a
    }

    fun newRandomFloatArray(@Nonnegative size: Int,
                            gd: GammaDistribution): FloatArray {
        val ret = FloatArray(size)
        for (i in 0 until size) {
            ret[i] = gd.sample().toFloat()
        }
        return ret
    }
}
