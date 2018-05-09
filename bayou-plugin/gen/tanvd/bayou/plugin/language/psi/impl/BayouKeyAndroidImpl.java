// This is a generated file. Not intended for manual editing.
package tanvd.bayou.plugin.language.psi.impl;

import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import tanvd.bayou.plugin.language.psi.*;

public class BayouKeyAndroidImpl extends ASTWrapperPsiElement implements BayouKeyAndroid {

  public BayouKeyAndroidImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BayouVisitor visitor) {
    visitor.visitKeyAndroid(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BayouVisitor) accept((BayouVisitor)visitor);
    else super.accept(visitor);
  }

}
