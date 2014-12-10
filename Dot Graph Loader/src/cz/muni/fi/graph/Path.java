package cz.muni.fi.graph;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Path {

    @Nullable
    private Node from;
    @Nullable
    private Node to;
    @NotNull
    private Set<Integer> colors = new HashSet<>();

    public Path(@Nullable Node from, @Nullable Node to) {
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

    @NotNull
    public Set<Integer> getColors() {
        return colors;
    }
    public boolean hasColor(int color) {
        return colors.contains(color);
    }

    @NotNull
    public Set<Integer> intersectColors(@NotNull Set<Integer> other) {
        @NotNull Set<Integer> copy = new HashSet<>();
        copy.addAll(other);
        copy.retainAll(colors);
        return copy;
    }

    @Nullable
    public Node getFrom() {
        return from;
    }

    @Nullable
    public Node getTo() {
        return to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Path)) return false;

        @NotNull Path path = (Path) o;

        return from.equals(path.from) && to.equals(path.to);

    }

    @Override
    public int hashCode() {
        int result = from.hashCode();
        result = 31 * result + to.hashCode();
        return result;
    }
}
