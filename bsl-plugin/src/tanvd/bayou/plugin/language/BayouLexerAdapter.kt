package tanvd.bayou.plugin.language

import com.intellij.lexer.FlexAdapter

import java.io.Reader

class BayouLexerAdapter : FlexAdapter(BayouLexer(null as Reader?))