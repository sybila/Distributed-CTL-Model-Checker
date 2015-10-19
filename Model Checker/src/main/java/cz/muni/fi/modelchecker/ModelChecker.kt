package cz.muni.fi.modelchecker

import com.github.daemontus.jafra.Terminator
import cz.muni.fi.ctl.Atom
import cz.muni.fi.ctl.Formula
import cz.muni.fi.ctl.Op
import mpjbuf.IllegalArgumentException
import java.util.*

public inline fun <reified T: Any> genericClass(): Class<T> = T::class.java

public class ModelChecker<N: Node, C: Colors<C>>(
        fragment: KripkeFragment<N, C>,
        private val partitionFunction: PartitionFunction<N>,
        private val terminators: Terminator.Factory,
        private val messengers: Messenger.Factory<N, C>
):
        KripkeFragment<N, C> by fragment,
        PartitionFunction<N> by partitionFunction
{

    private val results: MutableMap<Formula, NodeSet<N, C>> = HashMap()

    public fun verify(f: Formula): NodeSet<N, C> {
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

    private fun checkNegation(f: Formula): NodeSet<N, C> = allNodes() subtract verify(f[0])

    private fun checkAnd(f: Formula): NodeSet<N, C> = verify(f[0]) intersect verify(f[1])

    private fun checkOr(f: Formula): NodeSet<N, C> = verify(f[0]) union verify(f[1])

    private fun checkExistNext(f: Formula): NodeSet<N, C> {

        val phi = verify(f[0])

        val result = HashMap<N, C>().toMutableNodeSet(phi.default)

        val jobQueue = SingleThreadJobQueue(
                messengers, terminators, partitionFunction,
                genericClass<Job.EX<N, C>>()
        )

        //Push colors from all nodes where phi holds - targets are nodes where EX phi holds.
        for ((node, colors) in phi) {
            pushBack(node, colors, jobQueue) { s, t, c -> Job.EX(t, c) }
        }

        //Mark all targets as valid for EX phi
        jobQueue.start { val (node, colors) = it
            synchronized(result) { result.putOrUnion(node, colors) }
        }

        jobQueue.finish()

        return result
    }

    private fun checkExistUntil(f: Formula): NodeSet<N, C> {

        val phi_1 = verify(f[0])
        val phi_2 = verify(f[1])
        val result = HashMap<N, C>().toMutableNodeSet(phi_1.default)

        val jobQueue = SingleThreadJobQueue(
                messengers, terminators, partitionFunction,
                genericClass<Job.EU<N, C>>()
        )

        fun pushBack(node: N, colors: C) = pushBack(node, colors, jobQueue) { s, t, c -> Job.EU(t, c) }

        //Mark all nodes where phi_2 holds as valid and push colors from them
        for ((node, colors) in phi_2) {
            if (result.putOrUnion(node, colors)) pushBack(node, colors)
        }

        //Mark all targets as valid for intersection with phi_1 and continue pushing colors if something changed
        jobQueue.start { val (node, colors) = it
            val intersection = colors intersect phi_1.getOrDefault(node)
            val modified = synchronized(result) { result.putOrUnion(node, intersection) }
            if (modified) pushBack(node, intersection)
        }

        jobQueue.finish()

        return result
    }

    private fun checkAllUntil(f: Formula): NodeSet<N, C> {

        val phi_1 = verify(f[0])
        val phi_2 = verify(f[1])
        val result = HashMap<N, C>().toMutableNodeSet(phi_1.default)

        //Remembers which successors have been covered by explored edges (successors are lazily initialized)
        //Algorithm modifies the contents to satisfy following invariant:
        //uncoveredEdges(x,y) = { c such that there is an edge into y and !(phi_2 or (phi_1 AU phi_2)) holds in y }
        //Note: Maybe we could avoid this if we also allowed results for border states in results map.
        val uncoveredEdges = HashMap<N, MutableMap<N, C>>()

        val jobQueue = SingleThreadJobQueue(
                messengers, terminators, partitionFunction,
                genericClass<Job.AU<N, C>>()
        )

        fun pushBack(node: N, colors: C) = pushBack(node, colors, jobQueue) { s, t, c -> Job.AU(s, t, c) }

        //Push from all nodes where phi_2 holds, but do not mark anything
        for ((node, colors) in phi_2) pushBack(node, colors)

        //Update info about uncovered edges and if some colors become fully covered,
        //mark the target and push them further
        jobQueue.start { val (source, node, colors) = it
            synchronized(uncoveredEdges) {  //Lazy init map with successors
                if (node !in uncoveredEdges) uncoveredEdges[node] = HashMap(node.successors())
            }
            val uncoveredSuccessors = uncoveredEdges[node]!!
            val validColors = synchronized(uncoveredSuccessors) {
                //cover pushed edge
                //Would this be reasonably faster if we removed empty sets from map completely?
                uncoveredSuccessors[source] == uncoveredSuccessors[source]!! - colors
                //Compute what colors became covered by this change
                //Or should we cache results of this reduction?
                phi_1.getOrDefault(node) intersect (colors - uncoveredSuccessors.values().reduce { a, b -> a union b })
            }
            if (validColors.isNotEmpty()) { //if some colors survived all of this, mark them and push further
                val modified = synchronized(result) { result.putOrUnion(node, validColors) }
                if (modified) pushBack(node, validColors)
            }
        }

        jobQueue.finish()
        return result
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