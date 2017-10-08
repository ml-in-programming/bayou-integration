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
package tanvd.bayou.implementation.core.parser

import java.util.ArrayList
import java.util.Collections

/**
 * Models the identifier-list non-terminal.
 *
 * See doc/internal/evidencel_language/1_0/evidencel_language_1_0_grammar.txt
 */
interface IdentifierListNode {
    /**
     * @return the elements of the list
     */
    val identifiers: List<IdentifierNode>

    companion object {

        /**
         * Creates an IdentifierListNode with the given type identifier and ident list.
         *
         * @param idents the identifiers of the list
         * @return a new IdentifierListNode instance
         */
        fun make(idents: ArrayList<IdentifierNode>): IdentifierListNode {

            return object : IdentifierListNode {
                override val identifiers: List<IdentifierNode>
                    get() = Collections.unmodifiableList(idents)
            }
        }
    }
}
