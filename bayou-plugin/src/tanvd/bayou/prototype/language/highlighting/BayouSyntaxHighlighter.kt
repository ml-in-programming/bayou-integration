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
        if (tokenType == BayouTypes.SEPARATOR) {
            return arrayOf(SEPARATOR)
        } else if (tokenType == BayouTypes.API) {
            return arrayOf(KEY)
        } else if (tokenType == BayouTypes.TYPE) {
            return arrayOf(TYPE)
        } else if (tokenType == BayouTypes.CONTEXT) {
            return arrayOf(CONTEXT)
        } else if (tokenType == BayouTypes.VALUE) {
            return arrayOf(VALUE)
        } else if (tokenType == BayouTypes.COMMENT) {
            return arrayOf(COMMENT)
        } else if (tokenType == TokenType.BAD_CHARACTER) {
            return arrayOf(BAD_CHARACTER)
        } else {
            return arrayOf()
        }
    }

    companion object {
        val SEPARATOR = createTextAttributesKey("BAYOU_SEPARATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val KEY = createTextAttributesKey("BAYOU_KEY", DefaultLanguageHighlighterColors.KEYWORD)
        val TYPE = createTextAttributesKey("BAYOU_TYPE", DefaultLanguageHighlighterColors.KEYWORD)
        val CONTEXT = createTextAttributesKey("BAYOU_CONTEXT", DefaultLanguageHighlighterColors.KEYWORD)
        val VALUE = createTextAttributesKey("BAYOU_VALUE", DefaultLanguageHighlighterColors.CLASS_NAME)
        val COMMENT = createTextAttributesKey("BAYOU_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val BAD_CHARACTER = createTextAttributesKey("BAYOU_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
    }
}