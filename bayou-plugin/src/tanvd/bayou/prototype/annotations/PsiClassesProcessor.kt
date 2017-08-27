package tanvd.bayou.prototype.annotations

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiTypeElement
import com.intellij.util.Processor
import org.jetbrains.uast.java.annotations
import tanvd.bayou.prototype.api.BayouApi
import tanvd.bayou.prototype.api.BayouRequest
import tanvd.bayou.prototype.api.InputParameter
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
                        val stringElement = annotation.findAttributeValue(ApiCall::name.name)!!.text
                        val apiCallName = stringElement.trim('"')
                        apiCalls.add(apiCallName)
                    }
                    ApiType::class.qualifiedName -> {
                        val stringElement = annotation.findAttributeValue(ApiType::name.name)!!.text
                        val apiTypeName = stringElement.trim('"')
                        apiTypes.add(apiTypeName)
                    }
                }
            }
            val inputParams = ArrayList<InputParameter>()
            method.parameterList.parameters.forEach {
                if (it.name != null && it.type.qualifiedClassName != null) {
                    inputParams.add(InputParameter(it.name!!, it.type.qualifiedClassName!!))
                }
            }
            if (contextClasses.isNotEmpty() || apiCalls.isNotEmpty()) {
                val request = BayouRequest(inputParams, apiCalls, apiTypes, contextClasses)
                val response = BayouApi.executeRequest(request)
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

