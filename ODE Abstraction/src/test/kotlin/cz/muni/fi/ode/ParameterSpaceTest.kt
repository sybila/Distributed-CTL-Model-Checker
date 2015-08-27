package cz.muni.fi.ode

import org.junit.Test
import kotlin.test.assertEquals

//see https://www.dropbox.com/s/oe2gpotc3ypeib1/IMG_20150827_210551833.jpg?dl=0

class SubtractTest {

    val r1 = RectParamSpace(setOf(
            Rect(arrayOf(
                    Interval(0.0,3.0),
                    Interval(1.0,4.0)
            )),
            Rect(arrayOf(
                    Interval(5.0,8.0),
                    Interval(1.0,4.0)
            )),
            Rect(arrayOf(
                    Interval(9.0,10.5),
                    Interval(1.0,3.0)
            )),
            Rect(arrayOf(
                    Interval(10.0,11.0),
                    Interval(0.0,2.0)
            ))
    ))

    val r2 = RectParamSpace(setOf(
            Rect(arrayOf(
                    Interval(1.0,2.0),
                    Interval(2.0,3.0)
            )),
            Rect(arrayOf(
                    Interval(4.0,6.0),
                    Interval(0.0,2.0)
            )),
            Rect(arrayOf(
                    Interval(7.0,10.5),
                    Interval(2.0,3.0)
            )),
            Rect(arrayOf(
                    Interval(10.0,12.0),
                    Interval(0.0,1.0)
            ))
    ))

    val r1r2 = RectParamSpace(setOf(
            Rect(arrayOf(
                    Interval(0.0,1.0),
                    Interval(1.0,4.0)
            )),
            Rect(arrayOf(
                    Interval(2.0,3.0),
                    Interval(1.0,4.0)
            )),
            Rect(arrayOf(
                    Interval(1.0,2.0),
                    Interval(1.0,2.0)
            )),
            Rect(arrayOf(
                    Interval(1.0,2.0),
                    Interval(3.0,4.0)
            )),
            Rect(arrayOf(
                    Interval(5.0,6.0),
                    Interval(2.0,4.0)
            )),
            Rect(arrayOf(
                    Interval(6.0,7.0),
                    Interval(1.0,4.0)
            )),
            Rect(arrayOf(
                    Interval(7.0,8.0),
                    Interval(1.0,2.0)
            )),
            Rect(arrayOf(
                    Interval(7.0,8.0),
                    Interval(3.0,4.0)
            )),
            Rect(arrayOf(
                    Interval(9.0,11.0),
                    Interval(1.0,2.0)
            ))
    ))

    val r2r1 = RectParamSpace(setOf(
            Rect(arrayOf(
                    Interval(4.0,5.0),
                    Interval(0.0,2.0)
            )),
            Rect(arrayOf(
                    Interval(5.0,6.0),
                    Interval(0.0,1.0)
            )),
            Rect(arrayOf(
                    Interval(8.0,9.0),
                    Interval(2.0,3.0)
            )),
            Rect(arrayOf(
                    Interval(11.0,12.0),
                    Interval(0.0,1.0)
            ))
    ))

    @Test
    fun emptyEmpty() {
        val s = RectParamSpace.empty()
        s subtract RectParamSpace.empty()
        assertEquals(RectParamSpace.empty(), s)
    }

    @Test
    fun oneEmpty() {
        val s1 = RectParamSpace.empty()
        s1 subtract r1
        assertEquals(RectParamSpace.empty(), s1)
        val s2 = r1.copy()
        s2 subtract RectParamSpace.empty()
        assertEquals(r1, s2)
    }

    @Test
    fun identity() {
        val s1 = r1.copy()
        s1 subtract r1
        assertEquals(RectParamSpace.empty(), s1)

        val s2 = r2.copy()
        s2 subtract r2
        assertEquals(RectParamSpace.empty(), s2)
    }

    @Test
    fun complex() {
        val s1 = r1.copy()
        s1 subtract r2
        assertEquals(r1r2, s1)

        val s2 = r2.copy()
        s2 subtract r1
        assertEquals(r2r1, s2)
    }

}

class UnionTest {

    val r1 = RectParamSpace(setOf(
            Rect(arrayOf(
                    Interval(0.0, 3.0),
                    Interval(0.0, 3.0)
            )),
            Rect(arrayOf(
                    Interval(0.0, 3.0),
                    Interval(5.0, 6.0)
            )),
            Rect(arrayOf(
                    Interval(5.0, 8.0),
                    Interval(0.0, 3.0)
            ))
    ))

