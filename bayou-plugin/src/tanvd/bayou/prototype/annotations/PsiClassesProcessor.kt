package tanvd.bayou.prototype.annotations

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiTypeElement
import com.intellij.util.Processor
import tanvd.bayou.implementation.BayouClient
import tanvd.bayou.implementation.facade.DownloadProgress
import tanvd.bayou.implementation.facade.SynthesisProgress
import tanvd.bayou.prototype.annotations.kotlin.ApiType
import tanvd.bayou.prototype.annotations.kotlin.SynthesizerType
import tanvd.bayou.prototype.annotations.kotlin.android.ContextClass
import tanvd.bayou.prototype.synthesizer.BayouRequest
import tanvd.bayou.prototype.synthesizer.BayouSynthesizer
import tanvd.bayou.prototype.synthesizer.InputParameter
import tanvd.bayou.prototype.utils.*
import java.io.InputStreamReader


class PsiClassesProcessor(val project: Project, val progress: SynthesisProgress) : Processor<PsiClass> {
    override fun process(psiClass: PsiClass): Boolean {
        BayouClient.setDownloadProgress { DownloadProgressWrapper(ProgressManager.getInstance().progressIndicator) }

        psiClass.methods.forEach { method ->
            var model: SynthesizerType? = null

            val contextClasses = ArrayList<String>()
            val apiCalls = ArrayList<String>()
            val apiTypes = ArrayList<String>()

            method.annotations.forEach { annotation ->
                when (annotation.qualifiedName) {
                    tanvd.bayou.prototype.annotations.kotlin.BayouSynthesizer::class.qualifiedName -> {
                        val type = annotation.findAttributeValue(tanvd.bayou.prototype.annotations.kotlin.BayouSynthesizer::type.name)!!.text
                        model = SynthesizerType.valueOf(type.split(".").last())
                    }
                    ContextClass::class.qualifiedName -> {
                        val type = (annotation.findAttributeValue(ContextClass::klass.name)!!.children[0] as PsiTypeElement).type
                        if (type.className != null) {
                            contextClasses.add(type.className!!)
                        }
                    }
                    tanvd.bayou.prototype.annotations.kotlin.android.ApiCall::class.qualifiedName -> {
                        val stringElement = annotation.findAttributeValue(
                                tanvd.bayou.prototype.annotations.kotlin.android.ApiCall::name.name)!!.text.drop("AndroidFunctions.".length)
                        val apiCallName = stringElement.trim('"')
                        apiCalls.add(apiCallName)
                    }
                    tanvd.bayou.prototype.annotations.kotlin.stdlib.ApiCall::class.qualifiedName -> {
                        val stringElement = annotation.findAttributeValue(
                                tanvd.bayou.prototype.annotations.kotlin.stdlib.ApiCall::name.name)!!.text
                        val apiCallName = stringElement.trim('"')
                        apiCalls.add(apiCallName)
                    }
                    ApiType::class.qualifiedName -> {
                        val stringElement = annotation.findAttributeValue(ApiType::name.name)!!.text.dropLast(".class".length)
                        val apiTypeName = stringElement.trim('"')
                        apiTypes.add(apiTypeName)
                    }
                }
            }

            if (model == null && (contextClasses.isNotEmpty() || apiCalls.isNotEmpty() || apiTypes.isNotEmpty())) {
                ApplicationManager.getApplication().invokeLater{
                    executeWriteAction(project, method.containingFile) {
                        method.body?.replace(PsiUtils.createCodeBlock("{\n // You have not set type of synthesizer.\n}", project))
                    }
                }
                return true
            }
            else if (model != null && !BayouClient.existsModel(model!!.name.toLowerCase())){
                val config = InputStreamReader(BayouSynthesizer::class.java.classLoader.getResourceAsStream("${model!!.name.toLowerCase()}.json")).readText()

                BayouClient.downloadModel(config, DownloadProgressWrapper(ProgressManager.getInstance().progressIndicator))
            }

            val inputParams = ArrayList<InputParameter>()
            method.parameterList.parameters.forEach {
                if (it.name != null && it.type.qualifiedClassName != null) {
                    inputParams.add(InputParameter(it.name!!, it.type.canonicalText))
                }
            }
            if (contextClasses.isNotEmpty() || apiCalls.isNotEmpty()) {
                val response = BayouSynthesizer.invoke(model!!, BayouRequest(inputParams, apiCalls, apiTypes, contextClasses), progress)
                val codeBlock = if (response != null) {
                    val (imports, code) = response
                    val qualifiedCode = CodeUtils.qualifyWithImports(code, imports)
                    PsiUtils.createImportsShortenedBlock("{\n $qualifiedCode \n}", project)
                } else {
                    PsiUtils.createCodeBlock("{\n // Something went wrong. Try again with other params.\n}", project)
                }
                ApplicationManager.getApplication().invokeLater{
                    executeWriteAction(project, method.containingFile) {
                        method.body?.replace(codeBlock)
                        PsiUtils.reformatFile(method.containingFile, project)
                    }
                }
            }
        }
        return true
    }
}

class DownloadProgressWrapper(val progressIndicator: ProgressIndicator): DownloadProgress {

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

