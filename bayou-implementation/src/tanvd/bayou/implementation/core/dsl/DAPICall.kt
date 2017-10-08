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
package tanvd.bayou.implementation.core.dsl


import org.eclipse.jdt.core.dom.ASTNode
import org.eclipse.jdt.core.dom.Assignment
import org.eclipse.jdt.core.dom.IMethodBinding
import org.eclipse.jdt.core.dom.SimpleName
import tanvd.bayou.implementation.core.synthesizer.Environment
import tanvd.bayou.implementation.core.synthesizer.SynthesisException
import tanvd.bayou.implementation.core.synthesizer.Type
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.lang.reflect.TypeVariable
import java.util.*

class DAPICall : DASTNode {

    internal var node = "DAPICall"
    internal var _call: String
    @Transient
    var retVarName = ""
        internal set

    /* CAUTION: This field is only available during AST generation */
    @Transient lateinit var methodBinding: IMethodBinding
    @Transient internal var linenum: Int = 0
    /* CAUTION: These fields are only available during synthesis (after synthesize(...) is called) */
    @Transient lateinit var method: Method
    @Transient internal var constructor: Constructor<*>? = null

    private val className: String
        get() {
            val cls = methodBinding.declaringClass
            var className = cls.qualifiedName
            if (cls.isGenericType)
                className += "<" + cls.typeParameters.joinToString(",") { t -> (if (t.isTypeVariable) "Tau_" else "") + t.name } + ">"
            return className
        }

    private val signature: String
        get() {
            return methodBinding.name + "(" +
            methodBinding.parameterTypes.joinToString(",") { t -> (if (t.isTypeVariable) "Tau_" else "") + t.qualifiedName } +
            ")"
        }

    private/* get the type-erased name */// generic type variable
            // first bound is the class
            /* find the method in the class *//* .. or the constructor */ val constructorOrMethod: Executable
        @Throws(SynthesisException::class)
        get() {
            val qualifiedName = _call.substring(0, _call.indexOf("("))
            val args = _call.substring(_call.indexOf("(") + 1, _call.lastIndexOf(")")).split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            val className = qualifiedName.substring(0, qualifiedName.lastIndexOf("."))
            val methodName = qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1)
            val erasedClassName = className.replace("<.*>".toRegex(), "")
            val cls = Environment.getClass(erasedClassName)

