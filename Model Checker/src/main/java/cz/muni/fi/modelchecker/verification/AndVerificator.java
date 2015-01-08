package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Verificator for and operator.
 */
public class AndVerificator<N extends Node, C extends ColorSet> extends FormulaVerificator<N, C> {

    AndVerificator(int myId, @NotNull ModelAdapter<N, C> model, @NotNull StateSpacePartitioner<N> partitioner, Formula formula) {
        super(myId, model, partitioner, formula);
    }

    @Override
    public void verifyLocalGraph() {
        Map<N, C> first = model.initialNodes(formula.getSubFormulaAt(0));
        Map<N, C> second = model.initialNodes(formula.getSubFormulaAt(1));
        //intersect node sets
        for (Map.Entry<N, C> entry : first.entrySet()) {
            C colorSet = second.get(entry.getKey());
            if (colorSet != null) {
                colorSet.intersect(entry.getValue());
                model.addFormula(entry.getKey(), formula, colorSet);
            }
        }
    }

    @Override
    public void processTaskData(@NotNull N internal, @NotNull N external, @NotNull C candidates) {
        throw new UnsupportedOperationException("This should not happen");
    }
}
