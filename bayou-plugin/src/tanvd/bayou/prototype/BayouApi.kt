package tanvd.bayou.prototype

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import java.net.URL
import java.io.InputStreamReader
import java.io.BufferedReader


object BayouApi {
    private val url = URL("http://localhost:8080/apisynthesis")

    private val client = HttpClientBuilder.create().build()

    fun generateFromText(text: String) : String? {
        val postRequest = HttpPost(url.toURI())

        val gson = Gson()

        val textMap : Map<String, String> = mapOf("code" to text)

        val serialized = gson.toJson(textMap)

        postRequest.addHeader("User-Agent", "curl/7.47.0")
        postRequest.addHeader("Origin", "http://www.askbayou.com")
        postRequest.addHeader("Referer", "http://www.askbayou.com")
        postRequest.addHeader("Content-Type", "text/plain;charset=UTF-8")

        postRequest.entity = StringEntity(serialized)

        val postResponse = client.execute(postRequest)

        val reader = BufferedReader(InputStreamReader(postResponse.entity.content))

        val responseBayou = gson.fromJson(reader, BayouResponse::class.java)

        val result: String? = if (responseBayou.success) responseBayou.results?.firstOrNull() else null

        return result

    }

    class BayouResponse (val success: Boolean, val requestId: String?, val results: List<String>?)
}