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


/*
 * The DSL is implemented as a subset of the Eclipse AST. The convention
 * is to use the Eclipse AST class name prefixed with D. Every class that
 * wishes to be in the DSL has to provide an inner class Handle that extends
 * the class Handler. The Handle takes an Eclipse AST element and the visitor
 * (through its constructor) and upon invoking the handle() method, returns
 * the corresponding DSL element. The handle() method may return null if
 * the Eclipse AST element does not satisfy the conditions to be in the DSL.
 *
 * Note that if the DSL element is a subclass of DOMExpression or DASTNode
 * it has to be added to the switch cases in the Handles of both these classes.
 */



package tanvd.bayou.implementation.application.dom

import org.apache.commons.cli.ParseException
import org.apache.commons.io.FileUtils
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.dom.CompilationUnit

import java.io.File
import java.io.IOException

class Driver @Throws(ParseException::class, IOException::class)
constructor(args: Array<String>) {

    internal var options: Options = Options(args)

    @Throws(IOException::class)
    private fun createCompilationUnit(classpath: String?): CompilationUnit {
        val parser = ASTParser.newParser(AST.JLS8)
        val input = File(options.cmdLine.getOptionValue("input-file"))

        parser.setSource(FileUtils.readFileToString(input, "utf-8").toCharArray())
        parser.setKind(ASTParser.K_COMPILATION_UNIT)
        parser.setUnitName("Program.java")
        parser.setEnvironment(arrayOf(classpath ?: ""),
                arrayOf(""), arrayOf("UTF-8"), true)
        parser.setResolveBindings(true)

        return parser.createAST(null) as CompilationUnit
    }

    @Throws(IOException::class)
    fun execute(classpath: String) {
        val cu = createCompilationUnit(classpath)
        val visitor = Visitor(cu, options)
        cu.accept(visitor)
        visitor.output.close()
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                val classpath = System.getenv("CLASSPATH")
                Driver(args).execute(classpath)
            } catch (e: ParseException) {
                println("Unexpected exception: " + e.message)
            } catch (e: IOException) {
                println("Unexpected exception: " + e.message)
            }

        }
    }
}
