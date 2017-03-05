package core.ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static org.codehaus.groovy.ast.tools.GeneralUtils.ctorX
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class SingletonASTTransformation extends AbstractASTTransformation{
    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode singleton = nodes[1] as ClassNode

        singleton.addConstructor(ACC_PRIVATE, new Parameter[0], ClassNode.EMPTY_ARRAY, new BlockStatement())
        def instance = singleton.addField("instance", ACC_FINAL|ACC_PUBLIC|ACC_STATIC, singleton, ctorX(singleton))
        singleton.addMethod('getInstance', ACC_PUBLIC|ACC_STATIC, singleton, new Parameter[0], ClassNode.EMPTY_ARRAY, returnS(varX(instance)))
    }
}
