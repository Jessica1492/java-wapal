package wapal;

import java.util.Objects;

/** Tuple class, because Java has none canonical. */
public class Tuple<A,B> {
    public final A fst;
    public final B snd;
    public Tuple( A a, B b ) { this.fst=a; this.snd=b; }

    public boolean equals( Object o ) {
        return o instanceof Tuple
        && Objects.equals( ((Tuple)o).fst, this.fst )
        && Objects.equals( ((Tuple)o).snd, this.snd );
    }
    public int hashCode() { return fst.hashCode() + snd.hashCode(); }
    public String toString() { return String.format( "Tuple[%s,%s]", fst, snd ); }
}