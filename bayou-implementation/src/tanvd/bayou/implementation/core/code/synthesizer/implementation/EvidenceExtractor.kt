/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package tanvd.bayou.implementation.core.code.synthesizer.implementation

import com.google.gson.GsonBuilder
import org.eclipse.jdt.core.dom.*
import java.util.*

class EvidenceExtractor : ASTVisitor() {

    internal var output = JSONOutputWrapper()
    internal var evidenceBlock: Block? = null

    internal inner class JSONOutputWrapper {
        var apicalls: MutableList<String> = ArrayList()
        var types: MutableList<String> = ArrayList()
        var context: MutableList<String> = ArrayList()
        var keywords: MutableList<String> = ArrayList()

    }

    fun execute(parser: Parser): String? {
        val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
        parser.cu.accept(this)
        return gson.toJson(output)
    }

    @Throws(SynthesisException::class)
    override fun visit(invocation: MethodInvocation?): Boolean {
        val binding = invocation!!.resolveMethodBinding() ?: throw SynthesisException(SynthesisException.CouldNotResolveBinding)

        val cls = binding.declaringClass
        if (cls == null || cls.qualifiedName != "edu.rice.cs.caper.bayou.annotations.Evidence")
            return false

        if (invocation.parent.parent !is Block)
            throw SynthesisException(SynthesisException.EvidenceNotInBlock)
        val evidenceBlock = invocation.parent.parent as Block

        if (!isLegalEvidenceBlock(evidenceBlock))
            throw SynthesisException(SynthesisException.EvidenceMixedWithCode)

        if (this.evidenceBlock != null && this.evidenceBlock !== evidenceBlock)
            throw SynthesisException(SynthesisException.MoreThanOneHole)
        this.evidenceBlock = evidenceBlock

        // performing casts wildly.. if any exceptions occur it's due to incorrect input format
        if (binding.name == "apicalls") {
            for (arg in invocation.arguments()) {
                val a = arg as StringLiteral
                output.apicalls.add(a.literalValue)
            }
        } else if (binding.name == "types") {
            for (arg in invocation.arguments()) {
                val a = arg as StringLiteral
                output.types.add(a.literalValue)
            }
        } else if (binding.name == "context") {
            for (arg in invocation.arguments()) {
                val a = arg as StringLiteral
                output.context.add(a.literalValue)
            }
        } else if (binding.name == "keywords") {
            for (arg in invocation.arguments()) {
                val a = arg as StringLiteral
                output.keywords.add(a.literalValue)
            }
        } else
            throw SynthesisException(SynthesisException.InvalidEvidenceType)

        return false
    }

    companion object {

        // Check if the given block contains statements that are not evidence API calls
        internal fun isLegalEvidenceBlock(evidBlock: Block): Boolean {
            for (obj in evidBlock.statements()) {
                try {
                    val stmt = obj as Statement
                    val expr = (stmt as ExpressionStatement).expression
                    val invocation = expr as MethodInvocation

                    val binding = invocation.resolveMethodBinding() ?: throw SynthesisException(SynthesisException.CouldNotResolveBinding)

                    val cls = binding.declaringClass
                    if (cls == null || cls.qualifiedName != "edu.rice.cs.caper.bayou.annotations.Evidence")
                        return false
                } catch (e: ClassCastException) {
                    return false
                }

            }

            return true
        }
    }
}

