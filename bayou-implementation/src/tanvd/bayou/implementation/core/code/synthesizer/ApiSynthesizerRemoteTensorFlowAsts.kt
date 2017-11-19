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
import org.json.JSONObject
import tanvd.bayou.implementation.core.code.synthesizer.implementation.EvidenceExtractor
import tanvd.bayou.implementation.core.code.synthesizer.implementation.ParseException
import tanvd.bayou.implementation.core.code.synthesizer.implementation.Parser
import tanvd.bayou.implementation.core.code.synthesizer.implementation.Synthesizer
import tanvd.bayou.implementation.core.ml.AstGenerator
import tanvd.bayou.implementation.core.ml.Evidences
import tanvd.bayou.implementation.core.ml.lda.LdaApi
import tanvd.bayou.implementation.core.ml.lda.LdaContexts
import tanvd.bayou.implementation.core.ml.lda.LdaTypes
//import tanvd.bayou.implementation.core.ml.lda.LdaInference
import tanvd.bayou.implementation.utils.JsonUtil

import java.io.*
import java.net.Socket
import java.nio.charset.StandardCharsets

/**
 * A synthesis strategy that relies on a remote server that uses TensorFlow to create ASTs from extracted evidence.
 *
 * @param tensorFlowHost  The network name of the tensor flow host server. May not be null.
 * @param tensorFlowPort The port the tensor flow host server on which connections requests are expected.
 * May not be negative.
 * @param maxNetworkWaitTimeMs The maximum amount of time to wait on a response from the tensor flow server on each
 * request. 0 means forever. May not be negative.
 * @param evidenceClasspath A classpath string that includes the class edu.rice.cs.caper.bayou.annotations.Evidence.
 * May not be null.
 * @param androidJarPath The path to android.jar. May not be null.
 */
