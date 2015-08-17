package cz.muni.fi.modelchecker

import com.github.daemontus.jafra.Terminator
import com.lmax.disruptor.RingBuffer
import cz.muni.fi.ctl.Formula
import java.util.HashMap
import kotlin.concurrent.thread

abstract class QueueProcessor<N: Node, C: ColorSet<C>>(
    model: Model<N, C>
) :
        Processor<N, C>,
        Model<N, C> by model
{

    private val queue = HashMap<N, C>()

    override final fun verify() {
        var terminated = false

        loadQueue()

        val i : RingBuffer<Int>
        
        val worker = thread {

        }
    }

    /** Add data to the "merge queue"  */
    fun addToQueue(node: N, colors: C) {
        synchronized (queue) {
            queue[node] = if (node in queue) (queue[node] union colors) else colors
        }
    }

    /** Load initial data into the queue */
    abstract fun loadQueue()

    /** Process all data stored in the queue */
    abstract fun processQueue()
}

class NextProcessor<N : Node, C : ColorSet<C>>(
        private val formula: Formula,
        model: Model<N, C>,
        partitioner: StatePartitioner<N>,
        private val messenger: TaskMessenger<N, C>,
        private val terminator: Terminator
) :
        Processor<N, C>,
        Model<N, C> by model,
        StatePartitioner<N> by partitioner,
        OnTaskListener<N, C>
{
    private var done = false

    init {
        messenger.taskListener = this
    }

    override fun onTask(sourceProcess: Int, external: N, internal: N, colors: C) {
        synchronized(this) {
            //has to synchronize whole block, because setDone can be
            //called only if done was set before messageReceived and not changed
            terminator.messageReceived()
            internal.saveFormula(formula, colors)
            if (done) {
                terminator.setDone()
            }
        }
    }

    override fun verify() {
        for ((node, colour) in formula[0].initialNodes()) {
            for ((predecessor, pushedColour) in node.predecessors(colour)) {
                if (predecessor.owner() == myId) {
                    node.saveFormula(formula, pushedColour)
                } else {
                    messenger.sendTask(predecessor.owner(), node, predecessor, pushedColour)
                }
            }
        }
    }

}

