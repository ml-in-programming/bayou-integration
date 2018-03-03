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
package tanvd.bayou.implementation.model.configurable.synthesizer

import java.util.HashSet

/**
 * A scope of variables for the synthesizer to work with
 */
class Scope {
    /**
     * The set of variables in the scope
     */
    /**
     * Gets the set of variables in the current scope
     *
     * @return set of variables
     */
    var variables: MutableSet<Variable>? = null
        private set

    /**
     * The set of phantom variables in the scope. Phantom variables are those
     * that cannot be referenced during synthesis because they don't appear in
     * the current scope. But they need to be added to the method's variable
     * declarations because they're used in some inner scope.
     */
    private var phantomVariables: MutableSet<Variable>? = null

    /**
     * Initializes the scope
     *
     * @param variables variables present in the scope
     */
    constructor(variables: List<Variable>) {
        this.variables = HashSet(variables)
        this.phantomVariables = HashSet()
    }

    /**
     * Initializes the scope from another scope
     *
     * @param scope scope whose variables are used for initialization
     */
    constructor(scope: Scope) {
        this.variables = HashSet(scope.variables!!)
        this.phantomVariables = HashSet(scope.phantomVariables!!)
    }

    /**
     * Gets the set of phantom variables in the current scope
     *
     * @return set of phantom variables
     */
    fun getPhantomVariables(): Set<Variable>? {
        return phantomVariables
    }

    /**
     * Adds the given variable, whose properties must already be set, to the current scope
     *
     * @param var the variable to be added
     */
    fun addVariable(`var`: Variable) {
        val uniqueName = makeUnique(`var`.name!!)
        `var`.refactor(uniqueName!!)
        variables!!.add(`var`)
    }

    /**
     * Adds a variable with the given type and properties to the current scope
     *
     * @param type       variable type, from which a variable name will be derived
     * @param properties variable properties
     * @return a TypedExpression with a simple name (variable name) and variable type
     */
    fun addVariable(type: Type, properties: VariableProperties): Variable {
        // construct a nice name for the variable
        val name = createNameFromType(type)

        // add variable to scope and return it
        val uniqueName = makeUnique(name)
        val `var` = Variable(uniqueName!!, type, properties)
        variables!!.add(`var`)
        return `var`
    }

    /**
     * Creates a pretty name from a type
     *
     * @param type type from which name is created
     * @return the pretty name
     */
    private fun createNameFromType(type: Type): String {
        val name = type.C().canonicalName
        val sb = StringBuilder()
        for (c in name.toCharArray())
            if (Character.isUpperCase(c))
                sb.append(c)
        val prettyName = sb.toString().toLowerCase()
        return if (prettyName == "") name.substring(0, 1) else prettyName
    }

    /**
     * Make a given name unique in the current scope by appending an incrementing id to it
     *
     * @param name name that has to be made unique
     * @return the unique name
     */
    private fun makeUnique(name: String): String? {
        val existingNames = HashSet<String?>()
        for (`var` in variables!!)
            existingNames.add(`var`.name)
        for (`var` in phantomVariables!!)
            existingNames.add(`var`.name)

        var i: Int
        i = 1
        while (i < 9999) {
            if (!existingNames.contains(name!! + i))
                return name + i
            i++
        }
        return null
    }

    /**
     * Join a list of sub-scopes into this scope.
     * In the join operation, variables declared in ALL sub-scopes will be added to this scope.
     * Variables declared only in some sub-scopes will be added as phantom variables to this scope.
     * Variables that have their "join" flag set to false (e.g., catch clause vars) will be discarded.
     * Finally, variables will be refactored if necessary.
     *
     * @param subScopes list of sub-scopes that have to be joined into this scope
     */
    fun join(subScopes: List<Scope>) {
        val common = HashSet(subScopes[0].variables!!)
        for (subScope in subScopes)
            common.retainAll(subScope.variables!!)
        common.removeAll(variables!!)
        for (`var` in common)
            if (`var`.isJoinVar)
                variables!!.add(`var`)

        val varNames = HashSet<String>()
        val toRefactor = HashSet<Variable>()

        for (subScope in subScopes) {
            val uncommon = subScope.variables
            uncommon!!.removeAll(variables!!)
            uncommon.removeAll(common)
            for (`var` in uncommon) {
                if (!`var`.isJoinVar)
                    continue

                // Check if another scope added a variable with the same name, and refactor if so.
                // Note: if the other variable also had the same type (which is fine), it would've
                // been added to "common" above, and then to the current variables in scope.
                if (varNames.contains(`var`.name))
                    toRefactor.add(`var`)

                varNames.add(`var`.name!!)
                phantomVariables!!.add(`var`)
            }

            for (`var` in subScope.getPhantomVariables()!!) {
                if (phantomVariables!!.contains(`var`))
                    continue
                if (varNames.contains(`var`.name))
                    toRefactor.add(`var`)
                varNames.add(`var`.name!!)
                phantomVariables!!.add(`var`)
            }
        }

        for (`var` in toRefactor)
            `var`.refactor(makeUnique(`var`.name!!)!!)
    }
}
