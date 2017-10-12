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

import org.eclipse.jdt.core.dom.ASTNode
import org.eclipse.jdt.core.dom.ParameterizedType
import org.eclipse.jdt.core.dom.PrimitiveType
import org.eclipse.jdt.core.dom.SimpleType
import java.lang.reflect.TypeVariable
import java.util.*

class Type {

    /* TODO: add support for arrays (search for isArray) */

    private val c: Class<*>
    private var t: org.eclipse.jdt.core.dom.Type? = null
    private var concretization: MutableMap<String, Type>? = null
    internal var refCount: Int = 0

    val isConcretized: Boolean
        get() = t != null

    constructor(t: org.eclipse.jdt.core.dom.Type) {
        this.t = t
        this.c = getClass(t)

        if (this.c.isArray)
            throw SynthesisException(SynthesisException.InvalidKindOfType)

        this.refCount = 0
        autoConcretize()
    }

    constructor(t: org.eclipse.jdt.core.dom.Type, c: Class<*>) {
        this.t = t
        this.c = c

        if (this.c.isArray)
            throw SynthesisException(SynthesisException.InvalidKindOfType)

        this.refCount = 0
        autoConcretize()
    }

    constructor(c: Class<*>) {
        this.c = c
        this.t = null

        if (this.c.isArray)
            throw SynthesisException(SynthesisException.InvalidKindOfType)

        // in this case, type has to be concretized manually with an environment before it can be used
    }

    fun T(): org.eclipse.jdt.core.dom.Type {
        if (t == null)
            throw SynthesisException(SynthesisException.InvalidKindOfType)
        return t!!
    }

    fun C(): Class<*> {
        return c
    }

    fun concretizeType(env: Environment) {
        if (t != null)
            return
        val ast = env.ast

        if (c.isArray)
            throw SynthesisException(SynthesisException.InvalidKindOfType)

        if (c.isPrimitive)
            t = ast.newPrimitiveType(PrimitiveType.toCode(c.simpleName))
        else if (c.typeParameters.size == 0)
        // simple type
            t = ast.newSimpleType(ast.newName(c.canonicalName))
        else { // generic type
            concretization = HashMap()
            val rawType = ast.newSimpleType(ast.newName(c.canonicalName))
            t = ast.newParameterizedType(rawType)

            // search for a type from the environment and add it as a concretization
            for (tvar in c.typeParameters) {
                val name = tvar.name
                val type = env.searchType()

                (t as ParameterizedType).typeArguments().add(ASTNode.copySubtree(ast, type.T()))
                concretization!!.put(name, type)
            }
        }
    }

    fun getConcretization(type: java.lang.reflect.Type): Type {
        val ast = t!!.ast

        if (type is java.lang.reflect.ParameterizedType) {
            // substitute generic names with their types (recursively)
            val rawType_ = type.rawType
            val rawType = ast.newSimpleType(ast.newName((rawType_ as Class<*>).canonicalName))
            val retType = ast.newParameterizedType(rawType)

            for (arg in type.actualTypeArguments) {
                val argType = getConcretization(arg).T()
                retType.typeArguments().add(ASTNode.copySubtree(ast, argType))
            }

            return Type(retType, rawType_)
        } else if (type is TypeVariable<*>) {
            // return the type the generic name was concretized to
            val name = type.name
            if (!concretization!!.containsKey(name))
                throw SynthesisException(SynthesisException.GenericTypeVariableMismatch)
            return concretization!![name]!!
        } else if (type is Class<*>) {

            if (type.isArray)
                throw SynthesisException(SynthesisException.InvalidKindOfType)

            if (type.isPrimitive)
                return Type(ast.newPrimitiveType(PrimitiveType.toCode(type.simpleName)), type)
            else {
                // no generics, just return a simple type with the class
                val retType = ast.newSimpleType(ast.newName(type.canonicalName))
                return Type(retType, type)
            }
        } else
            throw SynthesisException(SynthesisException.InvalidKindOfType)
    }

