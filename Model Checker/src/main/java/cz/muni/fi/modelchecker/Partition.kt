package cz.muni.fi.modelchecker

import cz.muni.fi.ctl.Atom


public interface Node { }

public interface Colors<C> {

    fun intersect(other: C): C

    operator fun minus(other: C): C

    operator fun plus(other: C): C

    fun isEmpty(): Boolean

}

public interface ColorSpace<C: Colors<C>> {

    /**
     * Invert given color set with respect to the model bounds.
     */
    val invert: C.() -> C

    /**
     * Set of all colors of the model.
     */
    val fullColors: C

    /**
     * Empty set of colors.
     */
    val emptyColors: C

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

public interface KripkeFragment<N: Node, C: Colors<C>> {

    /**
     * Find all successors for given node. (Even in different partitions)
     */
    val successors: N.() -> Map<N, C>

    /**
     * Find all predecessors for given node. (Even in different partitions)
     */
    val predecessors: N.() -> Map<N, C>

    /**
     * Set of all (inner) nodes of the partition
     */
    fun allNodes(): Set<N>

    /**
     * Find all nodes (and respective colors) where given atomic proposition holds in this partition.
     */
    fun validNodes(a: Atom): Map<N, C>

}

public data class Edge<N: Node, C: Colors<C>>(
        public val start: N,
        public val end: N,
        public val colors: C
)
