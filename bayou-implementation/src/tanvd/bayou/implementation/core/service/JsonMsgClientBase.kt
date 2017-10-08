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

import org.apache.logging.log4j.LogManager
import org.json.JSONException
import org.json.JSONObject

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * Created by barnett on 3/15/17.
 */
open class JsonMsgClientBase
/**
 * Creates a new client that communicates with a remote completion server at the given address and port
 * to fulfil code completion requests.
 *
 * @param host the hostname of the remove server. May not be null.
 * @param port the port on the remote server listening for connection requests. May not be negative.
 * @throws IllegalArgumentException if host is null.
 * @throws IllegalArgumentException if host is only whitespace.
 * @throws IllegalArgumentException if port is less than 0 or greater than 65535.
 */
(
        /**
         * The hostname of the remote server to which requests will be routed.
         */
        protected val host: String?,
        /**
         * The port on _host to which requests will be routed.
         */
        protected val port: Int) {

    init {
        _logger.debug("entering")

        if (host == null)
            throw IllegalArgumentException("host may not be null")

        if (host.trim { it <= ' ' }.length == 0)
            throw IllegalArgumentException("host must not be only whitespace")

        if (port < 0 || port > 65535)
            throw IllegalArgumentException("port must be in the range 0...65535 inclusive.")

        _logger.debug("exiting")
    }// has a problem throwing an exception after a check for null without

    @Throws(IOException::class)
    protected fun sendRequest(msg: JSONObject, outStream: OutputStream) {
        _logger.debug("entering")

        /*
         * Determine the byte content of the request message header and body.
         */
        var requestMessageBodyBytes: ByteArray = kotlin.ByteArray(0)
        run {
            val jsonString = msg.toString()
            val jsonStringEncoded = Charset.forName("UTF-8").encode(jsonString)
            requestMessageBodyBytes = ByteArray(jsonStringEncoded.limit())
            System.arraycopy(jsonStringEncoded.array(), 0, requestMessageBodyBytes, 0, requestMessageBodyBytes.size)
        }

        val requestMessageHeaderBytes = ByteBuffer.allocate(4).putInt(requestMessageBodyBytes.size).array()

        /*
         * Write the request message to the outStream.
         */
        outStream.write(requestMessageHeaderBytes)
        outStream.write(requestMessageBodyBytes)
        outStream.flush()

        _logger.debug("exiting")
    }

    protected fun parseResponseMessageBodyToJson(messageBody: String?): JSONObject {
        _logger.debug("entering parseResponseMessageBodyToJson")

        if (messageBody == null)
            throw IllegalArgumentException("messageBody may not be null")

        /*
         * Parse messageBody into a JSON object.
         */
        val messageBodyObj: JSONObject
        try {
            messageBodyObj = JSONObject(messageBody)
        } catch (e: JSONException) // hide from public callers on the stack we are using a specific JSON lib.
        {
            _logger.debug("exiting parseResponseMessageBodyToJson")
            throw IllegalArgumentException(messageBody + " is not a valid JSON string", e)
        }

        /*
         * If server response is an error response, throw exception.
         */
        val successKey = "success"
        val success: Boolean
        try {
            success = messageBodyObj.getBoolean(successKey)
        } catch (e: JSONException) {
            _logger.debug("exiting parseResponseMessageBodyToJson")
            throw IllegalArgumentException("Given JSON " + messageBody + " does not have boolean " +
                    successKey + " member.")
        }

        if (!success) {
            val errorMessageKey = "errorMessage"
            try {
                messageBodyObj.getString(errorMessageKey)
            } catch (e: JSONException) {
                _logger.debug("exiting parseResponseMessageBodyToJson")
                throw IllegalArgumentException("Given JSON " + messageBody + " does not have string " +
                        errorMessageKey + " member.")
            }

        }

        _logger.debug("exiting parseResponseMessageBodyToJson")
        return messageBodyObj
    }

    companion object {
        /**
         * Place to send logging information.
         */
        private val _logger = LogManager.getLogger(JsonMsgClientBase::class.java.name)

        /**
         * Reads a response message from the given InputStream.  Expected format as defined in
         * edu.rice.pliny.programs.code_completion_server.CodeCompletionServer class contract.
         *
         * @param in the stream from which to read the response message
         * @return the response message body (without header) encoded in the UTF-8 charset.
         * @throws IOException if there is a problem reading from in.
         * @throws IllegalArgumentException if in is null.
         */
        // n.b. static to facilitate testing without construction
        @Throws(IOException::class)
        protected fun readResponseAsString(`in`: InputStream?): String {
            _logger.debug("entering readResponseAsString")

            if (`in` == null)
                throw IllegalArgumentException("in may not be null")

            /*
         * Read response header.
         */
            var numBytesInMessageBody: Int = 0
            run {
                val messageHeaderBytes = ByteArray(4)
                fillBuffer(messageHeaderBytes, `in`)
                numBytesInMessageBody = ByteBuffer.wrap(messageHeaderBytes).int
            }

            /*
         * Read response body.
         */
            val responseBodyBytes = ByteArray(numBytesInMessageBody)
            fillBuffer(responseBodyBytes, `in`)

            _logger.debug("exiting readResponseAsString")
            return String(responseBodyBytes, Charset.forName("UTF-8")) // UTF-8 by response message format contract
        }

        /**
         * Copies the next buffer.length bytes from in to buffer starting at buffer[0].
         *
         * @param buffer the buffer to fill
         * @param in the source of bytes
         * @throws IOException if the end of in is read before buffer is filled.
         * @throws IOException if there is a problem reading from in.
         */
        // n.b. static to facilitate testing without construction
        @Throws(IOException::class)
        internal fun fillBuffer(buffer: ByteArray, `in`: InputStream) {
            _logger.debug("entering fillBuffer")

            for (i in buffer.indices) {
                val nextByte = `in`.read()
                if (nextByte == -1)
                    throw IOException("Early end of stream detected.")

                buffer[i] = nextByte.toByte()
            }

            _logger.debug("exiting fillBuffer")
        }
    }
}
