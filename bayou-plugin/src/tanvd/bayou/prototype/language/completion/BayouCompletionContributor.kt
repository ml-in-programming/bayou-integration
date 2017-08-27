package tanvd.bayou.prototype.language.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.tree.IElementType
import com.intellij.util.ProcessingContext
import tanvd.bayou.prototype.language.BayouLanguage
import tanvd.bayou.prototype.language.psi.BayouTypes


class BayouCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(BayouTypes.VALUE as IElementType).withLanguage(BayouLanguage.INSTANCE),
                object : CompletionProvider<CompletionParameters>() {
                    public override fun addCompletions(parameters: CompletionParameters,
                                                       context: ProcessingContext,
                                                       resultSet: CompletionResultSet) {
                        resultSet.addElement(LookupElementBuilder.create("Bluetooth"))
                        resultSet.addElement(LookupElementBuilder.create("Camera"))
                    }
                }
        )
    }
}