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
 * Verificator for exists until operator.
 */
public class ExistsUntilVerificator<N extends Node, C extends ColorSet> implements FormulaProcessor, OnTaskListener<N, C> {


    private final Terminator.TerminatorFactory terminatorFactory;
    private final StateSpacePartitioner<N> partitioner;
    private final ModelAdapter<N, C> model;
    private final Formula formula;
    private final TaskMessenger<N, C> taskMessenger;
    private final int myId;

    private Terminator terminator;

    private final Map<N,C> queue = new HashMap<>();
    private boolean terminated;

    ExistsUntilVerificator(
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

    @Override
    public void verify() {
        terminated = false;
        terminator = terminatorFactory.createNew();
        taskMessenger.startSession(this);

        //fill queue with initial nodes
        Map<N, C> initialNodes = model.initialNodes(formula.getSubFormulaAt(1));
        System.out.println("Initial nodes: "+initialNodes.size());
        for (Map.Entry<N, C> initial : initialNodes.entrySet()) {
            if (model.addFormula(initial.getKey(), formula, initial.getValue())) {  //add formula to all initial nodes
                //process only nodes that have been modified by previous change
                addToQueue(initial.getKey(), initial.getValue());
            }
        }

        //start worker thread to process all queue entries
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!terminated) {
                    processExistsUntilQueue();
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
            //intersect received colors with my colors in node,
            //if this is not empty and there are new colors, run back
            candidates.intersect(model.validColorsFor(internal, formula.getSubFormulaAt(0)));
            if (model.addFormula(internal, formula, candidates)) {
                addToQueue(internal, candidates);
            }
            queue.notify();
            terminator.messageReceived();
        }
    }

    /** Process all items available in the task queue */
    private void processExistsUntilQueue() {
        //examine all predecessors
        while (!queue.isEmpty()) {
            Map.Entry<N,C> inspected;
            synchronized (queue) {
                inspected = queue.entrySet().iterator().next();
                queue.remove(inspected.getKey());
            }
            for (Map.Entry<N, C> predecessor : model.predecessorsFor(inspected.getKey(), inspected.getValue()).entrySet()) {
                int owner = partitioner.getNodeOwner(predecessor.getKey());
                if (myId == owner) {
                    //if predecessor is mine, intersect colors where sub formula 0 holds and add them
                    //if addition has changed anything, proceed evaluation with reduced colors
                    C candidates = predecessor.getValue();
                    candidates.intersect(model.validColorsFor(predecessor.getKey(), formula.getSubFormulaAt(0)));
                    if (model.addFormula(predecessor.getKey(), formula, candidates)) {
                        addToQueue(predecessor.getKey(), candidates);
                    }
                } else {
                    terminator.messageSent();
                    taskMessenger.sendTask(owner, inspected.getKey(), predecessor.getKey(), predecessor.getValue());
                }
            }
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
