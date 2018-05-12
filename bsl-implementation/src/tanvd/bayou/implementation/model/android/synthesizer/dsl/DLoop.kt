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
package tanvd.bayou.implementation.model.android.synthesizer.dsl


import org.eclipse.jdt.core.dom.*
import tanvd.bayou.implementation.model.android.synthesizer.Environment
import tanvd.bayou.implementation.model.android.synthesizer.SynthesisException
import tanvd.bayou.implementation.model.android.synthesizer.Type
import java.util.*

class DLoop : DASTNode {

    internal var node = "DLoop"
    internal var _cond: List<DAPICall>
    internal var _body: List<DASTNode>

    constructor() {
        this._cond = ArrayList()
        this._body = ArrayList()
        this.node = "DLoop"
    }

    constructor(cond: List<DAPICall>, _body: List<DASTNode>) {
        this._cond = cond
        this._body = _body
        this.node = "DLoop"
    }

    @Throws(DASTNode.TooManySequencesException::class, DASTNode.TooLongSequenceException::class)
    override fun updateSequences(soFar: MutableList<Sequence>, max: Int, max_length: Int) {
        if (soFar.size >= max)
            throw DASTNode.TooManySequencesException()
        for (call in _cond)
            call.updateSequences(soFar, max, max_length)

        //TODO-tanvd Check
        val num_unrolls = 1
        for (i in 0 until num_unrolls) {
            for (node in _body)
                node.updateSequences(soFar, max, max_length)
            for (call in _cond)
                call.updateSequences(soFar, max, max_length)
        }
    }

    override fun numStatements(): Int {
        var num = _cond.size
        for (b in _body)
            num += b.numStatements()
        return num
    }

    override fun numLoops(): Int {
        var num = 1 // this loop
        for (b in _body)
            num += b.numLoops()
        return num
    }

    override fun numBranches(): Int {
        var num = 0
        for (b in _body)
            num += b.numBranches()
        return num
    }

    override fun numExcepts(): Int {
        var num = 0
        for (b in _body)
            num += b.numExcepts()
        return num
    }

    override fun bagOfAPICalls(): Set<DAPICall> {
        val bag = HashSet<DAPICall>()
        bag.addAll(_cond)
        for (b in _body)
            bag.addAll(b.bagOfAPICalls())
        return bag
    }

    override fun exceptionsThrown(): Set<Class<*>> {
        val ex = HashSet<Class<*>>()
        for (c in _cond)
            ex.addAll(c.exceptionsThrown())
        for (b in _body)
            ex.addAll(b.exceptionsThrown())
        return ex
    }

    override fun exceptionsThrown(eliminatedVars: Set<String>): Set<Class<*>> {
        return this.exceptionsThrown()
    }

    override fun equals(o: Any?): Boolean {
        if (o == null || o !is DLoop)
            return false
        val loop = o as DLoop?
        return _cond == loop!!._cond && _body == loop._body
    }

    override fun hashCode(): Int {
        return 7 * _cond.hashCode() + 17 * _body.hashCode()
    }

    override fun toString(): String {
        return "while (\n$_cond\n) {\n$_body\n}"
    }


    @Throws(SynthesisException::class)
    override fun synthesize(env: Environment): WhileStatement {
        val ast = env.ast()
        val statement = ast.newWhileStatement()

        /* synthesize the condition */
        val clauses = ArrayList<Expression>()
        for (call in _cond) {
            val synth = call.synthesize(env) as? Assignment
            /* a call that returns void cannot be in condition */
                    ?: throw SynthesisException(SynthesisException.MalformedASTFromNN)
            if (call.method == null || call.method.returnType != Boolean::class.java && call.method.returnType != Boolean::class.javaPrimitiveType) {
                val pAssignment = ast.newParenthesizedExpression()
                pAssignment.expression = synth
                val notEqualsNull = ast.newInfixExpression()
                notEqualsNull.leftOperand = pAssignment
                notEqualsNull.operator = InfixExpression.Operator.NOT_EQUALS
                notEqualsNull.rightOperand = ast.newNullLiteral()

                clauses.add(notEqualsNull)
            } else
                clauses.add(synth)
        }
        when (clauses.size) {
            0 -> {
                val `var` = env.search(
                        Type(ast.newPrimitiveType(PrimitiveType.toCode("boolean")), Boolean::class.javaPrimitiveType!!)
                ).expression

                statement.expression = `var`
            }
            1 -> statement.expression = clauses[0]
            else -> {
                var expr = ast.newInfixExpression()
                expr.leftOperand = clauses[0]
                expr.operator = InfixExpression.Operator.AND
                expr.rightOperand = clauses[1]
                for (i in 2 until clauses.size) {
                    val joined = ast.newInfixExpression()
                    joined.leftOperand = expr
                    joined.operator = InfixExpression.Operator.AND
                    joined.rightOperand = clauses[i]
                    expr = joined
                }
                statement.expression = expr
            }
        }

        /* synthesize the body */
        val body = ast.newBlock()
        for (dNode in _body) {
            val aNode = dNode.synthesize(env)
            if (aNode is Statement)
                body.statements().add(aNode)
            else
                body.statements().add(ast.newExpressionStatement(aNode as Expression))
        }
        statement.body = body

        return statement
    }
}
