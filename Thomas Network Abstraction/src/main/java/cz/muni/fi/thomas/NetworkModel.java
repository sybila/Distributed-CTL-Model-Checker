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
public class NetworkModel implements ModelAdapter<LevelNode, BitMapColorSet> {


    @NotNull
    private final Set<FloatProposition> revealedPropositions = new HashSet<>();

    @NotNull
    private final Map<Integer, LevelNode> nodeCache = new HashMap<>();

    @NotNull
    private final Map<Integer, LevelNode> borderNodes = new HashMap<>();

    //filled by native code
    @NotNull
    public List<String> variableOrdering = new ArrayList<>();

    @NotNull
    private final StateSpacePartitioner<LevelNode> partitioner;
    private final int myId;
    //this is not a number of parameters, it's a bit width of vector that can hold all independent parameter templates.
    public int paramSpaceWidth = 0;

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
    public Map<LevelNode, BitMapColorSet> predecessorsFor(@NotNull LevelNode to, @Nullable BitMapColorSet borders) {
        return to.getPredecessors(borders);
    }

    @NotNull
    @Override
    public Map<LevelNode, BitMapColorSet> successorsFor(@NotNull LevelNode from, @Nullable BitMapColorSet borders) {
        return from.getSuccessors(borders);
    }

    @NotNull
    @Override
    public Map<LevelNode, BitMapColorSet> initialNodes(@NotNull Formula formula) {
        if (formula instanceof Tautology) {
            @NotNull Map<LevelNode, BitMapColorSet> results = new HashMap<>();
            for (LevelNode node : nodeCache.values()) {
                results.put(node, BitMapColorSet.createFull(paramSpaceWidth));
            }
            return results;
        }
        if (formula instanceof Contradiction) {
            return new HashMap<>();
        }
        if (formula instanceof FloatProposition && !revealedPropositions.contains(formula)) {
            revealProposition((FloatProposition) formula);
        }
        @NotNull Map<LevelNode, BitMapColorSet> results = new HashMap<>();
        for (@NotNull LevelNode n : nodeCache.values()) {
            BitMapColorSet validColors = n.getValidColors(formula);
            if (validColors != null && !validColors.isEmpty()) {
                results.put(n, validColors);
            }
        }
        return results;
    }

    @NotNull
    @Override
    public Map<LevelNode, BitMapColorSet> invertNodeSet(@NotNull Map<LevelNode, BitMapColorSet> nodes) {
        @NotNull Map<LevelNode, BitMapColorSet> results = new HashMap<>();
        for (LevelNode n : nodeCache.values()) {
            @NotNull BitMapColorSet full = BitMapColorSet.createFull(paramSpaceWidth);
            BitMapColorSet anti = nodes.get(n);
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
    public boolean addFormula(@NotNull LevelNode node, @NotNull Formula formula, @NotNull BitMapColorSet parameters) {
        return node.addFormula(formula, parameters);
    }

    @NotNull
    @Override
    public BitMapColorSet validColorsFor(@NotNull LevelNode node, @NotNull Formula formula) {
        if (formula instanceof Tautology) return BitMapColorSet.createFull(paramSpaceWidth);
        if (formula instanceof Contradiction) return new BitMapColorSet();
        if (formula instanceof FloatProposition && !revealedPropositions.contains(formula)) {
            revealProposition((FloatProposition) formula);
        }
        BitMapColorSet colorSet = node.getValidColors(formula);
        return colorSet == null ? new BitMapColorSet() : colorSet;
    }

    @Override
    public void purge(Formula formula) {

    }

    private void revealProposition(FloatProposition proposition) {
        for (LevelNode entry : nodeCache.values()) {
            if (proposition.evaluate((double) entry.getLevel(variableOrdering.indexOf(proposition.getVariable())))) {
                entry.addFormula(proposition, BitMapColorSet.createFull(paramSpaceWidth));
            }
        }
        revealedPropositions.add(proposition);
    }

    public void printOut() {
        for (String s : variableOrdering) {
            System.out.println("Var: "+s);
        }
        System.out.println("Param space width: "+paramSpaceWidth);
        for (LevelNode n : nodeCache.values()) {
            System.out.println("Node: "+n.fullString());
        }
    }
/*
    public void load(String filename) {
        loadNative(filename);
    }

    private native void loadNative(String filename);
*/
}
