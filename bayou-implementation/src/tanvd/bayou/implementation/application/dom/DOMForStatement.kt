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
package tanvd.bayou.implementation.application.dom


import tanvd.bayou.implementation.core.dsl.DLoop
import tanvd.bayou.implementation.core.dsl.DSubTree
import org.eclipse.jdt.core.dom.Expression
import org.eclipse.jdt.core.dom.ForStatement

class DOMForStatement(internal val statement: ForStatement) : Handler {

    override fun handle(): DSubTree {
        val tree = DSubTree()

        for (o in statement.initializers()) {
            val init = DOMExpression(o as Expression).handle()
            tree.addNodes(init.nodes)
        }
        val cond = DOMExpression(statement.expression).handle()
        val body = DOMStatement(statement.body).handle()
        for (o in statement.updaters()) {
            val update = DOMExpression(o as Expression).handle()
            body.addNodes(update.nodes) // updaters are part of body
        }

        val loop = cond.isValid

        if (loop)
            tree.addNode(DLoop(cond.nodesAsCalls, body.nodes))
        else {
            // only one of these will add nodes
            tree.addNodes(cond.nodes)
            tree.addNodes(body.nodes)
        }

        return tree
    }
}
