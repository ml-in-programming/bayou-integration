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
package tanvd.bayou.implementation.core.code.synthesizer.implementation

import org.eclipse.jdt.core.dom.*
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*

class Enumerator(internal val ast: AST, internal val env: Environment) {

    private val importsDuringSearch: MutableSet<Class<*>>

    internal inner class InvocationChain {
        val `var`: Variable
        val methods: MutableList<Method>
        val types: MutableList<Type>

        val currentType: Type
            get() = if (types.isEmpty()) `var`.type else types[types.size - 1]

        constructor(`var`: Variable) {
            this.`var` = `var`
            methods = ArrayList()
            types = ArrayList()
        }

        constructor(chain: InvocationChain) {
            `var` = chain.`var`
            methods = ArrayList(chain.methods)
            types = ArrayList(chain.types)
        }

        fun addMethod(m: Method) {
            types.add(currentType.getConcretization(m.genericReturnType))
            methods.add(m)
        }

        fun pop() {
            methods.removeAt(methods.size - 1)
            types.removeAt(types.size - 1)
        }

        fun structCost(): Int {
            val args = methods.stream().mapToInt { m -> m.parameterTypes.size }.sum()
            val chainLength = methods.size

            return chainLength + K * args // give some more weight to arguments because that involves further search
        }

        override fun equals(o: Any?): Boolean {
            return if (o == null || o !is InvocationChain) false else methods == o.methods
        }

        override fun hashCode(): Int {
            return methods.hashCode()
        }
    }

    init {
        this.importsDuringSearch = HashSet()
    }

    fun search(targetType: Type): TypedExpression? {
        val tExpr = search(targetType, 0)
        if (tExpr == null) {
            importsDuringSearch.clear()
            return null
        }

        env.imports.addAll(importsDuringSearch)
        importsDuringSearch.clear()

        if (tExpr.expression is SimpleName)
            return tExpr /* found a variable in scope, just return it */
        if (isFunctionalInterface(targetType))
            return tExpr /* synthesized code will be an anonymous class */

        /* assign a variable to this expression so that we don't have to search for it again in future */
        val assignment = ast.newAssignment()
        assignment.leftHandSide = env.addVariable(targetType).expression // just the variable name
        assignment.operator = Assignment.Operator.ASSIGN
        assignment.rightHandSide = tExpr.expression

        val parenExpr = ast.newParenthesizedExpression()
        parenExpr.expression = assignment
        return TypedExpression(parenExpr, tExpr.type)
    }

    private fun search(targetType: Type, argDepth: Int): TypedExpression? {
        if (argDepth > MAX_ARGUMENT_DEPTH)
            return null

        /* see if a variable with the type already exists in scope */
        val toSearch = ArrayList(env.scope)
        toSearch.addAll(env.mu_scope)
        sortVariablesByCost(toSearch)
        for (v in toSearch)
            if (targetType.isAssignableFrom(v.type)) {
                v.addRefCount()
                return TypedExpression(ast.newSimpleName(v.name), v.type)
            }

        /* check if this is a functional interface */
        if (isFunctionalInterface(targetType))
            return TypedExpression(createAnonymousClass(targetType), targetType)

        /* could not pick variable, so concretize target type and resort to enumerative search */
        targetType.concretizeType(env)
        return enumerate(targetType, argDepth, toSearch)
    }

