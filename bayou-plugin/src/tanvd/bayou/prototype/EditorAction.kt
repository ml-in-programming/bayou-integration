package tanvd.bayou.prototype

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import tanvd.bayou.prototype.annotations.kotlin.SynthesizerType
import tanvd.bayou.prototype.language.EvidencesInput


class EditorAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent?) {
        val project = event?.getData(CommonDataKeys.PROJECT) ?: return
        val file = FileEditorManager.getInstance(project).selectedTextEditor!!.document.text

        val mapAndroid = getEvidencesFromLang("ANDROID", file)
        val mapStdlib = getEvidencesFromLang("STDLIB", file)

        val runnable = BayouRunnable(project, "Bayou Synthesizer", mapAndroid + mapStdlib)

        ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, runnable.title, true, project)
    }

    private fun getEvidencesFromLang(name: String, text: String): Map<String, EvidencesInput> {
        val synthesizerType = when (name) {
            "STDLIB" -> SynthesizerType.StdLib
            "ANDROID" -> SynthesizerType.Android
            else -> error("UNKNOWN TYPE")
        }
        val totalMap = HashMap<String, EvidencesInput>()
        val bayouCommentsRegex = Regex("\\/\\*[\\s\\n]*$name([^\\*\\/]*)[\\s\\n]*\\*\\/[\\s\\n]*(.*)")
        for (group in bayouCommentsRegex.findAll(text)) {
            val input = EvidencesInput(synthesizerType)
            val definition = group.groupValues[1]
            val methodSign = group.groupValues[2]
            val methodName = methodSign.split(" ")[1].takeWhile { it != '(' }
            Regex("(\\w+)[\\s]*:=[\\s]*(\\w+)").findAll(definition).forEach {
                val typeName = it.groupValues[1]
                when (typeName) {
                    "API" -> {
                        input.apiCalls.add(it.groupValues[2])
                    }
                    "TYPE" -> {
                        input.apiTypes.add(it.groupValues[2])
                    }
                    "CONTEXT" -> {
                        input.contextClasses.add(it.groupValues[2])
                    }
                    else -> {
                    }
                }
            }

            totalMap[methodName] = input
        }

        return totalMap
    }

}