            val typeVars = cls.typeParameters
            val erasedArgs = ArrayList<String>()
            for (arg in args) {
                if (!arg.startsWith("Tau_")) {
                    erasedArgs.add(arg)
                    continue
                }
                val typeVarName = arg.substring(4)
                var typeVar: TypeVariable<*>? = null
                for (t in typeVars)
                    if (t.name == typeVarName) {
                        typeVar = t
                        break
                    }
                if (typeVar == null)
                    throw SynthesisException(SynthesisException.GenericTypeVariableMismatch)
                val bound = typeVar.bounds[0]
                erasedArgs.add((bound as Class<*>).name)
            }
            val erasedName = erasedClassName + "." + methodName + "(" + erasedArgs.joinToString(",") + ")"
            for (m in cls.methods) {
                var name: String? = null
                for (s in m.toString().split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                    if (s.contains("(")) {
                        name = s
                        break
                    }
                if (name != null && name.replace("\\$".toRegex(), ".") == erasedName)
                    return m
            }
            val _callC = erasedClassName + erasedName.substring(erasedName.indexOf("("))
            for (c in cls.constructors) {
                var name: String? = null
                for (s in c.toString().split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                    if (s.contains("(")) {
                        name = s
                        break
                    }
                if (name != null && name.replace("\\$".toRegex(), ".") == _callC)
                    return c
            }

            throw SynthesisException(SynthesisException.MethodOrConstructorNotFound)
        }

    /* TODO: Add refinement types (predicates) here */

    constructor() {
        this._call = ""
        this.node = "DAPICall"
    }

    constructor(methodBinding: IMethodBinding, linenum: Int) {
        this.methodBinding = methodBinding
        this._call = className + "." + signature
        this.linenum = linenum
        this.node = "DAPICall"
    }

    @Throws(DASTNode.TooManySequencesException::class, DASTNode.TooLongSequenceException::class)
    override fun updateSequences(soFar: MutableList<Sequence>, max: Int, max_length: Int) {
        if (soFar.size >= max)
            throw DASTNode.TooManySequencesException()
        for (sequence in soFar) {
            sequence.addCall(_call)
            if (sequence.calls.size > max_length)
                throw DASTNode.TooLongSequenceException()
        }
    }

    override fun numStatements(): Int {
        return 1
    }

    override fun numLoops(): Int {
        return 0
    }

    override fun numBranches(): Int {
        return 0
    }

    override fun numExcepts(): Int {
        return 0
    }

    override fun bagOfAPICalls(): Set<DAPICall> {
        val bag = HashSet<DAPICall>()
        bag.add(this)
        return bag
    }

    override fun exceptionsThrown(): Set<Class<*>> {
        return if (constructor != null)
            HashSet(Arrays.asList<Class<*>>(*constructor!!.exceptionTypes))
        else
            HashSet(Arrays.asList(*method.exceptionTypes))
    }

    override fun exceptionsThrown(eliminatedVars: Set<String>): Set<Class<*>> {
        return if (!eliminatedVars.contains(this.retVarName))
            this.exceptionsThrown()
        else
            HashSet()
    }

    override fun equals(o: Any?): Boolean {
        if (o == null || o !is DAPICall)
            return false
        val apiCall = o as DAPICall?
        return _call == apiCall!!._call
    }

    override fun hashCode(): Int {
        return _call.hashCode()
    }

    override fun toString(): String {
        return _call
    }


    @Throws(SynthesisException::class)
    override fun synthesize(env: Environment): ASTNode {
        val executable = constructorOrMethod
        if (executable is Constructor<*>) {
            constructor = executable
            return synthesizeClassInstanceCreation(env)
        } else {
            method = executable as Method
            return synthesizeMethodInvocation(env)
        }
    }

    @Throws(SynthesisException::class)
    private fun synthesizeClassInstanceCreation(env: Environment): Assignment {
        val ast = env.ast()
        val creation = ast.newClassInstanceCreation()

        /* constructor type */
        val type = Type(constructor!!.declaringClass)
        type.concretizeType(env)
        creation.type = type.T()

        /* constructor arguments */
        for (gType in constructor!!.genericParameterTypes) {
            val argType = type.getConcretization(gType)
            val arg = env.search(argType)
            creation.arguments().add(arg.expression)
        }

        /* constructor return object */
        val ret = env.addVariable(type)

        /* the assignment */
        val assignment = ast.newAssignment()
        assignment.leftHandSide = ret.expression
        assignment.rightHandSide = creation
        assignment.operator = Assignment.Operator.ASSIGN

        // Record the returned variable name
        if (ret.expression is SimpleName)
            this.retVarName = ret.expression.toString()

        return assignment
    }

    @Throws(SynthesisException::class)
    private fun synthesizeMethodInvocation(env: Environment): ASTNode {
        val ast = env.ast()
        val invocation = ast.newMethodInvocation()

        /* method name */
        val metName = ast.newSimpleName(method.name)
        invocation.name = metName

        /* object on which method is invoked */
        val `object` = env.search(Type(method.declaringClass))
        invocation.expression = `object`.expression

        /* concretizeType method argument types using the above object and search for them */
        for (gType in method.genericParameterTypes) {
            val argType = `object`.type.getConcretization(gType)
            val arg = env.search(argType)
            invocation.arguments().add(arg.expression)
        }

        if (method.returnType == Void.TYPE)
            return invocation

        /* method return value */
        val retType = `object`.type.getConcretization(method.genericReturnType)
        val ret = env.addVariable(retType)

        /* the assignment */
        val assignment = ast.newAssignment()
        assignment.leftHandSide = ret.expression
        assignment.rightHandSide = invocation
        assignment.operator = Assignment.Operator.ASSIGN

        return assignment
    }
}
