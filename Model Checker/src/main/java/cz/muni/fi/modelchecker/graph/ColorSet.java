package cz.muni.fi.modelchecker.graph;

/**
 * Represents a set of colors/parameters.
 */
public interface ColorSet {

    public void intersect(ColorSet set);

    public void subtract(ColorSet set);

    public void union(ColorSet set);

    public boolean isEmpty();
}
