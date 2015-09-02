package cz.muni.fi.ode;


import cz.muni.fi.ctl.Formula;
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
    private final Map<Formula, RectParamSpace> formulae = new HashMap<>();

    private Map<CoordinateNode, RectParamSpace> predecessors;

    public CoordinateNode(@NotNull int[] coordinates, long hash) {
        this.coordinates = new int[coordinates.length];
        System.arraycopy(coordinates, 0, this.coordinates, 0, coordinates.length);
        this.hash = hash;
    }

    public synchronized boolean hasPredecessorsFor() {
        return predecessors != null;
    }

    public synchronized void savePredecessors(@NotNull Map<CoordinateNode, RectParamSpace> predecessors) {
        this.predecessors = new HashMap<>();
        this.predecessors.putAll(predecessors);
    }

    @NotNull
    public synchronized Map<CoordinateNode, RectParamSpace> getPredecessors(RectParamSpace borders) {
        @NotNull Map<CoordinateNode, RectParamSpace> results = new HashMap<>();
        for (@NotNull Map.Entry<CoordinateNode, RectParamSpace> entry : predecessors.entrySet()) {
            @NotNull RectParamSpace colorSet = new RectParamSpace(entry.getValue().getItems());
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

    public synchronized RectParamSpace getValidColors(Formula formula) {
        return formulae.get(formula);
    }

    public synchronized boolean addFormula(Formula formula, @NotNull RectParamSpace colors) {
        if (colors.isEmpty()) return false;
        RectParamSpace colorSet = formulae.get(formula);
        if (colorSet == null) {
            formulae.put(formula, new RectParamSpace(colors.getItems()));
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
