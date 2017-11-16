package tanvd.bayou.implementation.core.ml

import org.tensorflow.*
import tanvd.bayou.implementation.utils.JsonUtil
import org.tensorflow.Tensor
import tanvd.bayou.implementation.core.code.dsl.*
import tanvd.bayou.implementation.core.code.synthesizer.implementation.SynthesisException
import tanvd.bayou.implementation.utils.RandomSelector
import tanvd.bayou.implementation.utils.getFloatTensor2D
import java.io.File
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.*
import kotlin.collections.ArrayList


object AstGenerator {

    val prefixPath = "/home/tanvd/Diploma/bayou-integration"

    val graph : Graph

    val session : Session

    val config = JsonUtil.readValue(String(File("$prefixPath/bayou-implementation/resources/artifacts/model/config.json").readBytes()), DecoderConfig::class)

    init {
        val path = "$prefixPath/full-model"
        val saved = SavedModelBundle.load(path, "train")
        graph = saved.graph()
        session = saved.session()
    }

//    # Include in here any conditions that dictate whether an AST should be returned or not
//    def okay(js, ast):
//    calls = ast['calls']
//    apicalls = list(set(chain.from_iterable([bayou.core.evidence.APICalls.from_call(call) for call in calls])))
//    types = list(set(chain.from_iterable([bayou.core.evidence.Types.from_call(call) for call in calls])))
//    context = list(set(chain.from_iterable([bayou.core.evidence.Context.from_call(call) for call in calls])))
//
//    ev_okay = all([c in apicalls for c in js['apicalls']]) and all([t in types for t in js['types']]) \
//    and all([c in context for c in js['context']])
//    return ev_okay

    fun generateAsts(response: String, evidence: Evidences): List<DSubTree> {
        val results = ArrayList<DSubTree>()
        for (i in 1..10) {
            try {
                val ast = generateAst(response)
                if (verifyAst(evidence)) {
                    results.add(ast)
                }
            } catch (e : Exception) {}
        }
        return results
    }

    private val calls_in_last_ast: MutableList<String> = ArrayList()

    fun verifyAst(evidence: Evidences) : Boolean {
        val callsTypes = calls_in_last_ast.mapNotNull { Evidences.typeFromCall(it) }
        val callsApis = calls_in_last_ast.mapNotNull { Evidences.apicallFromCall(it) }
        val typesContains = evidence.types.all {
            callsTypes.contains(it)
        }
        val apisContains = evidence.apicalls.all {
            callsApis.contains(it)
        }
        return typesContains && apisContains
    }

    private fun generateAst(response: String) : DSubTree {
        val splittedResponse  = response.split("\n")
        val runner = session.runner()
        for (i in 0..(splittedResponse.size/3 - 1)) {
            val list = JsonUtil.readValue(splittedResponse[i * 3 + 2].drop(1).dropLast(1), Array<Float>::class)
            val ty = Tensor.create(arrayOf(1, list.size.toLong()).toLongArray(), FloatBuffer.wrap(list.toFloatArray()))
            runner.feed(splittedResponse[i * 3].dropLast(2), ty)
        }
        runner.fetch("model_psi")
        val array = Array(1, {kotlin.FloatArray(32)})
        val result = runner.run()
        val psi = result.first()
        psi.copyTo(array)
        val ast = generateFromPsi(result.first()) as DSubTree
        return ast
    }


