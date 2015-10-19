package cz.muni.fi.modelchecker

import com.github.daemontus.jafra.SharedMemoryMessengers
import com.github.daemontus.jafra.Terminator
import org.junit.Test
import java.util.*
import kotlin.concurrent.thread
import kotlin.test.assertEquals

class SingleThreadJobQueueTest : JobQueueTest() {

    override val jobQueueConstructor:
            ( Messenger.Factory<IDNode, IDColors>,
              Terminator.Factory,
              PartitionFunction<IDNode>,
              Class<Job.EU<IDNode, IDColors>>
            ) -> JobQueue<IDNode, IDColors, Job.EU<IDNode, IDColors>>
            = { messenger, terminator, partition, cls -> SingleThreadJobQueue(messenger, terminator, partition, cls) }

}

abstract class JobQueueTest {

    abstract val jobQueueConstructor:
            ( Messenger.Factory<IDNode, IDColors>,
              Terminator.Factory,
              PartitionFunction<IDNode>,
              Class<Job.EU<IDNode, IDColors>>
            ) -> JobQueue<IDNode, IDColors, Job.EU<IDNode, IDColors>>

    private val undefinedMessengers = object : Messenger.Factory<IDNode, IDColors> {
        override fun <J : Job<IDNode, IDColors>> createNew(jobClass: Class<J>, onTask: (J) -> Unit): Messenger<IDNode, IDColors> {
            return object : Messenger<IDNode, IDColors> {
                override fun sendTask(receiver: Int, task: Job<IDNode, IDColors>) = throw UnsupportedOperationException()
                override fun close() { /*ok*/ }
            }
        }
    }

    @Test(timeout = 1000)
    fun multipleWithMixedMessages() {

        val terminatorMessengers = SharedMemoryMessengers(2)
        val jobMessengers = createSharedMemoryMessengers<IDNode, IDColors>(2)

        val n = (0..7).map { IDNode(it.toLong()) }

        val sent1 = listOf(
                Job.EU(n[0], IDColors(0,1)),
                Job.EU(n[1], IDColors(1,2)),
                Job.EU(n[0], IDColors(2)),
                Job.EU(n[3], IDColors(0,1,2))
        )

        val sent2 = listOf(
                Job.EU(n[3], IDColors(2,2)),
                Job.EU(n[1], IDColors(2,3)),
                Job.EU(n[5], IDColors(4,5)),
                Job.EU(n[3], IDColors(0,1)),
                Job.EU(n[7], IDColors(1,2,3)),
                Job.EU(n[0], IDColors(3))
        )

        val t1 = thread {
            val queue1 = jobQueueConstructor(
                    jobMessengers[0],
                    Terminator.Factory(terminatorMessengers.messengers[0]),
                    ExplicitPartitionFunction(0, inverseMapping = mapOf(
                            Pair(0, listOf(n[0], n[1], n[7])),
                            Pair(1, listOf(n[2], n[3], n[4], n[5], n[6])))),
                    genericClass<Job.EU<IDNode, IDColors>>()
            )

            val processed = ArrayList<Job.EU<IDNode, IDColors>>()
            var toSend = ArrayList(sent1)

            queue1.post(toSend.remove(0))

            queue1.start {
                processed.add(it)
                if (toSend.isNotEmpty()) queue1.post(toSend.remove(0))
            }

            queue1.finish()

            //compare sets because order does not have ot be exact
            assertEquals(setOf(sent1[0], sent1[1], sent1[2], sent2[1], sent2[4], sent2[5]), processed.toSet())
        }

        val t2 = thread {
            val queue2 = jobQueueConstructor(
                    jobMessengers[1],
                    Terminator.Factory(terminatorMessengers.messengers[1]),
                    ExplicitPartitionFunction(1, inverseMapping = mapOf(
                            Pair(0, listOf(n[0], n[1], n[7])),
                            Pair(1, listOf(n[2], n[3], n[4], n[5], n[6])))),
                    genericClass<Job.EU<IDNode, IDColors>>()
            )

            val processed = ArrayList<Job.EU<IDNode, IDColors>>()
            val toSend = ArrayList(sent2)

            queue2.post(toSend.remove(0))

            queue2.start {
                processed.add(it)
                //worker2 has to produce more messages because he wants ot sent more of them
                if (toSend.isNotEmpty()) { queue2.post(toSend.remove(0)) }
                if (toSend.isNotEmpty()) { queue2.post(toSend.remove(0)) }
            }

            queue2.finish()

            assertEquals(setOf(sent1[3], sent2[0], sent2[2], sent2[3]), processed.toSet())
        }

        t1.join()
        t2.join()
    }

