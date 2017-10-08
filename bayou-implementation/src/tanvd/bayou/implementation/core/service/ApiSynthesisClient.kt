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
package tanvd.bayou.implementation.core.service

import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.HttpClients
import org.apache.logging.log4j.LogManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.IOException
import java.net.SocketTimeoutException
import java.util.LinkedList

/**
 * A client for sending API synthesis JSON requests to a remote server over a socket.
 */
class ApiSynthesisClient
/**
 * Creates a new client that communicates with a remote synthesis server at the given address and port
 * to fulfil code completion requests.
 *
 * @param host the hostname of the remove server. May not be null.
 * @param port the port on the remote server listening for connection requests. May not be negative or greater than
 * 65535.
 * @throws IllegalArgumentException if host is null.
 * @throws IllegalArgumentException if host is only whitespace.
 * @throws IllegalArgumentException if port is less than 0 or greater than 65535.
 */
(host: String, port: Int) : JsonMsgClientBase(host, port) {

    /**
     * Sends an API synthesis request of the given code to the remote server and returns the solution responses
     * from the server.
     *
     * If any call to read() for the server's response from the underlying socket does not complete within
     * 30 seconds this method will throw a SocketTimeoutException.
     *
     * @param code the code to be examined by the remote server for synthesis. May not be null.
     * @param maxProgramCount the maximum number of programs to return
     * @return the result of the synthesis. Never null.
     * @throws IOException if there is a problem communicating with the remote server
     * @throws SynthesisError if a non-communication error occurs during synthesis
     * @throws IllegalArgumentException if code is null
     * @throws IllegalArgumentException if maxProgramCount is not a natural number
     * @throws SocketTimeoutException if the server does not respond in a timely fashion
     */
    @Throws(IOException::class, SynthesisError::class)
    fun synthesise(code: String, maxProgramCount: Int): List<String> {
        return synthesizeHelp(code, maxProgramCount, null)
    }

    /**
     * Sends an API synthesis request of the given code to the remote server and returns the solution responses
     * from the server.
     *
     * If any call to read() for the server's response from the underlying socket does not complete within
     * 30 seconds this method will throw a SocketTimeoutException.
     *
     * @param code the code to be examined by the remote server for synthesis. May not be null.
     * @param sampleCount the number of model samples to perform during synthesis. Must be a natural number.
     * @param maxProgramCount the maximum number of programs to return. Must be a natural number.
     * @return the result of the synthesis. Never null.
     * @throws IOException if there is a problem communicating with the remote server
     * @throws SynthesisError if a non-communication error occurs during synthesis
     * @throws IllegalArgumentException if code is null
     * @throws IllegalArgumentException if maxProgramCount or sampleCount is not a natural number
     * @throws SocketTimeoutException if the server does not respond in a timely fashion
     */
    @Throws(IOException::class, SynthesisError::class)
    fun synthesise(code: String, maxProgramCount: Int, sampleCount: Int): List<String> {
        return synthesizeHelp(code, maxProgramCount, sampleCount)
    }

    @Throws(IOException::class, SynthesisError::class)
    private fun synthesizeHelp(code: String?, maxProgramCount: Int, sampleCount: Int?): List<String> {
        _logger.debug("entering")

        if (code == null)
            throw IllegalArgumentException("code may not be null")

        if (maxProgramCount < 1)
            throw IllegalArgumentException("maxProgramCount must be a natural number")

        if (sampleCount != null && sampleCount < 1)
            throw IllegalArgumentException("sampleCount must be a natural number if non-null")

        /*
         * Create request and send to server.
         */
        val requestMsg = JSONObject()
        requestMsg.put("code", code)
        requestMsg.put("max program count", maxProgramCount)
        if (sampleCount != null)
            requestMsg.put("sample count", sampleCount)

        val httpclient = HttpClients.createDefault()
        val post = HttpPost("http://$host:$port/apisynthesis")
        post.addHeader("Origin", "http://askbayou.com")
        post.entity = ByteArrayEntity(requestMsg.toString(4).toByteArray())

        /*
         * Read and parse the response from the server.
         */
        var responseBodyObj: JSONObject? = null
        run {
            val response = httpclient.execute(post)
            if (response.statusLine.statusCode != 200 && response.statusLine.statusCode != 400) {
                throw IOException("Unexpected status code: " + response.statusLine.statusCode)
            }

            var responseBodyAsString: String = ""
            run {
                val responseBytes = IOUtils.toByteArray(response.entity.content)
                responseBodyAsString = String(responseBytes)
            }

            try {
                responseBodyObj = parseResponseMessageBodyToJson(responseBodyAsString)
            } catch (e: IllegalArgumentException) {
                _logger.debug("exiting")
                throw SynthesisError(e.message ?: "")
            }
        }

        _logger.debug("exiting")
        return parseResponseMessageBody(responseBodyObj)
    }

    /**
     * Extracts the ordered code completion results from the server's JSON response and returns them in List form.
     *
     * @param messageBodyObj the server's response
     * @return the value of the JSON results string array with name "results"
     * @throws SynthesisError if the success flag on the given JSON object is false.
     * @throws IllegalArgumentException if the JSON object is not formatted correctly.
     */
    @Throws(SynthesisError::class)
    private fun parseResponseMessageBody(messageBodyObj: JSONObject?): List<String> {
        _logger.debug("entering")

        if (messageBodyObj == null) {
            _logger.debug("exiting")
            throw IllegalArgumentException("messageBody may not be null")
        }

        /*
         * Retrieve the value of the success field.
         */
        var successValue: Boolean = false
        run {
            val SUCCESS = "success"
            if (!messageBodyObj.has(SUCCESS)) {
                _logger.debug("exiting")
                throw IllegalArgumentException("Given JSON does not have " + SUCCESS + " member: "
                        + messageBodyObj.toString())
            }

            try {
                successValue = messageBodyObj.getBoolean(SUCCESS)
            } catch (e: JSONException) {
                _logger.debug("exiting")
                throw IllegalArgumentException("Given JSON " + SUCCESS + " member is not a boolean: " +
                        messageBodyObj.toString())
            }
        }

        /*
         * If server response is an error response, throw exception.
         */
        if (!successValue) {
            val errorMessageValue = messageBodyObj.get("errorMessage").toString()
            if (errorMessageValue == "parseException") {
                val EXCEPTION_MESSAGE = "exceptionMessage"
                if (messageBodyObj.has(EXCEPTION_MESSAGE)) {
                    val exceptionMessage = messageBodyObj.get(EXCEPTION_MESSAGE).toString()
                    throw ParseError(exceptionMessage)
                }

            }

            _logger.debug("exiting")
            throw SynthesisError(errorMessageValue)
        }

        /*
         * Retrieve the completions from the response and populate them to completions.
         */
        val completions = LinkedList<String>()
        run {
            val resultsKey = "results"
            var completionsArray: JSONArray? = null
            run {
                try {
                    completionsArray = messageBodyObj.getJSONArray(resultsKey)
                } catch (e: JSONException) {
                    _logger.debug("exiting")
                    throw IllegalArgumentException("Given JSON does not have string array " + resultsKey + "" +
                            " member: " + messageBodyObj.toString())
                }
            }

            for (i in 0 until completionsArray!!.length()) {
                try {
                    completions.add(completionsArray!!.getString(i))
                } catch (e: JSONException) // some element of the array not a String
                {
                    _logger.debug("exiting")
                    throw IllegalArgumentException("Given JSON  has a non-string element in " + resultsKey +
                            " member: " + messageBodyObj.toString())
                }

            }
        }

        _logger.debug("exiting")
        return completions
    }

    companion object {
        /**
         * Place to send logging information.
         */
        private val _logger = LogManager.getLogger(ApiSynthesisClient::class.java.name)
    }


}
