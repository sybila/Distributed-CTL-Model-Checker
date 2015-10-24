package cz.muni.fi.modelchecker

import com.github.daemontus.jafra.Terminator
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.properties.Delegates


/**
 * A base class for all types of jobs that can be created during the algorithm processing.
 *
 * The distinction is made in order to provide better flexibility for future changes
 * (SSC detection, etc.) - also semantics of data may vary across algorithms for each operator.
 *
 * Note that messenger you are using must implement suitable serialization algorithms
 * if you want to send Jobs though it.
 */
open public class Job<N: Node, C: Colors<C>>(
        val destination: N
): Message {

    class AU<N: Node, C: Colors<C>>(
            val sourceNode: N,
            val targetNode: N,
            val colors: C
    ) : Job<N, C>(targetNode)

    class EU<N: Node, C: Colors<C>>(
            val node: N,
            val colors: C
    ) : Job<N, C>(node)

    class EX<N: Node, C: Colors<C>>(
            val node: N,
            val colors: C
    ) : Job<N, C>(node)

}


/**
 * Queue of jobs waiting for execution. Do not confuse with message queue.
 * Message queue should accept any message and only throw error's if it can't send it.
 * Jobs are specific to model checking algorithm and every job queue should have it's specific type.
 *
 * Job queue should use some sort of termination detection mechanism to detect when no jobs are
 * being posted in the system.
 *
 * Termination is reached when all queues are waiting for termination, all queues are empty,
 * all callbacks are finished and no inter-queue messages are waiting in the system.
 */
public interface JobQueue<N: Node, C: Colors<C>, J: Job<N, C>> {

    /**
     * Enqueue new job.
     * Should not block.
     */
    fun post(job: J)

    /**
     * By calling this method, you basically indicate that all initial work has been done and
     * new jobs can only be created as a result of executing other jobs (local or remote).
     * Wait for termination detection and then finalize this queue.
     * Should block.
     * New jobs can be posted while waiting for termination, but not after terminating.
     */
    fun waitForTermination()

    public interface Factory<N: Node, C: Colors<C>, J: Job<N, C>> {

        /**
         * Create new job queue that will execute onTask callback whenever job is posted.
         */
        fun createNew(onTask: (J) -> Unit): Messenger<J>
    }

}

public class SingleThreadJobQueue<N: Node, C: Colors<C>, J: Job<N, C>> (
        messengers: Messenger.Factory,
        terminators: Terminator.Factory,
        partitionFunction: PartitionFunction<N>,
        jobClass: Class<J>,
        private val onTask: JobQueue<N, C, J>.(J) -> Unit   //extension function allows recursive adding
) :
        JobQueue<N, C, J>,
        PartitionFunction<N> by partitionFunction
{

    private var active: Boolean = true

    private val localTaskQueue = LinkedBlockingQueue<Maybe<Message>>()

    private val terminator = terminators.createNew()

    private val messenger = messengers.createNew(jobClass) {
        synchronized(this) {
            localTaskQueue.put(Maybe.Just(it))
            terminator.messageReceived()
        }
    }

    private val worker: Thread = localTaskQueue.threadUntilPoisoned {
        @Suppress("UNCHECKED_CAST")
        onTask(it as J)
        doneIfEmpty()
    }

    private fun doneIfEmpty() = synchronized(this) {
        if (localTaskQueue.isEmpty() && terminator.working) {
            terminator.setDone()
        }
    }

    override fun post(job: J): Unit {
        synchronized(this) {
            if (!active) throw IllegalStateException("Posting job on finished job queue")
            if (job.destination.ownerId() == myId) {
                localTaskQueue.put(Maybe.Just(job))
            } else {
                terminator.messageSent()
                messenger.sendTask(job.destination.ownerId(), job)
            }
        }
    }

    override fun waitForTermination() {
        doneIfEmpty()
        terminator.waitForTermination()
        synchronized(this) {
            active = false
            localTaskQueue.put(Maybe.Nothing())
        }
        worker.join()
    }

}