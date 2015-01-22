package cz.muni.fi.modelchecker.mpi;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.verification.FormulaVerificator;
import cz.muni.fi.modelchecker.verification.FormulaVerificatorFactory;
import mpi.Comm;

/**
 * Manages creation of tasks requested from other network nodes.
 */
public abstract class TaskManager<N extends Node, C extends ColorSet> {

    private final FormulaVerificator<N, C> verificator;

    public TaskManager(FormulaVerificator<N, C> verificator) {
        this.verificator = verificator;
        verificator.bindTaskManager(this);
    }

    public void startProcessing() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean hasTask = true;
                while (hasTask) {
                    hasTask = tryReceivingTask(new TaskStarter<N, C>() {
                        @Override
                        public void startLocalTask(int sourceNode, N external, N internal, C colors) {
                            TaskManager.this.startLocalTask(sourceNode, external, internal, colors);
                        }
                    });
                }
            }
        }).start();
        verificator.verifyLocalGraph();
    }

    public final void dispatchTask(int destinationNode, N internal, N external, C colors) {
        sendTask(destinationNode, internal, external, colors);
    }

    private void startLocalTask(int sourceNode, final N external, final N internal, final C colors) {
        verificator.processTaskData(internal, external, colors);
    }

    /**
     * Sends provided arguments using MPI.COMM_WORLD to its destination.
     * @param destinationNode index of destination node.
     * @param internal local node (initiator of message)
     * @param external external node (receiver of message)
     * @param colors color set bounding message relevance
     */
    protected abstract void sendTask(int destinationNode, N internal, N external, C colors);

    /**
     * Tries to receive a full task information from sendTask. If a task has been received, returns true.
     * If data was invalid or termination message has been received, return false.
     * @return True if task has been received, false otherwise.
     */
    protected abstract boolean tryReceivingTask(TaskStarter<N, C> taskStarter);

    /**
     * Sends a message that would cause a negative result from tyeReceivingTask method.
     */
    public void finishSelf() {
        verificator.finishSelf();
    }

    protected interface TaskStarter<N extends Node, C extends ColorSet> {
        public void startLocalTask(int sourceNode, N external, N internal, C colors);
    }

    public static abstract class TaskManagerFactory<T extends Node, V extends ColorSet> {

        protected final FormulaVerificatorFactory<T, V> verificatorFactory;
        protected final Comm COMM;

        protected TaskManagerFactory(FormulaVerificatorFactory<T, V> verificatorFactory, Comm comm) {
            this.verificatorFactory = verificatorFactory;
            COMM = comm;
        }

        public abstract TaskManager<T, V> createTaskManager(Formula formula);
    }
}
