// This is a generated file. Not intended for manual editing.
package tanvd.bayou.prototype.language.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import tanvd.bayou.prototype.language.psi.impl.*;

public interface BayouTypes {

  IElementType BODY_ANDROID = new BayouElementType("BODY_ANDROID");
  IElementType BODY_STDLIB = new BayouElementType("BODY_STDLIB");
  IElementType KEY_ANDROID = new BayouElementType("KEY_ANDROID");
  IElementType KEY_STDLIB = new BayouElementType("KEY_STDLIB");

  IElementType ANDROID = new BayouTokenType("ANDROID");
  IElementType API = new BayouTokenType("API");
  IElementType CONTEXT = new BayouTokenType("CONTEXT");
  IElementType SEPARATOR = new BayouTokenType("SEPARATOR");
  IElementType STDLIB = new BayouTokenType("STDLIB");
  IElementType TYPE = new BayouTokenType("TYPE");
  IElementType VALUE = new BayouTokenType("VALUE");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == BODY_ANDROID) {
        return new BayouBodyAndroidImpl(node);
      }
      else if (type == BODY_STDLIB) {
        return new BayouBodyStdlibImpl(node);
      }
      else if (type == KEY_ANDROID) {
        return new BayouKeyAndroidImpl(node);
      }
      else if (type == KEY_STDLIB) {
        return new BayouKeyStdlibImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
