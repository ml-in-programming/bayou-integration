// This is a generated file. Not intended for manual editing.
package tanvd.bayou.prototype.language.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static tanvd.bayou.prototype.language.psi.BayouTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class BayouParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    if (t == KEY) {
      r = key(b, 0);
    }
    else if (t == PROPERTY) {
      r = property(b, 0);
    }
    else {
      r = parse_root_(t, b, 0);
    }
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return bayouFile(b, l + 1);
  }

  /* ********************************************************** */
  // (property | COMMENT)*
  static boolean bayouFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bayouFile")) return false;
    int c = current_position_(b);
    while (true) {
      if (!bayouFile_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "bayouFile", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // property | COMMENT
  private static boolean bayouFile_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bayouFile_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = property(b, l + 1);
    if (!r) r = consumeToken(b, COMMENT);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // API | TYPE | CONTEXT
  public static boolean key(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "key")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, KEY, "<key>");
    r = consumeToken(b, API);
    if (!r) r = consumeToken(b, TYPE);
    if (!r) r = consumeToken(b, CONTEXT);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // key SEPARATOR VALUE
  public static boolean property(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "property")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PROPERTY, "<property>");
    r = key(b, l + 1);
    r = r && consumeTokens(b, 0, SEPARATOR, VALUE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

}
