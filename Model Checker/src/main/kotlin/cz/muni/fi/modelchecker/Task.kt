package cz.muni.fi.modelchecker

/**
 * A communication class responsible for delivering tasks to other processes.
 */
public interface TaskMessenger<N : Node, C : ColorSet<C>> {

        var taskListener: OnTaskListener<N, C>?

    /** Set task listener that will receive notifications about new tasks and prepare for communication.  */
        fun startSession()

        /** Cut off all communications and restore to indifferent state.  */
        fun closeSession()

        /**
         * Create new task request on destination node.
         * @param destinationProcess Id of a process where task request should be created.
         * *
         * @param internal Inner node that initiated the requests.
         * *
         * @param external Border node, destination of request.
         * *
         * @param colors Additional info about request colors.
         */
        fun sendTask(destinationProcess: Int, internal: N, external: N, colors: C)
}

/**
 * Listener that processes incoming tasks.
 */
public interface OnTaskListener<N : Node, C : ColorSet<C>> {

    /**
     * Called when task messenger receives a task from other process.
     * @param sourceProcess Id of task sender.
     * *
     * @param external Border node, origin of task request.
     * *
     * @param internal Inner node, target of task request.
     * *
     * @param colors Additional info about request colors.
     */
    fun onTask(sourceProcess: Int, external: N, internal: N, colors: C)

}


/**
 * Partial implementation of task messenger that relies on blocking receive operation.
 */
public abstract class BlockingTaskMessenger<N : Node, C : ColorSet<C>> : TaskMessenger<N, C> {

    private var listener: Thread? = null

    override var taskListener: OnTaskListener<N, C>? = null

    override fun startSession() {
        listener = Thread(object : Runnable {
            override fun run() {
                var hasNext = blockingReceiveTask(taskListener ?: throw IllegalStateException("No task listener present"))
                while (hasNext) {
                    hasNext = blockingReceiveTask(taskListener ?: throw IllegalStateException("No task listener present"))
                }
            }
        })
        listener!!.start()
    }

    override fun closeSession() {
        //send termination signal to listener thread
        finishSelf()
        if (listener == null) {
            throw IllegalStateException("Closing session on Task Messenger with no active session.")
        } else {
            listener!!.join()
        }
    }

    /**
     * Receive a new task and if such task is valid, notify the given task listener about this event.
     * This method should return false at least once in finite time after finishSelf has been called.
     * @param taskListener Listener that should be notified when new task is received.
     * *
     * @return True is task has been successfully received, false if finishSelf has been called or other error occurred.
     */
    protected abstract fun blockingReceiveTask(taskListener: OnTaskListener<N, C>): Boolean

    /**
     * Sends termination signal that will eventually result in false being returned from blockingReceiveTask.
     */
    protected abstract fun finishSelf()

}

