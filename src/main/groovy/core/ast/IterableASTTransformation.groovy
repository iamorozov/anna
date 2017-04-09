package core.ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static org.codehaus.groovy.ast.tools.GeneralUtils.args
import static org.codehaus.groovy.ast.tools.GeneralUtils.block
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafeWithGenerics

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class IterableASTTransformation extends AbstractASTTransformation{
    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        def annotationNode = nodes[0] as AnnotationNode
        def iterable = nodes[1] as ClassNode

        def iterableElementName = getIterableElementValue(annotationNode)

        def iterableField = iterable.getField(iterableElementName)
        if(!iterableField) throw new Exception('No such field')
        def iterableType = ClassHelper.getWrapper(iterableField.type.componentType)

        def code = block(
            returnS(
                callX(
                    callX(ClassHelper.make(Arrays.class), 'stream', args(iterableElementName)),
                    'iterator'
                )
            )
        )

        iterable.addInterface(makeClassSafeWithGenerics(Iterable.class, iterableType))
        iterable.addMethod('iterator', ACC_PUBLIC, makeClassSafeWithGenerics(Iterator.class, iterableType), Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, code)
    }

    String getIterableElementValue(AnnotationNode node) {
        final Expression member = node.getMember('iterableElement')
        if (member != null && member instanceof ConstantExpression) return ((ConstantExpression) member).getValue()
        return null
    }
}
