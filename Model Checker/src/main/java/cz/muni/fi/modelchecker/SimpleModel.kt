package cz.muni.fi.modelchecker

import cz.muni.fi.ctl.Atom
import java.util.*

public data class IDNode(
        public val id: Long
) : Node

public data class IDColors(private val set: Set<Long> = HashSet()) : Colors<IDColors> {

    constructor(vararg items: Long): this(items.toSet())

    override fun minus(other: IDColors): IDColors = IDColors(set - other.set)

    override fun plus(other: IDColors): IDColors = IDColors(set + other.set)

    override fun isEmpty(): Boolean = set.isEmpty()

    override fun intersect(other: IDColors): IDColors = IDColors(set intersect other.set)

}

public class IDColorSpace(private val maxValue: Long) : ColorSpace<IDColors> {

    init {
        if (maxValue < 0) throw IllegalArgumentException("Color space has to contain at least one item. $maxValue given")
    }

    override val invert: IDColors.() -> IDColors = {
        fullColors - this
    }

    override val fullColors: IDColors = IDColors((0..maxValue).toSet())
    override val emptyColors: IDColors = IDColors()

}

public class ExplicitPartitionFunction<N: Node>(
        private val id: Int,
        directMapping: Map<N, Int> = mapOf(),
        inverseMapping: Map<Int, List<N>> = mapOf()
): PartitionFunction<N> {

    private val mapping =
            directMapping + inverseMapping.flatMap { entry ->
                entry.value.map { Pair(it, entry.key) }
            }.toMap()

    init {
        //check if we preserved size of input - i.e. the input is a valid function
        if (mapping.size() != directMapping.size() + inverseMapping.map { it.value.size() }.sum()) {
            throw IllegalArgumentException("Provided mapping is not a function! $directMapping  $inverseMapping")
        }
    }

    override val ownerId: N.() -> Int = { mapping[this]!! }
    override val myId: Int = id
}

public class ExplicitKripkeFragment(
        private val nodes: Set<IDNode>,
        edges: Set<Edge<IDNode, IDColors>>,
        private val validity: Map<Atom, Map<IDNode, IDColors>>
) : KripkeFragment<IDNode, IDColors> {

    private val succ = edges
            .groupBy { it.start }
            .mapValues { entry ->
                entry.value.toMap({ it.end }, { it.colors })
            }

    private val pred = edges
            .groupBy { it.end }
            .mapValues { entry ->
                entry.value.toMap({ it.start }, { it.colors })
            }

    init {
        val extraEdge = edges.firstOrNull { it.start !in nodes || it.end !in nodes }
        if (extraEdge != null)
            throw IllegalArgumentException("Unknown nodes at the edge $extraEdge")

        if (validity.any { it.value.any { it.getKey() !in nodes } })
            throw IllegalArgumentException("Validity contains unknown nodes")
    }

    override val successors: IDNode.() -> Map<IDNode, IDColors> = { succ[this] ?: mapOf() }

    override val predecessors: IDNode.() -> Map<IDNode, IDColors> = { pred[this] ?: mapOf() }

    override fun allNodes(): Set<IDNode> = nodes

    override fun validNodes(a: Atom): Map<IDNode, IDColors> = validity[a]!!

}
