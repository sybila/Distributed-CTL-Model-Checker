package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Verificator for or operator.
 */
public class OrVerificator<N extends Node, C extends ColorSet> extends FormulaVerificator<N, C> {

    OrVerificator(int myId, @NotNull ModelAdapter<N, C> model, @NotNull StateSpacePartitioner<N> partitioner, Formula formula) {
        super(myId, model, partitioner, formula);
    }

    @Override
    public void verifyLocalGraph() {
        for (Map.Entry<N, C> entry : model.initialNodes(formula.getSubFormulaAt(0)).entrySet()) {
            model.addFormula(entry.getKey(), formula, entry.getValue());
        }
        for (Map.Entry<N, C> entry : model.initialNodes(formula.getSubFormulaAt(1)).entrySet()) {
            model.addFormula(entry.getKey(), formula, entry.getValue());
        }
    }

    @Override
    public void processTaskData(@NotNull N internal, @NotNull N external, @NotNull C candidates) {
        throw new UnsupportedOperationException("This should not happen");
    }
}
