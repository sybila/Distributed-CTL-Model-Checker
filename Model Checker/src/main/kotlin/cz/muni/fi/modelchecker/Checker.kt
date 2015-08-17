package cz.muni.fi.modelchecker

import com.github.daemontus.jafra.Terminator
import com.github.daemontus.jafra.Token
import com.github.daemontus.jafra.TokenMessenger
import cz.muni.fi.ctl.Atom
import cz.muni.fi.ctl.Formula
import cz.muni.fi.ctl.Op
import java.util.HashSet
import java.util.concurrent.LinkedBlockingQueue


interface Processor<N : Node, C : ColorSet<C>> {
    fun verify()
}

public class ModelChecker<N : Node, C : ColorSet<C>> (
    private val model: Model<N, C>,

    private val partitioner: StatePartitioner<N> = object : StatePartitioner<N> {
        override val owner: Node.() -> Int = { 0 }
        override val myId: Int = 0
    },

    val messenger: TaskMessenger<N, C> = object : TaskMessenger<N, C> {

        var session: Boolean = false

        override public
        fun closeSession() {
            if (!session) throw IllegalStateException("Closing unopened session")
            session = false
        }

        //ignore IDE error - compiler is ok with this
        override public
        fun startSession(taskListener: OnTaskListener<N, C>) {
            if (session) throw IllegalStateException("Opening opened session")
            session = true
        }

        override public
        fun sendTask(destinationProcess: Int, internal: N, external: N, colors: C): Unit
                = throw IllegalStateException("Cannot send messages in single process mode.")
    },

    private val terminators: Terminator.TerminatorFactory = Terminator.TerminatorFactory(object : TokenMessenger {
        private val queue = LinkedBlockingQueue<Token>()
        override fun isMaster(): Boolean = true
        override fun receiveFromPrevious(): Token = queue.take()
        override fun sendToNextAsync(token: Token) { queue.add(token) }
    })

) {

    private val done = HashSet<Formula>()

    public fun verify(formula: Formula) {

        if (formula is Atom || formula in done) return;

        for (f in formula.subFormulas) {
            verify(f)
        }


        val processor: Processor<N, C> = when (formula.operator) {
            Op.NEGATION -> NegationProcessor<N, C>(formula, model)
            Op.AND -> AndProcessor<N, C>(formula, model)
            Op.OR -> OrProcessor<N, C>(formula, model)
            Op.EXISTS_NEXT -> NextProcessor<N, C>(formula, model, partitioner, messenger, terminators.createNew())
            else -> throw IllegalStateException("Unsupported operator ${formula.operator}")
        }
        messenger.startSession()

        processor.verify()

        //global synchronization on communication channel closing
        val t = terminators.createNew()
        messenger.closeSession()
        messenger.taskListener = null
        t.setDone()
        t.waitForTermination()

        done.add(formula)
        
    }

}