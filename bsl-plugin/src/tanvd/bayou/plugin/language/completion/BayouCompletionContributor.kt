package tanvd.bayou.plugin.language.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.tree.IElementType
import com.intellij.util.ProcessingContext
import tanvd.bayou.plugin.language.BayouLanguage
import tanvd.bayou.plugin.language.psi.BayouTypes
import tanvd.bayou.plugin.language.psi.BayouTypes.*
import tanvd.bayou.plugin.utils.Resource
import kotlin.reflect.KClass
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.completion.InsertionContext


class BayouCompletionContributor : CompletionContributor() {

    private val DUMMY_IDENTIFIER = CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED

    private val keywordSet = ArrayList<String>()

    override fun beforeCompletion(context: CompletionInitializationContext) {
        keywordSet.clear()
        val notBlankStrings = context.file.text.split("\n").filterNot { it.isBlank() }
        if (notBlankStrings.count() <= 1 && (!context.file.text.contains("STDLIB") && !context.file.text.contains("ANDROID"))) {
            keywordSet.add("STDLIB")
            keywordSet.add("ANDROID")
        } else {
            if (notBlankStrings.first().trim() == "STDLIB") {
                keywordSet.add("API")
                keywordSet.add("TYPE")
            } else if (notBlankStrings.first().trim() == "ANDROID") {
                keywordSet.add("API")
                keywordSet.add("TYPE")
                keywordSet.add("CONTEXT")
            }
            if (notBlankStrings.last().endsWith("API") || notBlankStrings.last().endsWith("TYPE") || notBlankStrings.last().endsWith("CONTEXT")) {
                keywordSet.add(":=")
            }
        }
        context.dummyIdentifier = DUMMY_IDENTIFIER
    }

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
                                    else -> {
                                    }
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
                                    else -> {
                                    }
                                }
                            }
                            else -> {
                            }
                        }
                    }
                }
        )

        val topLevelKeywordsProvider = object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    resultSet: CompletionResultSet) {
                keywordSet.forEach {
                    resultSet.addElement( LookupElementBuilder.create(it)
                            .bold()
                            .withTypeText("keyword")
                            .withInsertHandler(KeywordInsertionHandler()))
                }
            }
        }
        extend(CompletionType.BASIC, psiElement(), topLevelKeywordsProvider)
}
}

open class KeywordInsertionHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        var realStart = context.startOffset - 1
        val text = context.document.charsSequence
        while (text[realStart] != ' ' && text[realStart] != '\n' && text[realStart] != '\t' && realStart > 0 ) {
            realStart--
        }
        realStart++

        context.document.deleteString(realStart, context.tailOffset)
        context.document.insertString(realStart, item.lookupString)
        context.document.insertString(realStart + item.lookupString.length, " ")
        context.commitDocument()

        context.editor.caretModel.moveToOffset(realStart + item.lookupString.length)
    }
}