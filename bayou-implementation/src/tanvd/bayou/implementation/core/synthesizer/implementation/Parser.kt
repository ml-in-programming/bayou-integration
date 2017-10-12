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

import org.apache.logging.log4j.LogManager
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.compiler.IProblem
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.dom.CompilationUnit
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.util.*

class Parser @Throws(ParseException::class)
constructor(source: String, classpath: String) {

    var source: String
        internal set
    var classpath: String? = null
        internal set
    internal var classpathURLs: Array<URL>
    lateinit var cu: CompilationUnit
        internal set

    init {
        this.source = source
        this.classpath = classpath

        _logger.trace("source: " + source)
        _logger.trace("classpath:" + classpath)

        val urlList = ArrayList<URL>()
        for (cp in classpath.split(File.pathSeparator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            _logger.trace("cp: " + cp)
            try {
                urlList.add(URL("jar:file:$cp!/"))
            } catch (e: MalformedURLException) {
                throw ParseException("Malformed URL in classpath.")
            }

        }
        this.classpathURLs = urlList.toTypedArray()
    }

    @Throws(ParseException::class)
    fun parse() {
        val parser = ASTParser.newParser(AST.JLS8)
        parser.setSource(source.toCharArray())
        val options = JavaCore.getOptions()
        options.put("org.eclipse.jdt.core.compiler.source", "1.8")
        parser.setCompilerOptions(options)
        parser.setKind(ASTParser.K_COMPILATION_UNIT)
        parser.setUnitName("Program.java")
        parser.setEnvironment(arrayOf(classpath ?: ""),
                arrayOf(""), arrayOf("UTF-8"), true)
        parser.setResolveBindings(true)
        cu = parser.createAST(null) as CompilationUnit

        val problems = cu.problems.filter { p ->
            p.isError &&
                    p.id != IProblem.PublicClassMustMatchFileName && // we use "Program.java"

                    p.id != IProblem.ParameterMismatch
        }
        if (problems.isNotEmpty())
            throw ParseException(problems)
    }

    companion object {

        /**
         * Place to send logging information.
         */
        private val _logger = LogManager.getLogger(EvidenceExtractor::class.java.name)
    }
}
