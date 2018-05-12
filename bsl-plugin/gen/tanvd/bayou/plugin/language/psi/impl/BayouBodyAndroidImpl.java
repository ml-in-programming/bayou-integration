// This is a generated file. Not intended for manual editing.
package tanvd.bayou.plugin.language.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import tanvd.bayou.plugin.language.psi.*;

public class BayouBodyAndroidImpl extends ASTWrapperPsiElement implements BayouBodyAndroid {

  public BayouBodyAndroidImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BayouVisitor visitor) {
    visitor.visitBodyAndroid(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BayouVisitor) accept((BayouVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<BayouKeyAndroid> getKeyAndroidList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, BayouKeyAndroid.class);
  }

}
