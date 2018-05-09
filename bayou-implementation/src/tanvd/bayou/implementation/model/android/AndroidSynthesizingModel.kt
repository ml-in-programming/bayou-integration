package tanvd.bayou.implementation.model.android

import org.apache.logging.log4j.LogManager
import tanvd.bayou.implementation.core.evidence.EvidenceExtractor
import tanvd.bayou.implementation.facade.SynthesisProgress
import tanvd.bayou.implementation.model.SynthesizeException
import tanvd.bayou.implementation.model.SynthesizingModel
import tanvd.bayou.implementation.model.android.ast.AndroidAstGenerator
import tanvd.bayou.implementation.model.android.evidence.AndroidEvidences
import tanvd.bayou.implementation.model.android.quality.AndroidEvidenceAstVerification
import tanvd.bayou.implementation.model.android.quality.AndroidRelevanceMeasurement
import tanvd.bayou.implementation.model.android.synthesizer.ParseException
import tanvd.bayou.implementation.model.android.synthesizer.Parser
import tanvd.bayou.implementation.model.android.synthesizer.Synthesizer
import tanvd.bayou.implementation.utils.Resource
import java.io.File

class AndroidSynthesizingModel : SynthesizingModel {

    private val logger = LogManager.getLogger(AndroidSynthesizingModel::class.java)

    private val synthesizer = Synthesizer()

    private val astGenerator = AndroidAstGenerator()

    override fun synthesize(code: String, maxPrograms: Int, synthesisProgress: SynthesisProgress): Iterable<String> {
        val combinedClassPath = Resource.getClassPath("artifacts/jar/evidence.jar") + File.pathSeparator + Resource.getClassPath("artifacts/jar/android.jar")
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
        val evidence: AndroidEvidences = try {
            AndroidEvidences(EvidenceExtractor().execute(parser.cu)!!)
        } catch (e: ParseException) {
            throw SynthesizeException(e)
        }

        /**
         * Generate asts, verify them and then dedup and sort by relevance
         */
        val asts = AndroidRelevanceMeasurement.sortAndDedup(astGenerator.process(evidence.toAstGeneratorInput(),
                synthesisProgress, maxPrograms).filter { AndroidEvidenceAstVerification.verify(it, evidence) })

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