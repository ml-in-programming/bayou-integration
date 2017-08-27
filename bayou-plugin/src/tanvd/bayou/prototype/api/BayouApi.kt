package tanvd.bayou.prototype.api

import com.google.gson.Gson
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL


object BayouApi {
    private val url = URL("http://localhost:8080/apisynthesis")

    private val client = HttpClientBuilder.create().build()

    fun executeRequest(request: BayouRequest): BayouResponse? {
        return executeText(BayouTextConverter.toProgramText(request))
    }

    private fun executeText(text: String): BayouResponse? {
        val postRequest = HttpPost(url.toURI())

        val gson = Gson()

        val textMap: Map<String, String> = mapOf("code" to text)

        val serialized = gson.toJson(textMap)

        postRequest.addHeader("User-Agent", "curl/7.47.0")
        postRequest.addHeader("Origin", "http://www.askbayou.com")
        postRequest.addHeader("Referer", "http://www.askbayou.com")
        postRequest.addHeader("Content-Type", "text/plain;charset=UTF-8")

        postRequest.entity = StringEntity(serialized)

        val postResponse = client.execute(postRequest)

        val reader = BufferedReader(InputStreamReader(postResponse.entity.content))

        val responseBayou = gson.fromJson(reader, BayouNetworkResponse::class.java)

        return if (responseBayou.success) {
            responseBayou.results?.firstOrNull()?.let {
                BayouTextConverter.fromProgramText(it)
            }
        } else {
            null
        }

    }

    private class BayouNetworkResponse(val success: Boolean, val requestId: String?, val results: List<String>?)

}