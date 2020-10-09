package wapal;

/** Tuple class, because Java has none canonical. */
public class Tuple<A,B> {
    public final A fst;
    public final B snd;
    public Tuple( A a, B b ) { this.fst=a; this.snd=b; }
}