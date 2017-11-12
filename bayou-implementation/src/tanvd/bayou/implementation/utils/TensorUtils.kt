package tanvd.bayou.implementation.utils

import org.tensorflow.Tensor

fun getFloatTensor1D(tensor: Tensor): FloatArray {
    val array = FloatArray(tensor.shape()[0].toInt())
    tensor.copyTo(array)
    return array
}

fun getFloatTensor2D(tensor: Tensor): Array<FloatArray> {
    val array = Array(tensor.shape()[0].toInt(), {kotlin.FloatArray(tensor.shape()[1].toInt())})
    tensor.copyTo(array)
    return array
}