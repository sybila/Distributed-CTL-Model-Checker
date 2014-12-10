package cz.muni.fi.distributed.graph;


import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.model.ColorSet;
import cz.muni.fi.model.TreeColorSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Node {

    private final Graph graph;
    public final Map<Node, ColorSet> successors = new HashMap<>();
    //public final double[] valuation;
    @NotNull
    public final int[] coordinates;
    @NotNull
    public Map<Formula, ColorSet> formulae = new HashMap<>();

    public Node(@NotNull int[] coordinates, Graph graph) {
        this.graph = graph;
        this.coordinates = Arrays.copyOf(coordinates, coordinates.length);
        //this.valuation = Arrays.copyOf(valuation, valuation.length);
    }

    /*public double getValue(int dimension) {
        return valuation[dimension];
    }*/

    public int getCoordinate(int dimension) {
        return coordinates[dimension];
    }

    public Map<Node, ColorSet> getPredecessors(@Nullable Formula formula) {
        if (formula == null) {
            return graph.factory.computePredecessors(this, graph.model.getFullColorSet());
        } else {
            return graph.factory.computePredecessors(this, getValidColors(formula));
        }
    }

    public Map<Node, ColorSet> getSuccessors(@Nullable Formula formula) {
        if (formula == null) {
            return graph.factory.computeSuccessors(this, graph.model.getFullColorSet());
        } else {
            return graph.factory.computeSuccessors(this, getValidColors(formula));
        }
    }

    public ColorSet getValidColors(Formula formula) {
        if (formulae.containsKey(formula)) {
            return formulae.get(formula);
        } else {
            return TreeColorSet.createEmpty(graph.model.parameterCount());
        }
    }

    public void addFormula(Formula formula, @NotNull ColorSet colors) {
        if (!formulae.containsKey(formula)) {
            formulae.put(formula, TreeColorSet.createCopy(colors));
        } else {
            formulae.get(formula).union(colors);
        }
    }

    public void addSuccessor(Node node, ColorSet colorSet) {
        if (successors.containsKey(node)) {
            successors.get(node).union(colorSet);
        } else {
            successors.put(node, colorSet);
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(coordinates) + 31 * graph.id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Node) {
            @NotNull Node n = (Node) obj;
            return Arrays.equals(coordinates, n.coordinates) && graph.equals(n.graph);
        }
        return false;
    }
}
