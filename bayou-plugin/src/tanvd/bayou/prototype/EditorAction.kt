package tanvd.bayou.prototype

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressManager


class EditorAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent?) {
        val project = event?.getData(CommonDataKeys.PROJECT) ?: return
        val task = BayouRunnable(project, "Bayou Synthesizer")
        ProgressManager.getInstance().runProcessWithProgressSynchronously(task, task.title, true, project)
    }

}