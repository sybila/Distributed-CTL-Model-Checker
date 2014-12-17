package cz.muni.fi.modelchecker.graph;

import org.jetbrains.annotations.NotNull;

/**
 * A node with assigned colors.
 * (Basically a better Pair class)
 *
 * The equals and hashcode are modified to ensure equivalence only on node class.
 */
public class ColoredNode<N extends Node, C extends ColorSet> {

    private final N node;
    private final C colors;


    public ColoredNode(@NotNull N node, @NotNull C colors) {
        this.node = node;
        this.colors = colors;
    }

    public @NotNull N getNode() {
        return node;
    }

    public @NotNull C getColors() {
        return colors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColoredNode)) return false;

        ColoredNode that = (ColoredNode) o;

        return node.equals(that.node);

    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }
}
