package cz.muni.fi.modelchecker

import com.github.daemontus.jafra.Terminator
import cz.muni.fi.ctl.Atom
import cz.muni.fi.ctl.Formula
import cz.muni.fi.ctl.Op
import mpjbuf.IllegalArgumentException
import java.util.*

public class ModelChecker<N: Node, C: Colors<C>>(
        fragment: KripkeFragment<N, C>,
        private val partitionFunction: PartitionFunction<N>,
        private val terminators: Terminator.Factory,
        private val messengers: Messenger.Factory<N, C>
):
        KripkeFragment<N, C> by fragment,
        PartitionFunction<N> by partitionFunction
{

    private val results: MutableMap<Formula, MapWithDefault<N, C>> = HashMap()

    public fun verify(f: Formula): MapWithDefault<N, C> {
        if (f !in results) {
            results[f] = if (f is Atom) {
                validNodes(f)
            } else {
                when (f.operator) {
                    Op.NEGATION -> checkNegation(f)
                    Op.AND -> checkAnd(f)
                    Op.OR -> checkOr(f)
                    Op.EXISTS_NEXT -> checkExistNext(f)
                    Op.EXISTS_UNTIL -> checkExistUntil(f)
                    else -> throw IllegalArgumentException("Unsupported operator: ${f.operator}")
                }
            }
        }
        return results[f]!!
    }

    private fun checkNegation(f: Formula): MapWithDefault<N, C> = allNodes() subtract verify(f[0])

    private fun checkAnd(f: Formula): MapWithDefault<N, C> = verify(f[0]) intersect verify(f[1])

    private fun checkOr(f: Formula): MapWithDefault<N, C> = verify(f[0]) union verify(f[1])

    private fun checkExistNext(f: Formula): MapWithDefault<N, C> {

        val r = verify(f[0])

        val result = HashMap<N, C>().withDefaultMutable(r.default)

        val jobQueue = SingleThreadJobQueue(
                messengers, terminators, partitionFunction,
                Job.EX(r.keySet().first(), r.values().first()).javaClass  //TODO: There must be a better way to do this
        )

        for ((node, colors) in r) {
            for ((predecessor, edgeColors) in node.predecessors()) {
                val intersection = colors intersect edgeColors
                if (intersection.isNotEmpty()) jobQueue.post(Job.EX(predecessor, intersection))
            }
        }

        jobQueue.start { val (node, colors) = it
            result.addOrUnion(node, colors)
        }

        jobQueue.finish()

        return result.toMap()
    }

    private fun checkExistUntil(f: Formula): MapWithDefault<N, C> {

        val f0 = verify(f[0])
        val f1 = verify(f[1])
        val result = HashMap<N, C>().withDefaultMutable(f0.default)

        val jobQueue = SingleThreadJobQueue(
                messengers, terminators, partitionFunction,
                Job.EU(f1.keySet().first(), f1.values().first()).javaClass  //TODO: There must be a better way to do this
        )

        fun pushBack(node: N, colors: C) {
            for ((predecessor, edgeColors) in node.predecessors()) {    //push to predecessors
                val intersection = edgeColors intersect colors
                if (intersection.isNotEmpty()) jobQueue.post(Job.EU(predecessor, intersection))
            }
        }

        //prepare queue
        for ((node, colors) in f1) {
            if (result.addOrUnion(node, colors)) pushBack(node, colors)
        }

        jobQueue.start { val (node, colors) = it
            val intersection = colors intersect f0.getOrDefault(node)
            if (result.addOrUnion(node, intersection)) pushBack(node, intersection)
        }

        jobQueue.finish()

        return result.toMap()
    }

    private fun checkAllUntil(f: Formula): MapWithDefault<N, C> {

        val f0 = verify(f[0])
        val f1 = verify(f[1])
        val result = HashMap<N, C>().withDefaultMutable(f0.default)


        return result.toMap()
    }

    private fun <T> globalBarrier(f: () -> T): T {
        return GlobalBarrier(terminators.createNew(), f)()
    }
}

class GlobalBarrier<T>(
    private val terminator: Terminator,
    private val action: () -> T
): () -> T {

    override fun invoke(): T =
        try {
            action()
        } finally {
            terminator.setDone()
            terminator.waitForTermination()
        }

}