package tanvd.bayou.plugin.language

import com.intellij.lang.annotation.*
import com.intellij.psi.*
import com.intellij.psi.PsiErrorElement



class SimpleAnnotator : Annotator {
    override fun annotate(psiElement: PsiElement, holder: AnnotationHolder) {
        val sibling = psiElement.nextSibling
        if (sibling is PsiErrorElement) {
            if (sibling.errorDescription.contains("ANDROID") && sibling.errorDescription.contains("STDLIB")) {
                holder.createErrorAnnotation(sibling, "Wrong name of synthesizer. Should be STDLIB or ANDROID")
            } else if (sibling.errorDescription.contains("key android") || sibling.errorDescription.contains("body android")) {
                holder.createErrorAnnotation(sibling, "Wrong name of evidence. Should be API, TYPE or CONTEXT")
            } else if (sibling.errorDescription.contains("key stdlib") || sibling.errorDescription.contains("body stdlib")) {
                holder.createErrorAnnotation(sibling, "Wrong name of evidence. Should be API or TYPE")
            } else if (sibling.errorDescription.contains("SEPARATOR")) {
                holder.createErrorAnnotation(sibling, "Wrong separator. Use :=")
            }
        }
    }
}