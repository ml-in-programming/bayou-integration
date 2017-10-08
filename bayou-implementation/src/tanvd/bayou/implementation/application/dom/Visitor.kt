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
package tanvd.bayou.implementation.application.dom

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair
import org.eclipse.jdt.core.dom.*
import tanvd.bayou.implementation.core.dsl.DASTNode
import tanvd.bayou.implementation.core.dsl.DSubTree
import tanvd.bayou.implementation.core.dsl.Sequence
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.util.*

class Visitor @Throws(FileNotFoundException::class)
constructor(val unit: CompilationUnit, val options: Options) : ASTVisitor() {
    val output: PrintWriter
    val gson: Gson

    var allMethods: MutableList<MethodDeclaration>

    internal var first = true

    internal inner class JSONOutputWrapper(var file: String, var ast: DSubTree, var sequences: List<Sequence>, var javadoc: String)

    init {
        this.gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

        if (options.cmdLine.hasOption("output-file"))
            this.output = PrintWriter(options.cmdLine.getOptionValue("output-file"))
        else
            this.output = PrintWriter(System.out)

        allMethods = ArrayList()
        V = this
    }

    override fun visit(clazz: TypeDeclaration?): Boolean {
        if (clazz!!.isInterface)
            return false
        val classes = ArrayList<TypeDeclaration>()
        classes.addAll(Arrays.asList(*clazz.types))
        classes.add(clazz)

        for (cls in classes)
            allMethods.addAll(Arrays.asList(*cls.methods))
        val constructors = allMethods.filter { m -> m.isConstructor }
        val publicMethods = allMethods.filter { m -> !m.isConstructor && Modifier.isPublic(m.modifiers) }

        val astsWithJavadoc = HashSet<Pair<DSubTree, String>>()
        if (!constructors.isEmpty() && !publicMethods.isEmpty()) {
            for (c in constructors)
                for (m in publicMethods) {
                    val javadoc = Utils.getJavadoc(m, options.JAVADOC_TYPE)
                    val ast = DOMMethodDeclaration(c).handle()
                    ast.addNodes(DOMMethodDeclaration(m).handle().nodes)
                    if (ast.isValid)
                        astsWithJavadoc.add(ImmutablePair(ast, javadoc))
                }
        } else if (!constructors.isEmpty()) { // no public methods, only constructor
            for (c in constructors) {
                val javadoc = Utils.getJavadoc(c, options.JAVADOC_TYPE)
                val ast = DOMMethodDeclaration(c).handle()
                if (ast.isValid)
                    astsWithJavadoc.add(ImmutablePair(ast, javadoc))
            }
        } else if (!publicMethods.isEmpty()) { // no constructors, methods executed typically through Android callbacks
            for (m in publicMethods) {
                val javadoc = Utils.getJavadoc(m, options.JAVADOC_TYPE)
                val ast = DOMMethodDeclaration(m).handle()
                if (ast.isValid)
                    astsWithJavadoc.add(ImmutablePair(ast, javadoc))
            }
        }

        for (astDoc in astsWithJavadoc) {
            val sequences = ArrayList<Sequence>()
            sequences.add(Sequence())
            try {
                astDoc.left.updateSequences(sequences, options.MAX_SEQS, options.MAX_SEQ_LENGTH)
                val uniqSequences = ArrayList(HashSet(sequences))
                if (okToPrintAST(uniqSequences))
                    printJson(astDoc.getLeft(), uniqSequences, astDoc.getRight())
            } catch (e: DASTNode.TooManySequencesException) {
                System.err.println("Too many sequences from AST")
            } catch (e: DASTNode.TooLongSequenceException) {
                System.err.println("Too long sequence from AST")
            }

        }
        return false
    }

    private fun printJson(ast: DSubTree, sequences: List<Sequence>, javadoc: String) {
        val file = options.cmdLine.getOptionValue("input-file")
        val out = JSONOutputWrapper(file, ast, sequences, javadoc)
        output.write(if (first) "" else ",\n")
        output.write(gson.toJson(out))
        output.flush()
        first = false
    }

    private fun okToPrintAST(sequences: List<Sequence>): Boolean {
        val n = sequences.size
        return !(n == 0 || n == 1 && sequences[0].calls.size <= 1)
    }

    fun getLineNumber(node: ASTNode): Int {
        return unit.getLineNumber(node.startPosition)
    }

    companion object {

        private lateinit var V: Visitor

        fun V(): Visitor {
            return V
        }
    }
}
