package cz.muni.fi.modelchecker.mpi.termination;

/**
 * Represents one instance of a token passed during termination detection.
 */
public class Token {
    public final int count;
    public final int flag;

    public Token(int flag, int count) {
        this.count = count;
        this.flag = flag;
    }
}
