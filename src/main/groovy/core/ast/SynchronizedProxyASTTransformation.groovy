package core.ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.SynchronizedStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static org.codehaus.groovy.ast.tools.GeneralUtils.varX

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class SynchronizedProxyASTTransformation extends AbstractASTTransformation{
    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source)
        def parent = nodes[1] as ClassNode
        def node = nodes[0] as AnnotationNode

        String value = getMemberStringValue(node, 'lock')

        parent.methods.each {
            if (it.public && !it.abstract && !it.static) {
                FieldNode field = parent.getDeclaredField(value)
                if (!field) return
                Statement origCode = it.getCode()
                Statement newCode = new SynchronizedStatement(varX(value), origCode)
                it.setCode(newCode)
            }
        }
    }
}
