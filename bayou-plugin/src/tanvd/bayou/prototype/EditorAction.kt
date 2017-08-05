package tanvd.bayou.prototype

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction



class EditorAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent?) {
        val project = event?.getData(CommonDataKeys.PROJECT) ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val currentText = document.text

        val result = BayouApi.generateFromText(currentText)

        if (result != null) {
            val runnable = Runnable { document.replaceString(0, currentText.length, result) }
            WriteCommandAction.runWriteCommandAction(project, runnable)
        }
    }

}