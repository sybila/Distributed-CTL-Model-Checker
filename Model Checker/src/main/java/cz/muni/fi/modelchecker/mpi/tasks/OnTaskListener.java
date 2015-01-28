package cz.muni.fi.modelchecker.mpi.tasks;

import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import org.antlr.v4.runtime.misc.NotNull;

/**
 * Listener that processes incoming tasks.
 */
public interface OnTaskListener<N extends Node, C extends ColorSet> {

    /**
     * Called when task messenger receives a task from other process.
     * @param sourceProcess Id of task sender.
     * @param external Border node, origin of task request.
     * @param internal Inner node, target of task request.
     * @param colors Additional info about request colors.
     */
    public void onTask(int sourceProcess, @NotNull N external, @NotNull N internal, @NotNull C colors);

}
