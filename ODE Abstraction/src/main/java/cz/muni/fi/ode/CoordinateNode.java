package cz.muni.fi.ode;


import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CoordinateNode implements Node {

    public int[] coordinates;
    private long hash;

    private int owner = -1;

    @NotNull
    private final Map<Formula, TreeColorSet> formulae = new HashMap<>();

    private Map<CoordinateNode, TreeColorSet> predecessors;

    public CoordinateNode(@NotNull int[] coordinates, long hash) {
        this.coordinates = new int[coordinates.length];
        System.arraycopy(coordinates, 0, this.coordinates, 0, coordinates.length);
        this.hash = hash;
    }

    public synchronized boolean hasPredecessorsFor() {
        return predecessors != null;
    }

    public synchronized void savePredecessors(@NotNull Map<CoordinateNode, TreeColorSet> predecessors) {
        this.predecessors = new HashMap<>();
        this.predecessors.putAll(predecessors);
    }

    @NotNull
    public synchronized Map<CoordinateNode, TreeColorSet> getPredecessors(TreeColorSet borders) {
        @NotNull Map<CoordinateNode, TreeColorSet> results = new HashMap<>();
        for (@NotNull Map.Entry<CoordinateNode, TreeColorSet> entry : predecessors.entrySet()) {
            @NotNull TreeColorSet colorSet = TreeColorSet.createCopy(entry.getValue());
            colorSet.intersect(borders);
            if (!colorSet.isEmpty()) {
                results.put(entry.getKey(), colorSet);
            }
        }
        return results;
    }

    public int getCoordinate(int dim) {
        return coordinates[dim];
    }

    public void purgeFormula(Formula formula) {
        formulae.remove(formula);
    }

    public synchronized TreeColorSet getValidColors(Formula formula) {
        return formulae.get(formula);
    }

    public synchronized boolean addFormula(Formula formula, @NotNull TreeColorSet colors) {
        if (colors.isEmpty()) return false;
        TreeColorSet colorSet = formulae.get(formula);
        if (colorSet == null) {
            formulae.put(formula, TreeColorSet.createCopy(colors));
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
        return (int) hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CoordinateNode) {
            @NotNull CoordinateNode n = (CoordinateNode) obj;
            return hash == n.hash;
        }
        return false;
    }

    public long getHash() {
        return hash;
    }

    @NotNull
    @Override
    public String toString() {
        return Arrays.toString(coordinates) /*+" formulae: "+formulae.toString()*/;
    }

    public String fullString() {
        return Arrays.toString(coordinates) +" formulae: "+formulae.toString();
    }

    public int getOwner() {
        return owner;
    }

    public void setOwner(int owner) {
        this.owner = owner;
    }
}
