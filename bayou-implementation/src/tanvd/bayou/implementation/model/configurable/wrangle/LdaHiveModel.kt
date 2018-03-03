package tanvd.bayou.implementation.model.configurable.wrangle

import org.apache.commons.math3.distribution.GammaDistribution
import org.apache.commons.math3.special.Gamma
import tanvd.bayou.implementation.utils.ArrayUtils
import tanvd.bayou.implementation.utils.MathUtils
import tanvd.bayou.implementation.utils.Resource
import java.io.File
import java.util.*
import javax.annotation.Nonnegative
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

/**
 * Implementation of Online Bayes LDA
 * Proposed by Apache Hive project
 * Reimplementation of sklearn LDA
 */
open class LdaHiveModel(modelFile: File, private val alpha: Float, private val eta: Float) {
    private val shape = 100.0
    private val scale = 1.0 / shape

    //total number of documents
    private val d = 1000000
    // number of topics
    private val k: Int

    private var docRatio = 1f

    // positive value which downweights early iterations
    private val tau0: Double = 1020.0

    // exponential decay rate (i.e., learning rate) which must be in (0.5, 1] to guarantee convergence
    private val kappa: Double = 0.7
    private var rhot: Double
    private var updateCount = 0L


    // check convergence in the expectation (E) step
    private val delta: Float = 0.00001f

    // parameters
    private var phi: LinkedHashMap<String, ArrayList<Float>> = LinkedHashMap()
    private var gamma: Array<FloatArray?>? = null
    private var lambda: LinkedHashMap<String, FloatArray> = LinkedHashMap()

    // random number generator
    private val gammaDistribution: GammaDistribution = GammaDistribution(shape, scale).apply {
        reseedRandomGenerator(1001)
    }

    // for mini-batch
    private val miniBatchDocs: ArrayList<Map<String, Float>> = ArrayList()
    private var miniBatchSize: Int = 1


    init {
        val serialized = modelFile.readLines()

        val phi = deserialize(serialized)
        phi.forEach { list ->
            list.withIndex().forEach { (ind_word: Int, value_word) ->
                if (this.phi[ind_word.toString()] == null) {
                    this.phi.put(ind_word.toString(), ArrayList())
                }
                this.phi[ind_word.toString()]!!.add(value_word.toFloat())

            }

        }
        k = this.phi["0"]!!.size

        this.rhot = Math.pow(tau0 + updateCount, -kappa)
        this.docRatio = (d.toDouble() / miniBatchSize).toFloat()
    }

    private fun deserialize(serialized: List<String>): Array<DoubleArray> {
        val totalList = ArrayList<ArrayList<Double>>()
        for (str in serialized) {
            val curList = ArrayList<Double>()
            curList += str.split(" ").filter { it.isNotEmpty() }.map { it.toDouble() }
            totalList += curList
        }
        return totalList.map { it.toTypedArray().toDoubleArray() }.toTypedArray()
    }

    private fun initMiniBatch(miniBatch: Array<Array<String>>,
                              docs: MutableList<Map<String, Float>>) {
        docs.clear()
        // parse document
        for (e in miniBatch) {
            val doc = HashMap<String, Float>()
            // parse features
            for ((ind, value) in e.withIndex()) {
                doc.put(ind.toString(), value.toFloat())
            }
            docs.add(doc)
        }
    }

    private fun preprocessMiniBatch(miniBatch: Array<Array<String>>) {
        initMiniBatch(miniBatch, miniBatchDocs)

        this.miniBatchSize = miniBatchDocs.size
    }

    private fun initParams(gammaWithRandom: Boolean) {
        val gamma = arrayOfNulls<FloatArray>(miniBatchSize)

        for (d in 0 until miniBatchSize) {
            if (gammaWithRandom) {
                gamma[d] = ArrayUtils.newRandomFloatArray(k, gammaDistribution)
            } else {
                gamma[d] = ArrayUtils.newFloatArray(k, 1f)
            }

            miniBatchDocs[d].keys
                    .filterNot { lambda.containsKey(it) }
                    .forEach {
                        // lambda for newly observed word
                        lambda.put(it, ArrayUtils.newRandomFloatArray(k, gammaDistribution))
                    }
        }
        this.gamma = gamma
    }

    fun getTopicDistribution(doc: Array<String>): FloatArray {
        if (doc.all { it.toDouble() == 0.0 }) {
            return FloatArray(k)
        }
        preprocessMiniBatch(arrayOf(doc))

        initParams(true)

        mStep()

        initParams(false)

        eStep()

        // normalize topic distribution
        val topicDistr = FloatArray(k)
        val gamma0 = gamma!![0]
        val gammaSum = MathUtils.sum(gamma0)
        for (k in 0 until k) {
            topicDistr[k] = (gamma0!![k] / gammaSum).toFloat()
        }
        return topicDistr
    }

