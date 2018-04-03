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
package tanvd.bayou.implementation.model.configurable.synthesizer

//import org.eclipse.jdt.core.dom.Type

import org.eclipse.jdt.core.dom.*
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite
import org.eclipse.jface.text.BadLocationException
import org.eclipse.jface.text.Document
import tanvd.bayou.implementation.core.evidence.EvidenceExtractor
import tanvd.bayou.implementation.model.configurable.synthesizer.dsl.DSubTree
import java.util.*

/**
 * Main class that implements the visitor pattern on the draft program's AST
 */
class Visitor
/**
 * Initializes the visitor
 *
 * @param sketch   sketch to be synthesized
 * @param document draft program document
 * @param cu       draft program compilation unit
 * @param mode     enumeration mode
 */
(
        /**
         * The sketch to synthesize
         */
        private val sketch: DSubTree,
        /**
         * The document object to store the synthesized code
         */
        private val document: Document,
        /**
         * The compilation unit of the draft program
         */
        private val cu: CompilationUnit,
        /**
         * The enumeration mode
         */
        private val mode: Synthesizer.Mode) : ASTVisitor() {

    /**
     * Temporary store for the synthesized program. Gets updated with every invocation of visitor.
     */
    internal var synthesizedProgram: String? = null

    /**
     * The rewriter for the document
     */
    private val rewriter: ASTRewrite

    /**
     * The block where the evidence is present (i.e., where the synthesized code should be placed)
     */
    private var evidenceBlock: Block? = null

    /**
     * Temporary list of variables in scope (formal params and local declarations) for synthesis.
     * Gets cleared and updated as each method declaration is visited.
     */
    private val currentScope: MutableList<Variable>

    /**
     * Temporary store for the return type of the method for synthesis.
     * Gets cleared and updated as each method declaration is visited.
     */
    private var returnType: Type? = null

    init {

        this.rewriter = ASTRewrite.create(this.cu.ast)
        this.currentScope = ArrayList()
    }

    /**
     * Visits a method declaration and sets up the environment that may be used for synthesis:
     * - the variables in scope (formal parameters, local declarations)
     * - the return type of the method
     *
     * @param method the method declaration being visited
     * @return boolean indicating if the AST node should be explored further
     * @throws SynthesisException if there is an error with creating necessary types
     */
    @Throws(SynthesisException::class)
    override fun visit(method: MethodDeclaration?): Boolean {
        currentScope.clear()

        /* add variables in the formal parameters */
        for (o in method!!.parameters()) {
            val param = o as SingleVariableDeclaration
            val name = param.name.identifier
            val type = Type(param.type)
            val properties = VariableProperties().also {
                it.userVar = true
            }
            val v = Variable(name, type, properties)
            currentScope.add(v)
        }

        /* add local variables declared in the (beginning of) method body */
        val body = method.body
        for (o in body.statements()) {
            val stmt = o as Statement as? VariableDeclarationStatement ?: break
// stop at the first non-variable declaration
            for (f in stmt.fragments()) {
                val frag = f as VariableDeclarationFragment
                val name = frag.name.identifier
                val type = Type(stmt.type)
                val properties = VariableProperties().also {
                    it.userVar = true
                }
                val v = Variable(name, type, properties)
                currentScope.add(v)
            }
        }

        /* set the return type */
        returnType = Type(method.returnType2)

        return true
    }

    /**
     * Visits a method invocation and triggers synthesis of "sketch" if it's an evidence block
     *
     * @param invocation a method invocation
     * @return boolean indicating if the AST node should be explored further
     * @throws SynthesisException if evidence declaration is illegal
     */
    @Throws(SynthesisException::class)
    override fun visit(invocation: MethodInvocation?): Boolean {
        val binding = invocation!!.resolveMethodBinding()
                ?: throw SynthesisException(SynthesisException.CouldNotResolveBinding)

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
        if (!(name == "apicalls" || name == "types" || name == "keywords"))
            throw SynthesisException(SynthesisException.InvalidEvidenceType)

        val env = Environment(invocation.ast, currentScope, mode)
        var body = sketch.synthesize(env)

        // Apply dead code elimination here
        val dce = DCEOptimizor()
        body = dce.apply(body, sketch)
        if (body.statements().size == 0)
            return false

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

    /**
     * Performs local post-processing of synthesized code:
     * - Adds try-catch for uncaught exceptions
     * - Adds local variable declarations
     * - Adds return statement
     *
     * @param ast            the owner of the block
     * @param env            environment that was used for synthesis
     * @param body           block containing synthesized code
     * @param eliminatedVars variables eliminiated by DCE
     * @return updated block containing synthesized code
     */
    private fun postprocessLocal(ast: AST, env: Environment, body: Block, eliminatedVars: Set<String>): Block {
        var body = body
        /* add uncaught exeptions */
        val exceptions = sketch.exceptionsThrown(eliminatedVars)
        env.imports.addAll(exceptions)
        if (!exceptions.isEmpty()) {
            val statement = ast.newTryStatement()
            statement.body = body

            val exceptions_ = ArrayList<Class<*>>(exceptions)
            exceptions_.sortedWith(kotlin.Comparator { e1: Class<*>, e2: Class<*> -> if (e1.isAssignableFrom(e2)) 1 else -1 })
            for (except in exceptions_) {
                val catchClause = ast.newCatchClause()
                val ex = ast.newSingleVariableDeclaration()
                ex.type = ast.newSimpleType(ast.newName(except.getSimpleName()))
                ex.name = ast.newSimpleName("_e")
                catchClause.exception = ex
                catchClause.body = ast.newBlock()
                statement.catchClauses().add(catchClause)
            }

            body = ast.newBlock()
            body.statements().add(statement)
        }

        /* add variable declarations */
        val toDeclare = env.scope.variables
        toDeclare!!.addAll(env.scope.getPhantomVariables()!!)
        for (`var` in toDeclare) {
            if (eliminatedVars.contains(`var`.name) || `var`.isUserVar)
                continue

            // create the variable declaration fragment
            val varDeclFrag = ast.newVariableDeclarationFragment()
            varDeclFrag.name = `var`.createASTNode(ast)

            // set the default initializer if the variable is a dollar variable
            if (`var`.isDefaultInit) {
                env.addImport(Bayou::class.java) // import the "Bayou" class in Bayou
                varDeclFrag.initializer = `var`.createDefaultInitializer(ast)
            }

            // set the type for the statement
            val varDeclStmt = ast.newVariableDeclarationStatement(varDeclFrag)
            if (`var`.type.T().isPrimitiveType)
                varDeclStmt.type = ASTNode.copySubtree(ast, `var`.type.T()) as org.eclipse.jdt.core.dom.Type
            else if (`var`.type.T().isSimpleType) {
                val name = (ASTNode.copySubtree(ast, `var`.type.T()) as SimpleType).name
                val simpleName = if (name.isSimpleName)
                    (name as SimpleName).identifier
                else
                    (name as QualifiedName).name.identifier
                varDeclStmt.type = ast.newSimpleType(ast.newSimpleName(simpleName))
            } else if (`var`.type.T().isParameterizedType || `var`.type.T().isArrayType) {
                varDeclStmt.type = ASTNode.copySubtree(ast, `var`.type.T()) as org.eclipse.jdt.core.dom.Type
            } else
                throw SynthesisException(SynthesisException.InvalidKindOfType)

            body.statements().add(0, varDeclStmt)
        }

        /* add return statement */
        val ret = returnType!!.T()
        val toReturn = ArrayList<Variable>()
        for (`var` in env.scope.variables!!)
            if (returnType!!.isAssignableFrom(`var`.type))
                toReturn.add(`var`)
        toReturn.sortedWith(Comparator.comparingInt { v -> v.refCount })

        val returnStmt = ast.newReturnStatement()
        if (toReturn.isEmpty()) { // add "return null" (or primitive) in order to make the code compile
            if (ret.isPrimitiveType()) {
                val primitiveType = ret as PrimitiveType
                if (primitiveType.primitiveTypeCode === PrimitiveType.BOOLEAN)
                    returnStmt.expression = ast.newBooleanLiteral(false)
                else if (primitiveType.primitiveTypeCode !== PrimitiveType.VOID)
                    returnStmt.expression = ast.newNumberLiteral("0")
            } else
                returnStmt.expression = ast.newNullLiteral()
        } else {
            returnStmt.expression = toReturn[0].createASTNode(ast)
        }
        body.statements().add(returnStmt)

        return body
    }

    /**
     * Performs global post-processing of synthesized code:
     * - Adds import declarations
     *
     * @param ast      the owner of the document
     * @param env      environment that was used for synthesis
     * @param document draft program document
     * @throws BadLocationException if an error occurred when rewriting document
     */
    @Throws(BadLocationException::class)
    private fun postprocessGlobal(ast: AST, env: Environment, document: Document) {
        /* add imports */
        val rewriter = ASTRewrite.create(ast)
        val lrw = rewriter.getListRewrite(cu, CompilationUnit.IMPORTS_PROPERTY)
        val toImport = HashSet(env.imports)
        toImport.addAll(sketch.exceptionsThrown()) // add all catch(...) types to imports
        for (cls in toImport) {
            var cls = cls
            while (cls.isArray)
                cls = cls.componentType
            if (cls.isPrimitive || cls.`package`.name == "java.lang")
                continue
            val impDecl = cu.ast.newImportDeclaration()
            val className = cls.name.replace("\\$".toRegex(), "\\.")
            impDecl.name = cu.ast.newName(className.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
            lrw.insertLast(impDecl, null)
        }
        rewriter.rewriteAST(document, null).apply(document)
    }
}
