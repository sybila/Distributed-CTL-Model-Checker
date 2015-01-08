package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Verificator for exists until operator.
 */
public class ExistsUntilVerificator<N extends Node, C extends ColorSet> extends FormulaVerificator<N, C> {

    ExistsUntilVerificator(int myId, @NotNull ModelAdapter<N, C> model, @NotNull StateSpacePartitioner<N> partitioner, Formula formula) {
        super(myId, model, partitioner, formula);
    }

    @Override
    public void verifyLocalGraph() {
        Map<N, C> initialNodes = model.initialNodes(formula.getSubFormulaAt(1));
        System.out.println("Initial nodes: "+initialNodes.size());
        Queue<Map.Entry<N,C>> queue = new LinkedList<>();
        for (Map.Entry<N, C> initial : initialNodes.entrySet()) {
            if (model.addFormula(initial.getKey(), formula, initial.getValue())) {  //add formula to all initial nodes
                //process recursively only nodes that have been modified by previous change
                queue.add(initial);
            }
        }
        processExistsUntilQueue(queue);
    }

    @Override
    public void processTaskData(@NotNull N internal, @NotNull N external, @NotNull C candidates) {
        //intersect received colors with my colors in node,
        //if this is not empty and there are new colors, run back
        Queue<Map.Entry<N,C>> queue = new LinkedList<>();
        candidates.intersect(model.validColorsFor(internal, formula.getSubFormulaAt(0)));
        if (model.addFormula(internal, formula, candidates)) {
            queue.add(new AbstractMap.SimpleEntry<>(internal, candidates));
        }
        processExistsUntilQueue(queue);
    }


    private void processExistsUntilQueue(Queue<Map.Entry<N, C>> queue) {
        //examine all predecessors
        while (!queue.isEmpty()) {
            Map.Entry<N,C> inspected = queue.remove();
            for (Map.Entry<N, C> predecessor : model.predecessorsFor(inspected.getKey(), inspected.getValue()).entrySet()) {
                int owner = partitioner.getNodeOwner(predecessor.getKey());
                if (myId == owner) {
                    //if predecessor is mine, intersect colors where sub formula 0 holds and add them
                    //if addition has changed anything, proceed evaluation with reduced colors
                    C candidates = predecessor.getValue();
                    candidates.intersect(model.validColorsFor(predecessor.getKey(), formula.getSubFormulaAt(0)));
                    if (model.addFormula(predecessor.getKey(), formula, candidates)) {
                        queue.add(new AbstractMap.SimpleEntry<>(predecessor.getKey(), candidates));
                    }
                } else {
                    taskManager.dispatchTask(owner, inspected.getKey(), predecessor.getKey(), predecessor.getValue());
                }
            }
        }
    }
}
