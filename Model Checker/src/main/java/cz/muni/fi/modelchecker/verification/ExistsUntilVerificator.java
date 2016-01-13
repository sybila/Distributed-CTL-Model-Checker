package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.tasks.TaskMessenger;
import cz.muni.fi.modelchecker.mpi.termination.Terminator;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Verificator for exists until operator.
 */
class ExistsUntilVerificator<N extends Node, C extends ColorSet> extends MergeQueueProcessor<N, C> {

    ExistsUntilVerificator(
            @NotNull ModelAdapter<N, C> model,
            @NotNull StateSpacePartitioner<N> partitioner,
            @NotNull Formula formula,
            @NotNull Terminator.TerminatorFactory terminatorFactory,
            @NotNull TaskMessenger<N, C> taskMessenger
    ) {
        super(model, partitioner, formula, terminatorFactory, taskMessenger);
    }

    @Override
    protected void prepareQueue() {
        //fill queue with initial nodes
        @NotNull Map<N, C> initialNodes = model.initialNodes(formula.getSubFormulaAt(1));
        System.out.println("Initial nodes: "+initialNodes.size());
        for (@NotNull Map.Entry<N, C> initial : initialNodes.entrySet()) {
            if (model.addFormula(initial.getKey(), formula, initial.getValue())) {  //add formula to all initial nodes
                //process only nodes that have been modified by previous change
                addToQueue(initial.getKey(), initial.getValue());
            }
        }
    }

    @Override
    protected void processQueue() {
        //examine all predecessors
        while (!queue.isEmpty()) {
            Map.Entry<N,C> inspected;
            synchronized (queue) {
                inspected = queue.entrySet().iterator().next();
                System.out.print("\rQueue size: "+queue.size());
                queue.remove(inspected.getKey());
            }
            for (@NotNull Map.Entry<N, C> predecessor : model.predecessorsFor(inspected.getKey(), inspected.getValue()).entrySet()) {
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

    @Override
    public void onTask(int sourceProcess, N external, @NotNull N internal, @NotNull C candidates) {
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

}
