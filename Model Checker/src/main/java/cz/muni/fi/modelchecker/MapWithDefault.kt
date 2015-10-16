package cz.muni.fi.modelchecker

import java.util.*

/**
 * Although kotlin provides a map with default value, this explicit data
 * structure enables us to check for it's presence at compile time
 */

public fun <K: Any, V: Any> Map<K, V>.withDefault(value: V): MapWithDefault<K, V> = MapWithDefault(this, value)

public class MapWithDefault<K: Any, V: Any>(
        private val map: Map<K, V>,
        public val default: V
): Map<K, V> by map {

    fun getOrDefault(key: K): V =
            if (key !in map) default
            else map[key]!!

}

public fun <K: Any, V: Any> MutableMap<K, V>.withDefaultMutable(value: V): MutableMapWithDefault<K, V> = MutableMapWithDefault(this, value)

public class MutableMapWithDefault<K: Any, V: Any>(
        private val map: MutableMap<K, V>,
        public val default: V
): MutableMap<K, V> by map {

    fun getOrDefault(key: K): V =
            if (key !in map) default
            else map[key]!!

    fun toMap(): MapWithDefault<K,V> = MapWithDefault(map, default)

}