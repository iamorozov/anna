import org.junit.Test

class FactoryTest extends PatternTest{
    @Test
    void "should be able to use factory method to create specified products"() {
        assertScript '''
            import core.patterns.Factory
            
            class Product{}
            class ProductA extends Product{}
            class ProductB extends Product{}
            
            @Factory(products = [ProductA.class, ProductB.class])
            class ProductFactory{}
            
            def factory = new ProductFactory()
            assert factory.create('ProductA') instanceof ProductA
        '''
    }
}
