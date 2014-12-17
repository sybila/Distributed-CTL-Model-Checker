package cz.muni.fi.ode;


import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CoordinateNode implements Node {

    @NotNull
    public final int[] coordinates;

    @NotNull
    public Map<Formula, TreeColorSet> formulae = new HashMap<>();

    public CoordinateNode(@NotNull int[] coordinates) {
        this.coordinates = Arrays.copyOf(coordinates, coordinates.length);
    }

    public int getCoordinate(int dimension) {
        return coordinates[dimension];
    }

    public TreeColorSet getValidColors(Formula formula) {
        return formulae.get(formula);
    }

    public boolean addFormula(Formula formula, @NotNull TreeColorSet colors) {
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
        return Arrays.hashCode(coordinates);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CoordinateNode) {
            @NotNull CoordinateNode n = (CoordinateNode) obj;
            return Arrays.equals(coordinates, n.coordinates);
        }
        return false;
    }

    @Override
    public String toString() {
        return Arrays.toString(coordinates)+" formulae: "+formulae.toString();
    }
}
