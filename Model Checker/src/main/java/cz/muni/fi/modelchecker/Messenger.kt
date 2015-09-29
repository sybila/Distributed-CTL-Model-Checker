package cz.muni.fi.modelchecker

public interface Messenger<N: Node, C: Colors<C>> {

    fun sendTask(task: Job<N, C>)

    fun close()

    public interface Factory<N: Node, C: Colors<C>> {
        fun createNew( onTask: (Job<N, C>) -> Unit ): Messenger<N, C>
    }
}

sealed class Job<N: Node, C: Colors<C>> {
    data class AUTask<N: Node, C: Colors<C>>(
            val initNode: N,
            val targetNode: N,
            val colors: C
    ) : Job<N, C>()
    data class EUTask<N: Node, C: Colors<C>>(
            val node: N,
            val colors: C
    ) : Job<N, C>()
    class Poison<N: Node, C: Colors<C>>() : Job<N, C>()
}