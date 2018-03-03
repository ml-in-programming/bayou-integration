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
package tanvd.bayou.implementation.core.evidence

import org.eclipse.jdt.core.dom.*
import tanvd.bayou.implementation.model.android.synthesizer.Parser
import tanvd.bayou.implementation.model.android.synthesizer.SynthesisException

class EvidenceExtractor : ASTVisitor() {

    internal var output = EvidencesExtractorResult()
    internal var evidenceBlock: Block? = null


    fun execute(cu: CompilationUnit): EvidencesExtractorResult? {
        cu.accept(this)
        return output
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
        when {
            binding.name == "apicalls" -> invocation.arguments()
                    .map { it as StringLiteral }
                    .forEach { output.apicalls.add(it.literalValue) }
            binding.name == "types" -> invocation.arguments()
                    .map { it as StringLiteral }
                    .forEach { output.types.add(it.literalValue) }
            binding.name == "context" -> invocation.arguments()
                    .map { it as StringLiteral }
                    .forEach { output.context.add(it.literalValue) }
            else -> throw SynthesisException(SynthesisException.InvalidEvidenceType)
        }

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

