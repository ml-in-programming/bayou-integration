package tanvd.bayou.prototype.language.file

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.jetbrains.annotations.NotNull
import tanvd.bayou.prototype.language.BayouLanguage
import javax.swing.Icon

class BayouFile(@NotNull viewProvider: FileViewProvider) : PsiFileBase(viewProvider, BayouLanguage.INSTANCE) {
    override fun getFileType(): FileType {
        return BayouFileType.INSTANCE
    }

    override fun toString(): String {
        return "Bayou File"
    }

    @Override
    override fun getIcon(flags: Int): Icon {
        return super.getIcon(flags)!!
    }
}