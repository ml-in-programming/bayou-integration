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


import tanvd.bayou.implementation.core.lexer.EvidenceLLexer
import tanvd.bayou.implementation.core.lexer.Token

/**
 * A parser of the EvidenceL language.
 *
 * See doc/internal/evidencel_language/1_0/evidencel_language_1_0_grammar.txt
 */
interface EvidenceLParser {
    /**
     * Parses the given string into a parse tree.
     *
     * @param evidence the string to parse
     * @return the root of the parse tree
     * @throws ParseException if the given string does not conform to the EvidenceL grammar.
     */
    @Throws(ParseException::class)
    fun parse(evidence: String): SourceUnitNode {
        return parse(EvidenceLLexer.makeDefault().lex(evidence))
    }

    /**
     * Parses the given tokens into a parse tree.
     *
     * @param tokens the tokens to parse
     * @return the root of the parse tree
     * @throws ParseException if the given string does not conform to the EvidenceL grammar.
     */
    @Throws(ParseException::class)
    fun parse(tokens: Iterable<Token>): SourceUnitNode

    companion object {

        /**
         * @return an instance of a default EvidenceLParser implementation.
         */
        fun makeDefault(): EvidenceLParser {
            return EvidenceLParserRecursiveDescent()
        }
    }
}
