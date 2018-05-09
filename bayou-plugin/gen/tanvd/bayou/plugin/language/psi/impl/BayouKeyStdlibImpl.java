// This is a generated file. Not intended for manual editing.
package tanvd.bayou.plugin.language.psi.impl;

import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import tanvd.bayou.plugin.language.psi.*;

public class BayouKeyStdlibImpl extends ASTWrapperPsiElement implements BayouKeyStdlib {

  public BayouKeyStdlibImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BayouVisitor visitor) {
    visitor.visitKeyStdlib(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BayouVisitor) accept((BayouVisitor)visitor);
    else super.accept(visitor);
  }

}
