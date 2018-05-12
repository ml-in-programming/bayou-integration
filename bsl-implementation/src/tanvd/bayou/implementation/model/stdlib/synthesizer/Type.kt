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
import org.eclipse.jdt.core.dom.*
import java.lang.reflect.TypeVariable
import java.util.*

class Type {

    private val c: Class<*>
    private var t: org.eclipse.jdt.core.dom.Type? = null
    private var concretization: MutableMap<String, Type>? = null
    internal var refCount: Int = 0

    val isConcretized: Boolean
        get() = t != null

    constructor(t: org.eclipse.jdt.core.dom.Type) {
        this.t = t
        this.c = getClass(t)
        this.refCount = 0
        autoConcretize()

        if (t.resolveBinding() != null)
            this.t = releaseBinding(t, t.ast)
    }

    constructor(t: org.eclipse.jdt.core.dom.Type, c: Class<*>) {
        this.t = t
        this.c = c
        this.refCount = 0
        autoConcretize()

        if (t.resolveBinding() != null)
            this.t = releaseBinding(t, t.ast)
    }

    constructor(c: Class<*>) {
        this.c = c
        this.t = null

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

        if (c.isPrimitive)
            t = ast.newPrimitiveType(PrimitiveType.toCode(c.simpleName))
        else if (c.isArray) {
            var dimensions = 0
            var cls: Class<*> = c
            while (cls.isArray) {
                dimensions++
                cls = cls.componentType
            }
            val componentType = Type(c.componentType)
            componentType.concretizeType(env)
            t = ast.newArrayType(componentType.T(), dimensions)
        } else if (c.typeParameters.size == 0)
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
                concretization!![name] = type
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

            // FIXME: Add support for wildcard types and concretizing without a base parameterized type (e.g., Collections)
            if (concretization == null)
                throw SynthesisException(SynthesisException.InvalidKindOfType)

            if (!concretization!!.containsKey(name))
                throw SynthesisException(SynthesisException.GenericTypeVariableMismatch)
            return concretization!![name]!!
        } else if (type is Class<*>) {

            if (type.isArray) {
                if (type.componentType.isArray)
                // no support for multidim arrays
                    throw SynthesisException(SynthesisException.InvalidKindOfType)
                val componentType = getConcretization(type.componentType)
                return Type(ast.newArrayType(componentType.T(), 1), type)
            } else if (type.isPrimitive) {
                return Type(ast.newPrimitiveType(PrimitiveType.toCode(type.simpleName)), type)
            } else {
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
        if (!ClassUtils.isAssignable(type.C(), this.C()))
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
        if (!ClassUtils.isAssignable(type, this.c))
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
            if (t!!.isArrayType && !c.isArray)
                throw SynthesisException(SynthesisException.InvalidKindOfType)
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
            concretization!![name] = type
        }
    }

