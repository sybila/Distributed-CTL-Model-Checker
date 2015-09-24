package cz.muni.fi.modelchecker

public interface Messenger<N: Node, C: Colors<C>> {

    fun sendTask(task: Task<N, C>)

    public interface Factory<N: Node, C: Colors<C>> {
        fun createNew( onTask: (Task<N, C>) -> Unit ): Messenger<N, C>
    }
}

data class Task<N: Node, C: Colors<C>>(
        val initNode: N,
        val targetNode: N,
        val colors: C
)