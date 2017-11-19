package tanvd.bayou.implementation.utils

import java.util.*


internal class RandomSelector(val items: Map<Int, Float>) {
    var rand = Random()
    var totalSum: Float = items.values.sum()

    val random: Int
        get() {
            val totalBound = rand.nextFloat()
            var sum = 0f
            for ((key, value) in items) {
                sum += value
                if (sum > totalBound) {
                    return key
                }
            }
            return items.keys.last()
        }
}