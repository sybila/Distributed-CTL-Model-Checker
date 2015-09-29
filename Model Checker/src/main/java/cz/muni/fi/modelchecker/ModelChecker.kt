package cz.muni.fi.modelchecker

import com.github.daemontus.jafra.Terminator
import cz.muni.fi.ctl.Atom
import cz.muni.fi.ctl.Formula
import cz.muni.fi.ctl.Op
import mpjbuf.IllegalArgumentException
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

public class ModelChecker<N: Node, C: Colors<C>>(
        partition: Partition<N, C>,
        private val terminators: Terminator.Factory,
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
                    Op.EXISTS_UNTIL -> checkExistUntil(f)
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

    private fun checkExistUntil(f: Formula): Map<N, C> {
        val r0 = verify(f[0])
        val taskQueue = LinkedBlockingQueue<Job<N, C>>()
        val terminator = terminators.createNew()


        val messenger = messengers.createNew {
            synchronized(taskQueue) {   //Can we do better?
                terminator.messageReceived()
                taskQueue.add(it)
            }
        }

        val results = HashMap<N, C>()   //No need for sync, updated only from following thread

        //push given colors from given node into it's predecessors
        val pushBack: (N, C) -> Unit = { node, colors ->
            for ((predecessor, edgeColors) in node.predecessors()) {
                val job = Job.EUTask(predecessor, colors intersect edgeColors)
                if (node.ownerId() == myId) {
                    taskQueue.add(job)
                } else {
                    messenger.sendTask(job)
                    terminator.messageSent()
                }
            }
        }

        //prepare queue
        for ((node, colors) in verify(f[1])) {
            results[node] = colors
            pushBack(node, colors)
        }

        val worker = thread {
            var task = taskQueue.take()
            while (task is Job.EUTask<N, C>) {
                val (node, pushed) = task

                //intersect and save as valid
                val valid = pushed intersect r0.getOrEmpty(node)
                results[node] = results.getOrEmpty(node) + valid

                pushBack(node, valid)

                synchronized(taskQueue) {   //sync receive/done signals with the queue take
                    if (taskQueue.isEmpty()) terminator.setDone()
                    task = taskQueue.take()
                }
            }
            if (task !is Job.Poison) throw IllegalStateException("Somehow a different task got here! $task")
            //just finish - we received poison, everything is fine :)
        }

        terminator.waitForTermination()

        return globalBarrier {
            taskQueue.add(Job.Poison())
            worker.join()
            messenger.close()
            results
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