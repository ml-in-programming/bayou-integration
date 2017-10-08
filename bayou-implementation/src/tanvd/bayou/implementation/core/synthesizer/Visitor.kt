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
package tanvd.bayou.implementation.core.synthesizer

import tanvd.bayou.implementation.core.dsl.DSubTree
import org.eclipse.jdt.core.dom.*
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite
import org.eclipse.jdt.core.dom.rewrite.ListRewrite
import org.eclipse.jface.text.BadLocationException
import org.eclipse.jface.text.Document

import java.util.*

class Visitor(internal val dAST: DSubTree, internal val document: Document, internal val cu: CompilationUnit) : ASTVisitor() {
    internal var synthesizedProgram: String? = null
    protected var rewriter: ASTRewrite
    internal var evidenceBlock: Block? = null
    internal var currentScope: MutableList<Variable>

    init {

        this.rewriter = ASTRewrite.create(this.cu.ast)
        this.currentScope = ArrayList()
    }

    @Throws(SynthesisException::class)
    override fun visit(invocation: MethodInvocation?): Boolean {
        val binding = invocation!!.resolveMethodBinding() ?: throw SynthesisException(SynthesisException.CouldNotResolveBinding)

        val cls = binding.declaringClass
        if (cls == null || cls.qualifiedName != "edu.rice.cs.caper.bayou.annotations.Evidence")
            return false

        if (invocation.parent.parent !is Block)
            throw SynthesisException(SynthesisException.EvidenceNotInBlock)
        val evidenceBlock = invocation.parent.parent as Block

        if (!EvidenceExtractor.isLegalEvidenceBlock(evidenceBlock))
            throw SynthesisException(SynthesisException.EvidenceMixedWithCode)

        if (this.evidenceBlock != null)
            return if (this.evidenceBlock !== evidenceBlock)
                throw SynthesisException(SynthesisException.MoreThanOneHole)
            else
                false /* synthesis is already done */
        this.evidenceBlock = evidenceBlock

        val name = binding.name
        if (!(name == "apicalls" || name == "types" || name == "context"))
            throw SynthesisException(SynthesisException.InvalidEvidenceType)

        val env = Environment(invocation.ast, currentScope)
        var body = dAST.synthesize(env)

        // Apply dead code elimination here
        val dce = DCEOptimizor()
        body = dce.apply(body, dAST)

        /* make rewrites to the local method body */
        body = postprocessLocal(invocation.ast, env, body, dce.eliminatedVars)
        rewriter.replace(evidenceBlock, body, null)

        try {
            rewriter.rewriteAST(document, null).apply(document)

            /* make rewrites to the document */
            postprocessGlobal(cu.ast, env, document)
        } catch (e: BadLocationException) {
            throw SynthesisException(SynthesisException.CouldNotEditDocument)
        }

        synthesizedProgram = document.get()

        return false
    }

