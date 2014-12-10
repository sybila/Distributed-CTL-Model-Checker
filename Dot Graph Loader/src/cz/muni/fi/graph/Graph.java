package cz.muni.fi.graph;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Graph {

    @NotNull
    private Set<Integer> parameterSet = new HashSet<>();
    @NotNull
    private Set<Node> initialNodes = new HashSet<>();
    @NotNull
    private Set<Node> terminalNodes = new HashSet<>();

    public Graph(@Nullable Set<Integer> parameterSet) {
        if (parameterSet == null) {
            throw new NullPointerException("You have to provide parameter set");
        }
        this.parameterSet.addAll(parameterSet);
    }

    @NotNull
    public Set<Node> getInitialNodes() {
        return initialNodes;
    }

    public void addTerminalNode(Node node) {
        terminalNodes.add(node);
    }

    public void addInitialNode(Node node) {
        initialNodes.add(node);
    }

    @NotNull
    public Set<Node> getTerminalNodes() {
        return terminalNodes;
    }

    @NotNull
    public Set<Integer> getColors() {
        return Collections.unmodifiableSet(parameterSet);
    }

    @NotNull
    public Set<Integer> getColorsCopy() {
        @NotNull Set<Integer> copy = new HashSet<>();
        copy.addAll(parameterSet);
        return copy;
    }


}
