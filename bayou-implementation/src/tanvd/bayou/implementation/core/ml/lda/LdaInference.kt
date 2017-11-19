//package tanvd.bayou.implementation.core.ml.lda
//
//import tanvd.bayou.implementation.utils.JsonUtil
//import java.io.BufferedReader
//import java.io.FileInputStream
//import java.io.FileReader
//import java.io.InputStreamReader
//
//open class LdaInference(val modelFile: String) {
//
//    val prefix = "/home/tanvd/Diploma/bayou-integration/bayou-implementation/resources/artifacts/model/lda/"
//    val alpha = 0.1
//    val phi: Array<DoubleArray>
//
//    init {
//        val serialized = BufferedReader(InputStreamReader(FileInputStream(prefix + modelFile))).readLine()
//        phi = JsonUtil.readValue(serialized, Array<DoubleArray>::class)
//    }
//
//
//    private val ITERATIONS = 1000
//
//
//    /**
//     * Inference a new document by a pre-trained phi matrix
//     *
//     * @param phi pre-trained phi matrix
//     * @param doc document
//     * @return a p array
//     */
//    fun inference(doc: IntArray): DoubleArray {
//        val K = phi.size
//        val V = phi[0].size
//        // init
//
//        // initialise count variables.
//        val nw = Array(V) { IntArray(K) }
//        val nd = IntArray(K)
//        val nwsum = IntArray(K)
//        var ndsum = 0
//
//        // The z_i are are initialised to values in [1,K] to determine the
//        // initial state of the Markov chain.
//
//        val N = doc.size
//        val z = IntArray(N)   // z_i := 1
//        for (n in 0 until N) {
//            val topic = (Math.random() * K).toInt()
//            z[n] = topic
//            // number of instances of word i assigned to topic j
//            nw[doc[n]][topic]++
//            // number of words in document i assigned to topic j.
//            nd[topic]++
//            // total number of words assigned to topic j.
//            nwsum[topic]++
//        }
//        // total number of words in document i
//        ndsum = N
//        for (i in 0 until ITERATIONS) {
//            for (n in z.indices) {
//
//                // (z_i = z[m][n])
//                // sample from p(z_i|z_-i, w)
//                // remove z_i from the count variables
//                var topic = z[n]
//                nw[doc[n]][topic]--
//                nd[topic]--
//                nwsum[topic]--
//                ndsum--
//
//                // do multinomial sampling via cumulative method:
//                val p = DoubleArray(K)
//                for (k in 0 until K) {
//                    p[k] = phi[k][doc[n]] * (nd[k] + alpha) / (ndsum + K * alpha)
//                }
//                // cumulate multinomial parameters
//                for (k in 1 until p.size) {
//                    p[k] += p[k - 1]
//                }
//                // scaled sample because of unnormalised p[]
//                val u = Math.random() * p[K - 1]
//                topic = 0
//                while (topic < p.size) {
//                    if (u < p[topic])
//                        break
//                    topic++
//                }
//                if (topic == K) {
//                    throw RuntimeException("the param K or topic is set too small")
//                }
//                // add newly estimated z_i to count variables
//                nw[doc[n]][topic]++
//                nd[topic]++
//                nwsum[topic]++
//                ndsum++
//                z[n] = topic
//            }
//        }
//
//        val theta = DoubleArray(K)
//
//        for (k in 0 until K) {
//            theta[k] = (nd[k] + alpha) / (ndsum + K * alpha)
//        }
//        return theta
//    }
//}