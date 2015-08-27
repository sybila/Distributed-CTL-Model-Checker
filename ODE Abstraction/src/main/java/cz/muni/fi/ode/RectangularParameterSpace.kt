package cz.muni.fi.ode

import java.util.Arrays
import java.util.HashSet

//We need our own interval, because we do not allow single points (i.e. (3,3)) as valid intervals
//rest is copied from original Kotlin range
data class Interval(val start: Double, val end: Double) {

    fun isEmpty(): Boolean = start >= end

    fun encloses(other: Interval): Boolean =
            (other == this /*Handles empty ranges*/) || (other.start >= this.start && other.end <= this.end)

    fun clojure(other: Interval): Interval = when {
        this.isEmpty() -> other
        other.isEmpty() -> this
        else -> Interval(Math.min(this.start, other.start), Math.max(this.end, other.end))
    }

    fun intersect(other: Interval): Interval = when {
        this.isEmpty() || other.isEmpty() -> Interval.EMPTY
        else -> Interval(Math.max(this.start, other.start), Math.min(this.end, other.end))
    }

    override fun equals(other: Any?): Boolean =
            other is Interval && (isEmpty() && other.isEmpty() ||
                    java.lang.Double.compare(start, other.start) == 0 && java.lang.Double.compare(end, other.end) == 0)

    override fun hashCode(): Int {
        if (isEmpty()) return -1
        var temp = java.lang.Double.doubleToLongBits(start)
        val result = (temp xor (temp ushr 32))
        temp = java.lang.Double.doubleToLongBits(end)
        return (31 * result + (temp xor (temp ushr 32))).toInt()
    }

    companion object {
        /** An empty range of values of type Double. */
        public val EMPTY: Interval = Interval(0.0, 0.0)
    }
}

data class Rect(val ranges: Array<Interval>) {

    companion object {
        fun empty(dim: Int): Rect = Rect(Array(dim, { Interval.EMPTY }))
    }

    fun intersect(other: Rect): Rect =
        Rect(Array(ranges.size()) { i ->
            this[i] intersect other[i]
        })

    fun isEmpty(): Boolean = ranges.any { it.isEmpty() }

    fun encloses(other: Rect): Boolean {
        for (i in ranges.indices) {
            if (!(this[i] encloses other[i])) return false
        }
        return true
    }

    fun merge(other: Rect): Rect? {
        if (this.isEmpty()) return other
        if (other.isEmpty()) return this
        var mergeOn = -1
        for (i in ranges.indices) {
            if (this[i] != other[i]) {
                if (mergeOn == -1 && !(this[i] intersect other[i]).isEmpty()) {
                    mergeOn = i
                } else {
                    return null
                    //more than one range is different, or merge dimension
                    //does not share an intersection -> we cannot merge this
                }
            }
        }
        if (mergeOn == -1) return this  //rectangles are equal
        return Rect(Array(ranges.size()) { i ->
            if (i == mergeOn) this[i] clojure other[i]
            else this[i]
        })
    }

    fun subtract(other: Rect): Set<Rect> {
        if (this.isEmpty() || other.isEmpty()) return setOf(this)
        val working = Arrays.copyOf(ranges, ranges.size())
        val result = HashSet<Rect>()
        for (dim in ranges.indices) {
            fun add(it: Interval) {
                if (!it.isEmpty()) {
                    result.add(Rect(Array(ranges.size()) { i ->
                        if (i == dim) it
                        else working[i]
                    }))
                }
            }
            add(Interval(this[dim].start, other[dim].start))
            add(Interval(other[dim].end, this[dim].end))
            working[dim] = this[dim] intersect other[dim]
        }
        return result
    }

    fun get(i: Int): Interval = ranges[i]
}