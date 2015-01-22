package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.termination.Terminator;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * Verificator for next operator.
 */
public class NextVerificator<N extends Node, C extends ColorSet> extends FormulaVerificator<N, C> {

    NextVerificator(int myId, @NotNull ModelAdapter<N, C> model, @NotNull StateSpacePartitioner<N> partitioner, Formula formula, Terminator terminator) {
        super(myId, model, partitioner, formula, terminator);
    }

    @Override
    public void verifyLocalGraph() {
        //find all nodes that match sub formula
        Set<Map.Entry<N, C>> entries;
        synchronized (this) {
            entries = model.initialNodes(formula.getSubFormulaAt(0)).entrySet();
        }
        for (Map.Entry<N, C> initial : entries) {
            //for each node, find it's predecessors
            Set<Map.Entry<N, C>> predecessors;
            synchronized (this) {
                predecessors = model.predecessorsFor(initial.getKey(), initial.getValue()).entrySet();
            }
            for (Map.Entry<N, C> predecessor : predecessors) {
                //no need to intersect colors, since predecessorsFor respects color bounds
                int owner = partitioner.getNodeOwner(predecessor.getKey());
                if (owner == myId) {
                    synchronized (this) {
                        model.addFormula(predecessor.getKey(), formula, predecessor.getValue());
                    }
                } else {
                    dispatchTask(owner, initial.getKey(), predecessor.getKey(), predecessor.getValue());
                }
            }
        }
    }

    @Override
    public void  processTaskData(@NotNull N internal, @NotNull N external, @NotNull C candidates) {
        //just add this and ignore rest - if everything is correct,
        //this should be called only with colorSets that have been
        //correctly reduced and send to us by other nodes
        synchronized (this) {
            model.addFormula(internal, formula, candidates);
        }
    }
}
