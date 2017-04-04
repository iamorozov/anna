import org.junit.Test

class CachingProxyTest extends PatternTest {
    @Test
    void "should be able to use caching proxy"() {
        assertScript '''
            import core.patterns.CachingProxy
            import groovy.transform.Field
                        
            @CachingProxy
            class ToCache{
                boolean cachedCall
            
                int sum(int a, int b){
                    cachedCall = false
                    return a + b
                }
            }
            
            def toCache = new ToCache()
            toCache.sum(2, 3)
            assert !toCache.cachedCall
            toCache.cachedCall = true
            toCache.sum(2, 3)
            assert toCache.cachedCall
        '''
    }
}
