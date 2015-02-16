package cz.muni.fi.thomas;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LevelNode implements Node {

    public int[] levels;

    private int owner = -1;

    @NotNull
    //TODO: We have to store colors as list of color sets, because the set is not complete parameter set, it's a template for combinations of parameters
    private final Map<Formula, BitMapColorSet> formulae = new HashMap<>();

    private Map<LevelNode, BitMapColorSet> predecessors = new HashMap<>();

    private Map<LevelNode, BitMapColorSet> successors = new HashMap<>();

    public LevelNode(@NotNull int[] levels) {
        this.levels = Arrays.copyOf(levels, levels.length);
    }

    public int getLevel(int dimension) {
        return levels[dimension];
    }

    public synchronized void addPredecessor(LevelNode predecessor, BitMapColorSet transitionColors) {
        predecessors.put(predecessor, transitionColors);
    }

    public synchronized void addSuccessor(LevelNode successor, BitMapColorSet transitionColors) {
        successors.put(successor, transitionColors);
    }

    @NotNull
    public synchronized Map<LevelNode, BitMapColorSet> getPredecessors(@Nullable BitMapColorSet borders) {
        @NotNull Map<LevelNode, BitMapColorSet> results = new HashMap<>();
        for (@NotNull Map.Entry<LevelNode, BitMapColorSet> entry : predecessors.entrySet()) {
            @NotNull BitMapColorSet colorSet = BitMapColorSet.createCopy(entry.getValue());
            if (borders != null) {
                colorSet.intersect(borders);
            }
            if (!colorSet.isEmpty()) {
                results.put(entry.getKey(), colorSet);
            }
        }
        return results;
    }

    @NotNull
    public synchronized Map<LevelNode, BitMapColorSet> getSuccessors(@Nullable BitMapColorSet borders) {
        @NotNull Map<LevelNode, BitMapColorSet> results = new HashMap<>();
        for (@NotNull Map.Entry<LevelNode, BitMapColorSet> entry : successors.entrySet()) {
            @NotNull BitMapColorSet colorSet = BitMapColorSet.createCopy(entry.getValue());
            if (borders != null) {
                colorSet.intersect(borders);
            }
            if (!colorSet.isEmpty()) {
                results.put(entry.getKey(), colorSet);
            }
        }
        return results;
    }

    public synchronized BitMapColorSet getValidColors(Formula formula) {
        return formulae.get(formula);
    }

    public synchronized boolean addFormula(Formula formula, @NotNull BitMapColorSet colors) {
        if (colors.isEmpty()) return false;
        BitMapColorSet colorSet = formulae.get(formula);
        if (colorSet == null) {
            formulae.put(formula, BitMapColorSet.createCopy(colors));
            return true;
        } else {
            if (colorSet.encloses(colors)) {
                return false;
            } else {
                colorSet.union(colors);
                return true;
            }
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(levels);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LevelNode) {
            @NotNull LevelNode n = (LevelNode) obj;
            return Arrays.equals(levels, n.levels);
        }
        return false;
    }

    @NotNull
    @Override
    public String toString() {
        return Arrays.toString(levels)/* +" formulae: "+formulae.toString()*/;
    }

    @NotNull
    public String fullString() {
        return toString() + " Successors: " + Arrays.toString(successors.entrySet().toArray())+" Predecessors: " + Arrays.toString(predecessors.entrySet().toArray());
    }

    public int getOwner() {
        return owner;
    }

    public void setOwner(int owner) {
        this.owner = owner;
    }

}
