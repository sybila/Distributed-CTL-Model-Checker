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
                Job.EU::class.java as Class<Job.EX<N, C>>  //TODO: There must be a better way to do this
        )

        for ((node, colors) in r) {
            pushBack(node, colors, jobQueue) { s, t, c -> Job.EX(t, c) }
        }

        jobQueue.start { val (node, colors) = it
            synchronized(result) { result.addOrUnion(node, colors) }
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
                Job.EU::class.java as Class<Job.EU<N, C>>  //TODO: There must be a better way to do this
        )

        fun pushBack(node: N, colors: C) = pushBack(node, colors, jobQueue) { s, t, c -> Job.EU(t, c) }

        //prepare queue
        for ((node, colors) in f1) {
            if (result.addOrUnion(node, colors)) pushBack(node, colors)
        }

        jobQueue.start { val (node, colors) = it
            val intersection = colors intersect f0.getOrDefault(node)
            val modified = synchronized(result) { result.addOrUnion(node, intersection) }
            if (modified) pushBack(node, intersection)
        }

        jobQueue.finish()

        return result.toMap()
    }

    private fun checkAllUntil(f: Formula): MapWithDefault<N, C> {

        val f0 = verify(f[0])
        val f1 = verify(f[1])
        val result = HashMap<N, C>().withDefaultMutable(f0.default)
        val uncoveredEdges = HashMap<N, MutableMap<N, C>>()

        val jobQueue = SingleThreadJobQueue(
                messengers, terminators, partitionFunction,
                Job.AU::class.java as Class<Job.AU<N, C>>  //TODO: There must be a better way to do this
        )

        fun pushBack(node: N, colors: C) = pushBack(node, colors, jobQueue) { s, t, c -> Job.AU(s, t, c) }

        for ((node, colors) in f1) pushBack(node, colors)

        jobQueue.start { val (source, node, colors) = it
            synchronized(uncoveredEdges) {
                if (node !in uncoveredEdges) uncoveredEdges[node] = HashMap(node.successors())
            }
            val uncoveredSuccessors = uncoveredEdges[node]!!
            val validColors = synchronized(uncoveredSuccessors) {
                //Would this be reasonably faster if we removed empty sets from map completely?
                uncoveredSuccessors[source] == uncoveredSuccessors[source]!! - colors

                //Or should we cache results of this reduction?
                f0.getOrDefault(node) intersect (colors - uncoveredSuccessors.values().reduce { a, b -> a union b })
            }
            if (validColors.isNotEmpty()) {
                val modified = synchronized(result) { result.addOrUnion(node, validColors) }
                if (modified) pushBack(node, validColors)
            }
        }

        jobQueue.finish()
        return result.toMap()
    }

    private fun <J: Job<N, C>> pushBack(node: N, colors: C, jobQueue: JobQueue<N, C, J>, jobConstructor: (N, N, C) -> J) {
        for ((predecessor, edgeColors) in node.predecessors()) {
            val intersection = edgeColors intersect colors
            if (intersection.isNotEmpty()) jobQueue.post(jobConstructor(node, predecessor, intersection))
        }
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