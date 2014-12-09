package cz.muni.fi.distributed.checker;

import cz.muni.fi.distributed.graph.Node;
import cz.muni.fi.model.ColorSet;

/**
 * Created by daemontus on 24/11/14.
 */
public interface TaskDispatcher {
    public void dispatchNewTask(int parentTask, int destination, Node source, Node dest, ColorSet activeColors);
    public boolean hasIncompleteRequests();
    public void onLocalTaskFinished(Task task);
    public void onRemoteTaskFinished(int parentTask);
}
