package cz.muni.fi.modelchecker;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;

import java.util.Map;


/**
 * Manages required node actions.
 * Creation, caching, proposition evaluation and predecessors/successors computation.
 */
public interface ModelAdapter<N extends Node, C extends ColorSet> {

    /**
     * Compute predecessors for provided node with respect to provided parametric bounds.
     * @param to Destination node.
     * @param borders Parameter bounds.
     * @return Predecessors with colors that are valid along the paths from them.
     */
    @NotNull Map<N, C> predecessorsFor(@NotNull N to, @NotNull C borders);

    /**
     * Compute successors for provided node with respect to provided parametric bounds.
     * @param from Source node.
     * @param borders Parameter bounds.
     * @return Successors with colors that are valid along the paths to them.
     */
    @NotNull Map<N, C> successorsFor(@NotNull N from, @NotNull C borders);

    /**
     * Find all nodes where formula is valid and for which colors.
     * @param formula Some formula.
     * @return Nodes where formula holds with respective colors.
     */
    @NotNull Map<N, C> initialNodes(@NotNull Formula formula);

    /**
     * Return the inversion of given node set. This is used mostly for negation computation.
     * Note: Returned color sets should not be empty.
     * @param nodes nodes and colors that should be inverted
     * @return Set of nodes with respective colors that is an inversion to given set.
     */
    @NotNull Map<N, C> invertNodeSet(Map<N, C> nodes);

    /**
     * Set formula on given node as valid for given parameter set.
     * @param node Node
     * @param formula A valid formula.
     * @param parameters Parameter set where formula is valid.
     * @return True if any actual change has been made, false otherwise.
     */
    boolean addFormula(@NotNull N node, @NotNull Formula formula, @NotNull C parameters);

    /**
     * <p>Query node for parameters where given formula holds.</p>
     * <p>If given formula is a proposition, results can be computed on the fly or retrieved from cache.</p>
     * <p>If given formula is not a proposition, results should match data provided in previous addFormula calls.</p>
     * @param node Node
     * @param formula Requested formula
     * @return Set of colors where given formula holds on specified node.
     */
    @NotNull
    C validColorsFor(@NotNull N node, @NotNull Formula formula);

}
