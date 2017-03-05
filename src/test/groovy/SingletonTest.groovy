import org.junit.Test

@SuppressWarnings("GroovyAccessibility")
class SingletonTest {

    final assertScript = new GroovyTestCase().&assertScript

    @Test
    void 'singleton classes must have unique objects'() {
        assertScript '''
            @core.patterns.Singleton
            class SingletonToTest{}
            
            assert SingletonToTest.instance == SingletonToTest.instance
        '''
    }
}
