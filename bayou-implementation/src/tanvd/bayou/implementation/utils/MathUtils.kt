package tanvd.bayou.implementation.utils

fun Double.equals(value: Double, epsilon: Double) : Boolean {
    return Math.abs(this - value) < epsilon
}

fun Float.equals(value: Float, epsilon: Float) : Boolean {
    return Math.abs(this - value) < epsilon
}