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


import tanvd.bayou.implementation.core.lexer.Token

/**
 * A sequence of tokens.
 */
internal class TokenStream
/**
 * @param tokens the tokens of the stream.  No element of tokens may be null.
 */
(
        /**
         * Underlying token source.
         */
        private val _tokens: Iterator<Token>) {

    /**
     * The next token in the stream or null if the stream is exhausted.
     */
    private var _head: Token? = null

    /**
     * The token after _head or null if _head is the last token of the stream.
     */
    private var _next: Token? = null

    /**
     * @return true if the stream is exhausted of tokens.
     */
    val isEmpty: Boolean
        get() = _head == null

    init {

        if (!_tokens.hasNext()) {
            _head = null
            _next = null
        } else {
            _next = _tokens.next()
            pop(false) // moves _next to _head and reads the next token to _next
        }
    }

    /**
     * @return returns and removes the next token in the stream
     * @throws IllegalStateException if the stream is exhausted
     * @throws IllegalStateException if a token proved to the stream during construction is null
     */
    fun pop(): Token {
        return pop(true)
    }

    //  returns and removes the next token in the stream
    private fun pop(performHeadNullCheck: Boolean): Token {
        if (performHeadNullCheck && _head == null)
            throw IllegalStateException()

        val toReturn = _head
        _head = _next

        if (!_tokens.hasNext()) {
            _next = null
        } else {
            _next = _tokens.next()
            if (_next == null)
                throw IllegalStateException("_tokens may not contain null")
        }

        return toReturn!!
    }

    /**
     * @return true if the stream has at least two tokens remaining.
     */
    operator fun hasNext(): Boolean {
        return _next != null
    }

    /**
     * @return the next token of the stream without removing it from the stream.
     * @throws IllegalStateException if the stream is exhausted
     */
    fun peek(): Token {
        if (_head == null)
            throw IllegalStateException()

        return _head!!
    }

    /**
     * @return the token after next of the stream without removing any tokens from the stream.
     * @throws IllegalStateException if fewer than two tokens remain in the stream
     */
    fun lookAhead(): Token {
        if (_next == null)
            throw IllegalStateException()

        return _next!!
    }
}
