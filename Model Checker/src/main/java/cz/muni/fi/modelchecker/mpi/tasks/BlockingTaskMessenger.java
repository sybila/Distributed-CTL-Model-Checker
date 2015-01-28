package cz.muni.fi.modelchecker.mpi.tasks;

import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Partial implementation of task messenger that relies on blocking receive operation.
 */
public abstract class BlockingTaskMessenger<N extends Node, C extends ColorSet> implements TaskMessenger<N, C> {

    @Nullable
    private Thread listener;

    @Nullable
    private OnTaskListener<N, C> callback;

    @Override
    public void startSession(@NotNull OnTaskListener<N, C> taskListener) {
        this.callback = taskListener;
        listener = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean hasNext = blockingReceiveTask(callback);
                while (hasNext) {
                    hasNext = blockingReceiveTask(callback);
                }
            }
        });
        listener.start();
    }

    @Override
    public void closeSession() {
        //send termination signal to listener thread
        finishSelf();
        try {
            if (listener == null) {
                throw new IllegalStateException("Closing session on Task Messenger with no active session.");
            } else {
                listener.join();
            }
        } catch (InterruptedException e) {
            //OK
        }
    }

    /**
     * Receive a new task and if such task is valid, notify the given task listener about this event.
     * This method should return false at least once in finite time after finishSelf has been called.
     * @param taskListener Listener that should be notified when new task is received.
     * @return True is task has been successfully received, false if finishSelf has been called or other error occurred.
     */
    protected abstract boolean blockingReceiveTask(@NotNull OnTaskListener<N, C> taskListener);

    /**
     * Sends termination signal that will eventually result in false being returned from blockingReceiveTask.
     */
    protected abstract void finishSelf();

}
