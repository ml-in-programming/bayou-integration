package tanvd.bayou.prototype

import com.intellij.codeInsight.completion.AllClassesGetter
import com.intellij.codeInsight.completion.PlainPrefixMatcher
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.search.GlobalSearchScope
import tanvd.bayou.implementation.utils.Resource
import tanvd.bayou.prototype.annotations.PsiClassesProcessor
import java.nio.file.Paths


class EditorAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent?) {
        InitObject
        val project = event?.getData(CommonDataKeys.PROJECT) ?: return

        AllClassesGetter.processJavaClasses(
                PlainPrefixMatcher(""),
                project,
                GlobalSearchScope.projectScope(project),
                PsiClassesProcessor(project)
        )
    }

}

object InitObject {
    private val tmp: String = System.getProperty("java.io.tmpdir")
    private fun getFilePath(name: String) = Paths.get(tmp, "model", name)
    init {
        Resource.setHook { fileName ->
            if (fileName == "artifacts/model/stdlib/full-model") {
                "C:\\Users\\TanVD\\Work\\Diploma\\bayou-integration\\bayou-plugin\\resources\\stdlib-full-model"
            }
            else if (fileName == "artifacts/model/full-model") {
                "C:\\Users\\TanVD\\Work\\Diploma\\bayou-integration\\bayou-plugin\\resources\\android-full-model"
            } else {
                val newPath = getFilePath(fileName).toFile()
                newPath.parentFile.mkdirs()
                newPath.createNewFile()
                val input = Resource.getJarStream(fileName)
                newPath.writeBytes(input.readBytes())
                newPath.absolutePath
            }
        }
    }
}