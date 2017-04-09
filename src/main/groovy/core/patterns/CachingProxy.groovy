package core.patterns

import core.ast.CachingProxyASTTransformation
import core.ast.FactoryASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@GroovyASTTransformationClass(classes = CachingProxyASTTransformation.class)
@interface CachingProxy {
}
