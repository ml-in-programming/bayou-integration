package tanvd.bayou.plugin.language.psi

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls
import tanvd.bayou.plugin.language.BayouLanguage

class BayouTokenType(@NonNls debugName: String) : IElementType(debugName, BayouLanguage.INSTANCE) {

    override fun toString(): String {
        return "BayouTokenType." + super.toString()
    }
}