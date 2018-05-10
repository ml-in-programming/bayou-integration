package tanvd.bayou.plugin

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.formatting.WhiteSpace
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import tanvd.bayou.implementation.BayouClient
import tanvd.bayou.implementation.facade.DownloadProgress
import tanvd.bayou.implementation.facade.SynthesisPhase
import tanvd.bayou.implementation.facade.SynthesisProgress
import tanvd.bayou.plugin.annotations.kotlin.SynthesizerType
import tanvd.bayou.plugin.synthesizer.BayouRequest
import tanvd.bayou.plugin.synthesizer.BayouSynthesizer
import tanvd.bayou.plugin.synthesizer.InputParameter
import tanvd.bayou.plugin.utils.CodeUtils
import tanvd.bayou.plugin.utils.PsiUtils
import tanvd.bayou.plugin.utils.executeWriteAction
import tanvd.bayou.plugin.utils.qualifiedClassName
import java.io.InputStreamReader

class GenerateIntentionAction : PsiElementBaseIntentionAction(), IntentionAction {
    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (getContainingMethod(element) == null) return false

        val comment = asComment(element) ?: return false
        val request = getRequest(comment)
        return request.contains("STDLIB") || request.contains("ANDROID")
    }

    override fun getText(): String {
        return "Generate code with Bayou"
    }

    override fun getFamilyName(): String {
        return text
    }

    private fun asComment(element: PsiElement?): PsiComment? {
        var element = element
        if (element !is PsiComment) {
            element = PsiTreeUtil.skipSiblingsBackward(element, WhiteSpace::class.java)
            if (element !is PsiComment) return null
        }
        return element
    }

    private fun getContainingMethod(element: PsiElement?): PsiMethod? {
        var element = element
        while (element != null && element !is PsiMethod) element = element.parent
        return element as PsiMethod?
    }

    private fun getRequest(comment: PsiComment): String {
        return comment.text.drop("/*".length).dropLast("*/".length)
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val comment = asComment(element)
        val request = comment!!.text.drop("/*".length).dropLast("*/".length)
        val containingMethod = getContainingMethod(comment)!!
        val evidences = BayouSynthesizer.getEvidencesFromLang(request)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Bayou Code Generation", true) {
            override fun run(indicator: ProgressIndicator) {
                if (!BayouClient.existsModel(evidences.type.name.toLowerCase())) {
                    val config = InputStreamReader(BayouSynthesizer::class.java.classLoader.getResourceAsStream("${evidences.type.name.toLowerCase()}/config.json")).readText()

                    BayouClient.downloadModel(config, DownloadProgressWrapper(ProgressManager.getInstance().progressIndicator))
                }

                val inputParams = ArrayList<InputParameter>()
                runReadAction {
                    containingMethod.parameterList.parameters.forEach {
                        if (it.name != null && it.type.qualifiedClassName != null) {
                            inputParams.add(InputParameter(it.name!!, it.type.canonicalText))
                        }
                    }
                }
                val returnType = runReadAction {
                    containingMethod.returnTypeElement?.type?.qualifiedClassName
                            ?: containingMethod.returnTypeElement?.type?.canonicalText ?: "void"
                }

                indicator.isIndeterminate = false
                val response = try {
                    BayouSynthesizer.invoke(evidences.type, BayouRequest(returnType, inputParams, evidences.apiCalls,
                            evidences.apiTypes, evidences.contextClasses), ProgressIndicatorWrapper(indicator))
                } catch (e: Exception) {
                    null
                }

                val codeBlock = runReadAction {
                    if (response != null) {
                        val (imports, code) = response
                        val qualifiedCode = CodeUtils.qualifyWithImports(code, imports)
                        PsiUtils.createImportsShortenedBlock("{\n $qualifiedCode \n}", project)
                    } else {
                        PsiUtils.createCodeBlock("{\n // Something went wrong. Try again with other params.\n}", project)
                    }
                }

                executeWriteAction(project, containingMethod.containingFile, {
                    containingMethod.body?.replace(codeBlock)
                    PsiUtils.reformatFile(containingMethod.containingFile, project)
                })
            }
        })
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

data class EvidencesInput(val type: SynthesizerType, val contextClasses: MutableList<String> = ArrayList(),
                          val apiCalls: MutableList<String> = ArrayList(), val apiTypes: MutableList<String> = ArrayList())

class ProgressIndicatorWrapper(val p: ProgressIndicator) : SynthesisProgress {
    override var fraction: Double = 0.0
        set(value) {
            field = value
            p.fraction = value
        }
    override var phase: SynthesisPhase = SynthesisPhase.IDLE
        set(value) {
            field = value
            p.text = when (value) {
                SynthesisPhase.IDLE -> "Writing down"
                SynthesisPhase.Started -> "Started"
                SynthesisPhase.Parsing -> "Parsing Evidences"
                SynthesisPhase.Embedding -> "Wrangling Evidences"
                SynthesisPhase.SketchGeneration -> "Generating Sketches"
                SynthesisPhase.Concretization -> "Concretizating Sketches"
                SynthesisPhase.Finished -> "Done"
            }
        }

}

