package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.termination.Terminator;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Verificator for exists until operator.
 */
public class ExistsUntilVerificator<N extends Node, C extends ColorSet> extends FormulaVerificator<N, C> {

    //private final Queue<Map.Entry<N,C>> queue = new LinkedList<>();
    private final Map<N,C> queue = new HashMap<>();
    private Thread worker;
    private boolean running;

    ExistsUntilVerificator(int myId, @NotNull ModelAdapter<N, C> model, @NotNull StateSpacePartitioner<N> partitioner, Formula formula, Terminator terminator) {
        super(myId, model, partitioner, formula, terminator);
    }

    @Override
    public void verifyLocalGraph() {
        Map<N, C> initialNodes = model.initialNodes(formula.getSubFormulaAt(1));
        System.out.println("Initial nodes: "+initialNodes.size());
        for (Map.Entry<N, C> initial : initialNodes.entrySet()) {
            if (model.addFormula(initial.getKey(), formula, initial.getValue())) {  //add formula to all initial nodes
                //process recursively only nodes that have been modified by previous change
                addToQueue(initial.getKey(), initial.getValue());
                /*synchronized (queue) {
                    queue.add(initial);
                }*/
            }
        }
        if (!queue.isEmpty()) {
            terminator.setWorking(true);
        }
        running = true;
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    terminator.setWorking(true);
                    processExistsUntilQueue();
                 //   System.out.println("Main queue done: "+maxSize);
                    synchronized (queue) {
                        if (queue.isEmpty() && running) {
                            try {
                                terminator.setWorking(false);
                                queue.wait();
                            } catch (InterruptedException e) {
                                //OK?
                                e.printStackTrace();
                            }
                        }
                    }
                }
                //terminator.setWorking(false);
            }
        });
        worker.start();
    }

    @Override
    public void processTaskData(@NotNull N internal, @NotNull N external, @NotNull C candidates) {
        synchronized (queue) {  //must be synchronized because we are accessing model
            //intersect received colors with my colors in node,
            //if this is not empty and there are new colors, run back
            candidates.intersect(model.validColorsFor(internal, formula.getSubFormulaAt(0)));
            if (model.addFormula(internal, formula, candidates)) {
            /*synchronized (queue) {
                queue.add(new AbstractMap.SimpleEntry<>(internal, candidates));
            }*/
                addToQueue(internal, candidates);
            }
            queue.notify();
        }
        terminator.messageReceived();
    }

    @Override
    public void finishSelf() {
        super.finishSelf();
        System.out.println("Finish self");
        try {
            synchronized (queue) {
                running = false;
                queue.notify();
            }
            worker.join();
        } catch (InterruptedException e) {
            //OK
            e.printStackTrace();
        }
    }

    private void addToQueue(N node, C colors) {
        synchronized (queue) {
            if (queue.containsKey(node)) {
                queue.get(node).union(colors);
            } else {
                queue.put(node, colors);
            }
        }
    }

    private void processExistsUntilQueue() {
        //examine all predecessors
        while (!queue.isEmpty()) {
            Map.Entry<N,C> inspected;
            synchronized (queue) {
                //inspected = queue.remove();
                inspected = queue.entrySet().iterator().next();
                queue.remove(inspected.getKey());
               // System.out.println("Get");
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
                       /* synchronized (queue) {
                            queue.add(new AbstractMap.SimpleEntry<N, C>(predecessor.getKey(), candidates));
                        }*/
                    }
                } else {
                    dispatchTask(owner, inspected.getKey(), predecessor.getKey(), predecessor.getValue());
                }
            }
        }
    }
}
