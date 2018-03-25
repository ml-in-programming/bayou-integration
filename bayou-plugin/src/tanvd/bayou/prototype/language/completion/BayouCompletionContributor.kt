package tanvd.bayou.prototype.language.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.tree.IElementType
import com.intellij.util.ProcessingContext
import tanvd.bayou.prototype.language.BayouLanguage
import tanvd.bayou.prototype.language.psi.BayouTypes
import tanvd.bayou.prototype.language.psi.BayouTypes.*
import tanvd.bayou.prototype.utils.Resource
import kotlin.reflect.KClass


class BayouCompletionContributor : CompletionContributor() {

    companion object {
        private val androidFunctions = Resource.getObject("android/android_functions.json", List::class as KClass<List<String>>)
        private val androidClass = Resource.getObject("android/android_class.json", List::class as KClass<List<String>>)

        private val stdlibFunctions = Resource.getObject("stdlib/stdlib_functions.json", List::class as KClass<List<String>>)
        private val stdlibClass = Resource.getObject("stdlib/stdlib_class.json", List::class as KClass<List<String>>)

    }

    init {
            extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(BayouTypes.VALUE as IElementType).withLanguage(BayouLanguage.INSTANCE),
                object : CompletionProvider<CompletionParameters>() {
                    public override fun addCompletions(parameters: CompletionParameters,
                                                       context: ProcessingContext,
                                                       resultSet: CompletionResultSet) {
                        val parentType = parameters.position.context?.node?.elementType
                        when (parentType) {
                           BODY_STDLIB -> {
                               val key = parameters.position.prevSibling.prevSibling?.firstChild?.node?.elementType
                               when (key) {
                                   API -> {
                                       stdlibFunctions.forEach {
                                           resultSet.addElement(LookupElementBuilder.create(it))
                                       }
                                   }
                                   TYPE -> {
                                       stdlibClass.forEach {
                                           resultSet.addElement(LookupElementBuilder.create(it))
                                       }
                                   }
                                   else -> {}
                               }
                            }
                            BODY_ANDROID -> {
                                val key = parameters.position.prevSibling.prevSibling?.firstChild?.node?.elementType
                                when (key) {
                                    API -> {
                                        androidFunctions.forEach {
                                            resultSet.addElement(LookupElementBuilder.create(it))
                                        }
                                    }
                                    TYPE -> {
                                        androidClass.forEach {
                                            resultSet.addElement(LookupElementBuilder.create(it))
                                        }
                                    }
                                    CONTEXT -> {
                                        androidClass.forEach {
                                            resultSet.addElement(LookupElementBuilder.create(it))
                                        }
                                    }
                                    else -> {}
                                }
                            }
                            else -> {}
                        }
                    }
                }
        )
    }
}