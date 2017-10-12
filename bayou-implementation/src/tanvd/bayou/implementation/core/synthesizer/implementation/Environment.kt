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
package tanvd.bayou.implementation.core.synthesizer.implementation

import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.PrimitiveType
import java.util.*

class Environment(internal val ast: AST, scope: List<Variable>) {

    internal var scope: List<Variable> = Collections.unmodifiableList(scope) // unmutable
    internal var mu_scope: MutableList<Variable> = ArrayList() // mutable
    internal var prettyNameCounts: MutableMap<String, Int> = HashMap()

    internal var imports: MutableSet<Class<*>> = HashSet()

    fun ast(): AST {
        return ast
    }

    fun addVariable(type: Type): TypedExpression {
        /* construct a nice name for the variable */
        val name = getPrettyName(type)

        /* add variable to scope */
        val `var` = Variable(name, type)
        mu_scope.add(`var`)

        /* add type to imports */
        imports.add(type.C())

        return TypedExpression(ast.newSimpleName(`var`.name), type)
    }

    fun searchType(): Type {
        val enumerator = Enumerator(ast, this)
        return enumerator.searchType()
    }

    @Throws(SynthesisException::class)
    fun search(type: Type): TypedExpression {
        val enumerator = Enumerator(ast, this)
        return enumerator.search(type) ?: throw SynthesisException(SynthesisException.TypeNotFoundDuringSearch)
    }

    fun addScopedVariable(name: String, cls: Class<*>): Variable {
        val t: Type
        if (cls.isPrimitive)
            t = Type(ast.newPrimitiveType(PrimitiveType.toCode(cls.name)), cls)
        else
            t = Type(ast.newSimpleType(ast.newSimpleName(cls.simpleName)), cls)
        val `var` = Variable(name, t)
        mu_scope.add(`var`)
        return `var`
    }

    fun removeScopedVariable(v: Variable) {
        mu_scope.remove(v)
    }

    fun addImport(c: Class<*>) {
        imports.add(c)
    }

    internal fun getPrettyName(type: Type): String {
        var name: String
        if (type.C().isPrimitive)
            name = type.C().simpleName.substring(0, 1)
        else {
            name = ""
            for (c in type.C().name.toCharArray())
                if (Character.isUpperCase(c))
                    name += Character.toLowerCase(c)
        }

        if (prettyNameCounts.containsKey(name)) {
            prettyNameCounts.put(name, prettyNameCounts[name]!! + 1)
            name += prettyNameCounts[name]
        } else
            prettyNameCounts.put(name, 0)

        return name
    }

    companion object {

        /**
         * Attempts to find the Class representation of the given fully qualified `name` from
         * `Synthesizer.classLoader`.
         *
         * If no such class is found and the given name contains the character '.', a new search name will
         * be generated replacing the final '.' with a '$' and the search will continue in an iterative fashion.
         *
         * For example, if the given name is
         *
         * foo.bar.baz
         *
         * then this method will effectively search for the following classes in order until one (or none) is found:
         *
         * foo.bar.baz
         * foo.bar$baz
         * foo$bar$baz
         * << throws ClassNotFoundException  >>
         *
         * @param name the fully qualified class name to search for
         * @return the Class representation of name (or an attempted alternate) if found
         */
        fun getClass(name: String): Class<*> {
            try {
                return Class.forName(name, false, Synthesizer.classLoader)
            } catch (e: ClassNotFoundException) {
                val lastDotIndex = name.lastIndexOf('.')
                if (lastDotIndex == -1)
                    throw SynthesisException(SynthesisException.ClassNotFoundInLoader)
                val possibleInnerClassName = StringBuilder(name).replace(lastDotIndex, lastDotIndex + 1, "$").toString()
                return getClass(possibleInnerClassName)
            }

        }
    }
}
