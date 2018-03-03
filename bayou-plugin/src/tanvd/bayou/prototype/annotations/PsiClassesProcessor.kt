package tanvd.bayou.prototype.annotations

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiTypeElement
import com.intellij.util.Processor
import tanvd.bayou.prototype.annotations.java.ApiCall
import tanvd.bayou.prototype.annotations.java.ApiType
import tanvd.bayou.prototype.annotations.java.ContextClass
import tanvd.bayou.prototype.synthesizer.BayouRequest
import tanvd.bayou.prototype.synthesizer.BayouSynthesizer
import tanvd.bayou.prototype.synthesizer.implementation.BayouTextConverter
import tanvd.bayou.prototype.synthesizer.InputParameter
import tanvd.bayou.prototype.utils.*


class PsiClassesProcessor(val project: Project) : Processor<PsiClass> {
    override fun process(psiClass: PsiClass): Boolean {
        psiClass.methods.forEach { method ->
            val contextClasses = ArrayList<String>()
            val apiCalls = ArrayList<String>()
            val apiTypes = ArrayList<String>()
            method.annotations.forEach { annotation ->
                when (annotation.qualifiedName) {
                    ContextClass::class.qualifiedName -> {
                        val type = (annotation.findAttributeValue(ContextClass::klass.name)!!.children[0] as PsiTypeElement).type
                        if (type.className != null) {
                            contextClasses.add(type.className!!)
                        }
                    }
                    ApiCall::class.qualifiedName -> {
                        val stringElement = annotation.findAttributeValue(ApiCall::name.name)!!.text.drop("AndroidFunctions.".length)
                        val apiCallName = stringElement.trim('"')
                        apiCalls.add(apiCallName)
                    }
                    ApiType::class.qualifiedName -> {
                        val stringElement = annotation.findAttributeValue(ApiType::klass.name)!!.text.dropLast(".class".length)
                        val apiTypeName = stringElement.trim('"')
                        apiTypes.add(apiTypeName)
                    }
                }
            }
            val inputParams = ArrayList<InputParameter>()
            method.parameterList.parameters.forEach {
                if (it.name != null && it.type.qualifiedClassName != null) {
                    inputParams.add(InputParameter(it.name!!, it.type.canonicalText))
                }
            }
            if (contextClasses.isNotEmpty() || apiCalls.isNotEmpty()) {
                val response = BayouSynthesizer.invoke(BayouRequest(inputParams, apiCalls, apiTypes, contextClasses))
                val codeBlock = if (response != null) {
                    val (imports, code) = response
                    val qualifiedCode = CodeUtils.qualifyWithImports(code, imports)
                    PsiUtils.createImportsShortenedBlock("{\n $qualifiedCode \n}", project)
                } else {
                    PsiUtils.createCodeBlock("{\n // Something went wrong. Try again with other params.\n}", project)
                }
                executeWriteAction(project, method.containingFile) {
                    method.body?.replace(codeBlock)
                    PsiUtils.reformatFile(method.containingFile, project)
                }

            }
        }
        return true
    }
}

