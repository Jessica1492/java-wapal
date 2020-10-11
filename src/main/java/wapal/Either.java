package wapal;

import java.util.Objects;
import java.util.function.Function;

/** Utility class modeled after Haskell's 'Either' datatype.
 * Build instances through Either.left and Either.right.
 * Per convention 'right' means 'ok' and 'left' means 'error'
 * when using Either like Rust's 'Result' datatype.
 */
public interface Either<A,B> {

    static <AA,BB> Left<AA,BB> left( AA a ) { return new Left<>(a); }
    static <AA,BB> Right<AA,BB> right( BB b ) { return new Right<>(b); }

    default boolean isLeft() { return ! this.isRight(); }
    boolean isRight();

    A getLeft();
    B getRight();

    <T> Either<T,B> mapLeft( Function<A,T> f );
    <T> Either<A,T> mapRight( Function<B,T> f );

    static class Left<A,B> implements Either<A,B> {
        final A a;
        public Left( A a ) { this.a=a; }

        public boolean isRight() { return false; }

        public A getLeft() { return this.a; }
        public B getRight() { throw new IllegalStateException("Left has no Right"); }

        public <T> Either<T,B> mapLeft( Function<A,T> f ) { return new Left<>(f.apply(a)); }
        public <T> Either<A,T> mapRight( Function<B,T> f ) { return new Left<>(a); }

        public boolean equals( Object o ) {
            return o instanceof Either && Objects.equals( ((Either)o).getLeft(), a );
        }
        public int hashCode() { return a.hashCode() + 1; }
        public String toString() { return String.format("Left[%s]", a); }
    }

    static class Right<A,B> implements Either<A,B> {
        final B b;
        public Right( B b) { this.b=b; }

        public boolean isRight() { return true; }

        public A getLeft() { throw new IllegalStateException("Right has no Left"); }
        public B getRight() { return this.b; }

        public <T> Either<T,B> mapLeft( Function<A,T> f ) { return new Right<>(b); }
        public <T> Either<A,T> mapRight( Function<B,T> f ) { return new Right<>(f.apply(b)); }

        public boolean equals( Object o ) {
            return o instanceof Either && Objects.equals( ((Either)o).getRight(), b );
        }
        public int hashCode() { return b.hashCode() + 1; }
        public String toString() { return String.format("Right[%s]", b); }
    }

}