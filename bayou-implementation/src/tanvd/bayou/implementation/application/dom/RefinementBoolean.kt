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

import org.eclipse.jdt.core.dom.BooleanLiteral
import org.eclipse.jdt.core.dom.Expression
import org.eclipse.jdt.core.dom.Name

class RefinementBoolean(e: Expression) : Refinement() {
    internal var exists: Boolean = false
    internal var value: Boolean = false

    init {
        if (!knownConstants(e) && e is BooleanLiteral) {
            this.exists = true
            this.value = e.booleanValue()
        }
    }

    private fun knownConstants(e: Expression): Boolean {
        if (e !is Name)
            return false
        val s = e.fullyQualifiedName
        if (Visitor.V().options.KNOWN_CONSTANTS_BOOLEAN.containsKey(s)) {
            this.exists = true
            this.value = Visitor.V().options.KNOWN_CONSTANTS_BOOLEAN[s]!!
            return true
        }
        return false
    }
}
