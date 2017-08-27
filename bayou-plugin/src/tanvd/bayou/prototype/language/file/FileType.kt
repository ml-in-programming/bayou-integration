package tanvd.bayou.prototype.language.file

import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.annotations.NotNull
import tanvd.bayou.prototype.language.BayouLanguage
import tanvd.bayou.prototype.language.icon.BayouIcons
import javax.swing.Icon


class BayouFileType private constructor() : LanguageFileType(BayouLanguage.INSTANCE) {

    override fun getName(): String {
        return "Bayou file"
    }

    override fun getDescription(): String {
        return "Bayou language file"
    }

    override fun getDefaultExtension(): String {
        return "bayou"
    }

    override fun getIcon(): Icon? {
        return BayouIcons.FILE
    }

    companion object {
        val INSTANCE = BayouFileType()
    }
}

class BayouFileTypeFactory : FileTypeFactory() {
    override fun createFileTypes(@NotNull fileTypeConsumer: FileTypeConsumer) {
        fileTypeConsumer.consume(BayouFileType.INSTANCE, "Bayou")
    }
}