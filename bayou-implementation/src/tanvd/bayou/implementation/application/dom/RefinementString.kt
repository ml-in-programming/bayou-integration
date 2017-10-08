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

import org.eclipse.jdt.core.dom.Expression
import org.eclipse.jdt.core.dom.Name
import org.eclipse.jdt.core.dom.StringLiteral

import java.util.regex.Pattern

class RefinementString(e: Expression) : Refinement() {
    internal var exists: Boolean = false
    internal var length: Int = 0
    internal var containsPunct: Boolean = false

    init {
        if (!knownConstants(e) && e !is StringLiteral) {
            val s = (e as StringLiteral).literalValue
            this.exists = true
            this.length = s.length
            this.containsPunct = hasPunct(s)
        }
    }

    private fun knownConstants(e: Expression): Boolean {
        if (e !is Name)
            return false
        val s = e.fullyQualifiedName
        if (Visitor.V().options.KNOWN_CONSTANTS_STRING.containsKey(s)) {
            val v = Visitor.V().options.KNOWN_CONSTANTS_STRING[s]
            this.exists = true
            this.length = v!!.length
            this.containsPunct = hasPunct(v)

            return true
        }
        return false
    }

    private fun hasPunct(s: String): Boolean {
        val p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE)
        return p.matcher(s).find()
    }
}
