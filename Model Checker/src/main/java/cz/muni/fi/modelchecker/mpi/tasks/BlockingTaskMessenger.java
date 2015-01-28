package cz.muni.fi.modelchecker.mpi.tasks;

import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;

public abstract class BlockingTaskMessenger<N extends Node, C extends ColorSet> implements TaskMessenger<N, C> {

    private Thread listener;
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
        finishSelf();
        try {
            listener.join();
        } catch (InterruptedException e) {
            //OK
        }
    }

    protected abstract boolean blockingReceiveTask(OnTaskListener<N, C> taskListener);

    protected abstract void finishSelf();

}
