package tanvd.bayou.prototype.language.psi

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls
import tanvd.bayou.prototype.language.BayouLanguage

class BayouElementType(@NonNls debugName: String) : IElementType(debugName, BayouLanguage.INSTANCE)