    private fun generateFromPsi(psi: Tensor, depth: Long = 0, in_nodes : List<String> = listOf("DSubTree"),
                                in_edges: List<Edges> = listOf(Edges.ChildEdge)): DASTNode {
        val ast: SortedMap<String, Any> = TreeMap()

        var nodes = in_nodes.toMutableList()
        var edges = in_edges.toMutableList()
        val node = in_nodes.last()
        when (node) {
            "DBranch" -> {
                val resultFirst = genUntilStoop(psi, depth, nodes, edges, check_call = true)
                val ast_cond = resultFirst.ast_nodes
                nodes = resultFirst.nodes.toMutableList()
                edges = resultFirst.edges.toMutableList()
                val resultSecond = genUntilStoop(psi, depth, nodes, edges)
                val ast_then = resultSecond.ast_nodes
                nodes = resultSecond.nodes.toMutableList()
                edges = resultSecond.edges.toMutableList()
                val resultThird = genUntilStoop(psi, depth, nodes, edges)
                val ast_else = resultThird.ast_nodes
                nodes = resultThird.nodes.toMutableList()
                edges = resultThird.edges.toMutableList()
                return DBranch(ast_cond.map { it as DAPICall }, ast_then, ast_else)
            }
            "DExcept" -> {
                ast["node"] = node
                val resultFirst = genUntilStoop(psi, depth, nodes, edges)
                val ast_try = resultFirst.ast_nodes
                nodes = resultFirst.nodes.toMutableList()
                edges = resultFirst.edges.toMutableList()
                val resultSecond = genUntilStoop(psi, depth, nodes, edges)
                val ast_catch = resultSecond.ast_nodes
                nodes = resultSecond.nodes.toMutableList()
                edges = resultSecond.edges.toMutableList()
                ast["_try"] = ast_try
                ast["_catch"] = ast_catch
                return DExcept(ast_try, ast_catch)

            }
            "DLoop" -> {
                val resultFirst = genUntilStoop(psi, depth, nodes, edges, check_call=true)
                val ast_cond = resultFirst.ast_nodes
                nodes = resultFirst.nodes.toMutableList()
                edges = resultFirst.edges.toMutableList()
                val resultSecond = genUntilStoop(psi, depth, nodes, edges)
                val ast_body = resultSecond.ast_nodes
                nodes = resultSecond.nodes.toMutableList()
                edges = resultSecond.edges.toMutableList()
                return DLoop(ast_cond.map { it as DAPICall }, ast_body)

            }
            "DSubTree" -> {
                val (ast_nodes, _, _) = genUntilStoop(psi, depth, nodes, edges)
                return DSubTree(ast_nodes.toMutableList())
            }
            else -> {
                calls_in_last_ast.add(node)
                return DAPICall().apply {
                    _call = node
                }
            }
        }


    }

    private val maxGenUntilStop = 20;

    private data class CodeGenResult(val ast_nodes: List<DASTNode>, val nodes:List<String>, val edges: List<Edges>)

    private fun genUntilStoop(psi: Tensor, depth: Long, in_nodes: List<String>, in_edges: List<Edges>,
                              check_call: Boolean = false) : CodeGenResult {
        val nodes = in_nodes.toMutableList()
        val edges = in_edges.toMutableList()
        val ast : ArrayList<DASTNode> = ArrayList()
        var num = 0
        while (true) {
            val dist = infer_ast_model(psi, nodes, edges)
            val idx = RandomSelector((0 until dist.size).zip(dist.toList()).toMap()).random
            val prediction = config.chars[idx]
            nodes += arrayListOf(prediction)
            if (prediction == "STOP") {
                edges += Edges.SiblingEdge
                break
            }
            val js = generateFromPsi(psi, depth + 1, nodes, edges + arrayListOf(Edges.ChildEdge))
            ast.add(js)
            edges += Edges.SiblingEdge
            num += 1
            if (num > maxGenUntilStop) {
                throw SynthesisException(-1)
            }

        }
        return CodeGenResult(ast, nodes, edges)

    }

    private fun infer_ast_model(psi: Tensor, nodes: List<String>, edges: List<Edges>): FloatArray {
        val decoderInitialState = "initial_state_decoder"
        val decoderState = "decoder/rnn/decoder_state"
        val modelPsi = "model_psi"
        val modelProbs = "model_probs"
        var state = session.runner().feed(modelPsi, psi).fetch(decoderInitialState).run().first()
        var tensors = emptyList<Tensor>()
        for ((node, edge) in nodes.zip(edges)) {
            val n = config.vocab[node]!!
            val e = if (Edges.ChildEdge == edge) byteArrayOf(1) else byteArrayOf(0)
            val runner = session.runner()
            runner.feed(decoderInitialState, state)
            runner.feed("node0",Tensor.create(longArrayOf(1), IntBuffer.wrap(intArrayOf(n.toInt()))))
            runner.feed("edge0",Tensor.create(DataType.BOOL, longArrayOf(1), ByteBuffer.wrap(e)))
            runner.fetch(modelProbs)
            runner.fetch(decoderState)
            tensors = runner.run()
            state = tensors.last()
        }

        val tensor = tensors.first()
        val array = getFloatTensor2D(tensor)
        return array[0]
    }


}