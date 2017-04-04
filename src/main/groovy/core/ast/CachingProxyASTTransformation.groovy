package core.ast

import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.classgen.VariableScopeVisitor
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.tools.GenericsUtils.newClass

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class CachingProxyASTTransformation extends AbstractASTTransformation{
    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        def toProxy = nodes[1] as ClassNode

        List<MethodNode> methods = toProxy.getMethods()
        List<MethodNode> toAdd = []
        methods.each { it ->
            if (it.abstract || it.voidMethod) {
                return
            }

            def delegatingMethod = buildDelegatingMethod(it, toProxy)
            toAdd.add(delegatingMethod)

            int modifiers = ACC_PRIVATE | ACC_FINAL
            if (it.static) {
                modifiers = modifiers | ACC_STATIC
            }

            def memoizeClosureCallExpression = buildMemoizeClosureCallExpression(delegatingMethod)
            FieldNode memoizedClosureField = new FieldNode(
                buildUniqueName(toProxy, it, 'Closure'),
                modifiers,
                newClass(ClassHelper.CLOSURE_TYPE),
                null,
                memoizeClosureCallExpression
            )
            toProxy.addField(memoizedClosureField)

            BlockStatement newCode = new BlockStatement()
            MethodCallExpression closureCallExpression = callX(
                fieldX(memoizedClosureField),
                'call',
                args(it.getParameters())
            )
            closureCallExpression.setImplicitThis(false)
            newCode.addStatement(returnS(closureCallExpression))
            it.setCode(newCode)
        }

        toAdd.each {
            toProxy.addMethod(it)
        }

        VariableScopeVisitor visitor = new VariableScopeVisitor(source)
        visitor.visitClass(toProxy)
    }

    private MethodNode buildDelegatingMethod(final MethodNode annotatedMethod, final ClassNode ownerClassNode) {
        Statement code = annotatedMethod.getCode()
        int access = ACC_PROTECTED
        if (annotatedMethod.isStatic()) {
            access = ACC_PRIVATE | ACC_STATIC
        }
        MethodNode method = new MethodNode(
            buildUniqueName(ownerClassNode, annotatedMethod, 'DelegatingProxyMethod'),
            access,
            annotatedMethod.getReturnType(),
            cloneParams(annotatedMethod.getParameters()),
            annotatedMethod.getExceptions(),
            code
        )
        List<AnnotationNode> sourceAnnotations = annotatedMethod.getAnnotations()
        method.addAnnotations(new ArrayList<AnnotationNode>(sourceAnnotations))
        return method
    }

    private static String buildUniqueName(ClassNode owner, MethodNode methodNode, String type) {
        StringBuilder nameBuilder = new StringBuilder('memoized' + type + '$').append(methodNode.getName())
        if (methodNode.getParameters() != null) {
            for (Parameter parameter : methodNode.getParameters()) {
                nameBuilder.append(buildTypeName(parameter.getType()))
            }
        }
        while (owner.getField(nameBuilder.toString()) != null) {
            nameBuilder.insert(0, "_")
        }

        return nameBuilder.toString()
    }

    private static String buildTypeName(ClassNode type) {
        if (type.isArray()) {
            return String.format("%sArray", buildTypeName(type.getComponentType()))
        }
        return type.getNameWithoutPackage()
    }

    private MethodCallExpression buildMemoizeClosureCallExpression(MethodNode privateMethod) {
        Parameter[] srcParams = privateMethod.getParameters()
        Parameter[] newParams = cloneParams(srcParams)
        List<Expression> argList = new ArrayList<Expression>(newParams.length)
        for (int i = 0; i < srcParams.length; i++) {
            argList.add(varX(newParams[i]))
        }

        ClosureExpression expression = new ClosureExpression(
            newParams,
            stmt(callThisX(privateMethod.getName(), args(argList)))
        )
        MethodCallExpression mce = callX(expression, 'memoize')
        mce.setImplicitThis(false)
        return mce
    }
}
