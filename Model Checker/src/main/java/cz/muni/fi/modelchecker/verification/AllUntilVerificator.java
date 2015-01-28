package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
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
 * Verificator for all until operator.
 */
public class AllUntilVerificator<N extends Node, C extends ColorSet> implements FormulaProcessor, OnTaskListener<N, C> {

    private final Map<N, Map<N, C>> successorsAndUncoveredColors = new HashMap<>();


    private final Terminator.TerminatorFactory terminatorFactory;
    private final StateSpacePartitioner<N> partitioner;
    private final ModelAdapter<N, C> model;
    private final Formula formula;
    private final TaskMessenger<N, C> taskMessenger;
    private final int myId;

    private Terminator terminator;

    private final Map<N,C> queue = new HashMap<>();
    private boolean terminated;

    AllUntilVerificator(
            @NotNull ModelAdapter<N, C> model,
            @NotNull StateSpacePartitioner<N> partitioner,
            Formula formula,
            Terminator.TerminatorFactory terminatorFactory,
            TaskMessenger<N, C> taskMessenger
    ) {
        this.terminatorFactory = terminatorFactory;
        this.partitioner = partitioner;
        this.model = model;
        this.formula = formula;
        this.taskMessenger = taskMessenger;
        this.myId = partitioner.getMyId();
    }

    private void processAllUntilQueue() {
        while (!queue.isEmpty()) {
            Map.Entry<N,C> inspected;
            synchronized (queue) {
                inspected = queue.entrySet().iterator().next();
                queue.remove(inspected.getKey());
            }
            //go through all predecessors of an inspected node and
            //1) push new color to them through given edge
            //2) test if the AU actually holds now in the predecessor, if so, enqueue him for further inspection
            for (Map.Entry<N,C> predecessor : model.predecessorsFor(inspected.getKey(), inspected.getValue()).entrySet()) {
                int owner = partitioner.getNodeOwner(predecessor.getKey());
                if (myId == owner) {
                    processAllUntilNode(inspected.getKey(), predecessor.getKey(), predecessor.getValue());
                } else {
                    terminator.messageSent();
                    taskMessenger.sendTask(owner, inspected.getKey(), predecessor.getKey(), predecessor.getValue());
                }
            }
        }
    }

    //candidates represents colors that are pushed down to predecessor node but only through given edge
    //only one node can be processed at a time, because we need to synchronize access to successors
    private void processAllUntilNode(N inspected, N predecessor, C candidates) {
        synchronized (queue) {  //bit overkill, but meh
            //if local successor cache does not contain given node, we have to compute the successors first
            if (!successorsAndUncoveredColors.containsKey(predecessor)) {
                successorsAndUncoveredColors.put(predecessor, model.successorsFor(predecessor, null));
            }
            Map<N, C> predecessorsSuccessors = successorsAndUncoveredColors.get(predecessor);
            //subtract colors pushed from inspected node from colors on edge between him and his predecessor
            C inspectedEdgeColors = predecessorsSuccessors.get(inspected);
            if (inspectedEdgeColors != null) {  //can be null in case of a self loop on a node
                inspectedEdgeColors.subtract(candidates);
                //if the whole edge is now covered, remove it from uncovered colors
                if (inspectedEdgeColors.isEmpty()) {
                    predecessorsSuccessors.remove(inspected);
                }
            }
            //go through all edges from predecessor and subtract uncovered colors from our candidate
            for (C uncoveredEdgeColors : predecessorsSuccessors.values()) {
                candidates.subtract(uncoveredEdgeColors);
                if (candidates.isEmpty()) { //if candidate is already empty, just stop
                    break;
                }
            }
            //if there are colors covered by every edge, intersect them
            //with valid colors for sub formula 0, add them as valid and enqueue them for inspection
            if (!candidates.isEmpty()) {
                candidates.intersect(model.validColorsFor(predecessor, formula.getSubFormulaAt(0)));
                if (model.addFormula(predecessor, formula, candidates)) {
                    addToQueue(predecessor, candidates);
                }
            }
        }
    }

    @Override
    public void verify() {
        terminated = false;
        terminator = terminatorFactory.createNew();
        taskMessenger.startSession(this);

        //enqueue all nodes where second formula holds, but do not mark them as true yet
        for(Map.Entry<N, C> entry : model.initialNodes(formula.getSubFormulaAt(1)).entrySet()) {
           addToQueue(entry.getKey(), entry.getValue());
        }

        //start worker thread to process all queue entries
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!terminated) {
                    processAllUntilQueue();
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

    @Override
    public void onTask(int sourceProcess, N external, N internal, C candidates) {
        synchronized (queue) {  //must be synchronized because we are accessing model
            processAllUntilNode(external, internal, candidates);
            queue.notify();
            terminator.messageReceived();
        }
    }

    /** Add data to the "merge queue" */
    private void addToQueue(N node, C colors) {
        synchronized (queue) {
            if (queue.containsKey(node)) {
                queue.get(node).union(colors);
            } else {
                queue.put(node, colors);
            }
        }
    }
}
