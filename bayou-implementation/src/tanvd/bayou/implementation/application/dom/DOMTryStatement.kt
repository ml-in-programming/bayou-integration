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

import tanvd.bayou.implementation.core.dsl.DExcept
import tanvd.bayou.implementation.core.dsl.DSubTree
import org.eclipse.jdt.core.dom.CatchClause
import org.eclipse.jdt.core.dom.TryStatement

class DOMTryStatement(internal val statement: TryStatement) : Handler {

    override fun handle(): DSubTree {
        val tree = DSubTree()

        // restriction: considering only the first catch clause
        val Ttry = DOMBlock(statement.body).handle()
        val Tcatch: DSubTree
        if (!statement.catchClauses().isEmpty())
            Tcatch = DOMCatchClause(statement.catchClauses()[0] as CatchClause).handle()
        else
            Tcatch = DSubTree()
        val Tfinally = DOMBlock(statement.finally).handle()

        val except = Ttry.isValid && Tcatch.isValid

        if (except)
            tree.addNode(DExcept(Ttry.nodes, Tcatch.nodes))
        else {
            // only one of these will add nodes
            tree.addNodes(Ttry.nodes)
            tree.addNodes(Tcatch.nodes)
        }

        tree.addNodes(Tfinally.nodes)

        return tree
    }
}
