// This is a generated file. Not intended for manual editing.
package tanvd.bayou.prototype.language.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import tanvd.bayou.prototype.language.psi.impl.*;

public interface BayouTypes {

  IElementType KEY = new BayouElementType("KEY");
  IElementType PROPERTY = new BayouElementType("PROPERTY");

  IElementType API = new BayouTokenType("API");
  IElementType COMMENT = new BayouTokenType("COMMENT");
  IElementType CONTEXT = new BayouTokenType("CONTEXT");
  IElementType SEPARATOR = new BayouTokenType("SEPARATOR");
  IElementType TYPE = new BayouTokenType("TYPE");
  IElementType VALUE = new BayouTokenType("VALUE");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == KEY) {
        return new BayouKeyImpl(node);
      }
      else if (type == PROPERTY) {
        return new BayouPropertyImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
