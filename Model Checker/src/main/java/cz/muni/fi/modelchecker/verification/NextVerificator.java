package cz.muni.fi.modelchecker.verification;

import com.github.daemontus.jafra.Terminator;
import cz.muni.fi.ctl.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.tasks.OnTaskListener;
import cz.muni.fi.modelchecker.mpi.tasks.TaskMessenger;
import org.antlr.v4.runtime.misc.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * Verificator for next operator.
 */
class NextVerificator<N extends Node, C extends ColorSet> implements FormulaProcessor, OnTaskListener<N, C> {

    @NotNull
    private final Terminator.Factory terminatorFactory;
    @NotNull
    private final StateSpacePartitioner<N> partitioner;
    @NotNull
    private final ModelAdapter<N, C> model;
    @NotNull
    private final Formula formula;
    @NotNull
    private final TaskMessenger<N, C> taskMessenger;
    private final int myId;

    @Nullable
    private Terminator terminator;

    private boolean working;

    NextVerificator(
            @NotNull ModelAdapter<N, C> model,
            @NotNull StateSpacePartitioner<N> partitioner,
            @NotNull Formula formula,
            @NotNull Terminator.Factory terminatorFactory,
            @NotNull TaskMessenger<N,C> taskMessenger
    ) {
        this.model = model;
        this.partitioner = partitioner;
        this.formula = formula;
        this.terminatorFactory = terminatorFactory;
        this.taskMessenger = taskMessenger;
        this.myId = partitioner.getMyId();
    }

    @Override
    public void verify() {
        working = true;
        terminator = terminatorFactory.createNew();
        //start listening for external tasks
        taskMessenger.startSession(this);

        //find all nodes that match sub formula
        Set<Map.Entry<N, C>> entries;
        entries = model.initialNodes(formula.get(0)).entrySet();

        for (@NotNull Map.Entry<N, C> initial : entries) {
            //for each node, find it's predecessors
            Set<Map.Entry<N, C>> predecessors;
            predecessors = model.predecessorsFor(initial.getKey(), initial.getValue()).entrySet();

            for (@NotNull Map.Entry<N, C> predecessor : predecessors) {
                //no need to intersect colors, since predecessorsFor respects color bounds
                int owner = partitioner.getNodeOwner(predecessor.getKey());
                if (owner == myId) {
                    model.addFormula(predecessor.getKey(), formula, predecessor.getValue());
                } else {
                    terminator.messageSent();
                    taskMessenger.sendTask(owner, initial.getKey(), predecessor.getKey(), predecessor.getValue());
                }
            }

        }

        //wait for all tasks to finish
        working = false;
        terminator.setDone();
        terminator.waitForTermination();

        terminator = terminatorFactory.createNew();
        //stop listening for tasks - termination detection ensures all processes close session before opening a new one in next round.
        taskMessenger.closeSession();
        terminator.setDone();
        terminator.waitForTermination();

    }

    @Override
    public void onTask(int sourceProcess, N external, @NotNull N internal, @NotNull C colors) {
        //just add this and ignore rest - if everything is correct,
        //this should be called only with colorSets that have been
        //correctly reduced and send to us by other nodes
        synchronized (this) {   //must be synchronized in order to prevent main thread from marking terminator as finished
            terminator.messageReceived();
            model.addFormula(internal, formula, colors);
            if (!working) { //if main work is done, notify terminator we are cool
                terminator.setDone();
            }
        }
    }
}
