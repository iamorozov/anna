import org.junit.Test

class SynchronizedProxyTest extends PatternTest{

    @Test
    void  "should be able to apply SynchronizedProxy to class"() {
        assertScript '''
            import core.patterns.SynchronizedProxy

            @SynchronizedProxy(lock = 'a')
            class NonSynchronized{
                int[] a
            }        
            
            new NonSynchronized()
        '''
    }
}
