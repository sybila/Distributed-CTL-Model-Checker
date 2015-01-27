package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Verificator for or operator.
 */
public class OrVerificator<N extends Node, C extends ColorSet> implements FormulaProcessor {

    private ModelAdapter<N, C> model;
    private Formula formula;

    OrVerificator(@NotNull ModelAdapter<N, C> model, Formula formula) {
        this.model = model;
        this.formula = formula;
    }

    @Override
    public void verify() {
        for (Map.Entry<N, C> entry : model.initialNodes(formula.getSubFormulaAt(0)).entrySet()) {
            model.addFormula(entry.getKey(), formula, entry.getValue());
        }
        for (Map.Entry<N, C> entry : model.initialNodes(formula.getSubFormulaAt(1)).entrySet()) {
            model.addFormula(entry.getKey(), formula, entry.getValue());
        }
    }
}
