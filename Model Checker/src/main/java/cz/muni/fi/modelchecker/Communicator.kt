package cz.muni.fi.modelchecker

import com.github.daemontus.jafra.SharedMemoryMessengers
import com.github.daemontus.jafra.Terminator
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/**
 * NOTE: The strong/complicated semantics enforced in messenger-communicator relationship
 * is to prevent bugs that are result of two phases of model checking algorithm interleaving
 * due to faulty synchronization.
 * This way all such problems should be easily detected, since every phase creates it's own messenger.
 */

/**
 * Classic maybe - Maybe move it to some utility file?
 * Note: We need this because queues don't accept null values.
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
 * Messenger is responsible for sending messages of one type between processes.
 * Messenger should be active from the moment when it's created.
 * The creation of a messenger is a global synchronization event.
 * Only one messenger can be active at any time. (Closed messengers are fine though)
 */
public interface Messenger<M: Message> {

    /**
     * Send Job to process with specified ID.
     * Should not block.
     * Method should fail when sending message to itself.   (Bad design - use job queue for that)
     * Method should fail when message cannot be serialized.
     */
    fun sendTask(receiver: Int, message: M)

    /**
     * Mark this messenger as idle, meaning that no new messages will be sent unless a new message is received.
     * When a message is received, the messenger goes automatically from idle to active and has to be set to idle
     * again when message has been processed.
     *
     * After starting, messenger is always active and has to be set to
     * idle at least once even if no messages have been received.
     */
    fun setIdle()

    /**
     * Use this method to cleanup all possible communications.
     * No tasks can be sent using a closed messenger.
     *
     * This call should be a global barrier across all processes,
     * meaning that either all processes close the messenger,
     * or no messenger is closed and the method blocks until
     * all processes reach it.
     *
     * All messengers must be idle and all remaining messages should be consumed before closing the messenger.
     */
    fun close()

}

public interface Communicator {

    /**
     * Create new messenger instance accepting messages given by messageClass and performing onTask callback on
     * every message received (serially or in parallel).
     *
     * This way you can limit yourself only to specific types of messages.
     * WARNING: When message of unrecognized class is received, exception will be thrown.
     * When class is recognized, but there is no one to consume it,
     * it's also considered an error and exception will be also thrown.
     * There can be only one messenger in the system.
     * If you try to register a messenger but there is already an active one,
     * exception should be thrown.
     *
     * WARNING: messageClass looses info about generics at run time!
     *
     * This call should be a global barrier across all processes,
     * meaning that either all processes create new messenger,
     * or no messenger is created and the method blocks until
     * all processes reach it. (Or all processes throw an exception in case one of the calls was invalid)
     */
    fun <M: Message> listenTo(messageClass: Class<M>, onTask: Messenger<M>.(M) -> Unit): Messenger<M>

    /**
     * Use this to clean up global environment - i.e. close MPI connections, join threads...
     */
    fun finalize()

    /**
     * Total number of participating processes
     */
    fun size(): Int

    /**
     * My id.
     */
    fun rank(): Int
}

/**
 * Messenger that received info about tasks from
 */
private abstract class InMemoryMessenger<M: Message>(
        val messageClass: Class<M>,
        val onTask: Messenger<M>.(Message) -> Unit,
        val terminator: Terminator
): Messenger<M> { }

/**
 * Factory method that creates a set of in memory messenger factories connected by BlockingQueues.
 * (Blocking queues are shared across created messengers, so that errors should appear
 * if you use multiple messengers from one process -> this should provide easy detection
 * of problems with global synchronization in main algorithm)
 *
 * Suitable for in-memory computing, but note that it's optimized for readability/testing and not speed.
 */
public fun createSharedMemoryCommunicators(
        processCount: Int,
        queueFactory: () -> BlockingQueue<Maybe<Message>> = { LinkedBlockingQueue<Maybe<Message>>() }
): List<Communicator> {

    val processRange = 0..(processCount-1)
    val queues = processRange.map { queueFactory() }
    val terminatorFactories = SharedMemoryMessengers(processCount).messengers.map { Terminator.Factory(it) }

    val barrier = CyclicBarrier(processCount)

    return processRange.map { id ->
        object : Communicator {

            fun barrier() {
                barrier.await()
            }

            //Lock that guards all state variables of this communicator
            private val lock = Object()

            override fun size(): Int = processCount
            override fun rank(): Int = id

            var messenger: InMemoryMessenger<*>? = null

            val worker = queues[id].threadUntilPoisoned {
                synchronized(lock) {
                    if (messenger != null && it.javaClass.equals(messenger!!.messageClass)) {
                        messenger!!.terminator.messageReceived()
                        val onTask = messenger!!.onTask
                        messenger!!.onTask(it)
                    } else {
                        throw IllegalStateException("Received message of class ${it.javaClass} but no listener was " +
                                "found. Active listeners: ${messenger?.messageClass}")
                    }
                }
            }

            override fun <M : Message> listenTo(messageClass: Class<M>, onTask: Messenger<M>.(M) -> Unit): Messenger<M> {
                synchronized(lock) {
                    if (messenger != null)
                        throw IllegalStateException("Messenger for $messageClass already exist in $id, close it first")
                    else {

                        barrier()

                        val messenger = object : InMemoryMessenger<M>(
                                messageClass,
                                @Suppress("UNCHECKED_CAST") //Ok, because it's checked with messageClass
                                (onTask as (Messenger<M>.(Message) -> Unit)),
                                terminatorFactories[id].createNew()
                        ) {

                            override fun sendTask(receiver: Int, message: M) {
                                if (receiver == id) throw IllegalArgumentException("Sending message to yourself!")
                                terminator.messageSent()
                                queues[receiver].put(Maybe.Just(message))
                            }

                            override fun close() {
                                terminator.waitForTermination()
                                synchronized(lock) {
                                    barrier()
                                    messenger = null
                                }
                            }

                            override fun setIdle(): Unit {
                                synchronized(lock) {
                                    if (terminator.working) terminator.setDone()
                                }
                            }

                        }
                        this.messenger = messenger

                        barrier()
                        //We need two barriers, because all messengers have to be already constructed
                        //when returned to the outside world

                        return messenger
                    }
                }
            }


            override fun finalize() {
                synchronized(lock) {
                    if (messenger != null) {
                        throw IllegalStateException("Finalizing with unclosed messengers for ${messenger!!.messageClass}}")
                    }
                }
                queues[id].put(Maybe.Nothing())
                worker.join()
            }

        } }

}