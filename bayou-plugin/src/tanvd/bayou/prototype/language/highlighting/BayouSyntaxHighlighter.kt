package tanvd.bayou.prototype.language.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import tanvd.bayou.prototype.language.BayouLexerAdapter
import tanvd.bayou.prototype.language.psi.BayouTypes


class BayouSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer {
        return BayouLexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            BayouTypes.SEPARATOR -> arrayOf(SEPARATOR)
            BayouTypes.API -> arrayOf(KEY)
            BayouTypes.TYPE -> arrayOf(KEY)
            BayouTypes.CONTEXT -> arrayOf(KEY)
            BayouTypes.VALUE -> arrayOf(VALUE)
            BayouTypes.ANDROID -> arrayOf(KEY)
            BayouTypes.STDLIB -> arrayOf(KEY)
            TokenType.BAD_CHARACTER -> arrayOf(BAD_CHARACTER)
            else -> arrayOf()
        }
    }

    companion object {
        val SEPARATOR = createTextAttributesKey("BAYOU_SEPARATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val KEY = createTextAttributesKey("BAYOU_KEY", DefaultLanguageHighlighterColors.KEYWORD)
        val VALUE = createTextAttributesKey("BAYOU_VALUE", DefaultLanguageHighlighterColors.CLASS_NAME)
        val BAD_CHARACTER = createTextAttributesKey("BAYOU_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
    }
}