// This is a generated file. Not intended for manual editing.
package tanvd.bayou.plugin.language.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static tanvd.bayou.plugin.language.psi.BayouTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
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
    if (t == BODY_ANDROID) {
      r = bodyAndroid(b, 0);
    }
    else if (t == BODY_STDLIB) {
      r = bodyStdlib(b, 0);
    }
    else if (t == KEY_ANDROID) {
      r = keyAndroid(b, 0);
    }
    else if (t == KEY_STDLIB) {
      r = keyStdlib(b, 0);
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
  // (ANDROID bodyAndroid) | (STDLIB bodyStdlib)
  static boolean bayouFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bayouFile")) return false;
    if (!nextTokenIs(b, "", ANDROID, STDLIB)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = bayouFile_0(b, l + 1);
    if (!r) r = bayouFile_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ANDROID bodyAndroid
  private static boolean bayouFile_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bayouFile_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ANDROID);
    r = r && bodyAndroid(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // STDLIB bodyStdlib
  private static boolean bayouFile_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bayouFile_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, STDLIB);
    r = r && bodyStdlib(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (keyAndroid SEPARATOR VALUE)+
  public static boolean bodyAndroid(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bodyAndroid")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BODY_ANDROID, "<body android>");
    r = bodyAndroid_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!bodyAndroid_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "bodyAndroid", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // keyAndroid SEPARATOR VALUE
  private static boolean bodyAndroid_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bodyAndroid_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = keyAndroid(b, l + 1);
    r = r && consumeTokens(b, 0, SEPARATOR, VALUE);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (keyStdlib SEPARATOR VALUE)+
  public static boolean bodyStdlib(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bodyStdlib")) return false;
    if (!nextTokenIs(b, "<body stdlib>", API, TYPE)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, BODY_STDLIB, "<body stdlib>");
    r = bodyStdlib_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!bodyStdlib_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "bodyStdlib", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // keyStdlib SEPARATOR VALUE
  private static boolean bodyStdlib_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bodyStdlib_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = keyStdlib(b, l + 1);
    r = r && consumeTokens(b, 0, SEPARATOR, VALUE);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // API | TYPE | CONTEXT
  public static boolean keyAndroid(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "keyAndroid")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, KEY_ANDROID, "<key android>");
    r = consumeToken(b, API);
    if (!r) r = consumeToken(b, TYPE);
    if (!r) r = consumeToken(b, CONTEXT);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // API | TYPE
  public static boolean keyStdlib(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "keyStdlib")) return false;
    if (!nextTokenIs(b, "<key stdlib>", API, TYPE)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, KEY_STDLIB, "<key stdlib>");
    r = consumeToken(b, API);
    if (!r) r = consumeToken(b, TYPE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

}
