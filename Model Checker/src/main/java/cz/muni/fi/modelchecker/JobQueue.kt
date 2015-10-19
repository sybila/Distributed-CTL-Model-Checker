package cz.muni.fi.modelchecker

import com.github.daemontus.jafra.Terminator
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.properties.Delegates


public interface Job<N: Node, C: Colors<C>>: Message<N, C> {
    val target: N

    data class AU<N: Node, C: Colors<C>>(
            val sourceNode: N,
            val targetNode: N,
            val colors: C
    ) : Job<N, C> {
        override val target: N = targetNode
    }

    data class EU<N: Node, C: Colors<C>>(
            val node: N,
            val colors: C
    ) : Job<N, C> {
        override val target: N = node
    }

    data class EX<N: Node, C: Colors<C>>(
            val node: N,
            val colors: C
    ) : Job<N, C> {
        override val target: N = node
    }
}


public interface JobQueue<N: Node, C: Colors<C>, J: Message<N, C>> {

    fun start(onTask: (J) -> Unit)

    fun post(job: J)

    fun finish()

}

public class SingleThreadJobQueue<N: Node, C: Colors<C>, J: Job<N, C>> (
        messengers: Messenger.Factory<N, C>,
        terminators: Terminator.Factory,
        partitionFunction: PartitionFunction<N>,
        jobClass: Class<J>
) :
        JobQueue<N, C, J>,
        PartitionFunction<N> by partitionFunction
{

    private var active: Boolean = true

    private val localTaskQueue = LinkedBlockingQueue<Message<N, C>>()

    private val messenger = messengers.createNew(jobClass) {
        synchronized(this) {
            localTaskQueue.put(it)
            terminator.messageReceived()
        }
    }

    private val terminator = terminators.createNew()

    private var worker: Thread by Delegates.notNull()

    override fun start(onTask: (J) -> Unit) {

        fun doneIfEmpty() = synchronized(this) { if (localTaskQueue.isEmpty()) {
            terminator.setDone()
        } }

        worker = thread {
            //If task queue is empty on start, we have to set terminator as done, since the while loop might not even start
            doneIfEmpty()
            var job = localTaskQueue.take()
            while (job !is Message.Poison) {
                @Suppress("UNCHECKED_CAST")     //We only enter Jobs and poison. Job is not poison.
                onTask(job as J)
                //if queue is empty, we are done - we do this before loading next job, because that might not exist yet.
                doneIfEmpty()
                job = localTaskQueue.take()
            }
            //Note: At this point, terminator is finalized, because poison pill has been sent
        }
    }

    override fun post(job: J): Unit {
        synchronized(this) {
            if (!active) throw IllegalStateException("Posting job on finished job queue")
            if (job.target.ownerId() == myId) {
                localTaskQueue.put(job)
            } else {
                terminator.messageSent()
                messenger.sendTask(job.target.ownerId(), job)
            }
        }
    }

    override fun finish() {
        terminator.waitForTermination()
        synchronized(this) {
            active = false
            localTaskQueue.put(Message.Poison())
        }
        worker.join()
    }

}