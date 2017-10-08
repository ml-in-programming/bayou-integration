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
import org.eclipse.jdt.core.dom.*

class DOMStatement(internal val statement: Statement) : Handler {

    override fun handle(): DSubTree {
        if (statement is Block)
            return DOMBlock(statement).handle()
        if (statement is ExpressionStatement)
            return DOMExpressionStatement(statement).handle()
        if (statement is IfStatement)
            return DOMIfStatement(statement).handle()
        if (statement is SwitchStatement)
            return DOMSwitchStatement(statement).handle()
        if (statement is SwitchCase)
            return DOMSwitchCase(statement).handle()
        if (statement is DoStatement)
            return DOMDoStatement(statement).handle()
        if (statement is ForStatement)
            return DOMForStatement(statement).handle()
        if (statement is EnhancedForStatement)
            return DOMEnhancedForStatement(statement).handle()
        if (statement is WhileStatement)
            return DOMWhileStatement(statement).handle()
        if (statement is TryStatement)
            return DOMTryStatement(statement).handle()
        if (statement is VariableDeclarationStatement)
            return DOMVariableDeclarationStatement(statement).handle()
        if (statement is SynchronizedStatement)
            return DOMSynchronizedStatement(statement).handle()
        if (statement is ReturnStatement)
            return DOMReturnStatement(statement).handle()
        return if (statement is LabeledStatement) DOMLabeledStatement(statement).handle() else DSubTree()

    }
}