    @Test(timeout = 1000)
    fun multipleWithCrossMessages() {

        val terminatorMessengers = SharedMemoryMessengers(2)
        val jobMessengers = createSharedMemoryMessengers<IDNode, IDColors>(2)

        val sent1 = listOf(
                Job.EU(IDNode(0), IDColors(0,1)),
                Job.EU(IDNode(1), IDColors(1,2)),
                Job.EU(IDNode(0), IDColors(2)),
                Job.EU(IDNode(3), IDColors(0,1,2))
        )

        val sent2 = listOf(
                Job.EU(IDNode(1), IDColors(2,3)),
                Job.EU(IDNode(3), IDColors(2,2)),
                Job.EU(IDNode(5), IDColors(4,5)),
                Job.EU(IDNode(3), IDColors(0,1)),
                Job.EU(IDNode(7), IDColors(1,2,3)),
                Job.EU(IDNode(0), IDColors(3))
        )

        val t1 = thread {
            val queue1 = jobQueueConstructor(
                    jobMessengers[0],
                    Terminator.Factory(terminatorMessengers.messengers[0]),
                    FunctionalPartitionFunction(0, { 1 }),
                    genericClass<Job.EU<IDNode, IDColors>>()
            )

            val processed = ArrayList<Job.EU<IDNode, IDColors>>()
            var toSend = ArrayList(sent1)

            queue1.post(toSend.remove(0))

            queue1.start {
                processed.add(it)
                if (toSend.isNotEmpty()) queue1.post(toSend.remove(0))
            }

            queue1.finish()

            assertEquals(sent2, processed)
        }

        val t2 = thread {
            val queue2 = jobQueueConstructor(
                    jobMessengers[1],
                    Terminator.Factory(terminatorMessengers.messengers[1]),
                    FunctionalPartitionFunction(1, { 0 }),
                    genericClass<Job.EU<IDNode, IDColors>>()
            )

            val processed = ArrayList<Job.EU<IDNode, IDColors>>()
            val toSend = ArrayList(sent2)

            queue2.post(toSend.remove(0))

            queue2.start {
                processed.add(it)
                //worker2 has to produce more messages because he wants ot sent more of them
                if (toSend.isNotEmpty()) { queue2.post(toSend.remove(0)) }
                if (toSend.isNotEmpty()) { queue2.post(toSend.remove(0)) }
            }

            queue2.finish()

            assertEquals(sent1, processed)
        }

        t1.join()
        t2.join()
    }

