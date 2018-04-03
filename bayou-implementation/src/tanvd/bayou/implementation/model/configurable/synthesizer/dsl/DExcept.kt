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
package tanvd.bayou.implementation.model.configurable.synthesizer.dsl

import org.eclipse.jdt.core.dom.*
import tanvd.bayou.implementation.model.configurable.synthesizer.*
import tanvd.bayou.implementation.model.configurable.synthesizer.Type

import java.util.*
import kotlin.Comparator

class DExcept : DASTNode {

    internal var node = "DExcept"
    internal var _try: List<DASTNode>
    internal var _catch: List<DASTNode>
    @Transient
    internal var exceptToClause: MutableMap<Class<*>, CatchClause>? = null

    constructor() {
        this._try = ArrayList()
        this._catch = ArrayList()
        this.node = "DExcept"
    }

    constructor(_try: List<DASTNode>, _catch: List<DASTNode>) {
        this._try = _try
        this._catch = _catch
        this.node = "DExcept"
    }

    @Throws(DASTNode.TooManySequencesException::class, DASTNode.TooLongSequenceException::class)
    override fun updateSequences(soFar: MutableList<Sequence>, max: Int, max_length: Int) {
        if (soFar.size >= max)
            throw DASTNode.TooManySequencesException()
        for (node in _try)
            node.updateSequences(soFar, max, max_length)
        val copy = ArrayList<Sequence>()
        for (seq in soFar)
            copy.add(Sequence(seq.calls))
        for (e in _catch)
            e.updateSequences(copy, max, max_length)
        for (seq in copy)
            if (!soFar.contains(seq))
                soFar.add(seq)
    }

    override fun numStatements(): Int {
        var num = _try.size
        for (c in _catch)
            num += c.numStatements()
        return num
    }

    override fun numLoops(): Int {
        var num = 0
        for (t in _try)
            num += t.numLoops()
        for (c in _catch)
            num += c.numLoops()
        return num
    }

    override fun numBranches(): Int {
        var num = 0
        for (t in _try)
            num += t.numBranches()
        for (c in _catch)
            num += c.numBranches()
        return num
    }

    override fun numExcepts(): Int {
        var num = 1 // this except
        for (t in _try)
            num += t.numExcepts()
        for (c in _catch)
            num += c.numExcepts()
        return num
    }

    override fun bagOfAPICalls(): Set<DAPICall> {
        val bag = HashSet<DAPICall>()
        for (t in _try)
            bag.addAll(t.bagOfAPICalls())
        for (c in _catch)
            bag.addAll(c.bagOfAPICalls())
        return bag
    }

    override fun exceptionsThrown(): Set<Class<*>> {
        val ex = HashSet<Class<*>>()
        // no try: whatever thrown in try would have been caught in catch
        for (c in _catch)
            ex.addAll(c.exceptionsThrown())
        return ex
    }

    override fun exceptionsThrown(eliminatedVars: Set<String>): Set<Class<*>> {
        return this.exceptionsThrown()
    }

    override fun equals(o: Any?): Boolean {
        if (o == null || o !is DExcept)
            return false
        val other = o as DExcept?
        return _try == other!!._try && _catch == other._catch
    }

    override fun hashCode(): Int {
        return 7 * _try.hashCode() + 17 * _catch.hashCode()
    }

    override fun toString(): String {
        return "try {\n$_try\n} catch {\n$_catch\n}"
    }


    override fun synthesize(env: Environment): TryStatement {
        val ast = env.ast()
        val statement = ast.newTryStatement()

        /* synthesize try block */
        val tryBlock = ast.newBlock()
        val exceptionsThrown = HashSet<Class<*>>()
        for (dNode in _try) {
            val aNode = dNode.synthesize(env)
            if (aNode is Statement)
                tryBlock.statements().add(aNode)
            else
                tryBlock.statements().add(ast.newExpressionStatement(aNode as Expression))

            exceptionsThrown.addAll(dNode.exceptionsThrown())
        }
        statement.setBody(tryBlock)
        val exceptionsThrown_ = ArrayList(exceptionsThrown)
        exceptionsThrown_.sortWith(
                Comparator<Class<*>> { e1, e2 -> if (e1.isAssignableFrom(e2)) 1 else -1 }
        )


        if (this.exceptToClause == null)
            this.exceptToClause = HashMap()

        if (exceptionsThrown_.isEmpty())
        /* at least one exception must be thrown in try block */
            throw SynthesisException(SynthesisException.MalformedASTFromNN)

        /* synthesize catch clause body */
        val scopes = ArrayList<Scope>()
        for (except in exceptionsThrown_) {
            val catchClause = ast.newCatchClause()

            /* push a new scope with the catch(... e) variable */
            env.pushScope()
            val type = Type(except)
            type.concretizeType(env) // shouldn't be a generic type
            val target = SearchTarget(type)
            val name = env.addVariable(target.type, VariableProperties()).expression as SimpleName

            /* synthesize catch clause exception types */
            val ex = ast.newSingleVariableDeclaration()
            ex.setType(ast.newSimpleType(ast.newName(except.simpleName)))
            ex.setName(name)
            catchClause.setException(ex)
            statement.catchClauses().add(catchClause)

            val catchBlock = ast.newBlock()
            for (dNode in _catch) {
                val aNode = dNode.synthesize(env)
                if (aNode is Statement)
                    catchBlock.statements().add(aNode)
                else
                    catchBlock.statements().add(ast.newExpressionStatement(aNode as Expression))
            }
            catchClause.setBody(catchBlock)
            scopes.add(env.popScope())
            env.addImport(except)

            this.exceptToClause!![except] = catchClause
        }

        env.scope.join(scopes)
        return statement
    }

    fun cleanupCatchClauses(eliminatedVars: Set<String>) {
        val excepts = HashSet<Any>()
        for (tn in _try) {
            if (tn is DAPICall) {
                val retVarName = tn.retVarName
                if (retVarName != "" && eliminatedVars.contains(retVarName))
                    excepts.addAll(tn.exceptionsThrown())
            }
        }

        for (obj in this.exceptToClause!!.keys) {
            if (excepts.contains(obj)) {
                val catchClause = this.exceptToClause!![obj] as CatchClause
                catchClause.delete()
            }
        }
    }
}
