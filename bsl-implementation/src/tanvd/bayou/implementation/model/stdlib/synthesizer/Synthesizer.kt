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
package tanvd.bayou.implementation.model.stdlib.synthesizer

import com.google.googlejavaformat.java.Formatter
import com.google.googlejavaformat.java.FormatterException
import org.eclipse.jface.text.Document
import tanvd.bayou.implementation.model.stdlib.synthesizer.dsl.DSubTree
import java.net.URLClassLoader
import java.util.*

class Synthesizer {

    var mode: Mode

    enum class Mode {
        COMBINATORIAL_ENUMERATOR,
        CONDITIONAL_PROGRAM_GENERATOR
    }

    constructor() {
        this.mode = Mode.COMBINATORIAL_ENUMERATOR // default mode
    }

    constructor(mode: Mode) {
        this.mode = mode
    }

    fun execute(parser: Parser, ast: DSubTree): List<String> {
        val synthesizedPrograms = LinkedList<String>()
        classLoader = URLClassLoader.newInstance(parser.classpathURLs)

        val cu = parser.compilationUnit
        val programs = ArrayList<String>()
        val visitor = Visitor(ast, Document(parser.source), cu!!, mode)
        try {
            cu.accept(visitor)
            if (visitor.synthesizedProgram == null)
                return listOf("ERROR")
            val program = visitor.synthesizedProgram!!.replace("\\s".toRegex(), "")
            if (!programs.contains(program)) {
                val formattedProgram = Formatter().formatSource(visitor.synthesizedProgram)
                programs.add(program)
                synthesizedPrograms.add(formattedProgram)
            }
        } catch (e: SynthesisException) {
            // do nothing and try next ast
        } catch (e: FormatterException) {
        }

        return synthesizedPrograms
    }

    companion object {

        internal var classLoader: ClassLoader? = null
    }
}
