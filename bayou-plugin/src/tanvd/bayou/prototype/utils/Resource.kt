package tanvd.bayou.prototype.utils

import tanvd.bayou.implementation.utils.JsonUtils
import java.io.InputStreamReader
import kotlin.reflect.KClass

object Resource {
    fun getText(path: String): String {
        return InputStreamReader(Resource::class.java.classLoader.getResourceAsStream(path)).readText()
    }

    fun <T : Any> getObject(path: String, klass: KClass<T>): T {
        return JsonUtils.readValue(getText(path), klass)
    }
}