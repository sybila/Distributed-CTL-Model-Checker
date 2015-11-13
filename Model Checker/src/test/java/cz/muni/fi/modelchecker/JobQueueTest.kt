package cz.muni.fi.modelchecker

import org.junit.Test
import java.util.*
import java.util.concurrent.FutureTask
import kotlin.concurrent.thread
import kotlin.test.assertEquals


class BigSingleThreadJobQueueTest : SingleThreadJobQueueTest() {
    override val processCount: Int = 24

}

class SmallSingleThreadJobQueueTest : SingleThreadJobQueueTest() {
    override val processCount: Int = 2
}

class OneSingleThreadJobQueueTest : SingleThreadJobQueueTest() {
    override val processCount: Int = 1
}

abstract class SingleThreadJobQueueTest: JobQueueTest() {

    override fun createJobQueues(
            processCount: Int,
            partitioning: List<PartitionFunction<IDNode>>
    ): List<JobQueue.Factory<IDNode, IDColors, Job.EU<IDNode, IDColors>>>
            = createSharedMemoryJobQueue(
                processCount = processCount,
                jobClass = genericClass<Job.EU<IDNode, IDColors>>(),
                partitioning = partitioning
            )

}

val jobComparator = object : Comparator<Job.EU<IDNode, IDColors>> {

    override fun compare(o1: Job.EU<IDNode, IDColors>, o2: Job.EU<IDNode, IDColors>): Int {
        if (o1.colors == o2.colors) return o1.node.id.compareTo(o2.node.id)
        else {
            return o1.colors.toString().compareTo(o2.colors.toString())
        }
    }

}

/**
 * And abstract set of tests for your job queue implementation.
 * Just override queue constructor.
 */
public abstract class JobQueueTest {

    abstract val processCount: Int

    abstract fun createJobQueues(
            processCount: Int,
            partitioning: List<PartitionFunction<IDNode>> = (1..processCount).map { UniformPartitionFunction<IDNode>(it-1) }
    ): List<JobQueue.Factory<IDNode, IDColors, Job.EU<IDNode, IDColors>>>

    private val allColors = (1..5).toSet()

    private fun randomEuJob(): Job.EU<IDNode, IDColors> = Job.EU(
            IDNode((Math.random() * 10).toInt()),
            IDColors(allColors.randomSubset())
    )


    @Test(timeout = 1000)
    fun noMessages() {
        createJobQueues(processCount).map { f -> thread {
            val q = f.createNew {  }
            q.waitForTermination()
        } }.map { it.join() }
    }

    @Test(timeout = 1000)
    fun onlyInitialTest() {
        createJobQueues(processCount).map { f -> thread {

            val executed = ArrayList<Job.EU<IDNode, IDColors>>()
            val jobs = (1..10).map { randomEuJob() }

            val q = f.createNew(jobs) { synchronized(executed) {
                executed.add(it)
            } }

            q.waitForTermination()

            assertEquals<List<Job.EU<IDNode, IDColors>>>(
                    jobs.sortedWith(jobComparator),
                    executed.sortedWith(jobComparator)
            )   //bug
        } }.map { it.join() }
    }

    @Test(timeout = 5000)
    fun complexTest() {

        //As in messenger tests, create a flood of jobs that will jump across state space

        val allJobs = createJobQueues(
                processCount,
                (1..processCount).map { i -> FunctionalPartitionFunction(i-1) { it.id.toInt() % (i-1) } }
        ).map { f -> FutureTask {

            val executed = ArrayList<Job.EU<IDNode, IDColors>>()
            val posted = HashMap((1..processCount).toMap({it - 1}, { ArrayList<Job.EU<IDNode, IDColors>>() }))

            val initial = (1..(processCount * 10)).map { randomEuJob() }.filter { it.node.id % processCount == processCount }
            initial.forEach { job -> synchronized(posted) {
                    posted[job.node.id % processCount]!!.add(job)
            } }

            val queue = f.createNew(initial) {
                synchronized(executed) { executed.add(it) }
                if (it.node.id != 0) {
                    val newNodeId = it.node.id - 1
                    val newJob = Job.EU(IDNode(newNodeId), it.colors)
                    synchronized(posted) {
                        posted[newNodeId % processCount]!!.add(newJob)
                    }
                    this.post(newJob)
                }
            }

            queue.waitForTermination()

            Pair(posted, executed)
        } }.map { thread { it.run() }; it }.map { it.get() }

        //Merge sent messages by their destinations into something that has same type as received list
        val sent = allJobs.map { it.first }.foldRight(
                HashMap((0..(processCount - 1)).map { Pair(it, listOf<Job.EU<IDNode, IDColors>>()) }.toMap())
        ) { value, accumulator ->
            for ((key, list) in value) {
                accumulator[key] = list + accumulator[key]!!
            }
            accumulator
        }.mapValues {
            it.value.sortedWith(jobComparator)
        }
        val received = allJobs.map { it.second }.mapIndexed { i, list -> Pair(i, list.sortedWith(jobComparator)) }.toMap()

        assertEquals(received, sent)
    }

}