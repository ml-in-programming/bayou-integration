package tanvd.bayou.implementation.model.configurable

import org.apache.logging.log4j.LogManager
import tanvd.bayou.implementation.Config
import tanvd.bayou.implementation.core.evidence.EvidenceExtractor
import tanvd.bayou.implementation.model.SynthesizeException
import tanvd.bayou.implementation.model.SynthesizingModel
import tanvd.bayou.implementation.model.configurable.ast.ConfigurableAstGenerator
import tanvd.bayou.implementation.model.configurable.ast.ConfigurableAstGeneratorInput
import tanvd.bayou.implementation.model.configurable.evidence.ConfigurableEvidences
import tanvd.bayou.implementation.model.configurable.quality.ConfigurableEvidenceAstVerification
import tanvd.bayou.implementation.model.configurable.quality.ConfigurableRelevanceMeasurement
import tanvd.bayou.implementation.model.configurable.wrangle.WrangleModelProvider
import tanvd.bayou.implementation.model.configurable.synthesizer.ParseException
import tanvd.bayou.implementation.model.configurable.synthesizer.Parser
import tanvd.bayou.implementation.model.configurable.synthesizer.Synthesizer
import tanvd.bayou.implementation.model.configurable.synthesizer.toInnerMode
import tanvd.bayou.implementation.utils.Downloader
import java.io.File

class ConfigurableSynthesizingModel(val config: Config) : SynthesizingModel {

    private val logger = LogManager.getLogger(ConfigurableSynthesizingModel::class.java)

    private val synthesizer = Synthesizer(config.concretization.mode.toInnerMode())

    private val astGenerator = ConfigurableAstGenerator(config)

    override fun synthesize(code: String, maxPrograms: Int): Iterable<String> {
        /*
         * Get combined class path from config and download all files
         */
        val combinedClassPath = config.classpath.map {
            val file  = Downloader.downloadFile(config.name, it.lib_name, it.lib_url)
            file.absolutePath
        }.joinToString(separator = File.pathSeparator)

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
        val evidence: ConfigurableEvidences = try {
            ConfigurableEvidences(config.evidences.map { it.type }, EvidenceExtractor().execute(parser.compilationUnit!!)!!)
        } catch (e: ParseException) {
            throw SynthesizeException(e)
        }

        val input = evidence.evidences.map { (type, list) ->
            val evConfig = config.evidences.first { it.type == type}
            type to WrangleModelProvider.wrangle(config.name, evConfig, list)
        }.let {
                    ConfigurableAstGeneratorInput(it.map { it.first }, it.toMap())
                }

        /**
         * Generate asts, verify them and then dedup and sort by relevance
         */
        val asts = ConfigurableRelevanceMeasurement.sortAndDedup(astGenerator.process(input).filter { ConfigurableEvidenceAstVerification.verify(it, evidence) })

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