class ApiSynthesizerRemoteTensorFlowAsts(private val _tensorFlowHost: String?, private val _tensorFlowPort: Int,
                                         private val _maxNetworkWaitTimeMs: Int, private val _evidenceClasspath: String?,
                                         private val _androidJarPath: File?) : ApiSynthesizer {

    init {
        _logger.debug("entering")

        if (_tensorFlowHost == null) {
            _logger.debug("exiting")
            throw NullPointerException("tensorFlowHost")
        }

        if (_tensorFlowHost.trim { it <= ' ' } == "") {
            _logger.debug("exiting")
            throw IllegalArgumentException("tensorFlowHost must be nonempty and contain non-whitespace characters")
        }

        if (_tensorFlowPort < 0) {
            _logger.debug("exiting")
            throw IllegalArgumentException("tensorFlowPort may not be negative")
        }

        if (_maxNetworkWaitTimeMs < 0) {
            _logger.debug("exiting")
            throw IllegalArgumentException("tensorFlowPort may not be negative")
        }

        if (_evidenceClasspath == null) {
            _logger.debug("exiting")
            throw NullPointerException("evidenceClasspath")
        }

        if (_androidJarPath == null) {
            _logger.debug("exiting")
            throw NullPointerException("androidJarPath")
        }

        if (!_androidJarPath.exists()) {
            _logger.debug("exiting")
            throw IllegalArgumentException("androidJarPath does not exist: " + _androidJarPath.absolutePath)
        }
        _logger.trace("_tensorFlowHost:" + _tensorFlowHost)
        _logger.trace("_evidenceClasspath:" + _evidenceClasspath)
        _logger.debug("exiting")
    }

    @Throws(SynthesiseException::class)
    override fun synthesise(code: String, maxProgramCount: Int): Iterable<String> {
        return synthesiseHelp(code, maxProgramCount, null)
    }

    @Throws(SynthesiseException::class)
    override fun synthesise(code: String, maxProgramCount: Int, sampleCount: Int): Iterable<String> {
        return synthesiseHelp(code, maxProgramCount, sampleCount)
    }

    // sampleCount of null means don't send a sampleCount key in the request message.  Means use default sample count.
    @Throws(SynthesiseException::class)
    private fun synthesiseHelp(code: String?, maxProgramCount: Int, sampleCount: Int?): Iterable<String> {
        _logger.debug("entering")

        if (code == null) {
            _logger.debug("exiting")
            throw NullPointerException("code")
        }

        if (sampleCount != null && sampleCount < 1)
            throw IllegalArgumentException("sampleCount must be 1 or greater")

        val combinedClassPath = _evidenceClasspath + File.pathSeparator + _androidJarPath!!.absolutePath

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

        /*
         * Contact the remote Python server and provide evidence to be fed to Tensor Flow to generate solution
         * ASTs.
         */
        var astsJson: String = ""
        try {
            Socket(_tensorFlowHost, _tensorFlowPort).use { pyServerSocket ->
                pyServerSocket.soTimeout = _maxNetworkWaitTimeMs // only wait this long for response then throw exception

                val requestObj = JSONObject()
                requestObj.put("request type", "generate asts")
                requestObj.put("evidence", evidence)
                requestObj.put("max ast count", maxProgramCount)

                if (sampleCount != null)
                    requestObj.put("sample count", sampleCount)

                sendString(requestObj.toString(2), DataOutputStream(pyServerSocket.getOutputStream()))

                astsJson = receiveString(pyServerSocket.getInputStream())
                _logger.trace("astsJson:" + astsJson)
            }
        } catch (e: IOException) {
            _logger.debug("exiting")
            throw SynthesiseException(e)
        }

        val tfidf = JsonUtil.readValue(astsJson, Array<Array<Array<Double>>>::class)
        val ldaTypes = LdaTypes.getTopicDistribution(tfidf[1][0].map { it.toString() }.toTypedArray()).toTypedArray()
        val ldaApi = LdaApi.getTopicDistribution(tfidf[0][0].map { it.toString() }.toTypedArray()).toTypedArray()
        val ldaContext = LdaContexts.getTopicDistribution(tfidf[2][0].map { it.toString() }.toTypedArray()).toTypedArray()


        val result = AstGenerator.generateAsts(ldaApi, ldaTypes, ldaContext, JsonUtil.readValue(evidence, Evidences::class))

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
        private val _logger = LogManager.getLogger(ApiSynthesizerRemoteTensorFlowAsts::class.java.name)


        /**
         * Reads the first 4 bytes of inputStream and interprets them as a 32-bit big endian signed integer 'length'.
         * Reads the next 'length' bytes from inputStream and returns them as a UTF-8 encoded string.
         *
         * @param inputStream the source of bytes
         * @return the string form of the n+4 bytes
         * @throws IOException if there is a problem reading from the stream.
         */
        // n.b. package static for unit testing without creation
        @Throws(IOException::class)
        internal fun receiveString(inputStream: InputStream): String {
            _logger.debug("entering")
            var responseBytes: ByteArray = kotlin.ByteArray(0)
            run {
                val dis = DataInputStream(inputStream)
                val numResponseBytes = dis.readInt()
                responseBytes = ByteArray(numResponseBytes)
                dis.readFully(responseBytes)
            }

            _logger.debug("exiting")
            return String(responseBytes, StandardCharsets.UTF_8)

        }

        /**
         * Gets the byte representation of string in UTF-8 encoding, sends the length of the byte encoding across
         * outputStream via writeInt(...), and then sends the byte representation via write(byte[]).
         *
         * @param string the string to send
         * @param outputStream the destination of string
         * @throws IOException if there is a problem sending the string
         */
        // n.b. package static for unit testing without creation
        @Throws(IOException::class)
        internal fun sendString(string: String, outputStream: DataOutput) {
            _logger.debug("entering")
            val stringBytes = string.toByteArray(StandardCharsets.UTF_8)
            outputStream.writeInt(stringBytes.size)
            outputStream.write(stringBytes)
            _logger.debug("exiting")
        }

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