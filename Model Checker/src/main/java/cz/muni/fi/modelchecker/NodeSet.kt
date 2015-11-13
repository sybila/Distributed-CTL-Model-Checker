package cz.muni.fi.modelchecker

/**
 * Although kotlin provides a map with default value, this explicit data
 * structure enables us to check for it's presence at compile time and
 * has more convenient semantics
 */

public fun <N: Node, C: Colors<C>> nodeSetOf(default: C, vararg pairs: Pair<N, C>): NodeSet<N, C>
        = NodeSet(pairs.toMap({it.first}, {it.second}), default)

public open class NodeSet<N: Node, C: Colors<C>>(
        private val map: Map<N, C>,
        public val default: C
) : Map<N, C> by map {

    fun getOrDefault(key: N): C = getOrElse(key, { default })

    override fun equals(other: Any?): Boolean {
        if (other is NodeSet<*, *>) {
            return this.default == other.default && this.map == other.map
        } else return false
    }

    override fun hashCode(): Int {
        return this.map.hashCode()
    }

    override fun toString(): String {
        return "NodeSet(default=$default, values=$map)"
    }
}

public class MutableNodeSet<N: Node, C: Colors<C>>(
        map: MutableMap<N, C>,
        default: C
) : NodeSet<N, C>(map, default), MutableMap<N, C> by map {

    /** True if structure has changed **/
    fun putOrUnion(key: N, value: C): Boolean {
        val old = getOrDefault(key)
        val new = old union value
        if (new.isNotEmpty()) put(key, new)
        return old != new
    }

}

public fun <N: Node, C: Colors<C>> Map<N, C>.toNodeSet(value: C): NodeSet<N, C> = NodeSet(this, value)
public fun <N: Node, C: Colors<C>> MutableNodeSet<N, C>.toNodeSet(value: C): NodeSet<N, C> = NodeSet(this, value)
public fun <N: Node, C: Colors<C>> MutableMap<N, C>.toMutableNodeSet(value: C): MutableNodeSet<N, C> = MutableNodeSet(this, value)

public infix fun <N: Node, C: Colors<C>> NodeSet<N, C>.union(map: NodeSet<N, C>): NodeSet<N, C> {
    val newMap = this.toLinkedMap()
    for ((key, value) in map) {
        newMap[key] = getOrDefault(key) union value
    }
    return newMap.toNodeSet(default)
}

public infix fun <N: Node, C: Colors<C>> NodeSet<N, C>.subtract(map: NodeSet<N, C>): NodeSet<N, C> {
    val newMap = this.toLinkedMap()
    for ((key, value) in map) {
        newMap[key] = getOrDefault(key) subtract value
    }
    return newMap.filterValues { it.isNotEmpty() }.toNodeSet(default)
}

public infix fun <N: Node, C: Colors<C>> NodeSet<N, C>.intersect(map: NodeSet<N, C>): NodeSet<N, C> {
    val newMap = this.toLinkedMap()
    for ((key, value) in map) {
        newMap[key] = getOrDefault(key) intersect value
    }
    return newMap.filterValues { it.isNotEmpty() }.toNodeSet(default)
}