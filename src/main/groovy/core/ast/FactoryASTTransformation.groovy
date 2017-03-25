package core.ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.stmt.ForStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static org.codehaus.groovy.ast.tools.GeneralUtils.args
import static org.codehaus.groovy.ast.tools.GeneralUtils.assignS
import static org.codehaus.groovy.ast.tools.GeneralUtils.block
import static org.codehaus.groovy.ast.tools.GeneralUtils.callThisX
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX
import static org.codehaus.groovy.ast.tools.GeneralUtils.castX
import static org.codehaus.groovy.ast.tools.GeneralUtils.closureX
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX
import static org.codehaus.groovy.ast.tools.GeneralUtils.declS
import static org.codehaus.groovy.ast.tools.GeneralUtils.eqX
import static org.codehaus.groovy.ast.tools.GeneralUtils.equalsNullX
import static org.codehaus.groovy.ast.tools.GeneralUtils.ifS
import static org.codehaus.groovy.ast.tools.GeneralUtils.isTrueX
import static org.codehaus.groovy.ast.tools.GeneralUtils.notNullX
import static org.codehaus.groovy.ast.tools.GeneralUtils.param
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.propX
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS
import static org.codehaus.groovy.ast.tools.GeneralUtils.stmt
import static org.codehaus.groovy.ast.tools.GeneralUtils.ternaryX
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class FactoryASTTransformation extends AbstractASTTransformation {
    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {

        def factoryAnnotation = nodes[0] as AnnotationNode
        def productsList = factoryAnnotation.members['products'] as ListExpression
        def superClass = productsList.expressions[0].type.superClass

        def factoryClass = nodes[1] as ClassNode
        def productType = param(STRING_TYPE, 'productType')
        Parameter[] parameters = [productType]

        def body = block(
            declS(varX('products', new ClassNode(ArrayList.class)), productsList),
            declS(varX('concreteProduct'), constX(null)),
            new ForStatement(
                param(CLASS_Type, 'product'),
                varX('products'),
                block(
                    ifS(
                        isTrueX(
                            eqX(
                                propX(
                                    varX('product'),
                                    'name'
                                ),
                                varX('productType')
                            )
                        ),
                        assignS(
                            varX('concreteProduct'),
                            varX('product')
                        )
                    )
                )
            ),
            returnS(
                ternaryX(
                    isTrueX(notNullX(varX('concreteProduct', CLASS_Type))),
                    castX(
                        superClass,
                        callX(
                            varX('concreteProduct'),
                            'newInstance'
                        )
                    ),
                    constX(null)
                )
            )
        )

        factoryClass.addMethod('create', ACC_PUBLIC, superClass, parameters, ClassNode.EMPTY_ARRAY, body)
        return
    }
}
