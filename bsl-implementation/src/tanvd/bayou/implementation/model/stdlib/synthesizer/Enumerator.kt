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
package tanvd.bayou.implementation.model.stdlib.synthesizer

import org.apache.commons.text.similarity.LevenshteinDistance
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.Assignment
import org.eclipse.jdt.core.dom.Expression
import org.eclipse.jdt.core.dom.SimpleName
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*

class Enumerator(internal val ast: AST, internal val env: Environment, internal var mode: Synthesizer.Mode) {
    private val importsDuringSearch: MutableSet<Class<*>>

    init {
        this.importsDuringSearch = HashSet()
    }

    fun search(target: SearchTarget): TypedExpression? {
        val tExpr = search(target, 0)
        if (tExpr == null) {
            importsDuringSearch.clear()
            return null
        }

        env.imports.addAll(importsDuringSearch)
        importsDuringSearch.clear()

        if (tExpr.expression is SimpleName)
            return tExpr /* found a variable in scope, just return it */

        /* assign a variable to this expression so that we don't have to search for it again in future */
        val assignment = ast.newAssignment()
        val properties = VariableProperties().also {
            it.join = true
            it.defaultInit = false
            it.singleUse = target.singleUseVariable
        }

        val expr = if (target.paramName != null)
            env.addVariable(Variable(target.paramName!!, target.type, properties))
        else
            env.addVariable(target.type, properties)
        assignment.leftHandSide = expr.expression // just the variable name
        assignment.operator = Assignment.Operator.ASSIGN
        assignment.rightHandSide = tExpr.expression

        val parenExpr = ast.newParenthesizedExpression()
        parenExpr.expression = assignment
        return TypedExpression(parenExpr, tExpr.type)
    }

    private fun search(target: SearchTarget, argDepth: Int): TypedExpression? {
        if (argDepth > ExpressionChain.MAX_ARGUMENT_DEPTH)
            return null

        /* see if a variable with the type already exists in scope */
        val toSearch = ArrayList(env.scope.variables!!)
        sortVariablesByCost(toSearch, target)
        for (v in toSearch)
            if (!v.isSingleUseVar && target.type.isAssignableFrom(v.type)) {
                v.addRefCount()
                return TypedExpression(v.createASTNode(ast), v.type)
            }

        /* could not pick variable, so concretize target type */
        target.type.concretizeType(env)

        /* ... and start enumerative search or, if enumeration failed, add variable (with default init) */
        var expr = enumerate(target, argDepth, toSearch)
        if (expr == null) {
            val properties = VariableProperties().also {
                it.join = true
                it.defaultInit = true
                it.singleUse = target.singleUseVariable
            }
            if (target.paramName != null) { // create variable with name here, but note that it may be refactored
                val `var` = Variable(target.paramName!!, target.type, properties)
                expr = env.addVariable(`var`)
            } else {
                expr = env.addVariable(target.type, properties)
            }
        }

        return expr
    }

    private fun enumerate(target: SearchTarget, argDepth: Int, toSearch: List<Variable>): TypedExpression? {
        val enumerator = Enumerator(ast, env, mode)
        val targetType = target.type

        /* first, see if we can create a new object of target type directly */
        val constructors = ArrayList<Executable>(Arrays.asList<Constructor<*>>(*targetType.C().constructors))
        /* static methods that return the target type are considered "constructors" here */
        for (m in targetType.C().methods)
            if (Modifier.isStatic(m.modifiers) && targetType.isAssignableFrom(m.returnType))
                constructors.add(m)
        sortExecutablesByCost(constructors)
        for (constructor in constructors) {
            if (Modifier.isAbstract(targetType.C().modifiers))
                break
            if (!Modifier.isPublic(constructor.modifiers))
                continue

            if (constructor is Constructor<*>) { /* an actual constructor */
                val creation = ast.newClassInstanceCreation()
                creation.type = ast.newSimpleType(ast.newSimpleName(targetType.C().simpleName))

                var i: Int
                enumerator.importsDuringSearch.clear()
                i = 0
                while (i < constructor.getParameterCount()) {
                    val argType = constructor.getParameterTypes()[i]
                    val name = constructor.getParameters()[i].name
                    val newTarget = SearchTarget(Type(argType)).also {
                        it.setAPICallName(constructor.getName())
                        it.paramName = name
                        it.singleUseVariable = true
                    }
                    val tArg = enumerator.search(newTarget, argDepth + 1) ?: break
                    creation.arguments().add(tArg.expression)
                    i++
                }
                if (i == constructor.getParameterCount()) {
                    importsDuringSearch.addAll(enumerator.importsDuringSearch)
                    importsDuringSearch.add(targetType.C())
                    return TypedExpression(creation, targetType)
                }
            } else { /* a static method that returns the object type */
                val invocation = ast.newMethodInvocation()

                var i: Int
                enumerator.importsDuringSearch.clear()
                invocation.expression = ast.newSimpleName(targetType.C().simpleName)
                invocation.name = ast.newSimpleName(constructor.name)
                i = 0
                while (i < constructor.parameterCount) {
                    val argType = constructor.parameterTypes[i]
                    val name = constructor.parameters[i].name
                    val newTarget = SearchTarget(Type(argType)).also {
                        it.setAPICallName(constructor.name)
                        it.paramName = name
                        it.singleUseVariable = true

                    }
                    val tArg = enumerator.search(newTarget, argDepth + 1) ?: break
                    invocation.arguments().add(tArg.expression)
                    i++
                }
                if (i == constructor.parameterCount) {
                    importsDuringSearch.addAll(enumerator.importsDuringSearch)
                    importsDuringSearch.add(targetType.C())
                    return TypedExpression(invocation, targetType)
                }
            }
        }

        if (mode == Synthesizer.Mode.CONDITIONAL_PROGRAM_GENERATOR)
            return null

        /* otherwise, start recursive search for expression of target type */
        val chains = ArrayList<ExpressionChain>()
        for (`var` in toSearch)
            chains.addAll(searchForChains(targetType, `var`))
        sortChainsByCost(chains)

        var i: Int
        var j: Int
        for (chain in chains) {
            /* for each chain, see if we can synthesize all arguments in all methods in the chain */
            var invocation = ast.newMethodInvocation()
            var expr: Expression = chain.`var`.createASTNode(ast)
            enumerator.importsDuringSearch.clear()
            i = 0
            while (i < chain.methods.size) {
                val m = chain.methods[i]
                invocation.expression = expr
                invocation.name = ast.newSimpleName(m.name)

                j = 0
                while (j < m.parameterCount) {
                    val argType = m.parameterTypes[j]
                    val name = m.parameters[j].name
                    val newTarget = SearchTarget(Type(argType)).also {
                        it.setAPICallName(m.name)
                        it.paramName = name
                        it.singleUseVariable = true
                    }

                    val tArg: TypedExpression?
                    try {
                        tArg = enumerator.search(newTarget, argDepth + 1)
                    } catch (e: SynthesisException) {
                        break // could not synthesize some argument, ignore this chain
                    }

                    if (tArg == null)
                        break
                    invocation.arguments().add(tArg.expression)
                    j++
                }
                if (j != m.parameterCount)
                    break
                expr = invocation
                invocation = ast.newMethodInvocation()
                i++
            }
            if (i == chain.methods.size) {
                importsDuringSearch.addAll(enumerator.importsDuringSearch)
                return TypedExpression(expr, targetType)
            }
        }

        return null
    }