    private fun postprocessLocal(ast: AST, env: Environment, body: Block, eliminatedVars: Set<String>): Block {
        var body = body
        /* add uncaught exeptions */
        val exceptions = dAST.exceptionsThrown(eliminatedVars)
        env.imports.addAll(exceptions)
        if (!exceptions.isEmpty()) {
            val statement = ast.newTryStatement()
            statement.body = body

            val exceptions_ = ArrayList(exceptions)
            exceptions_.sortedWith(Comparator({ e1: Class<*>, e2: Class<*> -> if (e1.isAssignableFrom(e2)) 1 else -1 }))
            for (except in exceptions_) {
                val catchClause = ast.newCatchClause()
                val ex = ast.newSingleVariableDeclaration()
                ex.type = ast.newSimpleType(ast.newName(except.simpleName))
                ex.name = ast.newSimpleName("_e")
                catchClause.exception = ex
                catchClause.body = ast.newBlock()
                statement.catchClauses().add(catchClause)
            }

            body = ast.newBlock()
            body.statements().add(statement)
        }

        /* add variable declarations */
        for (`var` in env.mu_scope) {
            if (!eliminatedVars.contains(`var`.name)) {
                val varDeclFrag = ast.newVariableDeclarationFragment()
                varDeclFrag.name = ast.newSimpleName(`var`.name)
                val varDeclStmt = ast.newVariableDeclarationStatement(varDeclFrag)
                if (`var`.type.T().isPrimitiveType)
                    varDeclStmt.type = `var`.type.T()
                else if (`var`.type.T().isSimpleType) {
                    val name = (`var`.type.T() as SimpleType).name
                    val simpleName = if (name.isSimpleName)
                        (name as SimpleName).identifier
                    else
                        (name as QualifiedName).name.identifier
                    varDeclStmt.type = ast.newSimpleType(ast.newSimpleName(simpleName))
                } else if (`var`.type.T().isParameterizedType) {
                    varDeclStmt.type = ASTNode.copySubtree(ast, `var`.type.T()) as org.eclipse.jdt.core.dom.Type
                } else
                    throw SynthesisException(SynthesisException.InvalidKindOfType)
                body.statements().add(0, varDeclStmt)
            }
        }

        return body
    }

    @Throws(BadLocationException::class)
    private fun postprocessGlobal(ast: AST, env: Environment, document: Document) {
        /* add imports */
        val rewriter = ASTRewrite.create(ast)
        val lrw = rewriter.getListRewrite(cu, CompilationUnit.IMPORTS_PROPERTY)
        val toImport = HashSet(env.imports)
        toImport.addAll(dAST.exceptionsThrown()) // add all catch(...) types to imports
        for (cls in toImport) {
            if (cls.isPrimitive || cls.`package`.name == "java.lang")
                continue
            val impDecl = cu.ast.newImportDeclaration()
            val className = cls.name.replace("\\$".toRegex(), "\\.")
            impDecl.name = cu.ast.newName(className.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
            lrw.insertLast(impDecl, null)
        }
        rewriter.rewriteAST(document, null).apply(document)
    }

    /* setup the scope of variables for synthesis */
    @Throws(SynthesisException::class)
    override fun visit(method: MethodDeclaration?): Boolean {
        currentScope.clear()

        /* add variables in the formal parameters */
        for (o in method!!.parameters()) {
            val param = o as SingleVariableDeclaration
            val v = Variable(param.name.identifier, Type(param.type))
            currentScope.add(v)
        }

        /* add local variables declared in the (beginning of) method body */
        val body = method.body
        for (o in body.statements()) {
            val stmt = o as Statement as? VariableDeclarationStatement ?: break
// stop at the first non-variable declaration
            for (f in stmt.fragments()) {
                val frag = f as VariableDeclarationFragment
                val v = Variable(frag.name.identifier, Type(stmt.type))
                currentScope.add(v)
            }
        }

        return true
    }

    companion object {

        internal val primitiveToClass: Map<PrimitiveType.Code, Class<*>>

        init {
            val map = HashMap<PrimitiveType.Code, Class<*>>()
            map.put(PrimitiveType.INT, Int::class.javaPrimitiveType!!)
            map.put(PrimitiveType.LONG, Long::class.javaPrimitiveType!!)
            map.put(PrimitiveType.DOUBLE, Double::class.javaPrimitiveType!!)
            map.put(PrimitiveType.FLOAT, Float::class.javaPrimitiveType!!)
            map.put(PrimitiveType.BOOLEAN, Boolean::class.javaPrimitiveType!!)
            map.put(PrimitiveType.CHAR, Char::class.javaPrimitiveType!!)
            map.put(PrimitiveType.BYTE, Byte::class.javaPrimitiveType!!)
            map.put(PrimitiveType.VOID, Void.TYPE)
            map.put(PrimitiveType.SHORT, Short::class.javaPrimitiveType!!)
            primitiveToClass = Collections.unmodifiableMap<PrimitiveType.Code, Class<*>>(map)
        }
    }
}