    // same semantics as Class.isAssignableFrom for our type system but with generics
    // NOTE: assumes that the argument is a concretized type
    fun isAssignableFrom(type: Type): Boolean {
        if (!this.C().isAssignableFrom(type.C()))
            return false
        if (t == null || !t!!.isParameterizedType)
        // this type is not yet concretized or not parametric
            return true
        if (!type.T().isParameterizedType)
            return false

        // sanity check
        val pt1 = T() as ParameterizedType
        val pt2 = type.T() as ParameterizedType
        val n1 = pt1.typeArguments().size
        val n2 = pt2.typeArguments().size
        if (n1 != n2)
            throw SynthesisException(SynthesisException.GenericTypeVariableMismatch)

        for (i in 0 until n1) {
            val t1 = Type(pt1.typeArguments().get(i) as org.eclipse.jdt.core.dom.Type)
            val t2 = Type(pt2.typeArguments().get(i) as org.eclipse.jdt.core.dom.Type)

            // generic type arguments should always be invariant, not covariant
            // for example, a List<Dog> cannot be a List<Animal> even if Dog extends Animal
            if (!t1.isInvariant(t2))
                return false
        }
        return true
    }

    // checks if this type is invariant with the given type
    // NOTE: assumes that the argument is a concretized type
    fun isInvariant(type: Type): Boolean {
        if (this.C() != type.C())
            return false
        if (t == null || !t!!.isParameterizedType)
        // this type is not yet concretized or not parametric
            return true
        if (!type.T().isParameterizedType)
            return false

        // sanity check
        val pt1 = T() as ParameterizedType
        val pt2 = type.T() as ParameterizedType
        val n1 = pt1.typeArguments().size
        val n2 = pt2.typeArguments().size
        if (n1 != n2)
            throw SynthesisException(SynthesisException.GenericTypeVariableMismatch)

        for (i in 0 until n1) {
            val t1 = Type(pt1.typeArguments().get(i) as org.eclipse.jdt.core.dom.Type)
            val t2 = Type(pt2.typeArguments().get(i) as org.eclipse.jdt.core.dom.Type)

            if (!t1.isInvariant(t2))
                return false
        }
        return true
    }

    // Class objects are type-erased, so cannot do generics here
    fun isAssignableFrom(type: Class<*>): Boolean {
        if (!this.C().isAssignableFrom(type))
            return false
        return if (t == null || !t!!.isParameterizedType) true else false
// cannot assign an erased type to a parameterized type
    }

    private fun autoConcretize() {
        concretization = HashMap()

        // check sanity of types
        if (!t!!.isParameterizedType) {
            if (c.typeParameters.size > 0)
                throw SynthesisException(SynthesisException.GenericTypeVariableMismatch)
            return
        }
        val pType = t as ParameterizedType?
        val n1 = pType!!.typeArguments().size
        val n2 = c.typeParameters.size
        if (n1 != n2)
            throw SynthesisException(SynthesisException.GenericTypeVariableMismatch)

        // unify generic names with their actual types
        for (i in 0 until n1) {
            val name = c.typeParameters[i].name
            val type = Type(pType.typeArguments().get(i) as org.eclipse.jdt.core.dom.Type)
            concretization!!.put(name, type)
        }
    }

    internal fun getClass(type: org.eclipse.jdt.core.dom.Type): Class<*> {
        val binding = type.resolveBinding()
        if (type.isPrimitiveType)
            return Visitor.primitiveToClass[(type as PrimitiveType).primitiveTypeCode]!!
        else if (type.isSimpleType) {
            if (binding != null)
                return Environment.getClass(binding.qualifiedName)
            else {
                val t = (type as SimpleType).name.fullyQualifiedName
                return Environment.getClass(t)
            }
        } else if (type.isParameterizedType) {
            if (binding != null) {
                val erased = binding.erasure
                return Environment.getClass(erased.qualifiedName)
            } else {
                val baseType = (type as ParameterizedType).getType()
                val t = (baseType as SimpleType).name.fullyQualifiedName
                return Environment.getClass(t)
            }
        } else
            throw SynthesisException(SynthesisException.InvalidKindOfType)
    }

    fun addRefCount() {
        refCount += 1
    }

    override fun toString(): String {
        return (if (t != null) t else c).toString()
    }
}
