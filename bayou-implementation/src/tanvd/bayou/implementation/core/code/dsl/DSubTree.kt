/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package tanvd.bayou.implementation.core.code.dsl

import org.eclipse.jdt.core.dom.Block
import org.eclipse.jdt.core.dom.Expression
import org.eclipse.jdt.core.dom.Statement
import tanvd.bayou.implementation.core.code.synthesizer.implementation.Environment
import tanvd.bayou.implementation.core.code.synthesizer.implementation.SynthesisException
import java.util.*

class DSubTree : DASTNode {

    internal var node = "DSubTree"
    internal var _nodes: MutableList<DASTNode>

    val isValid: Boolean
        get() = !_nodes.isEmpty()

    val nodesAsCalls: List<DAPICall>
        get() {
            val calls = ArrayList<DAPICall>()
            for (node in _nodes) {
                assert(node is DAPICall) { "invalid branch condition" }
                calls.add(node as DAPICall)
            }
            return calls
        }

    val nodes: List<DASTNode>
        get() = _nodes

    constructor() {
        _nodes = ArrayList()
        this.node = "DSubTree"
    }

    constructor(_nodes: MutableList<DASTNode>) {
        this._nodes = _nodes
        this.node = "DSubTree"
    }

    fun addNode(node: DASTNode) {
        _nodes.add(node)
    }

    fun addNodes(otherNodes: List<DASTNode>) {
        _nodes.addAll(otherNodes)
    }

    @Throws(DASTNode.TooManySequencesException::class, DASTNode.TooLongSequenceException::class)
    override fun updateSequences(soFar: MutableList<Sequence>, max: Int, max_length: Int) {
        if (soFar.size >= max)
            throw DASTNode.TooManySequencesException()
        for (node in _nodes)
            node.updateSequences(soFar, max, max_length)
    }

    override fun numStatements(): Int {
        var num = 0
        for (node in _nodes)
            num += node.numStatements()
        return num
    }

    override fun numLoops(): Int {
        var num = 0
        for (node in _nodes)
            num += node.numLoops()
        return num
    }

    override fun numBranches(): Int {
        var num = 0
        for (node in _nodes)
            num += node.numBranches()
        return num
    }

    override fun numExcepts(): Int {
        var num = 0
        for (node in _nodes)
            num += node.numExcepts()
        return num
    }

    override fun bagOfAPICalls(): Set<DAPICall> {
        val bag = HashSet<DAPICall>()
        for (node in _nodes)
            bag.addAll(node.bagOfAPICalls())
        return bag
    }

    override fun exceptionsThrown(): Set<Class<*>> {
        val ex = HashSet<Class<*>>()
        for (n in _nodes)
            ex.addAll(n.exceptionsThrown())
        return ex
    }

    override fun exceptionsThrown(eliminatedVars: Set<String>): Set<Class<*>> {
        val ex = HashSet<Class<*>>()
        for (n in _nodes)
            ex.addAll(n.exceptionsThrown(eliminatedVars))
        return ex
    }

    fun cleanupCatchClauses(eliminatedVars: Set<String>) {
        for (n in _nodes) {
            (n as? DExcept)?.cleanupCatchClauses(eliminatedVars)
        }
    }

    override fun equals(o: Any?): Boolean {
        if (o == null || o !is DSubTree)
            return false
        val tree = o as DSubTree?
        return _nodes == tree!!.nodes
    }

    override fun hashCode(): Int {
        return _nodes.hashCode()
    }

    override fun toString(): String {
        return _nodes.joinToString("\n") { it.toString() }
    }


    @Throws(SynthesisException::class)
    override fun synthesize(env: Environment): Block {
        val ast = env.ast()
        val block = ast.newBlock()

        for (dNode in _nodes) {
            val aNode = dNode.synthesize(env)
            if (aNode is Statement)
                block.statements().add(aNode)
            else
                block.statements().add(ast.newExpressionStatement(aNode as Expression))
        }

        return block
    }
}
