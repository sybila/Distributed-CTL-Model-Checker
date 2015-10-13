package cz.muni.fi.modelchecker

import cz.muni.fi.ctl.FloatOp
import cz.muni.fi.ctl.FloatProposition
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExplicitKripkeFragmentTest {

    val n1 = IDNode(0)
    val n2 = IDNode(1)
    val n3 = IDNode(2)
    val n4 = IDNode(3)
    val n5 = IDNode(4)

    @Test fun invalidStructureTest() {

        assertFailsWith(IllegalArgumentException::class) {
            ExplicitKripkeFragment(setOf(), setOf(Edge(n1, n1, IDColors(1))), mapOf())
        }

        assertFailsWith(IllegalArgumentException::class) {
            ExplicitKripkeFragment(setOf(), setOf(),
                    mapOf(Pair(FloatProposition("foo", FloatOp.EQ, 1.0), mapOf(Pair(n1, IDColors(2)))))
            )
        }

    }

    @Test fun edgeTest() {

        val ks = ExplicitKripkeFragment(
                setOf(n1, n2, n3, n4, n5),
                setOf(
                        Edge(n1, n2, IDColors(2)),
                        Edge(n1, n4, IDColors(2,4)),
                        Edge(n1, n3, IDColors(2)),
                        Edge(n2, n2, IDColors(3,4)),
                        Edge(n3, n1, IDColors(1,2)),
                        Edge(n4, n3, IDColors(1,2,3,4,5)),
                        Edge(n3, n5, IDColors(2,3))
                ), mapOf())

        assertEquals(mapOf(
                Pair(n2, IDColors(2)), Pair(n4, IDColors(2,4)), Pair(n3, IDColors(2))
        ), n1.run(ks.successors))

        assertEquals(mapOf(Pair(n2, IDColors(3,4))), n2.run(ks.successors))

        assertEquals(mapOf(Pair(n1, IDColors(1,2)), Pair(n5, IDColors(2,3))), n3.run(ks.successors))

        assertEquals(mapOf(Pair(n3, IDColors(1,2,3,4,5))), n4.run(ks.successors))

        assertEquals(mapOf<IDNode, IDColors>(), n5.run(ks.successors))

        assertEquals(mapOf(Pair(n3, IDColors(1,2))), n1.run(ks.predecessors))

        assertEquals(mapOf(Pair(n1, IDColors(2)), Pair(n2, IDColors(3,4))), n2.run(ks.predecessors))

        assertEquals(mapOf(Pair(n4, IDColors(1,2,3,4,5)), Pair(n1, IDColors(2))), n3.run(ks.predecessors))

        assertEquals(mapOf(Pair(n1, IDColors(2,4))), n4.run(ks.predecessors))

        assertEquals(mapOf(Pair(n3, IDColors(2,3))), n5.run(ks.predecessors))

    }

    @Test fun allNodesTest() {
        val ks = ExplicitKripkeFragment(setOf(n1, n2, n4), setOf(), mapOf())
        assertEquals(setOf(n1, n2, n4), ks.allNodes())
    }

    @Test fun validNodesTest() {
        val ks = ExplicitKripkeFragment(
                setOf(n1, n3, n5), setOf(), mapOf(
                    Pair(
                            FloatProposition("name", FloatOp.EQ, 3.14),
                            listOf(n1).map { Pair(it, IDColors(1,2)) }.toMap()
                    ),
                    Pair(
                            FloatProposition("other", FloatOp.GT_EQ, 2.2),
                            listOf(n1, n5).map { Pair(it, IDColors(2,3)) }.toMap()
                    )
        ))

        assertEquals(listOf(n1).map { Pair(it, IDColors(1,2)) }.toMap(), ks.validNodes(FloatProposition("name", FloatOp.EQ, 3.14)))
        assertEquals(listOf(n1, n5).map { Pair(it, IDColors(2,3)) }.toMap(), ks.validNodes(FloatProposition("other", FloatOp.GT_EQ, 2.2)))
    }


}

class ExplicitPartitionFunctionTest {

    val n1 = IDNode(0)
    val n2 = IDNode(1)
    val n3 = IDNode(2)
    val n4 = IDNode(3)
    val n5 = IDNode(4)

