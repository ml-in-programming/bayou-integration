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

/**
 * Models the evidence-element-with-type-identifier non-terminal.
 *
 * See doc/internal/evidencel_language/1_0/evidencel_language_1_0_grammar.txt
 */
interface EvidenceElementWithTypeIdentifierNode : EvidenceElement {
    /**
     * @return the type identifier part of the evidence.
     */
    val typeIdentifier: TypeIdentifierNode

    override fun <R, T : Throwable> match(cases: EvidenceElementCases<R, T>): R {
        return cases.forWithTypeIdent(this)
    }

    companion object {

        /**
         * Creates a EvidenceElementWithTypeIdentifierNode with the given type identifier and ident list.
         *
         * @param typeIdentifier the type identifier part of the of the EvidenceElementWithoutTypeIdentifierNode
         * @param list the identifier list part of the EvidenceElementWithoutTypeIdentifierNode
         * @return  a new EvidenceElementWithTypeIdentifierNode instance
         */
        fun make(typeIdentifier: TypeIdentifierNode, list: IdentifierListNode): EvidenceElementWithTypeIdentifierNode {
            return object : EvidenceElementWithTypeIdentifierNode {
                override val typeIdentifier: TypeIdentifierNode
                    get() = typeIdentifier

                override val identifierList: IdentifierListNode
                    get() = list
            }
        }
    }
}