    val r2 = RectParamSpace(setOf(
            Rect(arrayOf(
                    Interval(1.0, 2.0),
                    Interval(2.0, 3.0)
            )),
            Rect(arrayOf(
                    Interval(4.0, 6.0),
                    Interval(1.0, 4.0)
            )),
            Rect(arrayOf(
                    Interval(7.0, 10.0),
                    Interval(0.0, 3.0)
            )),
            Rect(arrayOf(
                    Interval(4.0, 6.0),
                    Interval(8.0, 10.0)
            ))
    ))

    val result = RectParamSpace(setOf(
            Rect(arrayOf(
                    Interval(4.0, 6.0),
                    Interval(8.0, 10.0)
            )),
            Rect(arrayOf(
                    Interval(0.0, 3.0),
                    Interval(5.0, 6.0)
            )),
            Rect(arrayOf(
                    Interval(0.0, 3.0),
                    Interval(0.0, 3.0)
            )),
            Rect(arrayOf(
                    Interval(4.0, 6.0),
                    Interval(1.0, 4.0)
            )),
            Rect(arrayOf(
                    Interval(5.0, 10.0),
                    Interval(0.0, 3.0)
            ))
    ))

    @Test
    fun emptyEmpty() {
        val s = RectParamSpace.empty()
        s union RectParamSpace.empty()
        assertEquals(RectParamSpace.empty(), s)
    }

    @Test
    fun oneEmpty() {
        val simple = RectParamSpace(setOf(Rect(arrayOf(Interval(0.1, 0.2)))))
        val s1 = simple.copy()
        s1 union RectParamSpace.empty()
        assertEquals(simple, s1)

        val s2 = RectParamSpace.empty()
        s2 union simple
        assertEquals(simple, s2)
    }

    @Test
    fun identity() {
        val s1 = r1.copy()
        s1 union r1
        assertEquals(r1, s1)

        val s2 = r2.copy()
        s2 union r2
        assertEquals(r2, s2)
    }

    @Test
    fun complex() {
        val s1 = r1.copy()
        s1 union r2
        assertEquals(result, s1)

        val s2 = r2.copy()
        s2 union r1
        assertEquals(result, s2)
    }

}

class IntersectTest {

    val r1 = RectParamSpace(setOf(
            Rect(arrayOf(
                    Interval(0.0,1.0),
                    Interval(0.0,1.0)
            )),
            Rect(arrayOf(
                    Interval(0.0,1.0),
                    Interval(2.0,3.0)
            )),
            Rect(arrayOf(
                    Interval(2.0,7.0),
                    Interval(2.0,3.0)
            )),
            Rect(arrayOf(
                    Interval(3.0,6.0),
                    Interval(0.0,1.0)
            ))
    ))

    val r2 = RectParamSpace(setOf(
            Rect(arrayOf(
                    Interval(0.25,0.75),
                    Interval(0.25,0.75)
            )),
            Rect(arrayOf(
                    Interval(4.0,5.0),
                    Interval(0.5,2.5)
            ))
    ))

    val result = RectParamSpace(setOf(
            Rect(arrayOf(
                    Interval(0.25,0.75),
                    Interval(0.25,0.75)
            )),
            Rect(arrayOf(
                    Interval(4.0,5.0),
                    Interval(0.5,1.0)
            )),
            Rect(arrayOf(
                    Interval(4.0,5.0),
                    Interval(2.0,2.5)
            ))
    ))


    @Test
    fun emptyEmpty() {
        val space = RectParamSpace.empty()
        space intersect RectParamSpace.empty()
        assertEquals(RectParamSpace.empty(), space)
    }

    @Test
    fun oneEmpty() {
        val s1 = RectParamSpace.empty()
        s1 intersect r1

        val s2 = r1.copy()
        s2 intersect RectParamSpace.empty()

        assertEquals(RectParamSpace.empty(), s1)
        assertEquals(RectParamSpace.empty(), s2)
    }

    @Test
    fun identity() {
        val s1 = r1.copy()
        s1 intersect r1

        val s2 = r2.copy()
        s2 intersect r2

        assertEquals(r1, s1)
        assertEquals(r2, s2)
    }

    @Test
    fun complex() {
        val s1 = r1.copy()
        s1 intersect r2

        val s2 = r2.copy()
        s2 intersect r1

        assertEquals(result, s1)
        assertEquals(result, s2)
    }

}