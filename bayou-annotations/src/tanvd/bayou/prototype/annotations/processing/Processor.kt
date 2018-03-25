package tanvd.bayou.prototype.annotations.processing

import java.io.File
import java.io.InputStreamReader
import kotlin.reflect.KClass

class FunctionsProcessor {
    fun process(name: String) {
        val json = InputStreamReader(FunctionsProcessor::class.java.getResourceAsStream("/$name.json")).use {
            it.readText()
        }
        val functionArray = JsonUtils.readValue(json, List::class as KClass<List<String>>)
        val functionNameRegex = Regex(".*\\.([^.]+)\\(.*")
        val result = functionArray.filter { it.endsWith(")") }
                .map {
                    functionNameRegex.find(it)!!.groupValues[1]
                }
                .filter { it[0].isLowerCase() }
                .distinct().sorted()

        File("${name}_functions.json").writeText(JsonUtils.writeValueAsString(result))
    }
}

class ClassProcessor {
    fun process(name: String) {
        val json = InputStreamReader(FunctionsProcessor::class.java.getResourceAsStream("/$name.json")).use {
            it.readText()
        }
        val functionArray = JsonUtils.readValue(json, List::class as KClass<List<String>>)
        val result = functionArray.filter { it.endsWith(")") }
                .map {
                    it.takeWhile { it != '(' }
                }
                .filter { it.split(".").last().first().isUpperCase() }
                .map {
                    it.split(".").last()
                }
                .distinct()
        val sortedResult = result.sorted()

        File("${name}_class.json").writeText(JsonUtils.writeValueAsString(sortedResult))
    }
}

fun main(vararg str: String) {
    FunctionsProcessor().process("stdlib")
    ClassProcessor().process("stdlib")

    FunctionsProcessor().process("android")
    ClassProcessor().process("android")
}