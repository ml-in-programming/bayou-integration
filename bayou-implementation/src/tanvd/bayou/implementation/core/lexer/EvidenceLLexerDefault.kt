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
package tanvd.bayou.implementation.core.lexer

import java.util.ArrayList
import java.util.Collections

class EvidenceLLexerDefault : EvidenceLLexer {
    override fun lex(chars: Iterable<Char>): Iterable<Token> {
        return lex(chars.iterator())
    }

    private fun lex(chars: Iterator<Char>): Iterable<Token> {
        if (!chars.hasNext())
            return emptyList()

        var current: Char? = chars.next() ?: throw IllegalArgumentException("chars may not contain null")

        var next: Char? = null
        run {
            if (chars.hasNext()) {
                next = chars.next()
                if (next == null)
                    throw IllegalArgumentException("chars may not contain null")
            } else {
                next = null
            }
        }

        val tokensAccum = ArrayList<Token>()

        var lexemeAccum = StringBuilder()
        while (current != null) {
            if (!Character.isWhitespace(current))
                lexemeAccum.append(current)

            if (next == null || next == ':' || next == ',' || Character.isWhitespace(next!!)) {
                val lexeme = lexemeAccum.toString()
                lexemeAccum = StringBuilder()
                appendTokenIfNotWhitespace(lexeme, tokensAccum)
            } else if (current == ':') {
                lexemeAccum = StringBuilder()
                appendTokenIfNotWhitespace(":", tokensAccum)
            } else if (current == ',') {
                lexemeAccum = StringBuilder()
                appendTokenIfNotWhitespace(",", tokensAccum)
            }

            current = next
            if (chars.hasNext()) {
                next = chars.next()
                if (next == null)
                    throw IllegalArgumentException("chars may not contain null")
            } else {
                next = null
            }
        }

        return tokensAccum
    }

    private fun appendTokenIfNotWhitespace(lexeme: String, tokensAccum: ArrayList<Token>) {
        if (lexeme.trim { it <= ' ' }.isEmpty())
            return

        when (lexeme) {
            ":" -> tokensAccum.add(Token.make(":", TokenTypeColon()))
            "," -> tokensAccum.add(Token.make(",", TokenTypeComma()))
            else -> tokensAccum.add(Token.make(lexeme, TokenTypeIdentifier()))
        }
    }


}
