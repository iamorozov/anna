import org.junit.Test

class IteratorTest extends PatternTest{
    @Test
    void "should create iterable from annotation"() {
        assertScript '''
            import IterableClass

            @IterableClass(iterableElement = 'arrayLike')
            class IterableClass1{
                int[] arrayLike = [1, 2, 3] as int[]
            }
            
            def iterable = new core.IterableClass()
            iterable.forEach({e -> println e})
        '''
    }
}
