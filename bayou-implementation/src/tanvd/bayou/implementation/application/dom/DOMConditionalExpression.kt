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


import tanvd.bayou.implementation.core.dsl.DBranch
import tanvd.bayou.implementation.core.dsl.DSubTree
import org.eclipse.jdt.core.dom.ConditionalExpression

class DOMConditionalExpression(internal val expression: ConditionalExpression) : Handler {

    override fun handle(): DSubTree {
        val tree = DSubTree()

        val Tcond = DOMExpression(expression.expression).handle()
        val Tthen = DOMExpression(expression.thenExpression).handle()
        val Telse = DOMExpression(expression.elseExpression).handle()

        val branch = (Tcond.isValid && Tthen.isValid || Tcond.isValid && Telse.isValid
                || Tthen.isValid && Telse.isValid)

        if (branch)
            tree.addNode(DBranch(Tcond.nodesAsCalls, Tthen.nodes, Telse.nodes))
        else {
            // only one of these will add nodes, the rest will add nothing
            tree.addNodes(Tcond.nodes)
            tree.addNodes(Tthen.nodes)
            tree.addNodes(Telse.nodes)
        }
        return tree
    }
}
