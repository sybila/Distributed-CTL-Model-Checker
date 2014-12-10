package cz.muni.fi;

import cz.muni.fi.graph.Graph;
import cz.muni.fi.graph.Node;
import cz.muni.fi.graph.Path;
import cz.muni.fi.graph.SubGraph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created by daemontus on 14/10/14.
 */
public class GraphUtils {

    @NotNull
    private static Queue<Node> queue = new LinkedList<>();
    @NotNull
    private static Set<Node> border = new HashSet<>();
    @NotNull
    private static Set<Node> waitingForLink = new HashSet<>();

    @NotNull
    public static List<SubGraph> partitionGraph(@NotNull Graph graph, int maxNodes) {
        @NotNull List<SubGraph> results = new ArrayList<>();
        queue = new LinkedList<>();
        border = new HashSet<>();
        waitingForLink = new HashSet<>();
        int subGraph = 0;
        int rank = 0;
        @NotNull SubGraph active = new SubGraph(graph.getColors(), subGraph);
        results.add(active);
        for (@NotNull Node node : graph.getInitialNodes()) {
            @NotNull Node copy = node.copy();
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

    @Nullable
    private static SubGraph splitGraph(int subGraph, SubGraph active) {
        return null;
    }

}
