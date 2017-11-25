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

import org.apache.logging.log4j.LogManager
import tanvd.bayou.implementation.core.code.synthesizer.implementation.EvidenceExtractor
import tanvd.bayou.implementation.core.code.synthesizer.implementation.ParseException
import tanvd.bayou.implementation.core.code.synthesizer.implementation.Parser
import tanvd.bayou.implementation.core.code.synthesizer.implementation.Synthesizer
import tanvd.bayou.implementation.core.ml.AstGenerator
import tanvd.bayou.implementation.core.ml.Evidences
import tanvd.bayou.implementation.core.ml.lda.LdaApi
import tanvd.bayou.implementation.core.ml.lda.LdaContexts
import tanvd.bayou.implementation.core.ml.lda.LdaTypes
import tanvd.bayou.implementation.core.ml.tfidf.TfIdfApi
import tanvd.bayou.implementation.core.ml.tfidf.TfIdfContext
import tanvd.bayou.implementation.core.ml.tfidf.TfIdfTypes
import tanvd.bayou.implementation.utils.JsonUtils
import tanvd.bayou.implementation.utils.Resource
import java.io.*

class BayouSynthesizer {

    @Throws(SynthesiseException::class)
    fun synthesise(code: String): Iterable<String> {
        return synthesiseHelp(code)
    }


    // sampleCount of null means don't send a sampleCount key in the request message.  Means use default sample count.
    @Throws(SynthesiseException::class)
    private fun synthesiseHelp(code: String): Iterable<String> {
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
        val evidence: String
        try {
            evidence = extractEvidence(EvidenceExtractor(), parser)
        } catch (e: ParseException) {
            throw SynthesiseException(e)
        }

        val evidenceData = JsonUtils.readValue(evidence, Evidences::class)


        val tfidfTypes = TfIdfTypes.transform(evidenceData.types)
        val tfidfApi = TfIdfApi.transform(evidenceData.apicalls)
        val tfidfContext = TfIdfContext.transform(evidenceData.context)

        val ldaTypes = LdaTypes.getTopicDistribution(tfidfTypes.map { it.toString() }.toTypedArray()).toTypedArray()
        val ldaApi = LdaApi.getTopicDistribution(tfidfApi.map { it.toString() }.toTypedArray()).toTypedArray()
        val ldaContext = LdaContexts.getTopicDistribution(tfidfContext.map { it.toString() }.toTypedArray()).toTypedArray()


        val result = AstGenerator.generateAsts(ldaApi, ldaTypes, ldaContext, JsonUtils.readValue(evidence, Evidences::class))

        result.forEach { ast ->
            println("START PROGRAM")
            val synthesizedPrograms: List<String> = Synthesizer().execute(parser, ast)
            println(synthesizedPrograms.joinToString(separator = "\n"))
            println("END PROGRAM")
        }

        error("not implemented further")
    }

    companion object {
        /**
         * Place to send logging information.
         */
        private val _logger = LogManager.getLogger(BayouSynthesizer::class.java.name)


        /**
         * @return extractor.execute(code, evidenceClasspath) if value is non-null.
         * @throws SynthesiseException if extractor.execute(code, evidenceClasspath) is null
         */
        // n.b. package static for unit testing without creation
        @Throws(SynthesiseException::class, ParseException::class)
        internal fun extractEvidence(extractor: EvidenceExtractor, parser: Parser): String {
            _logger.debug("entering")
            val evidence = extractor.execute(parser)
            if (evidence == null) {
                _logger.debug("exiting")
                throw SynthesiseException("evidence may not be null.  Input was " + parser.source)
            }
            _logger.trace("evidence:" + evidence)
            _logger.debug("exiting")
            return evidence
        }
    }
}