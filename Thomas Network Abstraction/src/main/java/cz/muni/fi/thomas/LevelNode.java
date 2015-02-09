package cz.muni.fi.thomas;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LevelNode implements Node {

    private int[] levels;

    private int owner = -1;

    @NotNull
    private final Map<Formula, BitMapColorSet> formulae = new HashMap<>();

    private Map<LevelNode, BitMapColorSet> predecessors;

    private Map<LevelNode, BitMapColorSet> successors;

    public LevelNode(@NotNull int[] levels) {
        this.levels = Arrays.copyOf(levels, levels.length);
    }

    public int getLevel(int dimension) {
        return levels[dimension];
    }

    public synchronized boolean hasPredecessorsFor() {
        return predecessors != null;
    }

    public synchronized void savePredecessors(@NotNull Map<LevelNode, BitMapColorSet> predecessors) {
        this.predecessors = new HashMap<>(predecessors);
    }

    public synchronized boolean hasSuccessors() {
        return successors != null;
    }

    public synchronized void saveSuccessors(@NotNull Map<LevelNode, BitMapColorSet> successors) {
        this.successors = new HashMap<>(successors);
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
        return Arrays.toString(levels) +" formulae: "+formulae.toString();
    }

    public int getOwner() {
        return owner;
    }

    public void setOwner(int owner) {
        this.owner = owner;
    }

}
