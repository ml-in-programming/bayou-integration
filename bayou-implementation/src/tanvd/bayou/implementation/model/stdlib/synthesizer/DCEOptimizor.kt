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
import tanvd.bayou.implementation.model.stdlib.synthesizer.dsl.DSubTree

import java.util.*

class DCEOptimizor : ASTVisitor() {
    // The def variables
    protected var defs: MutableMap<String, MutableList<ASTNode>>

    // The use variables
    protected var uses: MutableMap<String, MutableList<ASTNode>>

    // The eliminated variable declarations
    var eliminatedVars: MutableSet<String> = HashSet()

    init {
        this.defs = HashMap()
        this.uses = HashMap()
        this.eliminatedVars = HashSet()
    }

    // Apply the optimization here
    fun apply(body: Block, sketch: DSubTree): Block {
        // Collect defs and uses
        collectDefUse(body)

        // Check if def has potential uses
        val tempVars = ArrayList<String>()
        for (def in defs.keys) {
            val defVals = defs[def]
            if (defVals!!.size == 1) {
                if (uses[def] == null) {
                    // No use, then remove this def's corresponding ExpressionStatement from synthesized code block
                    tempVars.add(def)
                }
            }
        }
        // Clean up the infeasible elimination
        for (def in tempVars) {
            val defVals = defs[def]
            val defNode = defVals!![0]

            if (defNode.parent is ExpressionStatement) {
                defNode.parent.delete()
                this.eliminatedVars.add(def)
            } else if (hasLegalParent(defNode)) {
                this.eliminatedVars.add(def)
            }
        }
        // Apply post optimizations to sketch
        sketch.cleanupCatchClauses(this.eliminatedVars)

        return body
    }

    protected fun hasLegalParent(node: ASTNode?): Boolean {
        var node = node
        node = node!!.parent
        while (node != null && (node is ClassInstanceCreation
                        || node is ParenthesizedExpression
                        || node is Assignment)) {
            node = node.parent
        }

        return node != null && node is ExpressionStatement
    }

    // Collect the def and use variables
    protected fun collectDefUse(body: Block) {
        body.accept(this)
    }

    override fun visit(assign: Assignment?): Boolean {
        return true
    }

    override fun visit(name: SimpleName?): Boolean {
        val stmt = getParentStatement(name)
        val varName = name!!.toString()

        var isDef = false
        var parent: ASTNode? = name.parent
        if (parent is Assignment) {
            isDef = (parent.leftHandSide === name
                    && parent.rightHandSide is ClassInstanceCreation
                    && parent.parent != null)
        }
        var isArgAssignment = false
        while (parent != null) {
            if (parent is MethodInvocation || parent is ClassInstanceCreation) {
                isArgAssignment = true
                break
            }
            parent = parent.parent
        }

        if (varName != null && stmt != null) {
            if (isDef && !isArgAssignment)
            // Add variable def
                addToMap(varName, name.parent, defs)
            else
            // Add potential use
                addToMap(varName, stmt, uses)
        }

        return false
    }

    override fun visit(name: QualifiedName?): Boolean {
        val stmt = getParentStatement(name)
        val varName = name!!.toString()

        var isDef = false
        val parent = name.parent
        if (parent is Assignment) {
            isDef = parent.leftHandSide === name
        }

        if (varName != null && stmt != null) {
            if (isDef)
            // Add variable def
                addToMap(varName, stmt, defs)
            else
            // Add potential use
                addToMap(varName, stmt, uses)
        }

        return false
    }

    override fun visit(constInvoke: ConstructorInvocation?): Boolean {
        return true
    }

    // Add variable and its parent to register map
    protected fun addToMap(varName: String, parent: ASTNode, varMap: MutableMap<String, MutableList<ASTNode>>) {
        var values: MutableList<ASTNode>? = varMap[varName]
        if (values == null) {
            values = ArrayList()
            varMap[varName] = values
        }
        values.add(parent)
    }

    // Get the parent statement
    protected fun getParentStatement(expr: Expression?): Statement {
        var node: ASTNode? = expr
        while (node!!.parent !is Statement) {
            node = node.parent
        }

        return node.parent as Statement
    }
}
