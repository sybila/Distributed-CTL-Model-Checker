package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.termination.Terminator;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Verificator for negation operator.
 */
public class NegationVerificator<N extends Node, C extends ColorSet> extends FormulaVerificator<N, C> {

    NegationVerificator(int myId, @NotNull ModelAdapter<N, C> model, @NotNull StateSpacePartitioner<N> partitioner, Formula formula, Terminator terminator) {
        super(myId, model, partitioner, formula, terminator);
    }

    @Override
    public void verifyLocalGraph() {
        Map<N, C> nodes = model.initialNodes(formula.getSubFormulaAt(0));
        Map<N, C> inversion = model.invertNodeSet(nodes);
        for (Map.Entry<N, C> entry : inversion.entrySet()) {
            model.addFormula(entry.getKey(), formula, entry.getValue());
        }
    }

    @Override
    public void processTaskData(@NotNull N internal, @NotNull N external, @NotNull C candidates) {
        throw new UnsupportedOperationException("This should not happen");
    }
}
