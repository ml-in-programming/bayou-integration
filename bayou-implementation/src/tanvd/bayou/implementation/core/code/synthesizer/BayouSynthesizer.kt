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
package tanvd.bayou.implementation.core.code.synthesizer

import tanvd.bayou.implementation.core.code.synthesizer.implementation.EvidenceExtractor
import tanvd.bayou.implementation.core.code.synthesizer.implementation.ParseException
import tanvd.bayou.implementation.core.code.synthesizer.implementation.Parser
import tanvd.bayou.implementation.core.code.synthesizer.implementation.Synthesizer
import tanvd.bayou.implementation.core.ml.AstGenerator
import tanvd.bayou.implementation.core.ml.Evidences
import tanvd.bayou.implementation.core.ml.lda.LdaApi
import tanvd.bayou.implementation.core.ml.lda.LdaContexts
import tanvd.bayou.implementation.core.ml.lda.LdaTypes
import tanvd.bayou.implementation.core.ml.quality.RelevanceMeasurement
import tanvd.bayou.implementation.core.ml.tfidf.TfIdfApi
import tanvd.bayou.implementation.core.ml.tfidf.TfIdfContext
import tanvd.bayou.implementation.core.ml.tfidf.TfIdfTypes
import tanvd.bayou.implementation.utils.Resource
import java.io.File

class BayouSynthesizer {

    private val synthesizer = Synthesizer()

    @Throws(SynthesiseException::class)
    fun synthesise(code: String, maxPrograms: Int): Iterable<String> {
        return synthesiseHelp(code, maxPrograms)
    }


    @Throws(SynthesiseException::class)
    private fun synthesiseHelp(code: String, maxPrograms: Int): Iterable<String> {
        val combinedClassPath = Resource.getPath("artifacts/classes/") + File.pathSeparator + Resource.getPath("artifacts/jar/android.jar")

        /*
         * Parse the program.
         */
        val parser: Parser
        try {
            parser = Parser(code, combinedClassPath)
            parser.parse()
        } catch (e: ParseException) {
            throw SynthesiseException(e)
        }

        /*
         * Extract a description of the evidence in the search code that should guide AST results generation.
         */
        val evidence: Evidences
        try {
            evidence = EvidenceExtractor().execute(parser)!!
        } catch (e: ParseException) {
            throw SynthesiseException(e)
        }


        /*
         * Generate TfIdf vectors for this evidences
         */
        val tfidfTypes = TfIdfTypes.transform(evidence.types)
        val tfidfApi = TfIdfApi.transform(evidence.apicalls)
        val tfidfContext = TfIdfContext.transform(evidence.context)

        /*
         * Generate LDA vectors for Tfidf got on previous step
         */

        val ldaTypes = LdaTypes.getTopicDistribution(tfidfTypes.map { it.toString() }.toTypedArray()).toTypedArray()
        val ldaApi = LdaApi.getTopicDistribution(tfidfApi.map { it.toString() }.toTypedArray()).toTypedArray()
        val ldaContext = LdaContexts.getTopicDistribution(tfidfContext.map { it.toString() }.toTypedArray()).toTypedArray()


        /*
         * Generate and weight by relevance asts
         */
        val generatedAsts = AstGenerator.generateAsts(ldaApi, ldaTypes, ldaContext, evidence)
        val weightedAsts = RelevanceMeasurement.dedupAndSort(generatedAsts)

        /*
         * Try to generate programs from asts
         */
        val generatedPrograms = weightedAsts.map {
            synthesizer.execute(parser, it.first)
        }.map {
            it.joinToString(separator = "\n") { it }
        }

        return generatedPrograms.take(maxPrograms)
    }
}