import org.junit.Test

class BuilderTest extends PatternTest{
    @Test
    void "should be able to construct classes with Builder"() {
        assertScript '''
            import core.patterns.Builder 
            
            @Builder
            class Account{
                String userId
                String token
            }
            
            def account = Account.builder().userId('1').token('token').build()
            assert account.userId == '1'
            assert account.token == 'token'
            assert account instanceof Account
        '''
    }
}
