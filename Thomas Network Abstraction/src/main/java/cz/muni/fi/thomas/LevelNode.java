package cz.muni.fi.thomas;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.proposition.FloatProposition;
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
    private final Map<Formula, BitmapColorSet> formulae = new HashMap<>();

    private Map<LevelNode, BitmapColorSet> predecessors;

    private Map<LevelNode, BitmapColorSet> successors;

    public LevelNode(@NotNull int[] levels) {
        this.levels = Arrays.copyOf(levels, levels.length);
    }

    public int getLevel(int dimension) {
        return levels[dimension];
    }

    public synchronized boolean hasPredecessorsFor() {
        return predecessors != null;
    }

    public synchronized void savePredecessors(@NotNull Map<LevelNode, BitmapColorSet> predecessors) {
        this.predecessors = new HashMap<>(predecessors);
    }

    public synchronized boolean hasSuccessors() {
        return successors != null;
    }

    public synchronized void saveSuccessors(@NotNull Map<LevelNode, BitmapColorSet> successors) {
        this.successors = new HashMap<>(successors);
    }

    @NotNull
    public synchronized Map<LevelNode, BitmapColorSet> getPredecessors(@Nullable BitmapColorSet borders) {
        @NotNull Map<LevelNode, BitmapColorSet> results = new HashMap<>();
        for (@NotNull Map.Entry<LevelNode, BitmapColorSet> entry : predecessors.entrySet()) {
            @NotNull BitmapColorSet colorSet = BitmapColorSet.createCopy(entry.getValue());
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
    public synchronized Map<LevelNode, BitmapColorSet> getSuccessors(@Nullable BitmapColorSet borders) {
        @NotNull Map<LevelNode, BitmapColorSet> results = new HashMap<>();
        for (@NotNull Map.Entry<LevelNode, BitmapColorSet> entry : successors.entrySet()) {
            @NotNull BitmapColorSet colorSet = BitmapColorSet.createCopy(entry.getValue());
            if (borders != null) {
                colorSet.intersect(borders);
            }
            if (!colorSet.isEmpty()) {
                results.put(entry.getKey(), colorSet);
            }
        }
        return results;
    }

    public synchronized BitmapColorSet getValidColors(Formula formula) {
        return formulae.get(formula);
    }

    public synchronized boolean addFormula(Formula formula, @NotNull BitmapColorSet colors) {
        if (colors.isEmpty()) return false;
        BitmapColorSet colorSet = formulae.get(formula);
        if (colorSet == null) {
            formulae.put(formula, BitmapColorSet.createCopy(colors));
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
