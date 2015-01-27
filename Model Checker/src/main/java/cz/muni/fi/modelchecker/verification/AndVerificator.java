package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Verificator for and operator.
 */
public class AndVerificator<N extends Node, C extends ColorSet> implements FormulaProcessor {

    private ModelAdapter<N, C> model;
    private Formula formula;

    AndVerificator(@NotNull ModelAdapter<N, C> model, Formula formula) {
        this.model = model;
        this.formula = formula;
    }

    @Override
    public void verify() {
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
}
