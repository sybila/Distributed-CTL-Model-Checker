package cz.muni.fi.modelchecker

import cz.muni.fi.ctl.Atom
import java.util.*


public interface Node { }

public interface Colors<C> {

    fun intersect(other: C): C

    fun invert(): C

    operator fun minus(other: C): C

    operator fun plus(other: C): C

    fun isEmpty(): Boolean

    fun isNotEmpty(): Boolean = !isEmpty()

    fun union(other: C): C = this + other

    fun subtract(other: C): C = this - other

    fun complement(): C = invert()

}

public fun <N: Node, C: Colors<C>> MapWithDefault<N, C>.union(
        other: MapWithDefault<N, C>
): MapWithDefault<N, C> {
    val result = LinkedHashMap<N, C>(Math.max((size()/.75f).toInt() + 1, 16))
    for (key in this.keySet() + other.keySet()) {
        result[key] = this.getOrDefault(key) + other.getOrDefault(key)
    }
    return result.withDefault(default)
}

public fun <N: Node, C: Colors<C>> MapWithDefault<N, C>.subtract(
        other: MapWithDefault<N, C>
): MapWithDefault<N, C> {
    val result = LinkedHashMap<N, C>(Math.max((size()/.75f).toInt() + 1, 16))
    for ((key, value) in this) {
        val remainder = value - other.getOrDefault(key)
        if (remainder.isNotEmpty()) result[key] = remainder
    }
    return result.withDefault(default)
}

public fun <N: Node, C: Colors<C>> MapWithDefault<N, C>.intersect(
        other: MapWithDefault<N, C>
): MapWithDefault<N, C> {
    val result = LinkedHashMap<N, C>(Math.max((size()/.75f).toInt() + 1, 16))
    for ((key, value) in this) {
        val intersection = value intersect other.getOrDefault(key)
        if (intersection.isNotEmpty()) result[key] = intersection
    }
    return result.withDefault(default)
}

/** Returns true if value in map has changed **/
public fun <N: Node, C: Colors<C>> MutableMapWithDefault<N, C>.addOrUnion(
        node: N, colors: C
): Boolean {
    val oldColors = getOrDefault(node)
    val newColors = oldColors union colors
    if (newColors.isNotEmpty()) put(node, newColors)
    return newColors != oldColors
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
    val successors: N.() -> MapWithDefault<N, C>

    /**
     * Find all predecessors for given node. (Even in different partitions)
     */
    val predecessors: N.() -> MapWithDefault<N, C>

    /**
     * Map of all (inner) nodes of the partition with colors for which they are valid
     */
    fun allNodes(): MapWithDefault<N, C>

    /**
     * Find all nodes (and respective colors) where given atomic proposition holds in this partition.
     */
    fun validNodes(a: Atom): MapWithDefault<N, C>

}

public data class Edge<N: Node, C: Colors<C>>(
        public val start: N,
        public val end: N,
        public val colors: C
)
