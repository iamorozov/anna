import org.junit.Ignore
import org.junit.Test


class MementoTest extends PatternTest{
//    @Ignore
    @Test
    void "should be able to apply memento pattern to save object state"() {
        assertScript '''
            import core.patterns.Memento
            
            @Memento
            class ToMemorizeState{
                String state
            }
            
            def originator = new ToMemorizeState(state: 'state1')
            originator.state = 'state2'
            originator.state = 'state3'
            originator.undo()
            assert originator.state == 'state2'
            originator.redo()
            assert originator.state == 'state3'
            originator.undo()
            assert originator.state == 'state2'
        '''
    }
}
