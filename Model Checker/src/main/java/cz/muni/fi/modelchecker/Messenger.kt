package cz.muni.fi.modelchecker

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/**
 * Classic maybe - Maybe move it to some utility file?
 */
public sealed class Maybe<T: Any> {

    public class Just<T: Any>(public val value: T): Maybe<T>() {
        override fun equals(other: Any?): Boolean = value.equals(other)
        override fun hashCode(): Int = value.hashCode()
    }

    public class Nothing<T: Any>(): Maybe<T>()
}

/**
 * A simple interface that represents messages that can be sent through messenger (Independent from Jobs)
 */
public interface Message;

fun <T: Any> BlockingQueue<Maybe<T>>.threadUntilPoisoned(onItem: (T) -> Unit) = thread {
    var job = this.take()
    while (job is Maybe.Just) {
        onItem(job.value)
        job = this.take()
    }   //got Nothing
}

/**
 * Messenger is responsible for sending messages between processes.
 * Messenger should be active from the moment when it's created.
 */
public interface Messenger<M: Message> {

    /**
     * Send Job to process with specified ID.
     * Should not block.
     * Method should fail when sending message to itself.   (Bad design - use job queue for that)
     * Method should fail when message is Poison. (Messenger should decide when to send poison, not outside world)
     */
    fun sendTask(receiver: Int, message: M)

    /**
     * Use this method to cleanup all possible communications.
     * No tasks will be sent using a closed messenger.
     * Can block.
     */
    fun close()

    public interface Factory {

        /**
         * Create new messenger instance accepting jobs given by jobClass and performing onTask callback on
         * every message receive (serially or in parallel).
         *
         * This way you can limit yourself only to specific types of messages.
         * WARNING: When message of different class is received and there is no one to consume it,
         * it's considered an error and exception will be thrown.
         */
        fun <M: Message> createNew(jobClass: Class<M>, onTask: (M) -> Unit): Messenger<M>
    }
}

/**
 * A sample implementation of job messenger that uses blocking queues and one listening thread
 * as means of communication.
 *
 * Suitable for in-memory computing, but note that it's optimized for readability/testing and not speed.
 */
class QueueJobMessenger<N: Node, C: Colors<C>, M : Message>(
        private val myId: Int,
        private val queues: List<BlockingQueue<Maybe<Message>>>,
        private val expectedMessage: Class<M>,
        onTask: (M) -> Unit
) : Messenger<M> {

    private val listener = queues[myId].threadUntilPoisoned { job ->
        if (job.javaClass != expectedMessage)
            throw IllegalStateException("Got message of invalid type: $job, expected $expectedMessage")
        @Suppress("UNCHECKED_CAST")
        onTask(job as M)
    }

    override fun sendTask(receiver: Int, message: M) {
        if (receiver == myId) throw IllegalArgumentException("Sending message to yourself!")
        queues[receiver].put(Maybe.Just(message))
    }

    override fun close() {
        queues[myId].put(Maybe.Nothing<Message>())
        listener.join()
    }

}

/**
 * Factory method that creates a set of in memory messenger factories connected by BlockingQueues.
 * (Blocking queues are shared across created messengers, so that errors should appear
 * if you use multiple messengers from one process -> this should provide easy detection
 * of problems with global synchronization in main algorithm)
 *
 * Suitable for in-memory computing, but note that it's optimized for readability/testing and not speed.
 */
public fun <N: Node, C: Colors<C>> createSharedMemoryMessengers(
        processCount: Int,
        queueFactory: () -> BlockingQueue<Maybe<Message>> = { LinkedBlockingQueue<Maybe<Message>>() }
): List<Messenger.Factory> {
    val processRange = 0..(processCount-1)
    val queues = processRange.map { queueFactory() }
    return processRange
            .map {
                object : Messenger.Factory {
                    override fun <M : Message> createNew(jobClass: Class<M>, onTask: (M) -> Unit): Messenger<M>
                            = QueueJobMessenger<N, C, M>(it, queues, jobClass, onTask)

                } }
}