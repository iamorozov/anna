import org.junit.Test

class IteratorTest extends PatternTest{
    @Test
    void "should create iterable from annotation"() {
        assertScript '''
            import core.patterns.Iterable

            @Iterable(iterableElement = 'arrayLike')
            class IterableClass{
                int[] arrayLike = [1, 2, 3] as int[]
            }
            
            def iterable = new IterableClass()
            iterable.forEach({e -> println e})
            iterable instanceof Iterable
        '''
    }
}
