package cz.muni.fi.modelchecker

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread


interface Message<N: Node, C: Colors<C>> {
    class Poison<N: Node, C: Colors<C>>() : Message<N, C>
}

public interface Messenger<N: Node, C: Colors<C>> {

    fun sendTask(receiver: Int, task: Job<N, C>)

    fun close()

    public interface Factory<N: Node, C: Colors<C>> {
        fun <J: Job<N,C>> createNew(jobClass: Class<J>, onTask: (J) -> Unit): Messenger<N, C>
    }
}

public class QueueJobMessenger<N: Node, C: Colors<C>, J: Job<N, C>>(
        private val myId: Int,
        private val queues: List<BlockingQueue<Message<N, C>>>,
        private val expectedMessage: Class<J>,
        onTask: (J) -> Unit
) : Messenger<N, C> {

    private val listener = thread {
        var job = queues[myId].take()
        while (job !is Message.Poison) {
            if (job.javaClass != expectedMessage) throw IllegalStateException("Got message of invalid type: $job, expected $expectedMessage")
            @Suppress("UNCHECKED_CAST")
            onTask(job as J)
            job = queues[myId].take()
        }
    }

    override fun sendTask(receiver: Int, task: Job<N, C>) {
        if (receiver == myId) throw IllegalArgumentException("Sending message to yourself!")
        queues[receiver].put(task)
    }

    override fun close() {
        queues[myId].put(Message.Poison<N, C>())
        listener.join()
    }

}

public fun <N: Node, C: Colors<C>> createSharedMemoryMessengers(
        processCount: Int,
        queueFactory: () -> BlockingQueue<Message<N, C>> = { LinkedBlockingQueue<Message<N, C>>() }
): List<Messenger.Factory<N, C>> {
    val processRange = 0..(processCount-1)
    val queues = processRange.map { queueFactory() }
    return processRange
            .map {
                object : Messenger.Factory<N, C> {
                    override fun <J : Job<N, C>> createNew(jobClass: Class<J>, onTask: (J) -> Unit): Messenger<N, C>
                            = QueueJobMessenger(it, queues, jobClass, onTask)
                } }
}