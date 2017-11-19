//package tanvd.bayou.implementation.core.ml.lda
//
//import tanvd.bayou.implementation.utils.JsonUtil
//import java.io.BufferedReader
//import java.io.FileInputStream
//import java.io.InputStreamReader
//import java.util.*
//import kotlin.collections.ArrayList
//
//
//open class LdaModel(val modelFile: String, val alpha: Double, val beta: Double) {
//
//    val prefix = "/home/tanvd/Diploma/bayou-integration/bayou-implementation/resources/artifacts/model/lda/"
//    val phi: Array<DoubleArray>
//
//    internal var V: Int//vocabulary size
//    internal var K: Int //topic number
//    internal var z: ArrayList<Int> = ArrayList()//topic label array
//    internal var nmk: ArrayList<Int>? = null//given document m, count times of topic k. M*K
//    internal var nkt: ArrayList<ArrayList<Int>>? = null//given topic k, count times of term t. K*V
//    internal var nmkSum: Int = 0//Sum for each row in nmk
//    internal var nktSum: ArrayList<Int>? = null//Sum for each row in nkt
//    internal var iterations: Int = 1000//Times of iterations
//    val theta: ArrayList<Double>
//
//
//    init {
//        val serialized = BufferedReader(InputStreamReader(FileInputStream(prefix + modelFile))).readLine()
//        phi = JsonUtil.readValue(serialized, Array<DoubleArray>::class)
//        K = phi.size
//        V = phi[0].size
//        theta = ArrayList<Double>(Collections.nCopies(K, 0.0))
//    }
//
//    fun inference(document: List<Int>): List<Double> {
//        //initialize topic lable z for each word
//        nmk = ArrayList(Collections.nCopies(K, 0));
//        nkt = ArrayList(Collections.nCopies(K, ArrayList(Collections.nCopies(V, 0))))
//        nktSum = ArrayList(Collections.nCopies(K, 0));
//        val N = document.size
//        z = ArrayList(Collections.nCopies(N, 0))
//        for (n in 0 until N) {
//            val initTopic = (Math.random() * K).toInt()// From 0 to K - 1
//            z[n] = initTopic
//            //number of words in doc m assigned to topic initTopic add 1
//            nmk!![initTopic]++
//            //number of terms doc[m][n] assigned to topic initTopic add 1
//            nkt!![initTopic][document[n]]++
//            // total number of words assigned to topic initTopic add 1
//            nktSum!![initTopic]++
//        }
//        // total number of words in document m is N
//        nmkSum = N
//
//        // TODO Auto-generated method stub
//        for (i in 0 until iterations) {
//            println("Iteration " + i)
//            updateEstimatedParameters()
//
//            //Use Gibbs Sampling to update z[][]
//            for (n in 0 until N) {
//                // Sample from p(z_i|z_-i, w)
//                val newTopic = sampleTopicZ(n, document)
//                z[n] = newTopic
//            }
//
//
//        }
//        return theta
//    }
//
//    private fun updateEstimatedParameters() {
//        // TODO Auto-generated method stub
//        for (k in 0 until K) {
//            theta[k] = (nmk!![k] + alpha) / (nmkSum + K * alpha).toDouble()
//        }
//    }
//
//    private fun sampleTopicZ(n: Int, doc: List<Int>): Int {
//        // TODO Auto-generated method stub
//        // Sample from p(z_i|z_-i, w) using Gibbs upde rule
//
//        //Remove topic label for w_{m,n}
//        val oldTopic = z[n]
//        nmk!![oldTopic]--
//        nkt!![oldTopic][doc[n]]--
//        nmkSum--
//        nktSum!![oldTopic]--
//
//        //Compute p(z_i = k|z_-i, w)
//        val p = DoubleArray(K)
//        for (k in 0 until K) {
//            p[k] = (nkt!![k][doc[n]] + beta) / (nktSum!![k] + V * beta) * (nmk!![k] + alpha) / (nmkSum + K * alpha)
//        }
//
//        //Sample a new topic label for w_{m, n} like roulette
//        //Compute cumulated probability for p
//        for (k in 1 until K) {
//            p[k] += p[k - 1]
//        }
//        val u = Math.random() * p[K - 1] //p[] is unnormalised
//        var newTopic: Int
//        newTopic = 0
//        while (newTopic < K) {
//            if (u < p[newTopic]) {
//                break
//            }
//            newTopic++
//        }
//
//        //Add new topic label for w_{m, n}
//        nmk!![newTopic]++
//        nkt!![newTopic][doc[n]]++
//        nmkSum++
//        nktSum!![newTopic]++
//        return newTopic
//    }
//}