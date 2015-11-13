package cz.muni.fi.modelchecker

import cz.muni.fi.ctl.Atom
import java.util.*

public data class IDNode(
        public val id: Int
) : Node

public data class IDColors(private val set: Set<Int> = HashSet()) : Colors<IDColors> {

    constructor(vararg items: Int): this(items.toSet())

    override fun minus(other: IDColors): IDColors = IDColors(set - other.set)

    override fun plus(other: IDColors): IDColors = IDColors(set + other.set)

    override fun isEmpty(): Boolean = set.isEmpty()

    override fun intersect(other: IDColors): IDColors = IDColors(set.intersect(other.set))

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
        if (mapping.size != directMapping.size + inverseMapping.map { it.value.size }.sum()) {
            throw IllegalArgumentException("Provided mapping is not a function! $directMapping  $inverseMapping")
        }
    }

    override val ownerId: N.() -> Int = { mapping[this]!! }
    override val myId: Int = id
}

public class FunctionalPartitionFunction(
        private val id: Int,
        private val function: (IDNode) -> Int
) : PartitionFunction<IDNode> {
    override val ownerId: IDNode.() -> Int = {function(this)}
    override val myId: Int = id
}

public class UniformPartitionFunction<N: Node>(private val id: Int = 0) : PartitionFunction<N> {
    override val ownerId: N.() -> Int = { id }
    override val myId: Int = id
}

public class ExplicitKripkeFragment(
        nodes: Map<IDNode, IDColors>,
        edges: Set<Edge<IDNode, IDColors>>,
        validity: Map<Atom, Map<IDNode, IDColors>>
) : KripkeFragment<IDNode, IDColors> {

    private val emptyNodeSet = NodeSet(mapOf<IDNode, IDColors>(), IDColors())

    private val successorMap = edges
            .groupBy { it.start }
            .mapValues { entry ->
                entry.value.toMap({ it.end }, { it.colors }).toNodeSet(IDColors())
            }.withDefault { emptyNodeSet }

    private val predecessorMap = edges
            .groupBy { it.end }
            .mapValues { entry ->
                entry.value.toMap({ it.start }, { it.colors }).toNodeSet(IDColors())
            }.withDefault { emptyNodeSet }

    private val nodes = nodes.toNodeSet(IDColors())

    private val validity = validity
            .mapValues { it.value.toNodeSet(IDColors()) }
            .withDefault { emptyNodeSet }

    override val successors: IDNode.() -> NodeSet<IDNode, IDColors> = { successorMap.getOrImplicitDefault(this) }

    override val predecessors: IDNode.() -> NodeSet<IDNode, IDColors> = { predecessorMap.getOrImplicitDefault(this) }

    init {
        for (valid in validity.values) {  //Invariant 1.
            for ((node, colors) in valid) {
                if (this.nodes.getOrDefault(node) intersect colors != colors) {
                    throw IllegalArgumentException("Suspicious atom color in $node for $colors")
                }
            }
        }
        for (node in this.nodes.keys) { //Invariant 2.
            if (node.successors().isEmpty()) {
                throw IllegalArgumentException("Missing successors for $node")
            }
        }
        for ((from, to, colors) in edges) {
            if (from !in allNodes() && to !in allNodes()) {
                throw IllegalArgumentException("Invalid edge $from $to $colors")
            }
        }
    }

    override fun allNodes(): NodeSet<IDNode, IDColors> = nodes

    override fun validNodes(a: Atom): NodeSet<IDNode, IDColors> = validity.getOrImplicitDefault(a)

}
