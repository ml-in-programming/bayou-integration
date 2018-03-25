package tanvd.bayou.prototype.language;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import tanvd.bayou.prototype.language.psi.BayouTypes;
import com.intellij.psi.TokenType;

%%

%class BayouLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

CRLF=\R
WHITE_SPACE=[\ \n\t\f]
FIRST_VALUE_CHARACTER=[^ \n\f\\] | "\\"{CRLF} | "\\".
VALUE_CHARACTER=[^\n\f\\] | "\\"{CRLF} | "\\".
SEPARATOR={WHITE_SPACE}*[:=]{WHITE_SPACE}*
STDLIB="STDLIB"
ANDROID="ANDROID"
API="API"
TYPE="TYPE"
CONTEXT="CONTEXT"

%state WAITING_VALUE

%%

<YYINITIAL> {STDLIB}                                        { yybegin(YYINITIAL); return BayouTypes.STDLIB; }
<YYINITIAL> {ANDROID}                                       { yybegin(YYINITIAL); return BayouTypes.ANDROID; }



<YYINITIAL> {API}                                           { yybegin(YYINITIAL); return BayouTypes.API; }
<YYINITIAL> {TYPE}                                          { yybegin(YYINITIAL); return BayouTypes.TYPE; }
<YYINITIAL> {CONTEXT}                                       { yybegin(YYINITIAL); return BayouTypes.CONTEXT; }

<YYINITIAL> {SEPARATOR}                                     { yybegin(WAITING_VALUE); return BayouTypes.SEPARATOR; }

<WAITING_VALUE> {CRLF}({CRLF}|{WHITE_SPACE})+               { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }

<WAITING_VALUE> {WHITE_SPACE}+                              { yybegin(WAITING_VALUE); return TokenType.WHITE_SPACE; }

<WAITING_VALUE> {FIRST_VALUE_CHARACTER}{VALUE_CHARACTER}*   { yybegin(YYINITIAL); return BayouTypes.VALUE; }

({CRLF}|{WHITE_SPACE})+                                     { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }

.                                                           { return TokenType.BAD_CHARACTER; }
