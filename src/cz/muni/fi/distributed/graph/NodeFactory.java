package cz.muni.fi.distributed.graph;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.proposition.FloatProposition;
import cz.muni.fi.ctl.formula.proposition.Proposition;
import cz.muni.fi.ctl.util.Log;
import cz.muni.fi.model.ColorSet;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NodeFactory {

    private Graph graph;

    private Set<FloatProposition> revealedPropositions = new HashSet<>();

    public final Map<Integer, Node> nodeCache = new HashMap<>();

    public NodeFactory(Graph graph) {
        this.graph = graph;
    }

    public synchronized Node getNode(int[] coordinates) {
        int hash = Arrays.hashCode(coordinates);
        if (!nodeCache.containsKey(hash)) {
            Node n = new Node(coordinates, graph);
            nodeCache.put(hash, n);
            return n;
        } else {
            return nodeCache.get(hash);
        }
    }

    public synchronized Map<Node, ColorSet> computePredecessors(Node node, ColorSet initial) {
        return getNativePredecessors(node.coordinates, initial, new HashMap<Node, ColorSet>());
    }

    public synchronized Map<Node, ColorSet> computeSuccessors(Node node, ColorSet initial) {
        return getNativeSuccessors(node.coordinates, initial, new HashMap<Node, ColorSet>());
    }

    public Map<Node, ColorSet> getAllValidNodes(Formula proposition) {
        //TODO: Fix tautology etc...
        if (proposition instanceof FloatProposition && !revealedPropositions.contains(proposition)) {
            FloatProposition floatProposition = (FloatProposition) proposition;
            List<Range<Double>> limit = graph.getLimit();
             Log.d("Check native "+floatProposition+" "+limit.size());
            revealedPropositions.add(floatProposition);
            return getNativeInit(
                    floatProposition.getVariable(),
                    floatProposition.getNativeOperator(),
                    floatProposition.getThreshold(),
                    limit, new HashMap<Node, ColorSet>());
        } else {
            Map<Node, ColorSet> results = new HashMap<>();
            for (Node n : nodeCache.values()) {
                ColorSet validColors = n.getValidColors(proposition);
                if (!validColors.isEmpty()) {
                    results.put(n, validColors);
                }
            }
            Log.d("Results: "+results.size()+" formula: "+proposition);
            return results;
        }
    }

    private native Map<Node, ColorSet> getNativePredecessors(int[] node, ColorSet initial, Map<Node, ColorSet> ret);

    private native Map<Node, ColorSet> getNativeSuccessors(int[] node, ColorSet initial, Map<Node, ColorSet> ret);

    private native Map<Node, ColorSet> getNativeInit(String var, int op, float th, List<Range<Double>> limit, Map<Node, ColorSet> ret);

}
