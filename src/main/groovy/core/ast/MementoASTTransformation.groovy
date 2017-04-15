package core.ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.InnerClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.classgen.VariableScopeVisitor
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation


import static org.codehaus.groovy.ast.ClassHelper.OBJECT_TYPE
import static org.codehaus.groovy.ast.tools.GeneralUtils.args
import static org.codehaus.groovy.ast.tools.GeneralUtils.assignS
import static org.codehaus.groovy.ast.tools.GeneralUtils.attrX
import static org.codehaus.groovy.ast.tools.GeneralUtils.block
import static org.codehaus.groovy.ast.tools.GeneralUtils.callThisX
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX
import static org.codehaus.groovy.ast.tools.GeneralUtils.ctorX
import static org.codehaus.groovy.ast.tools.GeneralUtils.declS
import static org.codehaus.groovy.ast.tools.GeneralUtils.fieldX
import static org.codehaus.groovy.ast.tools.GeneralUtils.propX
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS
import static org.codehaus.groovy.ast.tools.GeneralUtils.stmt
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafeWithGenerics

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class MementoASTTransformation extends AbstractASTTransformation{

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        def originator = nodes[1] as ClassNode

        generateGettersAndSetters(originator)

        def memento = addMemento(originator)
        def caretaker = addCaretaker(originator, memento)
        originator.addField('caretaker', ACC_PRIVATE, caretaker, ctorX(caretaker))

        addSaveStateMethod(originator, memento)

        originator.properties.each {
            def setter = originator.getSetterMethod(setterName(it.field.name))
            setter.setCode(
                block(
                    stmt(callThisX('saveState')),
                    setter.code
                )
            )
        }

        addUndo(originator, memento)
        addRedo(originator, memento)

        VariableScopeVisitor visitor = new VariableScopeVisitor(source)
        visitor.visitClass(originator)
        println('')
    }

    def generateGettersAndSetters(ClassNode parent) {
        parent.properties.each {
            parent.addMethod(getterMethod(it))
            parent.addMethod(setterMethod(it))
        }
    }

    MethodNode getterMethod(PropertyNode property) {
        new MethodNode(
            getterName(property.field.name),
            ACC_PUBLIC,
            property.type,
            Parameter.EMPTY_ARRAY,
            ClassNode.EMPTY_ARRAY,
            returnS(varX(property.field.name))
        )
    }

    MethodNode setterMethod(PropertyNode property) {
        new MethodNode(
            setterName(property.field.name),
            ACC_PUBLIC,
            ClassHelper.VOID_TYPE,
            [new Parameter(property.type, property.field.name)] as Parameter[],
            ClassNode.EMPTY_ARRAY,
            assignS(attrX(varX('this'), constX(property.field.name)), varX(property.field.name))
        )
    }

    String getterName(String fieldName) {
        'get' + fieldName.capitalize()
    }

    String setterName(String fieldName) {
        'set' + fieldName.capitalize()
    }

    void addUndo(ClassNode originator, ClassNode memento) {
        originator.addMethod(
            new MethodNode(
                'undo',
                ACC_PUBLIC,
                ClassHelper.VOID_TYPE,
                Parameter.EMPTY_ARRAY,
                ClassNode.EMPTY_ARRAY,
                block(
                    undoStatements(originator, memento)
                )
            )
        )
    }

    void addRedo(ClassNode originator, ClassNode memento) {
        originator.addMethod(
            new MethodNode(
                'redo',
                ACC_PUBLIC,
                ClassHelper.VOID_TYPE,
                Parameter.EMPTY_ARRAY,
                ClassNode.EMPTY_ARRAY,
                block(
                    redoStatements(originator, memento)
                )
            )
        )
    }

    Statement[] undoStatements(ClassNode parent, ClassNode memento) {
        List<Statement> stmtsList = [] as List<Statement>

        stmtsList.add(
            declS(
                varX('memento'), ctorX(memento)
            )
        )

        stmtsList += mementoInitStmts('memento', parent)

        stmtsList.add(
            stmt(
                callX(
                    propX(
                        fieldX(parent, 'caretaker'),
                        'redoStack'
                    ),
                    'push',
                    args(
                        varX('memento')
                    )
                )
            )
        )

        stmtsList.add(
            assignS(
                varX('memento'),
                callX(
                    propX(
                        fieldX(parent, 'caretaker'),
                        'undoStack'
                    ),
                    'pop'
                )
            )
        )

        parent.properties.each {
            stmtsList.add(
                assignS(
                    fieldX(parent, it.field.name),
                    callX(
                        varX('memento'),
                        getterName(it.name)
                    )
                )
            )
        }

        stmtsList.toArray(new Statement[stmtsList.size()])
    }

    Statement[] redoStatements(ClassNode parent, ClassNode memento) {
        List<Statement> stmtsList = [] as List<Statement>

        stmtsList.add(
            declS(
                varX('memento'), ctorX(memento)
            )
        )

        stmtsList += mementoInitStmts('memento', parent)

        stmtsList.add(
            stmt(
                callX(
                    propX(
                        fieldX(parent, 'caretaker'),
                        'undoStack'
                    ),
                    'push',
                    args(
                        varX('memento')
                    )
                )
            )
        )

        stmtsList.add(
            assignS(
                varX('memento'),
                callX(
                    propX(
                        fieldX(parent, 'caretaker'),
                        'redoStack'
                    ),
                    'pop'
                )
            )
        )

        parent.properties.each {
            stmtsList.add(
                assignS(
                    fieldX(parent, it.field.name),
                    callX(
                        varX('memento'),
                        getterName(it.name)
                    )
                )
            )
        }

        stmtsList.toArray(new Statement[stmtsList.size()])
    }

    private addSaveStateMethod(ClassNode parent, ClassNode memento) {
        parent.addMethod(
            new MethodNode(
                'saveState',
                ACC_PRIVATE,
                ClassHelper.void_WRAPPER_TYPE,
                Parameter.EMPTY_ARRAY,
                ClassNode.EMPTY_ARRAY,
                block(
                    saveStateCode(parent, memento)
                )
            )
        )
    }

    private ClassNode addCaretaker(ClassNode parent, ClassNode memento) {
        def caretaker = new InnerClassNode(parent, parent.name + '$Caretaker', ACC_PRIVATE | ACC_STATIC, OBJECT_TYPE)
        def undoStack = new FieldNode('undoStack', ACC_PRIVATE, makeClassSafeWithGenerics(Stack.class, memento), caretaker, ctorX(makeClassSafeWithGenerics(Stack.class, memento)))
        def undoStackProp = new PropertyNode(undoStack, ACC_PUBLIC, null, null)
        caretaker.addProperty(undoStackProp)
        caretaker.addMethod(getterMethod(undoStackProp))
        caretaker.addMethod(setterMethod(undoStackProp))

        def redoStack = new FieldNode('redoStack', ACC_PRIVATE, makeClassSafeWithGenerics(Stack.class, memento), caretaker, ctorX(makeClassSafeWithGenerics(Stack.class, memento)))
        def redoStackProp = new PropertyNode(redoStack, ACC_PUBLIC, null, null)
        caretaker.addProperty(redoStackProp)
        caretaker.addMethod(getterMethod(redoStackProp))
        caretaker.addMethod(setterMethod(redoStackProp))

        parent.module.addClass(caretaker)
        caretaker
    }

    private ClassNode addMemento(ClassNode parent) {
        def memento = new InnerClassNode(parent, parent.name + '$Memento', ACC_PRIVATE | ACC_STATIC, OBJECT_TYPE)
        parent.module.addClass(memento)

        parent.properties.each {
            def field = createFieldCopy(memento, it.field)
//            memento.addField(field)
            def property = new PropertyNode(field, ACC_PUBLIC, null, null)
            memento.addProperty(property)

            memento.addMethod(setterMethod(property))
            memento.addMethod(getterMethod(property))
        }

        memento
    }

    private FieldNode createFieldCopy(ClassNode parent, FieldNode fieldNode) {
        new FieldNode(fieldNode.name, ACC_PRIVATE, fieldNode.type, parent, null)
    }

    private Statement[] saveStateCode(ClassNode parent, ClassNode memento) {
        def stmts = [] as List<Statement>

        stmts.add(
            declS(
                varX('memento'), ctorX(memento)
            )
        )

        stmts += mementoInitStmts('memento', parent)

        stmts.add(
            stmt(
                callX(
                    propX(
                        fieldX(parent, 'caretaker'),
                        'undoStack'
                    ),
                    'push',
                    args(
                        varX('memento')
                    )
                )
            )
        )

        stmts.add(
            stmt(
                callX(
                    propX(
                        fieldX(parent, 'caretaker'),
                        'redoStack'
                    ),
                    'clear'
                )
            )
        )

        stmts.toArray(new Statement[stmts.size()])
    }

    private List<Statement> mementoInitStmts(String mementoName, ClassNode parent) {
        def stmts = [] as List<Statement>

        parent.properties.each {
            stmts.add(
                stmt(
                    callX(
                        varX(mementoName),
                        setterName(it.field.name),
                        fieldX(it.field)
                    )
                )
            )
        }

        stmts
    }
}
