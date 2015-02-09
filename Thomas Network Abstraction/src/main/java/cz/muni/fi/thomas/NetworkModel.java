package cz.muni.fi.thomas;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.proposition.Contradiction;
import cz.muni.fi.ctl.formula.proposition.FloatProposition;
import cz.muni.fi.ctl.formula.proposition.Tautology;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Data if filled by C++ state space generator by calling load(). Nothing is computed on the fly, everything is prepared before verification.
 */
public class NetworkModel implements ModelAdapter<LevelNode, BitmapColorSet> {


    @NotNull
    private final Set<FloatProposition> revealedPropositions = new HashSet<>();

    private final Map<Integer, LevelNode> nodeCache = new HashMap<>();
    private final Map<Integer, LevelNode> borderNodes = new HashMap<>();

    //filled by native code
    private List<String> variableOrdering = new ArrayList<>();

    @NotNull
    private final StateSpacePartitioner<LevelNode> partitioner;
    private final int myId;
    private int numOfParameters;

    public NetworkModel(@NotNull StateSpacePartitioner<LevelNode> partitioner) {
        this.partitioner = partitioner;
        this.myId = partitioner.getMyId();
    }

    @NotNull
    public Collection<LevelNode> getNodes() {
        return nodeCache.values();
    }

    public synchronized LevelNode getNode(@NotNull int[] coordinates) {
        int hash = Arrays.hashCode(coordinates);
        if (nodeCache.containsKey(hash)) {
            return nodeCache.get(hash);
        } else if (borderNodes.containsKey(hash)) {
            return borderNodes.get(hash);
        } else {
            @NotNull LevelNode n = new LevelNode(coordinates);
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
    public Map<LevelNode, BitmapColorSet> predecessorsFor(@NotNull LevelNode to, @Nullable BitmapColorSet borders) {
        return to.getPredecessors(borders);
    }

    @NotNull
    @Override
    public Map<LevelNode, BitmapColorSet> successorsFor(@NotNull LevelNode from, @Nullable BitmapColorSet borders) {
        return from.getSuccessors(borders);
    }

    @NotNull
    @Override
    public Map<LevelNode, BitmapColorSet> initialNodes(@NotNull Formula formula) {
        if (formula instanceof Tautology) {
            @NotNull Map<LevelNode, BitmapColorSet> results = new HashMap<>();
            for (LevelNode node : nodeCache.values()) {
                results.put(node, BitmapColorSet.createFull(numOfParameters));
            }
            return results;
        }
        if (formula instanceof Contradiction) {
            return new HashMap<>();
        }
        if (formula instanceof FloatProposition && !revealedPropositions.contains(formula)) {
            revealProposition((FloatProposition) formula);
        }
        @NotNull Map<LevelNode, BitmapColorSet> results = new HashMap<>();
        for (@NotNull LevelNode n : nodeCache.values()) {
            BitmapColorSet validColors = n.getValidColors(formula);
            if (validColors != null && !validColors.isEmpty()) {
                results.put(n, validColors);
            }
        }
        return results;
    }

    @NotNull
    @Override
    public Map<LevelNode, BitmapColorSet> invertNodeSet(@NotNull Map<LevelNode, BitmapColorSet> nodes) {
        @NotNull Map<LevelNode, BitmapColorSet> results = new HashMap<>();
        for (LevelNode n : nodeCache.values()) {
            @NotNull BitmapColorSet full = BitmapColorSet.createFull(numOfParameters);
            BitmapColorSet anti = nodes.get(n);
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
    public boolean addFormula(@NotNull LevelNode node, @NotNull Formula formula, @NotNull BitmapColorSet parameters) {
        return node.addFormula(formula, parameters);
    }

    @NotNull
    @Override
    public BitmapColorSet validColorsFor(@NotNull LevelNode node, @NotNull Formula formula) {
        if (formula instanceof Tautology) return BitmapColorSet.createFull(numOfParameters);
        if (formula instanceof Contradiction) return new BitmapColorSet();
        if (formula instanceof FloatProposition && !revealedPropositions.contains(formula)) {
            revealProposition((FloatProposition) formula);
        }
        BitmapColorSet colorSet = node.getValidColors(formula);
        return colorSet == null ? new BitmapColorSet() : colorSet;
    }

    private void revealProposition(FloatProposition proposition) {
        for (LevelNode entry : nodeCache.values()) {
            if (proposition.evaluate((double) entry.getLevel(variableOrdering.indexOf(proposition.getVariable())))) {
                entry.addFormula(proposition, BitmapColorSet.createFull(numOfParameters));
            }
        }
        revealedPropositions.add(proposition);
    }

    public void load(String filename) {
        loadNative(filename);
    }

    private native void loadNative(String filename);

}
