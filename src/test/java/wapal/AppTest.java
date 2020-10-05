package wapal;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import wapal.App;
import wapal.App.Tuple;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void testChooseNextDFS()
    {
        // 2 choices in the 1st position, 1 choice in the 2nd position
        final List<Tuple<Integer,Integer>> run = Arrays.asList(t(0,2), t(0,1));

        // at 0
        assertEquals( 1, App.chooseNextDFS( run, 0 ) );

        // at 1
        final Exception exn = assertThrows( IllegalArgumentException.class, () -> {
            App.chooseNextDFS( run, 1 );
        });
    }

    /** Shorthand for new tuple */
    static <A,B> Tuple<A,B> t( A a, B b ) { return new Tuple<>(a,b); }
}
