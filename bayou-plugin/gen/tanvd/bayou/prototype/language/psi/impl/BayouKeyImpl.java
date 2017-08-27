// This is a generated file. Not intended for manual editing.
package tanvd.bayou.prototype.language.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static tanvd.bayou.prototype.language.psi.BayouTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import tanvd.bayou.prototype.language.psi.*;

public class BayouKeyImpl extends ASTWrapperPsiElement implements BayouKey {

  public BayouKeyImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BayouVisitor visitor) {
    visitor.visitKey(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BayouVisitor) accept((BayouVisitor)visitor);
    else super.accept(visitor);
  }

}
