package cz.muni.fi.modelchecker.mpi.tasks;

import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;

/**
 * A communication class responsible for delivering tasks to other processes.
 */
public interface TaskMessenger<N extends Node, C extends ColorSet> {

    /** Set task listener that will receive notifications about new tasks and prepare for communication. */
    public void startSession(@NotNull OnTaskListener<N,C> taskListener);

    /** Cut off all communications and restore to indifferent state. */
    public void closeSession();

    /**
     * Create new task request on destination node.
     * @param destinationProcess Id of a process where task request should be created.
     * @param internal Inner node that initiated the requests.
     * @param external Border node, destination of request.
     * @param colors Additional info about request colors.
     */
    public void sendTask(int destinationProcess, @NotNull N internal, @NotNull N external, @NotNull C colors);
}