    private fun computeElogBetaPerDoc(@Nonnegative d: Int,
                                      digamma_lambda: Map<String, FloatArray>,
                                      digamma_lambdaSum: DoubleArray): Map<String, FloatArray> {
        val doc = miniBatchDocs[d]

        // Dirichlet expectation (2d) for lambda
        val eLogBeta_d = HashMap<String, FloatArray>(doc.size)
        for (label in doc.keys) {
            var eLogBeta_label: FloatArray? = eLogBeta_d[label]
            if (eLogBeta_label == null) {
                eLogBeta_label = FloatArray(k)
                eLogBeta_d[label] = eLogBeta_label
            }
            val digamma_lambda_label = digamma_lambda[label]
            for (k in 0 until k) {
                eLogBeta_label[k] = (digamma_lambda_label!![k] - digamma_lambdaSum[k]).toFloat()
            }
        }

        return eLogBeta_d
    }

    private fun updatePhiPerDoc(@Nonnegative d: Int,
                                eLogBeta_d: Map<String, FloatArray>) {
        // Dirichlet expectation (2d) for gamma
        val gamma_d = gamma!![d]
        val digamma_gammaSum_d = Gamma.digamma(MathUtils.sum(gamma_d))
        val eLogTheta_d = DoubleArray(k)
        for (k in 0 until k) {
            eLogTheta_d[k] = Gamma.digamma(gamma_d!![k].toDouble()) - digamma_gammaSum_d
        }

        // updating phi w/ normalization
        val phi_d = phi//.get(d)
        val doc = miniBatchDocs[d]
        for (label in doc.keys) {
            val phi_label = phi_d[label]
            val eLogBeta_label = eLogBeta_d[label]

            var normalizer = 0.0
            for (k in 0 until k) {
                val phiVal = Math.exp(eLogBeta_label!![k] + eLogTheta_d[k]).toFloat() + 1E-20f
                phi_label!![k] = phiVal
                normalizer += phiVal.toDouble()
            }

            for (k in 0 until k) {
                phi_label!![k] /= normalizer.toFloat()
            }
        }
    }

    private fun updateGammaPerDoc(@Nonnegative d: Int) {
        val doc = miniBatchDocs[d]
        val phi_d = phi//.get(d)

        val gamma_d = gamma!![d]
        for (k in 0 until k) {
            gamma_d!![k] = alpha
        }
        for ((key, value) in doc) {
            val phi_label = phi_d[key]
            for (k in 0 until k) {
                gamma_d!![k] += phi_label!![k] * value
            }
        }
    }

    private fun checkGammaDiff(gammaPrev: FloatArray,
                               gammaNext: FloatArray): Boolean {
        val diff = (0 until k).sumByDouble { Math.abs(gammaPrev[it] - gammaNext[it]).toDouble() }
        return diff / k < delta
    }

    private fun mStep() {
        // calculate lambdaTilde for vocabularies in the current mini-batch
        val lambdaTilde = HashMap<String, FloatArray>()
        for (d in 0 until miniBatchSize) {
            val phi_d = phi
            for (label in miniBatchDocs[d].keys) {
                var lambdaTilde_label: FloatArray? = lambdaTilde[label]
                if (lambdaTilde_label == null) {
                    lambdaTilde_label = ArrayUtils.newFloatArray(k, eta)
                    lambdaTilde.put(label, lambdaTilde_label)
                }

                val phi_label = phi_d[label]
                for (k in 0 until k) {
                    lambdaTilde_label[k] += docRatio * phi_label!![k]
                }
            }
        }

        // update lambda for all vocabularies
        for ((label, lambda_label) in lambda) {

            var lambdaTilde_label: FloatArray? = lambdaTilde[label]
            if (lambdaTilde_label == null) {
                lambdaTilde_label = ArrayUtils.newFloatArray(k, eta)
            }

            for (k in 0 until k) {
                lambda_label[k] = ((1.0 - rhot) * lambda_label[k] + rhot * lambdaTilde_label[k]).toFloat()
            }
        }
    }

    private fun eStep() {
        // since lambda is invariant in the expectation step,
        // `digamma`s of lambda values for Elogbeta are pre-computed
        val lambdaSum = DoubleArray(k)
        val digamma_lambda = HashMap<String, FloatArray>()
        for (e in lambda.entries) {
            val label = e.key
            val lambda_label = e.value

            // for digamma(lambdaSum)
            MathUtils.add(lambda_label, lambdaSum, k)

            digamma_lambda.put(label, MathUtils.digamma(lambda_label))
        }

        val digamma_lambdaSum = MathUtils.digamma(lambdaSum)
        // for each of mini-batch documents, update gamma until convergence
        var gamma_d: FloatArray
        var gammaPrev_d: FloatArray
        var eLogBeta_d: Map<String, FloatArray>
        for (d in 0 until miniBatchSize) {
            gamma_d = gamma!![d]!!
            eLogBeta_d = computeElogBetaPerDoc(d, digamma_lambda, digamma_lambdaSum)

            do {
                gammaPrev_d = gamma_d.clone() // deep copy the last gamma values

                updatePhiPerDoc(d, eLogBeta_d)
                updateGammaPerDoc(d)
            } while (!checkGammaDiff(gammaPrev_d, gamma_d))
        }
    }
}