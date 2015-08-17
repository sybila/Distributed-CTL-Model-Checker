package cz.muni.fi.modelchecker

import cz.muni.fi.ctl.Formula

/**
 * Represents a set of colors/parameters.
 */
public interface ColorSet<C> {

    fun intersect(other: C): C
    fun subtract(other: C): C
    fun union(other: C): C
    fun isEmpty(): Boolean
    fun isNotEmpty(): Boolean

}

/**
 * Interface representing one node of a distributed graph used for CTL model checking.
 */
public interface Node

/**
 * Performs node actions:
 * Creation, caching, proposition evaluation and predecessors/successors computation.
 * WARNING: Every model adapter is responsible for it's own synchronization. All methods can (and will)
 * be accessed from multiple threads during computation, often concurrently.
 */
public interface Model<N : Node, C: ColorSet<C>> {

    val emptyColorSet: C
    val fullColorSet: C

    /**
     * Compute successors for provided node with respect to provided parametric bounds.
     * @param from Source node.
     * *
     * @param borders Parameter bounds.
     * *
     * @return Successors with colors that are valid along the paths to them.
     */
    val successors: N.(limit: C = fullColorSet) -> Map<N, C>

    /**
     * Compute predecessors for provided node with respect to provided parametric bounds.
     * @param to Destination node.
     * *
     * @param borders Parameter bounds.
     * *
     * @return Predecessors with colors that are valid along the paths from them.
     */
    val predecessors: N.(limit: C = fullColorSet) -> Map<N, C>

    /**
     * Set formula on given node as valid for given parameter set.
     * @param node Node
     * *
     * @param formula A valid formula.
     * *
     * @param parameters Parameter set where formula is valid.
     * *
     * @return True if any actual change has been made, false otherwise.
     */
    val saveFormula: N.(formula: Formula, colours: C) -> Unit

    /**
     *
     * Query node for parameters where given formula holds.
     *
     * If given formula is a proposition, results can be computed on the fly or retrieved from cache.
     *
     * If given formula is not a proposition, results should match data provided in previous addFormula calls.
     * @param node Node
     * *
     * @param formula Requested formula
     * *
     * @return Set of colors where given formula holds on specified node.
     */
    val validColours: N.(formula: Formula) -> C

    /**
     * Find all nodes where formula is valid and for which colors.
     * @param formula Some formula.
     * *
     * @return Nodes where formula holds with respective colors.
     */
    val initialNodes: Formula.() -> Map<N, C>

    fun allNodes(): Set<N>
}

/**
 * Class used to partition provided state space into separate sub graphs that can be processed on different machines.
 */
public interface StatePartitioner<N : Node> {

    /**
     * Computes the id of a sub graph where provided node is located.
     * @param node some node of a graph
     * *
     * @return ID of sub graph that holds provided node
     */
    val owner: Node.() -> Int

    /**
     * @return Id of my sub graph (so that I can tell whether a node is mine)
     */
    val myId: Int

}