    @Test(timeout = 1000)
    fun multipleWithInternalMessages() {

        val terminatorMessengers = SharedMemoryMessengers(2)
        val jobMessengers = createSharedMemoryMessengers<IDNode, IDColors>(2)

        val t1 = thread {
            val queue1 = jobQueueConstructor(
                    jobMessengers[0],
                    Terminator.Factory(terminatorMessengers.messengers[0]),
                    FunctionalPartitionFunction(0, { 0 }),
                    genericClass<Job.EU<IDNode, IDColors>>()
            )

            val sent = listOf(
                    Job.EU(IDNode(0), IDColors(0,1)),
                    Job.EU(IDNode(1), IDColors(1,2)),
                    Job.EU(IDNode(0), IDColors(2)),
                    Job.EU(IDNode(3), IDColors(0,1,2))
            )

            val processed = ArrayList<Job.EU<IDNode, IDColors>>()

            queue1.post(sent.first())

            queue1.start { processed.add(it) }

            sent.drop(1).forEach { queue1.post(it) }

            queue1.finish()

            assertEquals(sent, processed)
        }

        val t2 = thread {
            val queue2 = jobQueueConstructor(
                    jobMessengers[1],
                    Terminator.Factory(terminatorMessengers.messengers[1]),
                    FunctionalPartitionFunction(1, { 1 }),
                    genericClass<Job.EU<IDNode, IDColors>>()
            )

            val sent = listOf(
                    Job.EU(IDNode(1), IDColors(2,3)),
                    Job.EU(IDNode(3), IDColors(2,2)),
                    Job.EU(IDNode(5), IDColors(4,5)),
                    Job.EU(IDNode(3), IDColors(0,1)),
                    Job.EU(IDNode(7), IDColors(1,2,3)),
                    Job.EU(IDNode(0), IDColors(3))
            )

            val processed = ArrayList<Job.EU<IDNode, IDColors>>()

            queue2.post(sent.first())

            queue2.start { processed.add(it) }

            sent.drop(1).forEach { queue2.post(it) }

            queue2.finish()

            assertEquals(sent, processed)
        }

        t1.join()
        t2.join()
    }

    @Test(timeout = 1000)
    fun multipleNoMessages() {

        val terminatorMessengers = SharedMemoryMessengers(2)
        val jobMessengers = createSharedMemoryMessengers<IDNode, IDColors>(2)

        val t1 = thread {
            val queue1 = jobQueueConstructor(
                    jobMessengers[0],
                    Terminator.Factory(terminatorMessengers.messengers[0]),
                    FunctionalPartitionFunction(0, { 0 }),
                    genericClass<Job.EU<IDNode, IDColors>>()
            )

            queue1.start { }
            queue1.finish()
        }

        val t2 = thread {
            val queue2 = jobQueueConstructor(
                    jobMessengers[1],
                    Terminator.Factory(terminatorMessengers.messengers[1]),
                    FunctionalPartitionFunction(1, { 1 }),
                    genericClass<Job.EU<IDNode, IDColors>>()
            )

            queue2.start { }
            queue2.finish()
        }

        t1.join()
        t2.join()
    }

    @Test(timeout = 1000)
    fun singleWithInternalMessagesTest() {

        val terminatorMessengers = SharedMemoryMessengers(1)

        val queue = jobQueueConstructor(
                undefinedMessengers,
                Terminator.Factory(terminatorMessengers.messengers[0]),
                FunctionalPartitionFunction(0, { 0 }),
                genericClass<Job.EU<IDNode, IDColors>>()
        )

        val executed = ArrayList<Job.EU<IDNode, IDColors>>()

        val posted = listOf(
                Job.EU(IDNode(1), IDColors(1,2)),
                Job.EU(IDNode(2), IDColors(2,3)),
                Job.EU(IDNode(1), IDColors(3)),
                Job.EU(IDNode(3), IDColors(0,1)),
                Job.EU(IDNode(2), IDColors(0,1))
        )

        queue.post(posted[0])

        queue.start { executed.add(it) }

        queue.post(posted[1])
        queue.post(posted[2])
        queue.post(posted[3])
        queue.post(posted[4])

        queue.finish()

        assertEquals(posted, executed)
    }

    @Test(timeout = 1000)
    fun singleNoMessages() {
        val terminatorMessengers = SharedMemoryMessengers(1)

        val queue = jobQueueConstructor(
                undefinedMessengers,
                Terminator.Factory(terminatorMessengers.messengers[0]),
                ExplicitPartitionFunction<IDNode>(0, mapOf()),
                genericClass<Job.EU<IDNode, IDColors>>()
        )

        queue.start {  }
        queue.finish()
    }
}