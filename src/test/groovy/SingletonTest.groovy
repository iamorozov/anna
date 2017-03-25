import org.junit.Test

class SingletonTest extends PatternTest{

    @Test
    void 'singleton classes must have unique objects'() {
        assertScript '''
            @core.patterns.Singleton
            class SingletonToTest{
                static int counter = 0
                int intField = counter++
            }
            
            assert SingletonToTest.getInstance().intField == SingletonToTest.getInstance().intField
        '''
    }
}
