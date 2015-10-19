package cz.muni.fi.modelchecker

import cz.muni.fi.ctl.Atom

/**
 * Represents a Node
 */
public interface Node { }

/**
 * Represents a set of colors (parameters) of the model.
 * All operations should behave as normal mathematical set.
 */
public interface Colors<C> {

    fun intersect(other: C): C

    operator fun minus(other: C): C

    operator fun plus(other: C): C

    fun isEmpty(): Boolean

    fun isNotEmpty(): Boolean = !isEmpty()

    fun union(other: C): C = this + other

    fun subtract(other: C): C = this - other

}

public interface PartitionFunction<N: Node> {

    /**
     * Get ID of partition which owns given node.
     */
    val ownerId: N.() -> Int

    /**
     * My partition ID.
     */
    val myId: Int

}


/**
 * Note: Several invariants should hold for each valid Kripke fragment
 * 1. validNodes should always return subset of allNodes (with respect to colors).
 * 2. for every node N and color C from allNodes, successors(N) should be non empty.
 * 3. Border states set is a union of all successors/predecessors minus all nodes (with respect to colors).
 * 4. If N is successor of M for colors C, M is predecessor of N for colors C.
 * 5. The default value in all node sets is always an empty color space.
 */
public interface KripkeFragment<N: Node, C: Colors<C>> {

    /**
     * Find all successors of given node. (Even in different fragments)
     */
    val successors: N.() -> NodeSet<N, C>

    /**
     * Find all predecessors of given node. (Even in different fragments)
     */
    val predecessors: N.() -> NodeSet<N, C>

    /**
     * Map of all (non border) nodes of the fragment with colors for which they are valid
     */
    fun allNodes(): NodeSet<N, C>

    /**
     * Find all nodes (and respective colors) where given atomic proposition holds in this fragment.
     */
    fun validNodes(a: Atom): NodeSet<N, C>

}

/**
 * Utility data class
 */
public data class Edge<N: Node, C: Colors<C>>(
        public val start: N,
        public val end: N,
        public val colors: C
)
