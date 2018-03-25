package tanvd.bayou.prototype.language

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.util.Processor
import tanvd.bayou.implementation.BayouClient
import tanvd.bayou.implementation.facade.DownloadProgress
import tanvd.bayou.implementation.facade.SynthesisProgress
import tanvd.bayou.prototype.annotations.kotlin.SynthesizerType
import tanvd.bayou.prototype.synthesizer.BayouRequest
import tanvd.bayou.prototype.synthesizer.BayouSynthesizer
import tanvd.bayou.prototype.synthesizer.InputParameter
import tanvd.bayou.prototype.utils.CodeUtils
import tanvd.bayou.prototype.utils.PsiUtils
import tanvd.bayou.prototype.utils.executeWriteAction
import tanvd.bayou.prototype.utils.qualifiedClassName
import java.io.InputStreamReader

data class EvidencesInput(val type: SynthesizerType, val contextClasses: MutableList<String> = ArrayList(),
                          val apiCalls: MutableList<String> = ArrayList(), val apiTypes: MutableList<String> = ArrayList())

class PsiClassesProcessor(val project: Project, val progress: SynthesisProgress, val methods: Map<String, EvidencesInput>) : Processor<PsiClass> {
    override fun process(psiClass: PsiClass): Boolean {
        BayouClient.setDownloadProgress { DownloadProgressWrapper(ProgressManager.getInstance().progressIndicator) }

        psiClass.methods.forEach { method ->
            val input = methods[method.name] ?: return false

            if (!BayouClient.existsModel(input.type.name.toLowerCase())) {
                val config = InputStreamReader(BayouSynthesizer::class.java.classLoader.getResourceAsStream("${input.type.name.toLowerCase()}.json")).readText()

                BayouClient.downloadModel(config, DownloadProgressWrapper(ProgressManager.getInstance().progressIndicator))
            }

            val inputParams = ArrayList<InputParameter>()
            method.parameterList.parameters.forEach {
                if (it.name != null && it.type.qualifiedClassName != null) {
                    inputParams.add(InputParameter(it.name!!, it.type.canonicalText))
                }
            }
            val response = BayouSynthesizer.invoke(input.type, BayouRequest(inputParams, input.apiCalls,
                    input.apiTypes, input.contextClasses), progress)
            val codeBlock = if (response != null) {
                val (imports, code) = response
                val qualifiedCode = CodeUtils.qualifyWithImports(code, imports)
                PsiUtils.createImportsShortenedBlock("{\n $qualifiedCode \n}", project)
            } else {
                PsiUtils.createCodeBlock("{\n // Something went wrong. Try again with other params.\n}", project)
            }
            ApplicationManager.getApplication().invokeLater {
                executeWriteAction(project, method.containingFile) {
                    method.body?.replace(codeBlock)
                    PsiUtils.reformatFile(method.containingFile, project)
                }
            }
        }
        return true
    }
}

class DownloadProgressWrapper(val progressIndicator: ProgressIndicator) : DownloadProgress {

    override var progress: Double = 0.0
        set(value) {
            field = value
            progressIndicator.fraction = progress
        }

    override var name: String = ""
        set(value) {
            field = value
            progressIndicator.text = "$phase: $name"
        }

    override var phase: String = ""
        set(value) {
            field = value
            progressIndicator.text = "$phase: $name"
        }
}
