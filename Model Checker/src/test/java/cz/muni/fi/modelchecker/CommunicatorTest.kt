package cz.muni.fi.modelchecker

import org.junit.Test
import java.util.*
import kotlin.concurrent.thread
import kotlin.test.assertEquals

data class TestMessage(val number: Int): Message, Comparable<TestMessage> {
    override fun compareTo(other: TestMessage): Int = number.compareTo(other.number)
}

class SmallSharedMemoryCommunicatorTest : CommunicatorTest() {

    override val repetitions: Int = 100
    override val processCount: Int = 2

    override val communicatorConstructor: (Int) -> List<Communicator>
            = { c -> createSharedMemoryCommunicators(c) }

}

class BigSharedMemoryCommunicatorTest : CommunicatorTest() {

    override val repetitions: Int = 1
    override val processCount: Int = 24

    override val communicatorConstructor: (Int) -> List<Communicator>
            = { c -> createSharedMemoryCommunicators(c) }

}

public abstract class CommunicatorTest {

    abstract val processCount: Int
    abstract val repetitions: Int
    abstract val communicatorConstructor: (Int) -> List<Communicator>


    @Test(timeout = 1000)
    fun emptyRun() {
        communicatorConstructor(processCount).map { it.finalize() }
    }

    @Test(timeout = 1000)
    fun oneMessengerNoMessages() {
        communicatorConstructor(processCount).map {
            thread {
                val messenger = it.listenTo(TestMessage::class.java) {
                    throw IllegalStateException("Unexpected message")
                }
                messenger.setIdle()
                messenger.close()
                it.finalize()
            }
        }.map { it.join() }
    }

    @Test(timeout = 1000)
    fun moreMessengersNoMessages() {
        communicatorConstructor(processCount).map {
            thread {
                val m1 = it.listenTo(TestMessage::class.java) {
                    throw IllegalStateException("Unexpected message")
                }
                m1.setIdle()
                m1.close()

                val m2 = it.listenTo(TestMessage::class.java) {
                    throw IllegalStateException("Unexpected message")
                }
                m2.setIdle()
                m2.close()
                it.finalize()
            }
        }.map { it.join() }
    }

    @Test(timeout = 1000)
    fun oneMessengerWithMessages() {
        communicatorConstructor(processCount).map {
            thread {
                val received = ArrayList<TestMessage>()
                val expected = ArrayList<TestMessage>()

                var flag = 0
                val messenger = it.listenTo(TestMessage::class.java) {
                    received.add(it)
                    if (flag == 1) this.setIdle()
                }

                for (i in 0..(processCount-1)) {
                    if (i != it.rank()) {
                        messenger.sendTask(i, TestMessage(it.rank()))
                        expected.add(TestMessage(i))    //messages I will receive from other people
                    }
                }
                flag = 1
                messenger.setIdle()
                messenger.close()
                it.finalize()
                assertEquals(expected.sorted(), received.sorted())
            }
        }.map { it.join() }
    }

    @Test(timeout = 2000)
    fun moreMessengersWithMessages() {
        //Always sends i messages to all other processes and then closes the messenger,
        //therefore we can safely predict how the expected received messages should look.
        //(It's going to be all messages repeated i-times except for message from itself)

        for (a in 1..repetitions) { //Repeat this a lot and hope for the best!
            val allMessages = (1..processCount).map { TestMessage(it - 1) }

            communicatorConstructor(processCount).map { comm ->
                thread {
                    for (i in 1..10) {
                        val received = ArrayList<TestMessage>()
                        val expected = (allMessages - TestMessage(comm.rank())).flatRepeat(i)

                        var flag = 0    //strictly speaking, we should be synchronizing this
                        val messenger = comm.listenTo(TestMessage::class.java) {
                            received.add(it)
                            if (flag == 1) this.setIdle()
                        }
                        for (p in 0..(processCount * i - 1)) {
                            //Growing number of messages for each iteration
                            if (p % comm.size() != comm.rank()) {
                                //Don't send to yourself
                                messenger.sendTask(p % comm.size(), TestMessage(comm.rank()))
                            }
                        }
                        flag = 1
                        messenger.setIdle()
                        messenger.close()
                        assertEquals(expected.sorted(), received.sorted())
                    }
                    comm.finalize()
                }
            }.map { it.join() }
        }
    }

    @Test(timeout = 10000)
    fun complexTest() {
        //Initialize 10 * processCount floods with various lifespans and send them to random receivers.
        //If you receive positive flood message, pass it to next random process.
        //Since we can't send messages to ourselves, for two processes, this is completely deterministic.

        for (a in 1..repetitions) { //Repeat this a lot and hope for the best!
            val allMessages = communicatorConstructor(processCount).map { comm ->

                fun randomReceiver(): Int {
                    var receiver = comm.rank()
                    while (receiver == comm.rank()) {
                        receiver = (Math.random() * this.processCount).toInt()
                    }
                    return receiver
                }

                val received = ArrayList<TestMessage>()
                val sent = HashMap((1..comm.size()).toMap({it - 1}, { ArrayList<TestMessage>() }))

                val worker = thread {
                    for (i in 1..5) {   //Create more messengers in a row in order to fully test the communicator
                        var flag = 0    //Strictly speaking, this variable should be also synchronized

                        val messenger = comm.listenTo(TestMessage::class.java) {
                            synchronized(received) { received.add(it) }
                            if (it.number > 0) {
                                val receiver = randomReceiver()
                                val message = TestMessage(it.number - 1)
                                synchronized(sent) { sent[receiver]!!.add(message) }
                                sendTask(receiver, message)
                            }
                            if (flag == 1) this.setIdle()
                        }

                        for (p in 1..(processCount * 10)) {
                            val receiver = randomReceiver()
                            val message = TestMessage(p)
                            synchronized(sent) { sent[receiver]!!.add(message) }
                            messenger.sendTask(receiver, message)
                        }

                        flag = 1
                        messenger.setIdle()
                        messenger.close()
                    }
                    comm.finalize()
                }

                Pair(worker, Pair(sent, received))
            }.map {
                it.first.join(); it.second
            }

            //Merge sent messages by their destinations into something that has same type as received list
            val sent = allMessages.map { it.first }.foldRight(
                    HashMap((0..(processCount - 1)).map { Pair(it, listOf<TestMessage>()) }.toMap())
            ) { value, accumulator ->
                for ((key, list) in value) {
                    accumulator[key] = list + accumulator[key]!!
                }
                accumulator
            }.mapValues {
                it.value.sorted()
            }
            val received = allMessages.map { it.second }.mapIndexed { i, list -> Pair(i, list.sorted()) }.toMap()

            assertEquals(received, sent)
        }

    }

}