    internal fun getClass(type: org.eclipse.jdt.core.dom.Type): Class<*> {
        val binding = type.resolveBinding()
        if (type.isPrimitiveType)
            return primitiveToClass[(type as PrimitiveType).primitiveTypeCode]!!
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
        } else if (type.isArrayType) {
            if (binding != null)
                return Environment.getClass(binding.erasure.qualifiedName)
            else {
                val elementType = (type as ArrayType).elementType
                val name = StringBuilder()
                if (elementType.isPrimitiveType) {
                    name.append(primitiveToString[(elementType as PrimitiveType).primitiveTypeCode])
                } else if (elementType.isSimpleType) {
                    name.append((elementType as SimpleType).name.fullyQualifiedName)
                } else if (elementType.isParameterizedType) {
                    name.append(((elementType as ParameterizedType).getType() as SimpleType).name.fullyQualifiedName)
                } else
                    throw SynthesisException(SynthesisException.InvalidKindOfType)
                for (i in type.dimensions downTo 1)
                    name.append("[]") // add "[]" to denote array type dimension
                return Environment.getClass(name.toString())
            }
        } else
            throw SynthesisException(SynthesisException.InvalidKindOfType)
    }

    // make a DOM type independent of its bindings, because when copying subtrees bindings don't copy over
    internal fun releaseBinding(type: org.eclipse.jdt.core.dom.Type, ast: AST): org.eclipse.jdt.core.dom.Type {
        val binding = type.resolveBinding()
        if (type.isPrimitiveType)
            return ast.newPrimitiveType((type as PrimitiveType).primitiveTypeCode)
        else if (type.isSimpleType)
            return ast.newSimpleType(ast.newName(binding.qualifiedName))
        else if (binding.isParameterizedType) {
            val erasure = binding.erasure
            val baseType = ast.newSimpleType(ast.newName(erasure.qualifiedName))
            val retType = ast.newParameterizedType(baseType)

            for (o in (type as ParameterizedType).typeArguments()) {
                val arg = o as org.eclipse.jdt.core.dom.Type
                val argType = releaseBinding(arg, ast)
                retType.typeArguments().add(argType)
            }

            return retType
        } else if (type.isArrayType) {
            val arrayType = type as ArrayType
            val elementType = releaseBinding(arrayType.elementType, ast)
            val dimensions = arrayType.dimensions
            return ast.newArrayType(elementType, dimensions)
        } else {
            throw SynthesisException(SynthesisException.InvalidKindOfType)
        }
    }

    fun simpleT(ast: AST): org.eclipse.jdt.core.dom.Type {
        if (t!!.isPrimitiveType)
            return t!!
        if (t!!.isSimpleType || t!!.isQualifiedType) {
            val name = if (t!!.isSimpleType) (t as SimpleType).name else (t as QualifiedType).name
            val simple: SimpleName
            if (name.isSimpleName)
                simple = ast.newSimpleName((name as SimpleName).identifier)
            else
                simple = ast.newSimpleName((name as QualifiedName).name.identifier)
            return ast.newSimpleType(simple)
        }
        if (t!!.isParameterizedType) {
            val baseType = (t as ParameterizedType).getType()
            val name = if (baseType.isSimpleType()) (baseType as SimpleType).name else (baseType as QualifiedType).name
            val simple: SimpleName
            if (name.isSimpleName)
                simple = ast.newSimpleName((name as SimpleName).identifier)
            else
                simple = ast.newSimpleName((name as QualifiedName).name.identifier)
            return ast.newSimpleType(simple)
        }
        if (t!!.isArrayType) {
            val elementType = (t as ArrayType).elementType
            val simpleElementType = Type(elementType).simpleT(ast)
            return ast.newArrayType(ASTNode.copySubtree(ast, simpleElementType) as org.eclipse.jdt.core.dom.Type,
                    (t as ArrayType).dimensions)
        }
        throw SynthesisException(SynthesisException.InvalidKindOfType)
    }

    fun addRefCount() {
        refCount += 1
    }

    override fun toString(): String {
        return (if (t != null) t else c).toString()
    }

    /**
     * Checks for equality of two types by comparing the class AND the DOM type
     * @param o the object to compare with
     * @return whether they are equal
     */
    override fun equals(o: Any?): Boolean {
        if (o == null || o !is Type)
            return false
        val type = o as Type?
        return C() == type!!.C() && if (T() == null) type.T() == null else T().subtreeMatch(ASTMatcher(), type.T())
    }

    /**
     * Returns the hash code based only on class.
     * This is fine since equals(t1, t2) => hashCode(t1) == hashCode(t2), but not the other way around.
     * @return this type's hash code
     */
    override fun hashCode(): Int {
        return 7 * C().hashCode()
    }

    companion object {

        internal val primitiveToClass: Map<PrimitiveType.Code, Class<*>>

        //TODO-tanvd check change from primitive to .java
        init {
            val map = HashMap<PrimitiveType.Code, Class<*>>()
            map[PrimitiveType.INT] = Int::class.java
            map[PrimitiveType.LONG] = Long::class.java
            map[PrimitiveType.DOUBLE] = Double::class.java
            map[PrimitiveType.FLOAT] = Float::class.java
            map[PrimitiveType.BOOLEAN] = Boolean::class.java
            map[PrimitiveType.CHAR] = Char::class.java
            map[PrimitiveType.BYTE] = Byte::class.java
            map[PrimitiveType.VOID] = Void.TYPE
            map[PrimitiveType.SHORT] = Short::class.java
            primitiveToClass = Collections.unmodifiableMap<PrimitiveType.Code, Class<*>>(map)
        }

        internal val primitiveToString: Map<PrimitiveType.Code, String>

        init {
            val map = HashMap<PrimitiveType.Code, String>()
            map[PrimitiveType.INT] = "int"
            map[PrimitiveType.LONG] = "long"
            map[PrimitiveType.DOUBLE] = "double"
            map[PrimitiveType.FLOAT] = "float"
            map[PrimitiveType.BOOLEAN] = "boolean"
            map[PrimitiveType.CHAR] = "char"
            map[PrimitiveType.BYTE] = "byte"
            map[PrimitiveType.VOID] = "void"
            map[PrimitiveType.SHORT] = "short"
            primitiveToString = Collections.unmodifiableMap<PrimitiveType.Code, String>(map)
        }
    }
}