    private fun enumerate(targetType: Type, argDepth: Int, toSearch: List<Variable>): TypedExpression? {
        val enumerator = Enumerator(ast, env)

        /* first, see if we can create a new object of target type directly */
        val constructors = targetType.C().constructors.toMutableList<Executable>()
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
                while (i < constructor.getParameterTypes().size) {
                    val argType = constructor.getParameterTypes()[i]
                    val tArg = enumerator.search(Type(argType), argDepth + 1) ?: break
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
                while (i < constructor.parameterTypes.size) {
                    val argType = constructor.parameterTypes[i]
                    val tArg = enumerator.search(Type(argType), argDepth + 1) ?: break
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

        /* otherwise, start recursive search for expression of target type */
        val chains = ArrayList<InvocationChain>()
        for (`var` in toSearch)
            chains.addAll(searchForChains(targetType, `var`))
        sortChainsByCost(chains)

        var i: Int
        var j: Int
        for (chain in chains) {
            /* for each chain, see if we can synthesize all arguments in all methods in the chain */
            var invocation = ast.newMethodInvocation()
            var expr: Expression = ast.newSimpleName(chain.`var`.name)
            enumerator.importsDuringSearch.clear()
            i = 0
            while (i < chain.methods.size) {
                val m = chain.methods[i]
                invocation.expression = expr
                invocation.name = ast.newSimpleName(m.name)

                j = 0
                while (j < m.parameterTypes.size) {
                    val argType = m.parameterTypes[j]
                    val tArg: TypedExpression?
                    try {
                        tArg = enumerator.search(Type(argType), argDepth + 1)
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

    /* returns a list of method call chains that all produce the target type
     * TODO : use a memoizer to prune even more of the search space */
    private fun searchForChains(targetType: Type, `var`: Variable): List<InvocationChain> {
        val chains = ArrayList<InvocationChain>()
        searchForChains(targetType, InvocationChain(`var`), chains, 0)
        return chains
    }

    private fun searchForChains(targetType: Type, chain: InvocationChain, chains: MutableList<InvocationChain>, composeLength: Int) {
        //TODO-tanvd Fix for too long chains of invocations
        if (chains.size > 50 || chain.methods.size > 50) {
            throw SynthesisException(-1)
        }
        val currType = chain.currentType
        if (composeLength >= MAX_COMPOSE_LENGTH || currType.C().isPrimitive)
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
                chains.add(InvocationChain(chain))
            else
                searchForChains(targetType, chain, chains, composeLength + 1)
            chain.pop()
        }
    }

    fun searchType(): Type {
        val vars = ArrayList(env.scope)
        vars.addAll(env.mu_scope)
        val types = vars.map { v -> v.type }.filter { t -> t.T().isSimpleType }

        sortTypesByCost(types)
        if (types.isEmpty())
            throw SynthesisException(SynthesisException.TypeNotFoundDuringSearch)
        val type = types[0]
        type.addRefCount()
        return type
    }

    private fun sortTypesByCost(types: List<Type>) {
        Collections.shuffle(types)
        types.sortedBy { it.refCount }
    }

    private fun sortConstructorsByCost(constructors: List<Constructor<*>>) {
        Collections.shuffle(constructors)
        constructors.sortedBy { it.parameterTypes.size }
    }

    private fun sortMethodsByCost(methods: List<Method>) {
        Collections.shuffle(methods)
        methods.sortedBy { it.parameterTypes.size }
    }

    private fun sortExecutablesByCost(exes: List<Executable>) {
        Collections.shuffle(exes)
        exes.sortedBy { it.parameterTypes.size }
    }

    private fun sortVariablesByCost(variables: List<Variable>) {
        variables.sortedBy { it.refCount }
    }

    private fun sortChainsByCost(chains: List<InvocationChain>) {
        chains.sortedBy { it.structCost() }
    }

    private fun isFunctionalInterface(type: Type): Boolean {
        return type.C().isInterface && type.C().methods.size == 1
    }

    private fun createAnonymousClass(targetType: Type): ClassInstanceCreation {
        val creation = ast.newClassInstanceCreation()
        creation.type = ast.newSimpleType(ast.newSimpleName(targetType.C().simpleName))
        val anonymousClass = ast.newAnonymousClassDeclaration()
        creation.anonymousClassDeclaration = anonymousClass

        /* TODO: synthesize a stub of the (one) method in functional interface */
        importsDuringSearch.add(targetType.C())
        return creation
    }

    companion object {
        internal val MAX_COMPOSE_LENGTH = 3 // a().b().c().d()...
        internal val MAX_ARGUMENT_DEPTH = 2 // a(b(c(d(...))))
        internal val K = 3 // number of arguments is given K times more weight than length of composition in cost
    }

}
