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

class DOMExpression(internal val expression: Expression) : Handler {

    override fun handle(): DSubTree {
        if (expression is MethodInvocation)
            return DOMMethodInvocation(expression).handle()
        if (expression is ClassInstanceCreation)
            return DOMClassInstanceCreation(expression).handle()
        if (expression is InfixExpression)
            return DOMInfixExpression(expression).handle()
        if (expression is PrefixExpression)
            return DOMPrefixExpression(expression).handle()
        if (expression is ConditionalExpression)
            return DOMConditionalExpression(expression).handle()
        if (expression is VariableDeclarationExpression)
            return DOMVariableDeclarationExpression(expression).handle()
        if (expression is Assignment)
            return DOMAssignment(expression).handle()
        return if (expression is ParenthesizedExpression) DOMParenthesizedExpression(expression).handle() else DSubTree()

    }
}
