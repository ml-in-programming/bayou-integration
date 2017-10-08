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

import tanvd.bayou.implementation.core.dsl.DSubTree
import org.eclipse.jdt.core.dom.SynchronizedStatement

class DOMSynchronizedStatement(internal val statement: SynchronizedStatement) : Handler {

    override fun handle(): DSubTree {
        val tree = DSubTree()

        val Texpr = DOMExpression(statement.expression).handle()
        val Tbody = DOMBlock(statement.body).handle()

        tree.addNodes(Texpr.nodes)
        tree.addNodes(Tbody.nodes)

        return tree
    }
}
