package cz.muni.fi.graph;

import java.util.HashSet;
import java.util.Set;

public class Path {

    private Node from;
    private Node to;
    private Set<Integer> colors = new HashSet<>();

    public Path(Node from, Node to) {
        if (from == null) {
            throw new NullPointerException("Cannot create path with no start");
        }
        if (to == null) {
            throw new NullPointerException("Cannot create path with no end");
        }
        this.from = from;
        this.to = to;
    }

    public void addColor(int color) {
        colors.add(color);
    }

    public boolean hasColor(int color) {
        return colors.contains(color);
    }

    public Set<Integer> intersectColors(Set<Integer> other) {
        Set<Integer> copy = new HashSet<>();
        copy.addAll(other);
        copy.retainAll(colors);
        return copy;
    }

    public Node getFrom() {
        return from;
    }

    public Node getTo() {
        return to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Path)) return false;

        Path path = (Path) o;

        return from.equals(path.from) && to.equals(path.to);

    }

    @Override
    public int hashCode() {
        int result = from.hashCode();
        result = 31 * result + to.hashCode();
        return result;
    }
}
