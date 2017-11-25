package tanvd.bayou.implementation.core.ml.lda

import org.apache.commons.math3.distribution.GammaDistribution
import org.apache.commons.math3.special.Gamma
import tanvd.bayou.implementation.utils.ArrayUtils
import tanvd.bayou.implementation.utils.MathUtils
import tanvd.bayou.implementation.utils.Resource
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*
import javax.annotation.Nonnegative
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap


open class LdaHiveModel(modelFile: String,
                        val _alpha: Float,
                        val _eta: Float) {

    private val SHAPE = 100.0
    private val SCALE = 1.0 / SHAPE

    // ---------------------------------
    // HyperParameters

    //total number of documents
    private val _D = 1000000
    // number of topics
    protected val _K: Int

    private var _docRatio = 1f

    // positive value which downweights early iterations
    private val _tau0: Double = 1020.0

    // exponential decay rate (i.e., learning rate) which must be in (0.5, 1] to guarantee convergence
    private val _kappa: Double = 0.7
    private var _rhot: Double
    private var _updateCount = 0L


    // check convergence in the expectation (E) step
    private val _delta: Float = 0.00001f

    //-------------------------
    // parameters
    private var _phi: LinkedHashMap<String, ArrayList<Float>> = LinkedHashMap()
    private var _gamma: Array<FloatArray?>? = null
    private var _lambda: LinkedHashMap<String, FloatArray> = LinkedHashMap()

    // random number generator
    private val _gd: GammaDistribution = GammaDistribution(SHAPE, SCALE).apply {
        reseedRandomGenerator(1001)
    }

    // for mini-batch
    protected val _miniBatchDocs: ArrayList<Map<String, Float>> = ArrayList()
    protected var _miniBatchSize: Int = 1


    init {
        val serialized = Resource.getLines("artifacts/model/lda/$modelFile")

        val phi = deserialize(serialized)
        phi.forEach { list ->
            list.withIndex().forEach { (ind_word: Int, value_word) ->
                if (_phi[ind_word.toString()] == null) {
                    _phi.put(ind_word.toString(), ArrayList())
                }
                _phi[ind_word.toString()]!!.add(value_word.toFloat())

            }

        }
        _K = _phi["0"]!!.size

        this._rhot = Math.pow(_tau0 + _updateCount, -_kappa)
        this._docRatio = (_D.toDouble() / _miniBatchSize).toFloat()
    }

    fun deserialize(serialized: List<String>): Array<DoubleArray> {
        val totalList = ArrayList<ArrayList<Double>>()
        for (str in serialized) {
            val curList = ArrayList<Double>()
            curList += str.split(" ").filter { it.isNotEmpty() }.map { it.toDouble() }
            totalList += curList
        }
        return totalList.map { it.toTypedArray().toDoubleArray() }.toTypedArray()
    }

    protected fun initMiniBatch(miniBatch: Array<Array<String>>,
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
        initMiniBatch(miniBatch, _miniBatchDocs)

        this._miniBatchSize = _miniBatchDocs.size
    }

    private fun initParams(gammaWithRandom: Boolean) {
        val gamma = arrayOfNulls<FloatArray>(_miniBatchSize)

        for (d in 0 until _miniBatchSize) {
            if (gammaWithRandom) {
                gamma[d] = ArrayUtils.newRandomFloatArray(_K, _gd)
            } else {
                gamma[d] = ArrayUtils.newFloatArray(_K, 1f)
            }

            _miniBatchDocs[d].keys
                    .filterNot { _lambda.containsKey(it) }
                    .forEach {
                        // lambda for newly observed word
                        _lambda.put(it, ArrayUtils.newRandomFloatArray(_K, _gd))
                    }
        }
        this._gamma = gamma
    }

    fun getTopicDistribution(doc: Array<String>): FloatArray {
        if (doc.all { it.toDouble() == 0.0 }) {
            return FloatArray(_K)
        }
        preprocessMiniBatch(arrayOf(doc))

        initParams(true)

        mStep()

        initParams(false)

        eStep()

        // normalize topic distribution
        val topicDistr = FloatArray(_K)
        val gamma0 = _gamma!![0]
        val gammaSum = MathUtils.sum(gamma0)
        for (k in 0 until _K) {
            topicDistr[k] = (gamma0!![k] / gammaSum).toFloat()
        }
        return topicDistr
    }

    private fun computeElogBetaPerDoc(@Nonnegative d: Int,
                                      digamma_lambda: Map<String, FloatArray>,
                                      digamma_lambdaSum: DoubleArray): Map<String, FloatArray> {
        val doc = _miniBatchDocs[d]

        // Dirichlet expectation (2d) for lambda
        val eLogBeta_d = HashMap<String, FloatArray>(doc.size)
        for (label in doc.keys) {
            var eLogBeta_label: FloatArray? = eLogBeta_d[label]
            if (eLogBeta_label == null) {
                eLogBeta_label = FloatArray(_K)
                eLogBeta_d.put(label, eLogBeta_label)
            }
            val digamma_lambda_label = digamma_lambda[label]
            for (k in 0 until _K) {
                eLogBeta_label[k] = (digamma_lambda_label!![k] - digamma_lambdaSum[k]).toFloat()
            }
        }

        return eLogBeta_d
    }

    private fun updatePhiPerDoc(@Nonnegative d: Int,
                                eLogBeta_d: Map<String, FloatArray>) {
        // Dirichlet expectation (2d) for gamma
        val gamma_d = _gamma!![d]
        val digamma_gammaSum_d = Gamma.digamma(MathUtils.sum(gamma_d))
        val eLogTheta_d = DoubleArray(_K)
        for (k in 0 until _K) {
            eLogTheta_d[k] = Gamma.digamma(gamma_d!![k].toDouble()) - digamma_gammaSum_d
        }

        // updating phi w/ normalization
        val phi_d = _phi//.get(d)
        val doc = _miniBatchDocs[d]
        for (label in doc.keys) {
            val phi_label = phi_d[label]
            val eLogBeta_label = eLogBeta_d[label]

            var normalizer = 0.0
            for (k in 0 until _K) {
                val phiVal = Math.exp(eLogBeta_label!![k] + eLogTheta_d[k]).toFloat() + 1E-20f
                phi_label!![k] = phiVal
                normalizer += phiVal.toDouble()
            }

            for (k in 0 until _K) {
                phi_label!![k] /= normalizer.toFloat()
            }
        }
    }

    private fun updateGammaPerDoc(@Nonnegative d: Int) {
        val doc = _miniBatchDocs[d]
        val phi_d = _phi//.get(d)

        val gamma_d = _gamma!![d]
        for (k in 0 until _K) {
            gamma_d!![k] = _alpha
        }
        for ((key, value) in doc) {
            val phi_label = phi_d[key]
            for (k in 0 until _K) {
                gamma_d!![k] += phi_label!![k] * value
            }
        }
    }

    private fun checkGammaDiff(gammaPrev: FloatArray,
                               gammaNext: FloatArray): Boolean {
        val diff = (0 until _K).sumByDouble { Math.abs(gammaPrev[it] - gammaNext[it]).toDouble() }
        return diff / _K < _delta
    }

    private fun mStep() {
        // calculate lambdaTilde for vocabularies in the current mini-batch
        val lambdaTilde = HashMap<String, FloatArray>()
        for (d in 0 until _miniBatchSize) {
            val phi_d = _phi
            for (label in _miniBatchDocs[d].keys) {
                var lambdaTilde_label: FloatArray? = lambdaTilde[label]
                if (lambdaTilde_label == null) {
                    lambdaTilde_label = ArrayUtils.newFloatArray(_K, _eta)
                    lambdaTilde.put(label, lambdaTilde_label)
                }

                val phi_label = phi_d.get(label)
                for (k in 0 until _K) {
                    lambdaTilde_label[k] += _docRatio * phi_label!![k]
                }
            }
        }

        // update lambda for all vocabularies
        for ((label, lambda_label) in _lambda) {

            var lambdaTilde_label: FloatArray? = lambdaTilde[label]
            if (lambdaTilde_label == null) {
                lambdaTilde_label = ArrayUtils.newFloatArray(_K, _eta)
            }

            for (k in 0 until _K) {
                lambda_label[k] = ((1.0 - _rhot) * lambda_label[k] + _rhot * lambdaTilde_label[k]).toFloat()
            }
        }
    }

    private fun eStep() {
        // since lambda is invariant in the expectation step,
        // `digamma`s of lambda values for Elogbeta are pre-computed
        val lambdaSum = DoubleArray(_K)
        val digamma_lambda = HashMap<String, FloatArray>()
        for (e in _lambda.entries) {
            val label = e.key
            val lambda_label = e.value

            // for digamma(lambdaSum)
            MathUtils.add(lambda_label, lambdaSum, _K)

            digamma_lambda.put(label, MathUtils.digamma(lambda_label))
        }

        val digamma_lambdaSum = MathUtils.digamma(lambdaSum)
        // for each of mini-batch documents, update gamma until convergence
        var gamma_d: FloatArray
        var gammaPrev_d: FloatArray
        var eLogBeta_d: Map<String, FloatArray>
        for (d in 0 until _miniBatchSize) {
            gamma_d = _gamma!![d]!!
            eLogBeta_d = computeElogBetaPerDoc(d, digamma_lambda, digamma_lambdaSum)

            do {
                gammaPrev_d = gamma_d.clone() // deep copy the last gamma values

                updatePhiPerDoc(d, eLogBeta_d)
                updateGammaPerDoc(d)
            } while (!checkGammaDiff(gammaPrev_d, gamma_d))
        }
    }
}