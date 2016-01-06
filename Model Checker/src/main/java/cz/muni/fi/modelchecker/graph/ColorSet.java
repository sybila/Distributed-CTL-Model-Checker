package cz.muni.fi.modelchecker.graph;

/**
 * Represents a set of colors/parameters.
 */
public interface ColorSet {

    void intersect(ColorSet set);

    void subtract(ColorSet set);

    void union(ColorSet set);

    boolean isEmpty();
}
