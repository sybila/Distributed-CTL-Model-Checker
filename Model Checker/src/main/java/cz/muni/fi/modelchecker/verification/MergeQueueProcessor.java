package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.ModelChecker;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.tasks.OnTaskListener;
import cz.muni.fi.modelchecker.mpi.tasks.TaskMessenger;
import cz.muni.fi.modelchecker.mpi.termination.Terminator;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Common functionality of the until operator processors.
 */
abstract class MergeQueueProcessor<N extends Node, C extends ColorSet> implements FormulaProcessor, OnTaskListener<N,C> {

    @NotNull
    private final Terminator.TerminatorFactory terminatorFactory;
    @NotNull
    final StateSpacePartitioner<N> partitioner;
    @NotNull
    final ModelAdapter<N, C> model;
    @NotNull
    final Formula formula;
    @NotNull
    final TaskMessenger<N, C> taskMessenger;

    final int myId;

    Terminator terminator;

    final Map<N,C> queue = new HashMap<>();
    private boolean terminated;

    MergeQueueProcessor(
            @NotNull ModelAdapter<N, C> model,
            @NotNull StateSpacePartitioner<N> partitioner,
            @NotNull Formula formula,
            @NotNull Terminator.TerminatorFactory terminatorFactory,
            @NotNull TaskMessenger<N, C> taskMessenger
    ) {
        this.terminatorFactory = terminatorFactory;
        this.partitioner = partitioner;
        this.model = model;
        this.formula = formula;
        this.taskMessenger = taskMessenger;
        this.myId = partitioner.getMyId();
    }

    @Override
    public void verify() {
        terminated = false;
        terminator = terminatorFactory.createNew();
        taskMessenger.startSession(this);

        prepareQueue();

        //start worker thread to process all queue entries
        @NotNull Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!terminated) {
                    processQueue();
                    synchronized (queue) {
                        if (queue.isEmpty() && !terminated) {
                            try {
                                terminator.setDone();
                                queue.wait();
                            } catch (InterruptedException e) {
                                //OK?
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });
        worker.start();

        //wait for the work to finish
        terminator.waitForTermination();
        //finalize worker thread and task messenger session
        terminator = terminatorFactory.createNew();
        taskMessenger.closeSession();
        synchronized (queue) {
            terminated = true;
            queue.notify();
        }
        try {
            worker.join();
        } catch (InterruptedException e) {
            //OK?
            e.printStackTrace();
        }
        terminator.setDone();
        terminator.waitForTermination();
    }

    /** Add data to the "merge queue" */
    void addToQueue(N node, C colors) {
        synchronized (queue) {
            if (queue.containsKey(node)) {
                ModelChecker.merged += 1;
                queue.get(node).union(colors);
            } else {
                queue.put(node, colors);
            }
        }
    }

    /** Load initial data into the queue */
    protected abstract void prepareQueue();

    /** Process all data stored in the queue */
    protected abstract void processQueue();

}
