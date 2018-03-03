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
package tanvd.bayou.implementation.model.stdlib.synthesizer.dsl

import org.eclipse.jdt.core.dom.*
import tanvd.bayou.implementation.model.stdlib.synthesizer.*
import tanvd.bayou.implementation.model.stdlib.synthesizer.Type

import java.util.ArrayList
import java.util.HashSet

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

    override fun updateSequences(soFar: MutableList<Sequence>, max: Int, max_length: Int) {
        if (soFar.size >= max)
            throw DASTNode.TooManySequencesException()
        for (call in _cond)
            call.updateSequences(soFar, max, max_length)

        //TODO-tanvd Check Visitor and DomDriver deleted
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


    override fun synthesize(env: Environment): WhileStatement {
        val ast = env.ast()
        val statement = ast.newWhileStatement()

        /* synthesize the condition */
        val clauses = ArrayList<Expression>()
        for (call in _cond) {
            val synth = call.synthesize(env) as? Assignment
            /* a call that returns void cannot be in condition */
                    ?: throw SynthesisException(SynthesisException.MalformedASTFromNN)

            val pAssignment = ast.newParenthesizedExpression()
            pAssignment.setExpression(synth)
            // if the method does not return a boolean, add != null or != 0 to the condition
            if (call.method == null || !call.method!!.getReturnType().equals(Boolean::class.java) && !call.method!!.getReturnType().equals(Boolean::class.javaPrimitiveType)) {
                val notEqualsNull = ast.newInfixExpression()
                notEqualsNull.setLeftOperand(pAssignment)
                notEqualsNull.setOperator(InfixExpression.Operator.NOT_EQUALS)
                if (call.method != null && call.method!!.getReturnType().isPrimitive())
                    notEqualsNull.setRightOperand(ast.newNumberLiteral("0")) // primitive but not boolean
                else
                // some object
                    notEqualsNull.setRightOperand(ast.newNullLiteral())

                clauses.add(notEqualsNull)
            } else
                clauses.add(pAssignment)
        }
        when (clauses.size) {
            0 -> {
                val target = SearchTarget(
                        Type(ast.newPrimitiveType(PrimitiveType.toCode("boolean")), Boolean::class.java))
                target.singleUseVariable = true
                val `var` = env.search(target).expression

                statement.setExpression(`var`)
            }
            1 -> statement.setExpression(clauses[0])
            else -> {
                var expr = ast.newInfixExpression()
                expr.setLeftOperand(clauses[0])
                expr.setOperator(InfixExpression.Operator.CONDITIONAL_AND)
                expr.setRightOperand(clauses[1])
                for (i in 2 until clauses.size) {
                    val joined = ast.newInfixExpression()
                    joined.setLeftOperand(expr)
                    joined.setOperator(InfixExpression.Operator.CONDITIONAL_AND)
                    joined.setRightOperand(clauses[i])
                    expr = joined
                }
                statement.setExpression(expr)
            }
        }

        /* synthesize the body under a new scope */
        env.pushScope()
        val body = ast.newBlock()
        for (dNode in _body) {
            val aNode = dNode.synthesize(env)
            if (aNode is Statement)
                body.statements().add(aNode)
            else
                body.statements().add(ast.newExpressionStatement(aNode as Expression))
        }
        statement.setBody(body)

        /* join with parent scope itself (the "sub-scope" of a loop if condition was false) */
        val scopes = ArrayList<Scope>()
        scopes.add(env.popScope())
        scopes.add(Scope(env.scope))
        env.scope.join(scopes)

        return statement
    }
}