    @Test fun directMappingTest() {

        val function = ExplicitPartitionFunction(
                0,
                directMapping = mapOf(
                        Pair(n1, 0),
                        Pair(n2, 1),
                        Pair(n3, 0),
                        Pair(n4, 2),
                        Pair(n5, 1)
                )
        )

        assertEquals(0, n1.run(function.ownerId))
        assertEquals(0, n3.run(function.ownerId))
        assertEquals(1, n2.run(function.ownerId))
        assertEquals(1, n5.run(function.ownerId))
        assertEquals(2, n4.run(function.ownerId))

    }

    @Test fun inverseMappingTest() {

        val function = ExplicitPartitionFunction(
                0,
                inverseMapping = mapOf(
                        Pair(0, listOf(n1, n3)),
                        Pair(1, listOf(n2, n4)),
                        Pair(2, listOf(n5))
                )
        )

        assertEquals(0, n1.run(function.ownerId))
        assertEquals(0, n3.run(function.ownerId))
        assertEquals(1, n2.run(function.ownerId))
        assertEquals(1, n4.run(function.ownerId))
        assertEquals(2, n5.run(function.ownerId))

    }

    @Test fun invalidMappingTest() {

        assertFailsWith(IllegalArgumentException::class) {
            ExplicitPartitionFunction(
                    0,
                    inverseMapping = mapOf(
                            Pair(0, listOf(n1, n3)),
                            Pair(1, listOf(n2, n4)),
                            Pair(2, listOf(n5, n1))
                    )
            )
        }

        val function = ExplicitPartitionFunction(0, mapOf())
        assertFailsWith(NullPointerException::class) {
            n1.run(function.ownerId)
        }
    }
}

class IDColorSpaceTest {

    @Test fun illegalSpaceTest() {
        assertFailsWith(IllegalArgumentException::class) {
            IDColorSpace(-1)
        }
    }

    @Test fun fullSpaceTest() {
        val space1 = IDColorSpace(0)
        assertEquals(IDColors(0), space1.fullColors)

        val space2 = IDColorSpace(10)
        assertEquals(IDColors(0,1,2,3,4,5,6,7,8,9,10), space2.fullColors)
    }

    @Test fun emptySpaceTest() {
        val space1 = IDColorSpace(0)
        assertEquals(IDColors(), space1.emptyColors)

        val space2 = IDColorSpace(10)
        assertEquals(IDColors(), space2.emptyColors)
    }

    @Test fun invertSpaceTest() {
        val space1 = IDColorSpace(0)
        val space2 = IDColorSpace(5)
        assertEquals(IDColors(), IDColors(0).run(space1.invert))
        assertEquals(IDColors(), IDColors(0, 1).run(space1.invert))
        assertEquals(IDColors(0), IDColors().run(space1.invert))
        assertEquals(IDColors(0), IDColors(1).run(space1.invert))

        assertEquals(IDColors(), IDColors(0,1,2,3,4,5).run(space2.invert))
        assertEquals(IDColors(0, 3), IDColors(1,2,4,5).run(space2.invert))
        assertEquals(IDColors(0,1,2,3,4,5), IDColors().run(space2.invert))
        assertEquals(IDColors(1,2,4), IDColors(0,3,5,6).run(space2.invert))
    }


}

class IDColorsTest {

    @Test fun intersectTest() {
        assertEquals(IDColors(), IDColors(1,2,3) intersect IDColors())
        assertEquals(IDColors(), IDColors() intersect IDColors(1,2,3))
        assertEquals(IDColors(2), IDColors(1,2) intersect IDColors(2,3))
    }

    @Test fun plusTest() {
        assertEquals(IDColors(1,2,3), IDColors(1,2,3) + IDColors())
        assertEquals(IDColors(1,2,3), IDColors() + IDColors(1,2,3))
        assertEquals(IDColors(), IDColors() + IDColors())
        assertEquals(IDColors(1,2), IDColors(1,2) + IDColors(1,2))
        assertEquals(IDColors(1,2,3), IDColors(1,2) + IDColors(2,3))
        assertEquals(IDColors(2,3,5,6), IDColors(2,5) + IDColors(3,6))
    }

    @Test fun minusTest() {
        assertEquals(IDColors(1,2,3), IDColors(1,2,3) - IDColors())
        assertEquals(IDColors(), IDColors() - IDColors(1,2,3))
        assertEquals(IDColors(), IDColors(1,2,3) - IDColors(1,2,3))
        assertEquals(IDColors(1,2), IDColors(1,2,3) - IDColors(3,4))
    }

}