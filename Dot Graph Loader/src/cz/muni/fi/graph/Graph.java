package cz.muni.fi.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Graph {

    private Set<Integer> parameterSet = new HashSet<>();
    private Set<Node> initialNodes = new HashSet<>();
    private Set<Node> terminalNodes = new HashSet<>();

    public Graph(Set<Integer> parameterSet) {
        if (parameterSet == null) {
            throw new NullPointerException("You have to provide parameter set");
        }
        this.parameterSet.addAll(parameterSet);
    }

    public Set<Node> getInitialNodes() {
        return initialNodes;
    }

    public void addTerminalNode(Node node) {
        terminalNodes.add(node);
    }

    public void addInitialNode(Node node) {
        initialNodes.add(node);
    }

    public Set<Node> getTerminalNodes() {
        return terminalNodes;
    }

    public Set<Integer> getColors() {
        return Collections.unmodifiableSet(parameterSet);
    }

    public Set<Integer> getColorsCopy() {
        Set<Integer> copy = new HashSet<>();
        copy.addAll(parameterSet);
        return copy;
    }


}
