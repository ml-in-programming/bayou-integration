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
package tanvd.bayou.implementation.application.server

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.eclipse.jetty.http.HttpStatus
import tanvd.bayou.implementation.synthesis.ApiSynthesizer
import tanvd.bayou.implementation.synthesis.ApiSynthesizerRemoteTensorFlowAsts

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by barnett on 4/7/17.
 */
class ApiSynthesisHealthCheckServlet : HttpServlet() {

    /**
     * How requests should be fulfilled.
     */
    private val _synthesisRequestProcessor: ApiSynthesizer

    /**
     * Public so Jetty can instantiate.
     */
    init {
        _logger.debug("entering")
        _synthesisRequestProcessor = ApiSynthesizerRemoteTensorFlowAsts("localhost", 8084,
                    Configuration.SynthesizeTimeoutMs,
                    Configuration.EvidenceClasspath,
                    Configuration.AndroidJarPath)

        _logger.debug("exiting")
    }

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        _logger.debug("entering")

        /*
         * Perform a generic API synth call and check that no exceptions are generated and that at least one result
         * is found. If so return HTTP status 200. Otherwise, 500.
         */
        try {

            val code = "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                    "\n" +
                    "public class TestIO1 {\n" +
                    "\n" +
                    "    // Read from a file\n" +
                    "    void read(String file) {\n" +
                    "        Evidence.apicalls(\"readLine\");\n" +
                    "    }   \n" +
                    "}"

            val results = _synthesisRequestProcessor.synthesise(code, 1)

            if (!results.iterator().hasNext()) {
                _logger.error("health check failed due to empty results.")
                resp.status = HttpStatus.INTERNAL_SERVER_ERROR_500
            }

            resp.writer.write("Ok.")

        } catch (e: Throwable) {
            _logger.error("health check failed due to exception", e)
            resp.status = HttpStatus.INTERNAL_SERVER_ERROR_500
        }

        _logger.debug("exiting")
    }

    companion object {
        /**
         * Place to send logging information.
         */
        private val _logger = LogManager.getLogger(ApiSynthesisHealthCheckServlet::class.java.name)
    }


}
