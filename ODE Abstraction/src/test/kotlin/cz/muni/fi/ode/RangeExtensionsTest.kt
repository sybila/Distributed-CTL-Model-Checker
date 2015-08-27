package cz.muni.fi.ode

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RangeIntersectionTest {

    @Test
    fun emptyEmpty() {
        assertTrue((Interval.EMPTY intersect Interval.EMPTY).isEmpty())
        assertTrue((Interval.EMPTY intersect Interval(12.4, -3.2)).isEmpty())
        assertTrue((Interval(-1.0, -2.0) intersect Interval(12.4, -3.2)).isEmpty())
    }

    @Test
    fun oneEmpty() {
        assertTrue((Interval(0.0, 1.0) intersect Interval.EMPTY).isEmpty())
        assertTrue((Interval(-3.2, -4.4) intersect Interval(-5.14, -3.3)).isEmpty())
    }

    @Test
    fun identity() {
        val r1 = Interval(0.0, 1.0)
        assertEquals(r1, r1 intersect r1)
        val r2 = Interval(-4.3, 13.2)
        assertEquals(r2, r2 intersect r2)
    }

    @Test
    fun complex() {
        val r1 = Interval(-3.2, 4.5)
        val r2 = Interval(3.3, 6.7)
        val r3 = Interval(1.2, 3.8)
        assertEquals(Interval(3.3, 4.5), r1 intersect r2)
        assertEquals(Interval(3.3, 3.8), r2 intersect r3)
        assertEquals(r3, r1 intersect r3)
    }

}

class RangeClojureTest {

    @Test
    fun emptyEmpty() {
        assertTrue((Interval.EMPTY clojure Interval.EMPTY).isEmpty())
        assertTrue((Interval.EMPTY clojure Interval(12.4, -3.2)).isEmpty())
        assertTrue((Interval(-1.0, -2.0) clojure Interval(12.4, -3.2)).isEmpty())
    }

    @Test
    fun oneEmpty() {
        assertEquals(Interval(0.0, 1.0), Interval(0.0, 1.0) clojure Interval.EMPTY)
        assertEquals(Interval(-5.14, -3.3), Interval(-3.2, -4.4) clojure Interval(-5.14, -3.3))
    }

    @Test
    fun identity() {
        val r1 = Interval(0.0, 1.0)
        assertEquals(r1, r1 clojure r1)
        val r2 = Interval(-4.3, 13.2)
        assertEquals(r2, r2 clojure r2)
    }

    @Test
    fun complex() {
        val r1 = Interval(-3.2, 4.5)
        val r2 = Interval(3.3, 6.7)
        val r3 = Interval(1.2, 3.8)
        assertEquals(Interval(-3.2, 6.7), r1 clojure r2)
        assertEquals(Interval(1.2, 6.7), r2 clojure r3)
        assertEquals(r1, r1 clojure r3)
    }

}

class RangeEnclosesTest {

    @Test
    fun emptyEmpty() {
        assertTrue(Interval.EMPTY encloses Interval.EMPTY)
        assertTrue(Interval.EMPTY encloses Interval(12.4, -3.2))
        assertTrue(Interval(-1.0, -2.0) encloses Interval(12.4, -3.2))
    }

    @Test
    fun oneEmpty() {
        assertTrue(Interval(0.0, 1.0) encloses Interval.EMPTY)
        assertTrue(Interval(-5.14, -3.3) encloses Interval(-3.2, -4.4))
        assertFalse(Interval.EMPTY encloses Interval(0.0, 1.0))
        assertFalse(Interval(-3.2, -4.4) encloses Interval(-5.14, -3.3))
    }

    @Test
    fun identity() {
        val r1 = Interval(0.0, 1.0)
        assertTrue(r1 encloses r1)
        val r2 = Interval(-4.3, 13.2)
        assertTrue(r2 encloses r2)
    }

    @Test
    fun complex() {
        val r1 = Interval(-3.2, 4.5)
        val r2 = Interval(3.3, 6.7)
        val r3 = Interval(1.2, 3.8)
        assertFalse(r1 encloses r2)
        assertFalse(r2 encloses r1)
        assertFalse(r2 encloses r3)
        assertFalse(r3 encloses r2)
        assertTrue(r1 encloses r3)
        assertFalse(r3 encloses r1)
    }

}