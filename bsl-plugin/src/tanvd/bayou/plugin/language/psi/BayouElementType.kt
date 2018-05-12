package tanvd.bayou.plugin.language.psi

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls
import tanvd.bayou.plugin.language.BayouLanguage

class BayouElementType(@NonNls debugName: String) : IElementType(debugName, BayouLanguage.INSTANCE)