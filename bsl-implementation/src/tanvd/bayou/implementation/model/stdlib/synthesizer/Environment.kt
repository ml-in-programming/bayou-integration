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

import org.apache.commons.lang3.ClassUtils
import org.eclipse.jdt.core.dom.AST
import java.util.*

class Environment(internal val ast: AST, variables: List<Variable>, internal val mode: Synthesizer.Mode) {

    private val scopes: Stack<Scope>
    internal var imports: MutableSet<Class<*>>

    val scope: Scope
        get() = scopes.peek()

    fun ast(): AST {
        return ast
    }

    init {
        this.scopes = Stack()
        this.scopes.push(Scope(variables))
        imports = HashSet()
    }

    /**
     * Adds a variable with the given type (and default properties) to the current scope
     *
     * @param type variable type, from which a variable name will be derived
     * @return a TypedExpression with a simple name (variable name) and variable type
     */
    fun addVariable(type: Type): TypedExpression {
        val properties = VariableProperties().also {
            it.join = true
        } // default properties
        val `var` = scopes.peek().addVariable(type, properties)
        imports.add(`var`.type.C())
        return TypedExpression(`var`.createASTNode(ast), `var`.type)
    }

    /**
     * Adds a variable with the given type and properties to the current scope
     *
     * @param type       variable type, from which a variable name will be derived
     * @param properties variable properties
     * @return a TypedExpression with a simple name (variable name) and variable type
     */
    fun addVariable(type: Type, properties: VariableProperties): TypedExpression {
        val `var` = scopes.peek().addVariable(type, properties)
        imports.add(`var`.type.C())
        return TypedExpression(`var`.createASTNode(ast), `var`.type)
    }

    /**
     * Adds the given variable, whose properties must already be set, to the current scope
     *
     * @param var the variable to be added
     * @return a TypedExpression with a simple name (variable name) and variable type
     */
    fun addVariable(`var`: Variable): TypedExpression {
        scopes.peek().addVariable(`var`)
        imports.add(`var`.type.C())
        return TypedExpression(`var`.createASTNode(ast), `var`.type)
    }

    fun searchType(): Type {
        val enumerator = Enumerator(ast, this, mode)
        return enumerator.searchType()
    }

    @Throws(SynthesisException::class)
    fun search(target: SearchTarget): TypedExpression {
        val enumerator = Enumerator(ast, this, mode)
        return enumerator.search(target) ?: throw SynthesisException(SynthesisException.TypeNotFoundDuringSearch)
    }

    fun pushScope() {
        val newScope = Scope(scopes.peek())
        scopes.push(newScope)
    }

    fun popScope(): Scope {
        return scopes.pop()
    }

    fun addImport(c: Class<*>) {
        imports.add(c)
    }

    companion object {

        fun getClass(name: String): Class<*> {
            try {
                return ClassUtils.getClass(Synthesizer.classLoader, name)
            } catch (e: ClassNotFoundException) {
                println(name)
                throw SynthesisException(SynthesisException.ClassNotFoundInLoader)
            }

        }
    }

}