    /* returns a list of method call chains that all produce the target type */
    private fun searchForChains(targetType: Type, `var`: Variable): List<ExpressionChain> {
        val chains = ArrayList<ExpressionChain>()
        searchForChains(targetType, ExpressionChain(`var`), chains, 0)
        return chains
    }

    private fun searchForChains(targetType: Type, chain: ExpressionChain, chains: MutableList<ExpressionChain>, composeLength: Int) {
        val currType = chain.currentType
        if (composeLength >= ExpressionChain.MAX_COMPOSE_LENGTH || currType.C().isPrimitive)
            return
        val methods = Arrays.asList(*currType.C().methods)
        sortMethodsByCost(methods)
        for (m in methods) {
            if (!Modifier.isPublic(m.modifiers))
                continue
            try {
                chain.addMethod(m)
            } catch (e: SynthesisException) {
                continue // some problem with adding this method to chain, so ignore it
            }

            if (targetType.isAssignableFrom(chain.currentType))
                chains.add(ExpressionChain(chain))
            else
                searchForChains(targetType, chain, chains, composeLength + 1)
            chain.pop()
        }
    }

    fun searchType(): Type {
        val vars = ArrayList(env.scope.variables!!)
        val types = vars.map { v -> v.type }
                .filter { t -> t.T().isSimpleType }
                .toList() // only consider simple types

        sortTypesByCost(types)
        if (types.isEmpty()) {
            return Type(ast.newSimpleType(ast.newName("java.lang.String")), String::class.java) // String is the default type
        }
        val type = types[0]
        type.addRefCount()
        return type
    }

    private fun sortTypesByCost(types: List<Type>) {
        Collections.shuffle(types)
        types.sortedWith(Comparator.comparingInt<Type> { t -> t.refCount })
    }

    private fun sortConstructorsByCost(constructors: List<Constructor<*>>) {
        Collections.shuffle(constructors)
        constructors.sortedWith(Comparator.comparingInt<Constructor<*>> { c -> c.getParameterTypes().size })
    }

    private fun sortMethodsByCost(methods: List<Method>) {
        Collections.shuffle(methods)
        methods.sortedWith(Comparator.comparingInt<Method> { c -> c.parameterTypes.size })
    }

    private fun sortExecutablesByCost(exes: List<Executable>) {
        Collections.shuffle(exes)
        exes.sortedWith(Comparator.comparingInt<Executable> { e -> e.parameterTypes.size })
    }

    private fun sortVariablesByCost(variables: List<Variable>, target: SearchTarget) {
        var compareWith = ""
        if (target.paramName != null)
            compareWith += target.paramName!!.toLowerCase()
        if (target.apiCallName != null)
            compareWith += target.apiCallName!!.toLowerCase()

        if (compareWith == "")
            variables.sortedWith(Comparator.comparingInt<Variable> { v -> v.refCount })
        else {
            val compare = compareWith
            variables.sortedWith(Comparator.comparingInt<Variable> { v -> LevenshteinDistance.getDefaultInstance().apply(compare, v.name) })
        }
    }

    private fun sortChainsByCost(chains: List<ExpressionChain>) {
        chains.sortedWith(Comparator.comparingInt<ExpressionChain> { chain -> chain.structCost() })
    }
}
