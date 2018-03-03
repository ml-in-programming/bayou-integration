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

import org.eclipse.jdt.core.dom.*

import java.util.HashSet

/**
 * A variable in the synthesizer's type system
 */
class Variable
/**
 * Initializes a variable with the given parameters
 *
 * @param name       variable name
 * @param type       variable type
 * @param properties variable properties
 */
internal constructor(name: String,
        /**
         * Type of the variable
         */
                     /**
                      * Gets the variable type
                      *
                      * @return variable type
                      */
                     val type: Type,
                     /**
                      * Properties of this variable
                      */
                     private val properties: VariableProperties) {

    /**
     * Name of the variable
     */
    /**
     * Gets the variable name
     *
     * @return variable name
     */
    var name: String? = null
        private set

    /**
     * Reference count of the variable (used for cost metric)
     */
    /**
     * Gets the reference counter of this variable
     *
     * @return current value
     */
    var refCount: Int = 0
        private set

    /**
     * Set of AST node references of this variable. If/when the variable is refactored, these
     * AST nodes will be updated. Nodes are automatically added when createASTNode() is called.
     */
    private val astNodeRefs: MutableSet<SimpleName>

    /**
     * Checks if this variable can participate in joins
     *
     * @return current value
     */
    val isJoinVar: Boolean
        get() = properties.join

    /**
     * Checks if this variable is a user-defined variable
     *
     * @return current value
     */
    val isUserVar: Boolean
        get() = properties.userVar

    /**
     * Checks if a default initializer needs to be synthesized for this variable
     *
     * @return current value
     */
    val isDefaultInit: Boolean
        get() = properties.defaultInit

    /**
     * Checks if this variable is a single use variable
     *
     * @return current value
     */
    val isSingleUseVar: Boolean
        get() = properties.singleUse

    init {
        this.name = name
        this.refCount = 0
        this.astNodeRefs = HashSet()
    }

    /**
     * Increments the reference counter of this variable
     */
    fun addRefCount() {
        refCount += 1
    }

    /**
     * Creates and associates an AST node (of type SimpleName) referring to this variable
     *
     * @param ast the owner of the node
     * @return the AST node corresponding to this variable
     */
    fun createASTNode(ast: AST): SimpleName {
        val node = ast.newSimpleName(name!!)
        astNodeRefs.add(node)
        return node
    }

    /**
     * Refactors this variable's name and updates all AST nodes associated with this variable.
     * It is the responsibility of the refactoring method to ensure the name is unique wherever
     * this variable is referenced. Note: a variable's type cannot be refactored.
     *
     * @param newName the new name of this variable
     */
    fun refactor(newName: String) {
        this.name = newName
        for (node in astNodeRefs)
            node.identifier = newName
    }

    /**
     * Creates a default initializer expression for this variable
     *
     * @param ast the owner of the expression
     * @return expression that initializes this variable
     */
    fun createDefaultInitializer(ast: AST): Expression {
        val cast = ast.newCastExpression()
        cast.type = type.simpleT(ast)

        val invocation = ast.newMethodInvocation()
        invocation.expression = ast.newSimpleName("Bayou")
        invocation.name = ast.newSimpleName("\$init")
        cast.expression = invocation

        return cast
    }

    /**
     * Compares two variables based on their name AND type
     *
     * @param o the object to compare with
     * @return whether they are equal
     */
    override fun equals(o: Any?): Boolean {
        if (o == null || o !is Variable)
            return false
        val v = o as Variable?
        return v!!.name == name && v.type == type
    }

    override fun hashCode(): Int {
        return 7 * name!!.hashCode() + 17 * type.hashCode()
    }

    /**
     * Returns a string representation of this variable (for debug purposes only)
     *
     * @return string
     */
    override fun toString(): String {
        return name + ":" + type
    }
}
