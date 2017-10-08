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

import tanvd.bayou.implementation.core.dsl.DAPICall
import tanvd.bayou.implementation.core.dsl.DSubTree
import org.eclipse.jdt.core.dom.Expression
import org.eclipse.jdt.core.dom.IMethodBinding
import org.eclipse.jdt.core.dom.MethodInvocation

class DOMMethodInvocation(internal val invocation: MethodInvocation) : Handler {

    override fun handle(): DSubTree {
        val tree = DSubTree()
        // add the expression's subtree (e.g: foo(..).bar() should handle foo(..) first)
        val Texp = DOMExpression(invocation.expression).handle()
        tree.addNodes(Texp.nodes)

        // evaluate arguments first
        for (o in invocation.arguments()) {
            val Targ = DOMExpression(o as Expression).handle()
            tree.addNodes(Targ.nodes)
        }

        var binding: IMethodBinding? = invocation.resolveMethodBinding()
        // get to the generic declaration, if this binding is an instantiation
        while (binding != null && binding.methodDeclaration !== binding)
            binding = binding.methodDeclaration
        val localConstructor = Utils.checkAndGetLocalMethod(binding)
        if (localConstructor != null) {
            val Tconstructor = DOMMethodDeclaration(localConstructor).handle()
            tree.addNodes(Tconstructor.nodes)
        } else if (Utils.isRelevantCall(binding))
            tree.addNode(DAPICall(binding!!, Visitor.V().getLineNumber(invocation)))
        return tree
    }
}

