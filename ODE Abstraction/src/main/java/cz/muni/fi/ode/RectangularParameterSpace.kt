package cz.muni.fi.ode

data class Rect(val ranges: Array<DoubleRange>) {

    constructor(dim: Int): this(Array(dim, { DoubleRange(0.0, 0.0) })) {}

    fun hasIntersection(other: Rect): Boolean =
            if (ranges.size() != other.ranges.size())
                throw IllegalStateException("Incomparable rectangles: $this $other")
            else
                ranges.zip(other.ranges).all {
                    (it.first.start >= it.second.start && it.second.end > it.first.start) ||
                    (it.second.start >= it.first.start && it.first.end > it.second.start)
    }

}