package cz.muni.fi.ode;

import com.google.common.collect.Range;
import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.proposition.Contradiction;
import cz.muni.fi.ctl.formula.proposition.FloatProposition;
import cz.muni.fi.ctl.formula.proposition.Tautology;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.graph.ColorSet;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class NodeFactory implements ModelAdapter<CoordinateNode, TreeColorSet> {


    @NotNull
    private Set<FloatProposition> revealedPropositions = new HashSet<>();

    private final Map<Integer, CoordinateNode> nodeCache = new HashMap<>();
    private final Map<Integer, CoordinateNode> borderNodes = new HashMap<>();
    private final OdeModel model;
    private final RectangularPartitioner partitioner;
    private final int myId;
    private boolean hasAllNodes = false;

    public NodeFactory(OdeModel model, RectangularPartitioner partitioner) {
        this.model = model;
        this.partitioner = partitioner;
        this.myId = partitioner.getMyId();
    }

    public Collection<CoordinateNode> getNodes() {
        return nodeCache.values();
    }

    public synchronized CoordinateNode getNode(@NotNull int[] coordinates) {
        int hash = Arrays.hashCode(coordinates);
        if (nodeCache.containsKey(hash)) {
            return nodeCache.get(hash);
        } else if (borderNodes.containsKey(hash)) {
            return borderNodes.get(hash);
        } else {
            @NotNull CoordinateNode n = new CoordinateNode(coordinates);
            if (partitioner.getNodeOwner(n) == myId) {
                nodeCache.put(hash, n);
            } else {
                borderNodes.put(hash, n);
            }
            return n;
        }
    }

    @NotNull
    @Override
    public synchronized Map<CoordinateNode, TreeColorSet> predecessorsFor(@NotNull CoordinateNode to, @NotNull TreeColorSet borders) {
      //  System.out.println("Get predecessors nodes: "+Arrays.toString(to.coordinates)+" "+ MPI.COMM_WORLD.Rank());
        return getNativePredecessors(to.coordinates, borders, new HashMap<CoordinateNode, TreeColorSet>());
    }

    @NotNull
    @Override
    public synchronized Map<CoordinateNode, TreeColorSet> successorsFor(@NotNull CoordinateNode from, @NotNull TreeColorSet borders) {
        return getNativeSuccessors(from.coordinates, borders, new HashMap<CoordinateNode, TreeColorSet>());
    }

    @NotNull
    @Override
    public synchronized Map<CoordinateNode, TreeColorSet> initialNodes(@NotNull Formula formula) {
        if (formula instanceof Tautology) {
            if (!hasAllNodes) {
                cacheAllNodes(partitioner.getMyLimit());
                hasAllNodes = true;
            }
            Map<CoordinateNode, TreeColorSet> results = new HashMap<>();
            for (CoordinateNode node : nodeCache.values()) {
                results.put(node, model.getFullColorSet());
            }
            return results;
        }
        if (formula instanceof Contradiction) {
            return new HashMap<>();
        }
        if (formula instanceof FloatProposition && !revealedPropositions.contains(formula)) {
            FloatProposition proposition = (FloatProposition) formula;
            revealedPropositions.add(proposition);
            Map<CoordinateNode, TreeColorSet> values = getNativeInit(
                    proposition.getVariable(),
                    proposition.getNativeOperator(),
                    proposition.getThreshold(),
                    partitioner.getMyLimit(), new HashMap<CoordinateNode, TreeColorSet>());
            for (Map.Entry<CoordinateNode, TreeColorSet> entry : values.entrySet()) {
                entry.getKey().addFormula(proposition, entry.getValue());
            }
            return values;
        }
        Map<CoordinateNode, TreeColorSet> results = new HashMap<>();
        for (CoordinateNode n : nodeCache.values()) {
            TreeColorSet validColors = n.getValidColors(formula);
            if (validColors != null && !validColors.isEmpty()) {
                results.put(n, validColors);
            }
        }
        return results;
    }

    @NotNull
    @Override
    public synchronized Map<CoordinateNode, TreeColorSet> invertNodeSet(Map<CoordinateNode, TreeColorSet> nodes) {
        if (!hasAllNodes) {
            cacheAllNodes(partitioner.getMyLimit());
            hasAllNodes = true;
        }
        Map<CoordinateNode, TreeColorSet> results = new HashMap<>();
        for (CoordinateNode n : nodeCache.values()) {
            TreeColorSet full = model.getFullColorSet();
            TreeColorSet anti = nodes.get(n);
            if (anti != null) {
                full.subtract(anti);
            }
            if (!full.isEmpty()) {
                results.put(n, full);
            }
        }
        return results;
    }

    @Override
    public synchronized boolean addFormula(@NotNull CoordinateNode node, @NotNull Formula formula, @NotNull TreeColorSet parameters) {
        return node.addFormula(formula, parameters);
    }

    @NotNull
    @Override
    public TreeColorSet validColorsFor(@NotNull CoordinateNode node, @NotNull Formula formula) {
        if (formula instanceof Tautology) return model.getFullColorSet();
        if (formula instanceof Contradiction) return TreeColorSet.createEmpty(model.parameterCount());
        if (formula instanceof FloatProposition && !revealedPropositions.contains(formula)) {
            FloatProposition proposition = (FloatProposition) formula;
            revealedPropositions.add(proposition);
            Map<CoordinateNode, TreeColorSet> values = getNativeInit(
                    proposition.getVariable(),
                    proposition.getNativeOperator(),
                    proposition.getThreshold(),
                    partitioner.getMyLimit(), new HashMap<CoordinateNode, TreeColorSet>());
            for (Map.Entry<CoordinateNode, TreeColorSet> entry : values.entrySet()) {
                entry.getKey().addFormula(proposition, entry.getValue());
            }
        }
        TreeColorSet colorSet = node.getValidColors(formula);
        if (colorSet == null) return TreeColorSet.createEmpty(model.parameterCount());
        return colorSet;
    }


    @NotNull
    private native Map<CoordinateNode, TreeColorSet> getNativePredecessors(int[] node, ColorSet initial, Map<CoordinateNode, TreeColorSet> ret);

    @NotNull
    private native Map<CoordinateNode, TreeColorSet> getNativeSuccessors(int[] node, ColorSet initial, Map<CoordinateNode, TreeColorSet> ret);

    @NotNull
    private native Map<CoordinateNode, TreeColorSet> getNativeInit(String var, int op, double th, List<Range<Double>> limit, Map<CoordinateNode, TreeColorSet> ret);

    private native void cacheAllNodes(List<Range<Double>> limit);

}
