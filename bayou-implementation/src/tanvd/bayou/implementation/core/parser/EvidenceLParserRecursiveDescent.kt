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

import tanvd.bayou.implementation.core.lexer.*

import java.util.ArrayList
import java.util.Collections
import java.util.LinkedList

/**
 * A recursive descent implementation of EvidenceLParser.
 */
class EvidenceLParserRecursiveDescent : EvidenceLParser {
    @Throws(ParseException::class)
    override fun parse(tokens: Iterable<Token>): SourceUnitNode {
        return parse(TokenStream(tokens.iterator()))
    }

    // consumes tokens from tokens to create a SourceUnitNode
    @Throws(ParseException::class)
    private fun parse(tokens: TokenStream): SourceUnitNode {
        if (tokens.isEmpty)
            return SourceUnitNode.make(emptyList())

        val elements = LinkedList<EvidenceElement>()
        while (!tokens.isEmpty) {
            val element = makeEvidenceElement(tokens)
            elements.add(element)
        }

        return SourceUnitNode.make(elements)
    }

    // consumes tokens from tokens to create a EvidenceElement
    @Throws(ParseException::class)
    private fun makeEvidenceElement(tokens: TokenStream): EvidenceElement {
        return if (isAtStartOfTypeIdentifier(tokens)) {
            makeEvidenceElementWithTypeIdentifier(tokens)
        } else {
            makeEvidenceElementWithoutTypeIdentifier(tokens)
        }
    }

    // consumes tokens from tokens to create an EvidenceElementWithoutTypeIdentifierNode
    @Throws(ParseException::class)
    private fun makeEvidenceElementWithoutTypeIdentifier(tokens: TokenStream): EvidenceElementWithoutTypeIdentifierNode {
        val list = makeIdentifierList(tokens)
        return EvidenceElementWithoutTypeIdentifierNode.make(list)
    }

    // consumes tokens from tokens to create an EvidenceElementWithTypeIdentifierNode
    @Throws(ParseException::class)
    private fun makeEvidenceElementWithTypeIdentifier(tokens: TokenStream): EvidenceElementWithTypeIdentifierNode {
        val typeIdentifier = makeTypeIdentifier(tokens)
        val list = makeIdentifierList(tokens)

        return EvidenceElementWithTypeIdentifierNode.make(typeIdentifier, list)

    }

    // consumes tokens from tokens to create an IdentifierListNode
    @Throws(ParseException::class)
    private fun makeIdentifierList(tokens: TokenStream): IdentifierListNode {
        if (tokens.isEmpty)
            throw UnexpectedEndOfTokens()

        val idents = ArrayList<IdentifierNode>()
        idents.add(makeIdentifierNode(tokens))

        while (!tokens.isEmpty && tokens.peek().type is TokenTypeComma) {
            tokens.pop()
            idents.add(makeIdentifierNode(tokens))
        }

        return IdentifierListNode.make(idents)

    }

    // consumes tokens from tokens to create an IdentifierNode
    @Throws(UnexpectedEndOfTokens::class, UnexpectedTokenException::class)
    private fun makeIdentifierNode(tokens: TokenStream): IdentifierNode {
        if (tokens.isEmpty)
            throw UnexpectedEndOfTokens()

        val token = tokens.pop()

        return token.type.match(object : TokenTypeCases<IdentifierNode, UnexpectedTokenException> {
            override fun forIdentifier(identifier: TokenTypeIdentifier): IdentifierNode {
                return IdentifierNode.make(token.lexeme)
            }

            @Throws(UnexpectedTokenException::class)
            override fun forColon(colon: TokenTypeColon): IdentifierNode {
                throw UnexpectedTokenException(token)
            }

            @Throws(UnexpectedTokenException::class)
            override fun forComma(comma: TokenTypeComma): IdentifierNode {
                throw UnexpectedTokenException(token)
            }
        })
    }

    // consumes tokens from tokens to create a TypeIdentifierNode
    @Throws(UnexpectedEndOfTokens::class, UnexpectedTokenException::class)
    private fun makeTypeIdentifier(tokens: TokenStream): TypeIdentifierNode {
        if (tokens.isEmpty)
            throw UnexpectedEndOfTokens()

        val first = tokens.pop()

        return first.type.match(object : TokenTypeCases<TypeIdentifierNode, UnexpectedTokenException> {
            @Throws(UnexpectedTokenException::class)
            override fun forIdentifier(identifier: TokenTypeIdentifier): TypeIdentifierNode {
                val second = tokens.pop()
                return second.type.match(object : TokenTypeCases<TypeIdentifierNode, UnexpectedTokenException> {
                    @Throws(UnexpectedTokenException::class)
                    override fun forIdentifier(identifier: TokenTypeIdentifier): TypeIdentifierNode {
                        throw UnexpectedTokenException(second)
                    }

                    override fun forColon(colon: TokenTypeColon): TypeIdentifierNode {
                        return TypeIdentifierNode.make(first.lexeme)
                    }

                    @Throws(UnexpectedTokenException::class)
                    override fun forComma(comma: TokenTypeComma): TypeIdentifierNode {
                        throw UnexpectedTokenException(second)
                    }
                })
            }

            @Throws(UnexpectedTokenException::class)
            override fun forColon(colon: TokenTypeColon): TypeIdentifierNode {
                throw UnexpectedTokenException(first)
            }

            @Throws(UnexpectedTokenException::class)
            override fun forComma(comma: TokenTypeComma): TypeIdentifierNode {
                throw UnexpectedTokenException(first)
            }
        })
    }

    // tests whether at least two tokens remain and the next two are TokenTypeIdentifier TokenTypeColon
    private fun isAtStartOfTypeIdentifier(tokens: TokenStream): Boolean {
        return if (!tokens.hasNext()) false else tokens.peek().type.match(object : TokenTypeCases<Boolean, RuntimeException> {
            override fun forIdentifier(identifier: TokenTypeIdentifier): Boolean {
                return tokens.lookAhead().type.match(object : TokenTypeCases<Boolean, RuntimeException> {
                    override fun forIdentifier(identifier: TokenTypeIdentifier): Boolean {
                        return false
                    }

                    override fun forColon(colon: TokenTypeColon): Boolean {
                        return true
                    }

                    override fun forComma(comma: TokenTypeComma): Boolean {
                        return false
                    }
                })
            }

            override fun forColon(colon: TokenTypeColon): Boolean {
                return false
            }

            override fun forComma(comma: TokenTypeComma): Boolean {
                return false
            }
        })

    }

}
