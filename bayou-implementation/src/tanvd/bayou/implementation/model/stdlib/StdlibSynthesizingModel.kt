package tanvd.bayou.implementation.model.stdlib

import org.apache.logging.log4j.LogManager
import tanvd.bayou.implementation.core.evidence.EvidenceExtractor
import tanvd.bayou.implementation.model.SynthesizeException
import tanvd.bayou.implementation.model.SynthesizingModel
import tanvd.bayou.implementation.model.stdlib.ast.StdlibAstGenerator
import tanvd.bayou.implementation.model.stdlib.evidence.StdlibEvidences
import tanvd.bayou.implementation.model.stdlib.quality.StdlibEvidenceAstVerification
import tanvd.bayou.implementation.model.stdlib.quality.StdlibRelevanceMeasurement
import tanvd.bayou.implementation.model.stdlib.synthesizer.ParseException
import tanvd.bayou.implementation.model.stdlib.synthesizer.Parser
import tanvd.bayou.implementation.model.stdlib.synthesizer.Synthesizer
import tanvd.bayou.implementation.utils.Resource

class  StdlibSynthesizingModel : SynthesizingModel {

    private val logger = LogManager.getLogger(StdlibSynthesizingModel::class.java)

    private val synthesizer = Synthesizer()

    private val astGenerator = StdlibAstGenerator()

    override fun synthesize(code: String, maxPrograms: Int): Iterable<String> {
        val combinedClassPath = Resource.getClassPath("artifacts/jar/evidence.jar")
        /*
         * Parse the program.
         */
        val parser: Parser = try {
            Parser(code, combinedClassPath).also {
                it.parse()
            }
        } catch (e: ParseException) {
            throw SynthesizeException(e)
        }

        /*
         * Extract a description of the evidence in the search code that should guide AST results generation.
         */
        val evidence: StdlibEvidences = try {
            StdlibEvidences(EvidenceExtractor().execute(parser.compilationUnit!!)!!)
        } catch (e: ParseException) {
            throw SynthesizeException(e)
        }

        /**
         * Generate asts, verify them and then dedup and sort by relevance
         */
        val asts = StdlibRelevanceMeasurement.sortAndDedup(astGenerator.process(evidence.toAstGeneratorInput()).filter { StdlibEvidenceAstVerification.verify(it, evidence) })

        /*
         * Try to generate programs from asts
         */
        val generatedPrograms = asts.mapNotNull {
            try {
                synthesizer.execute(parser, it)
            } catch (e: Exception) {
                logger.error("Exception during synthezing (concretizing) of program", e)
                null
            }
        }.map {
                    it.joinToString(separator = "\n") { it }
                }

        return generatedPrograms.take(maxPrograms)
    }

}