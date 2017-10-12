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
package tanvd.bayou.implementation.core.synthesizer.implementation

import com.google.gson.GsonBuilder
import org.eclipse.jface.text.Document
import tanvd.bayou.implementation.core.dsl.*
import java.net.URLClassLoader
import java.util.*

class Synthesizer {

    internal inner class JSONInputWrapper {
        var asts: List<DSubTree>? = null
    }

    private fun getASTsFromNN(astJson: String): List<DSubTree>? {
        val nodeAdapter = RuntimeTypeAdapterFactory.of(DASTNode::class.java, "node")
                .registerSubtype(DAPICall::class.java)
                .registerSubtype(DBranch::class.java)
                .registerSubtype(DExcept::class.java)
                .registerSubtype(DLoop::class.java)
                .registerSubtype(DSubTree::class.java)
        val gson = GsonBuilder().serializeNulls()
                .registerTypeAdapterFactory(nodeAdapter)
                .create()
        val js = gson.fromJson(astJson, JSONInputWrapper::class.java)

        return js.asts
    }

    fun execute(parser: Parser, astJson: String): List<String> {
        val synthesizedPrograms = LinkedList<String>()
        val asts = getASTsFromNN(astJson)

        classLoader = URLClassLoader.newInstance(parser.classpathURLs)

        val cu = parser.cu
        val programs = ArrayList<String>()
        for (ast in asts!!) {
            val visitor = Visitor(ast, Document(parser.source), cu)
            try {
                cu.accept(visitor)
                if (visitor.synthesizedProgram == null)
                    continue
                val program = visitor.synthesizedProgram!!.replace("\\s".toRegex(), "")
                if (!programs.contains(program)) {
                    programs.add(program)
                    synthesizedPrograms.add(visitor.synthesizedProgram!!)
                }
            } catch (e: SynthesisException) {
                // do nothing and try next ast
            }

        }

        return synthesizedPrograms
    }

    companion object {
        lateinit var classLoader: ClassLoader
    }
}
