package core.ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.InnerClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static org.codehaus.groovy.ast.ClassHelper.OBJECT_TYPE
import static org.codehaus.groovy.ast.tools.GeneralUtils.assignX
import static org.codehaus.groovy.ast.tools.GeneralUtils.block
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX
import static org.codehaus.groovy.ast.tools.GeneralUtils.ctorX
import static org.codehaus.groovy.ast.tools.GeneralUtils.declS
import static org.codehaus.groovy.ast.tools.GeneralUtils.param
import static org.codehaus.groovy.ast.tools.GeneralUtils.params
import static org.codehaus.groovy.ast.tools.GeneralUtils.propX
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS
import static org.codehaus.groovy.ast.tools.GeneralUtils.stmt
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass
import static org.codehaus.groovy.transform.BuilderASTTransformation.NO_EXCEPTIONS
import static org.codehaus.groovy.transform.BuilderASTTransformation.NO_PARAMS

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class BuilderASTTransformation extends AbstractASTTransformation{
    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        def buildee = nodes[1] as ClassNode
        def anno = nodes[0] as AnnotationNode

        def builder = new InnerClassNode(buildee, buildee.name + 'Builder', ACC_PUBLIC | ACC_STATIC, OBJECT_TYPE)
        buildee.getModule().addClass(builder)

        def body = block(
            returnS(ctorX(builder))
        )
        buildee.addMethod('builder', ACC_PUBLIC | ACC_STATIC, builder, NO_PARAMS, NO_EXCEPTIONS, body)

        def fields = getInstancePropertyFields(buildee)
        fields.each {
            builder.addField(createFieldCopy(buildee, it.name, it.type))
            builder.addMethod(createBuilderMethodForProp(builder, it))
        }
        builder.addMethod(createBuildMethod(anno, buildee, fields))
    }

    private FieldNode createFieldCopy(ClassNode buildee, String fieldName, ClassNode fieldType) {
        new FieldNode(fieldName, ACC_PRIVATE, fieldType, buildee, null)
    }

    private List<FieldNode> getInstancePropertyFields(ClassNode cNode) {
        final result = []
        cNode.getProperties().each {
            if (!it.static) {
                result.add(it.field)
            }
        }
        return result
    }

    private MethodNode createBuilderMethodForProp(ClassNode builder, FieldNode node) {
        ClassNode fieldType = node.type
        String fieldName = node.name
        return new MethodNode(fieldName, ACC_PUBLIC, newClass(builder), params(param(fieldType, fieldName)), NO_EXCEPTIONS, block(
            stmt(assignX(propX(varX("this"), constX(fieldName)), varX(fieldName, fieldType))),
            returnS(varX("this", builder))
        ))
    }

    private MethodNode createBuildMethod(AnnotationNode anno, ClassNode buildee, List<FieldNode> fields) {
        String buildMethodName = getMemberStringValue(anno, "buildMethodName", "build")
        final BlockStatement body = new BlockStatement()
        body.addStatement(returnS(initializeInstance(buildee, fields, body)))
        return new MethodNode(buildMethodName, ACC_PUBLIC, newClass(buildee), NO_PARAMS, NO_EXCEPTIONS, body)
    }

    private Expression initializeInstance(ClassNode buildee, List<FieldNode> fields, BlockStatement body) {
        Expression instance = varX("_the" + buildee.getNameWithoutPackage(), buildee)
        body.addStatement(declS(instance, ctorX(buildee)))
        for (FieldNode field : fields) {
            body.addStatement(stmt(assignX(propX(instance, field.getName()), varX(field))))
        }
        return instance
    }
}
