package tanvd.bayou.implementation.core.ml

import org.tensorflow.*
import tanvd.bayou.implementation.utils.JsonUtil
import org.tensorflow.Tensor
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

    fun generateAsts(response: String): List<Map<String, Any>> {
        val results = ArrayList<Map<String, Any>>()
        for (i in 1..10) {
            val ast = generateAst(response).toMutableMap()
            ast["calls"] = calls_in_last_ast
            calls_in_last_ast.clear()
            results.add(ast)
        }
        return results
    }

    private fun generateAst(response: String) : Map<String, Any> {
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
        val ast = generateFromPsi(result.first())
        return ast
    }

    val calls_in_last_ast: MutableList<String> = ArrayList()

    private fun generateFromPsi(psi: Tensor, depth: Long = 0, in_nodes : List<String> = listOf("DSubTree"),
                                in_edges: List<Edges> = listOf(Edges.ChildEdge)): SortedMap<String, Any> {
        val ast: SortedMap<String, Any> = TreeMap()

        var nodes = in_nodes.toMutableList()
        var edges = in_edges.toMutableList()
        val node = in_nodes.last()
        when (node) {
            "DBranch" -> {
                ast["node"] = node
                val resultFirst = getUntilStop(psi, depth, nodes, edges, check_call = true)
                val ast_cond = resultFirst.ast_node
                nodes = resultFirst.nodes.toMutableList()
                edges = resultFirst.edges.toMutableList()
                val resultSecond = getUntilStop(psi, depth, nodes, edges)
                val ast_then = resultSecond.ast_node
                nodes = resultSecond.nodes.toMutableList()
                edges = resultSecond.edges.toMutableList()
                val resultThird = getUntilStop(psi, depth, nodes, edges)
                val ast_else = resultThird.ast_node
                nodes = resultThird.nodes.toMutableList()
                edges = resultThird.edges.toMutableList()
                ast["_cond"] = ast_cond
                ast["_then"] = ast_then
                ast["_else"] = ast_else
                return ast
            }
            "DExcept" -> {
                ast["node"] = node
                val resultFirst = getUntilStop(psi, depth, nodes, edges)
                val ast_try = resultFirst.ast_node
                nodes = resultFirst.nodes.toMutableList()
                edges = resultFirst.edges.toMutableList()
                val resultSecond = getUntilStop(psi, depth, nodes, edges)
                val ast_catch = resultSecond.ast_node
                nodes = resultSecond.nodes.toMutableList()
                edges = resultSecond.edges.toMutableList()
                ast["_try"] = ast_try
                ast["_catch"] = ast_catch
                return ast

            }
            "DLoop" -> {
                ast["node"] = node
                val resultFirst = getUntilStop(psi, depth, nodes, edges, check_call=true)
                val ast_cond = resultFirst.ast_node
                nodes = resultFirst.nodes.toMutableList()
                edges = resultFirst.edges.toMutableList()
                val resultSecond = getUntilStop(psi, depth, nodes, edges)
                val ast_body = resultSecond.ast_node
                nodes = resultSecond.nodes.toMutableList()
                edges = resultSecond.edges.toMutableList()
                ast["_cond"] = ast_cond
                ast["_body"] = ast_body
                return ast

            }
            "DSubTree" -> {
                ast["node"] = node
                val (ast_nodes, _, _) = getUntilStop(psi, depth, nodes, edges)
                ast["_nodes"] = ast_nodes
                return ast
            }
            else -> {
                ast["node"] = "DAPICall"
                ast["_call"] = node
                calls_in_last_ast.add(node)
                return ast
            }
        }


    }

    private data class CodeGenResult(val ast_node: List<Map<String, Any>>, val nodes:List<String>, val edges: List<Edges>)

//    ast = []
//    nodes, edges = in_nodes[:], in_edges[:]
//    num = 0
//    while True:
//    assert num < MAX_GEN_UNTIL_STOP # exception caught in main
//    dist = self.model.infer_ast(self.sess, psi, nodes, edges)
//    idx = np.random.choice(range(len(dist)), p=dist)
//    prediction = self.model.config.decoder.chars[idx]
//    nodes += [prediction]
//    if check_call:  # exception caught in main
//    assert prediction not in ['DBranch', 'DExcept', 'DLoop', 'DSubTree']
//    if prediction == 'STOP':
//    edges += [SIBLING_EDGE]
//    break
//    js = self.generate_ast(psi, depth + 1, nodes, edges + [CHILD_EDGE])
//    ast.append(js)
//    edges += [SIBLING_EDGE]
//    num += 1
//    return ast, nodes, edges
    private fun getUntilStop(psi: Tensor, depth: Long, in_nodes: List<String>, in_edges: List<Edges>,
                             check_call: Boolean = false) : CodeGenResult {
        val nodes = in_nodes.toMutableList()
        val edges = in_edges.toMutableList()
        val ast : ArrayList<Map<String, Any>> = ArrayList()
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

        }
        return CodeGenResult(ast, nodes, edges)

    }

//    def infer_ast(self, sess, psi, nodes, edges):
//    # use the given psi and get decoder's start state
//    state = sess.run(self.initial_state, {self.psi: psi})
//
//    # run the decoder for every time step
//    for node, edge in zip(nodes, edges):
//    assert edge == CHILD_EDGE or edge == SIBLING_EDGE, 'invalid edge: {}'.format(edge)
//    n = np.array([self.config.decoder.vocab[node]], dtype=np.int32)
//    e = np.array([edge == CHILD_EDGE], dtype=np.bool)
//
//    feed = {self.decoder.initial_state: state,
//        self.decoder.nodes[0].name: n,
//        self.decoder.edges[0].name: e}
//    [probs, state] = sess.run([self.probs, self.decoder.state], feed)
//
//    dist = probs[0]
//    return dist
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