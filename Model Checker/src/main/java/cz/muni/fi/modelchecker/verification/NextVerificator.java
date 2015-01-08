package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Verificator for next operator.
 */
public class NextVerificator<N extends Node, C extends ColorSet> extends FormulaVerificator<N, C> {

    NextVerificator(int myId, @NotNull ModelAdapter<N, C> model, @NotNull StateSpacePartitioner<N> partitioner, Formula formula) {
        super(myId, model, partitioner, formula);
    }

    @Override
    public void verifyLocalGraph() {
        //find all nodes that match sub formula
        for (Map.Entry<N, C> initial : model.initialNodes(formula.getSubFormulaAt(0)).entrySet()) {
            //for each node, find it's predecessors
            for (Map.Entry<N, C> predecessor : model.predecessorsFor(initial.getKey(), initial.getValue()).entrySet()) {
                //no need to intersect colors, since predecessorsFor respects color bounds
                int owner = partitioner.getNodeOwner(predecessor.getKey());
                if (owner == myId) {
                    model.addFormula(predecessor.getKey(), formula, predecessor.getValue());
                } else {
                    taskManager.dispatchTask(owner, initial.getKey(), predecessor.getKey(), predecessor.getValue());
                }
            }
        }
    }

    @Override
    public void processTaskData(@NotNull N internal, @NotNull N external, @NotNull C candidates) {
        //just add this and ignore rest - if everything is correct,
        //this should be called only with colorSets that have been
        //correctly reduced and send to us by other nodes
        model.addFormula(internal, formula, candidates);
    }
}
