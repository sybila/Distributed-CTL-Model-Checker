package cz.muni.fi.modelchecker.mpi;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.FormulaVerificator;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.termination.Terminator;
import mpi.Comm;

/**
 * Manages creation of tasks requested from other network nodes.
 */
public abstract class TaskManager<T extends Node, V extends ColorSet> {

    private final Object COUNT_LOCK = new Object();

    private int activeTasks = 0;

    private final Formula activeFormula;
    private final Terminator terminator;
    private final FormulaVerificator<T, V> verificator;

    public TaskManager(Formula activeFormula, Terminator terminator, FormulaVerificator<T, V> verificator) {
        this.activeFormula = activeFormula;
        this.terminator = terminator;
        this.verificator = verificator;
    }

    public void startListening() {
        new Thread(() -> {
            boolean hasTask = true;
            while (hasTask) {
                hasTask = tryReceivingTask(this::startLocalTask);
            }
        }).start();
    }

    public final void dispatchTask(int destinationNode, T internal, T external, V colors) {
        terminator.messageSent();
        sendTask(destinationNode, internal, external, colors);
    }

    private void startLocalTask(int sourceNode, T external, T internal, V colors) {
        synchronized (COUNT_LOCK) {
            terminator.messageReceived();
            activeTasks++;
            if (activeTasks == 1) terminator.setWorking(true);
        }
        new Thread(() -> {
            verificator.processFormula(activeFormula, internal, colors);
            synchronized (COUNT_LOCK) {
                activeTasks--;
                if (activeTasks == 0) terminator.setWorking(false);
            }
        }).start();
    }

    /**
     * Sends provided arguments using MPI.COMM_WORLD to its destination.
     * @param destinationNode index of destination node.
     * @param internal local node (initiator of message)
     * @param external external node (receiver of message)
     * @param colors color set bounding message relevance
     */
    protected abstract void sendTask(int destinationNode, T internal, T external, V colors);

    /**
     * Tries to receive a full task information from sendTask. If a task has been received, returns true.
     * If data was invalid or termination message has been received, return false.
     * @return True if task has been received, false otherwise.
     */
    protected abstract boolean tryReceivingTask(TaskStarter<T, V> taskStarter);

    /**
     * Sends a message that would cause a negative result from tyeReceivingTask method.
     */
    public abstract void finishSelf();

    protected interface TaskStarter<N extends Node, C extends ColorSet> {
        public void startLocalTask(int sourceNode, N external, N internal, C colors);
    }

    public interface TaskManagerFactory<N extends Node, C extends ColorSet> {
        public TaskManager<N, C> createTaskManager(Formula formula, Terminator terminator, FormulaVerificator<N,C> verificator, Comm comm);
    }
}
