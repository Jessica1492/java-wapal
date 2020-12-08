package wapal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import wapal.Either;

public class EitherTest {

    @Test
    public void testEitherMap() {

        // check normal mapping operation
        final Either<String,Integer> left1 = Either.left("Hello");
        final Either<Integer,Integer> left2 = left1.mapLeft( String::length );

        assertEquals( 5, left2.getLeft() );

        // check if ClassCastException is raised
        final Either<String,Integer> right1 = Either.right(2);
        final Either<Integer,Integer> right2 = right1.mapLeft( String::length );

        assertEquals( right1.getRight(), right2.getRight() );
    }
}