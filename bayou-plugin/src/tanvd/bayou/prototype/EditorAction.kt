package tanvd.bayou.prototype

import com.intellij.codeInsight.completion.AllClassesGetter
import com.intellij.codeInsight.completion.PlainPrefixMatcher
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.search.GlobalSearchScope
import tanvd.bayou.prototype.annotations.PsiClassesProcessor


class EditorAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent?) {
        val project = event?.getData(CommonDataKeys.PROJECT) ?: return

        AllClassesGetter.processJavaClasses(
                PlainPrefixMatcher(""),
                project,
                GlobalSearchScope.projectScope(project),
                PsiClassesProcessor(project)
        )
    }

}