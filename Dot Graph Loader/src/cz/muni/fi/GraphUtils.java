package cz.muni.fi;

import cz.muni.fi.graph.Graph;
import cz.muni.fi.graph.Node;
import cz.muni.fi.graph.Path;
import cz.muni.fi.graph.SubGraph;

import java.util.*;

/**
 * Created by daemontus on 14/10/14.
 */
public class GraphUtils {

    private static Queue<Node> queue = new LinkedList<>();
    private static Set<Node> border = new HashSet<>();
    private static Set<Node> waitingForLink = new HashSet<>();

    public static List<SubGraph> partitionGraph(Graph graph, int maxNodes) {
        List<SubGraph> results = new ArrayList<>();
        queue = new LinkedList<>();
        border = new HashSet<>();
        waitingForLink = new HashSet<>();
        int subGraph = 0;
        int rank = 0;
        SubGraph active = new SubGraph(graph.getColors(), subGraph);
        results.add(active);
        for (Node node : graph.getInitialNodes()) {
            Node copy = node.copy();
            node.setSubGraphId(subGraph);
            queue.add(node);
            border.add(node);
            active.addInitialNode(node);
            rank++;
        }
        while (!queue.isEmpty()) {
            Node process = queue.remove();
            for (Path path : process.getAfter()) {

            }
        }
        return results;
    }

    private static SubGraph splitGraph(int subGraph, SubGraph active) {
        return null;
    }

}
