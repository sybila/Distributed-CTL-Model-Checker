package cz.muni.fi.modelchecker

import cz.muni.fi.ctl.Atom
import cz.muni.fi.ctl.Formula
import cz.muni.fi.ctl.Op
import cz.muni.fi.modelchecker.mpi.termination.Terminator
import mpjbuf.IllegalArgumentException
import java.util.*

public class ModelChecker<N: Node, C: Colors<C>>(
        partition: Partition<N, C>,
        private val terminators: Terminator.TerminatorFactory,
        private val messengers: Messenger.Factory<N, C>
): Partition<N, C> by partition {

    private val getOrEmpty: Map<N, C>.(N) -> C = { this.getOrElse(it, { emptyColors }) }
    private val getOrFull: Map<N, C>.(N) -> C = { this.getOrElse(it, { fullColors }) }

    private val results: MutableMap<Formula, Map<N, C>> = HashMap()

    public fun verify(f: Formula): Map<N, C> {
        if (f !in results) {
            results[f] = if (f is Atom) {
                validNodes(f)
            } else {
                when (f.operator) {
                    Op.NEGATION -> checkNegation(f)
                    Op.AND -> checkAnd(f)
                    Op.OR -> checkOr(f)
                    else -> throw IllegalArgumentException("Unsupported operator: ${f.operator}")
                }
            }
        }
        return results[f]!!
    }

    private fun checkNegation(f: Formula): Map<N, C> = globalBarrier {
        val original = verify(f[0])
        allNodes()
                .filter { original[it] != fullColors }  //get rid of full colored nodes
                .toMap({ it }, { fullColors - original.getOrEmpty(it) })  //create color diffs
    }

    private fun checkAnd(f: Formula): Map<N, C> = globalBarrier {
        val r1 = verify(f[1])
        verify(f[0]).mapValues { it.value intersect r1.getOrEmpty(it.key) }
    }

    private fun checkOr(f: Formula): Map<N, C> = globalBarrier {
        val r0 = verify(f[0])
        val r1 = verify(f[1])
        (r0.keySet() + r1.keySet()).toMap({ it }, { r0.getOrEmpty(it) + r1.getOrEmpty(it) })
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