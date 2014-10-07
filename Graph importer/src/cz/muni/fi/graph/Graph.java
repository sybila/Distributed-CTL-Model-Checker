package cz.muni.fi.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Graph {

    private Node initialNode;
    private Set<Node> endNodes = new HashSet<Node>();

    public Graph(Node initialNode) {
        this.initialNode = initialNode;
    }

    public Node getInitialNode() {
        return initialNode;
    }

    public void addEnd(Node node) {
        endNodes.add(node);
    }

    public Collection<Node> endNodes() {
        return endNodes;
    }
}
