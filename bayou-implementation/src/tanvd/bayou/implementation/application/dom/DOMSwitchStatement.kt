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

import tanvd.bayou.implementation.core.dsl.DASTNode
import tanvd.bayou.implementation.core.dsl.DBranch
import tanvd.bayou.implementation.core.dsl.DSubTree
import org.eclipse.jdt.core.dom.Statement
import org.eclipse.jdt.core.dom.SwitchStatement

import java.util.ArrayList

class DOMSwitchStatement(internal val statement: SwitchStatement) : Handler {

    internal var tree = DSubTree()

    internal var DBodies = ArrayList<DSubTree>()
    internal var bodies = ArrayList<List<DASTNode>>()
    internal var nodeType = ArrayList<Int>()


    private fun BuildTree(Sexpr: DSubTree, itPos: Int): List<DASTNode> {
        val bodyPrev = ArrayList<DASTNode>()
        val bodyNext: List<DASTNode>
        val caseNodes = ArrayList<DBranch>()
        for (it1 in itPos until bodies.size) {
            val Dbody = DBodies[it1]
            val typePrev = nodeType[it1]
            if (typePrev == 49) {//checks for 'case' statement
                bodyNext = BuildTree(Sexpr, it1 + 1)
                val caseNode = DBranch(Sexpr.nodesAsCalls, bodyPrev, bodyNext)
                caseNodes.add(caseNode)
                return caseNodes
            } else {
                bodyPrev.addAll(bodies[it1])
            }

        }

        return bodyPrev
    }


    override fun handle(): DSubTree {


        val Sexpr = DOMExpression(statement.expression).handle()
        var branch = Sexpr.isValid

        for (o in statement.statements()) {
            val type = (o as Statement).nodeType
            nodeType.add(type)
            val body = DOMStatement(o).handle()
            bodies.add(body.nodes)
            DBodies.add(body)
            if (type != 49)
            //excludes 'case' statement
                branch = branch or body.isValid
        }


        if (branch) {

            var switchNodes: List<DASTNode> = ArrayList()
            switchNodes = BuildTree(Sexpr, 1)
            tree.addNode(switchNodes[0])
        } else {
            // only one  will add nodes, the rest will add nothing
            tree.addNodes(Sexpr.nodes)
            val iter = statement.statements().iterator()
            while (iter.hasNext()) {
                val o = iter.next()
                val body = DOMStatement(o as Statement).handle()
                tree.addNodes(body.nodes)
            }
        }


        return tree
    }
}


