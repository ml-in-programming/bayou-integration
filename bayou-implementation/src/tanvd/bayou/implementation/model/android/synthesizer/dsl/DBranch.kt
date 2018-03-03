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

class DBranch : DASTNode {

    internal var node = "DBranch"
    internal var _cond: List<DAPICall>
    internal var _then: List<DASTNode>
    internal var _else: List<DASTNode>

    constructor() {
        this._cond = ArrayList()
        this._then = ArrayList()
        this._else = ArrayList()
        this.node = "DBranch"
    }

    constructor(_cond: List<DAPICall>, _then: List<DASTNode>, _else: List<DASTNode>) {
        this._cond = _cond
        this._then = _then
        this._else = _else
        this.node = "DBranch"
    }

    @Throws(DASTNode.TooManySequencesException::class, DASTNode.TooLongSequenceException::class)
    override fun updateSequences(soFar: MutableList<Sequence>, max: Int, max_length: Int) {
        if (soFar.size >= max)
            throw DASTNode.TooManySequencesException()
        for (call in _cond)
            call.updateSequences(soFar, max, max_length)
        val copy = ArrayList<Sequence>()
        for (seq in soFar)
            copy.add(Sequence(seq.calls))
        for (t in _then)
            t.updateSequences(soFar, max, max_length)
        for (e in _else)
            e.updateSequences(copy, max, max_length)
        for (seq in copy)
            if (!soFar.contains(seq))
                soFar.add(seq)
    }

    override fun numStatements(): Int {
        var num = _cond.size
        for (t in _then)
            num += t.numStatements()
        for (e in _else)
            num += e.numStatements()
        return num
    }

    override fun numLoops(): Int {
        var num = 0
        for (t in _then)
            num += t.numLoops()
        for (e in _else)
            num += e.numLoops()
        return num
    }

    override fun numBranches(): Int {
        var num = 1 // this branch
        for (t in _then)
            num += t.numBranches()
        for (e in _else)
            num += e.numBranches()
        return num
    }

    override fun numExcepts(): Int {
        var num = 0
        for (t in _then)
            num += t.numExcepts()
        for (e in _else)
            num += e.numExcepts()
        return num
    }

    override fun bagOfAPICalls(): Set<DAPICall> {
        val bag = HashSet<DAPICall>()
        bag.addAll(_cond)
        for (t in _then)
            bag.addAll(t.bagOfAPICalls())
        for (e in _else)
            bag.addAll(e.bagOfAPICalls())
        return bag
    }

    override fun exceptionsThrown(): Set<Class<*>> {
        val ex = HashSet<Class<*>>()
        for (c in _cond)
            ex.addAll(c.exceptionsThrown())
        for (t in _then)
            ex.addAll(t.exceptionsThrown())
        for (e in _else)
            ex.addAll(e.exceptionsThrown())
        return ex
    }

    override fun exceptionsThrown(eliminatedVars: Set<String>): Set<Class<*>> {
        return this.exceptionsThrown()
    }

    override fun equals(o: Any?): Boolean {
        if (o == null || o !is DBranch)
            return false
        val branch = o as DBranch?
        return _cond == branch!!._cond && _then == branch._then && _else == branch._else
    }

    override fun hashCode(): Int {
        return 7 * _cond.hashCode() + 17 * _then.hashCode() + 31 * _else.hashCode()
    }

    override fun toString(): String {
        return "if (\n$_cond\n) then {\n$_then\n} else {\n$_else\n}"
    }


    @Throws(SynthesisException::class)
    override fun synthesize(env: Environment): IfStatement {
        val ast = env.ast()
        val statement = ast.newIfStatement()

        /* synthesize the condition */
        val clauses = ArrayList<Expression>()
        for (call in _cond) {
            /* this cast is safe (unless NN has gone crazy) because a call that returns void cannot be in condition */
            val assignment = call.synthesize(env) as Assignment
            if (call.method == null || call.method.returnType != Boolean::class.java && call.method.returnType != Boolean::class.javaPrimitiveType) {
                val pAssignment = ast.newParenthesizedExpression()
                pAssignment.expression = assignment
                val notEqualsNull = ast.newInfixExpression()
                notEqualsNull.leftOperand = pAssignment
                notEqualsNull.operator = InfixExpression.Operator.NOT_EQUALS
                notEqualsNull.rightOperand = ast.newNullLiteral()

                clauses.add(notEqualsNull)
            } else
                clauses.add(assignment)
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

        /* synthesize then and else body */
        val thenBlock = ast.newBlock()
        for (dNode in _then) {
            val aNode = dNode.synthesize(env)
            if (aNode is Statement)
                thenBlock.statements().add(aNode)
            else
                thenBlock.statements().add(ast.newExpressionStatement(aNode as Expression))
        }
        statement.thenStatement = thenBlock

        val elseBlock = ast.newBlock()
        for (dNode in _else) {
            val aNode = dNode.synthesize(env)
            if (aNode is Statement)
                elseBlock.statements().add(aNode)
            else
                elseBlock.statements().add(ast.newExpressionStatement(aNode as Expression))
        }
        statement.elseStatement = elseBlock

        return statement
    }
}
