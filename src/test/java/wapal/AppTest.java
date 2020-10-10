package wapal;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import wapal.App;
import wapal.Either;
import wapal.Tuple;
import wapal.App.SearchSpaceExhaustedException;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /** Shorthand for new tuple */
    static <A,B> Tuple<A,B> t( A a, B b ) { return new Tuple<>(a,b); }

    @Test
    public void testNextDFSPrefix() {

        // 2 choices in the 1st position, 1 choice in the 2nd position
        final List<Tuple<Integer,Integer>> run = Arrays.asList(t(1,3), t(0,2), t(0,1));

        final List<Tuple<Integer,Integer>> prefix;
        try {  prefix = App.nextPrefixDFS( run ); }
        catch( SearchSpaceExhaustedException exn ) {
            throw new AssertionError("Search space is not exhausted in example", exn);
        }
        final List<Tuple<Integer,Integer>> expected_prefix = Arrays.asList( t(1,3), t(1,2) );

        assertEquals( expected_prefix, prefix );
    }

    @Test
    public void testNextDFSExhaustion() {

        final List<Tuple<Integer,Integer>> run = Arrays.asList(t(2,3), t(0,1), t(1,2));

        final Exception exn = assertThrows( SearchSpaceExhaustedException.class, () -> {
            App.nextPrefixDFS( run );
        }, "There are no more options to choose from in step 1");
    }
}
