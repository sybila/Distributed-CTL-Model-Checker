package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Verificator for negation operator.
 */
public class NegationVerificator<N extends Node, C extends ColorSet> implements FormulaProcessor {

    private ModelAdapter<N, C> model;
    private Formula formula;

    NegationVerificator(@NotNull ModelAdapter<N, C> model, Formula formula) {
        this.formula = formula;
        this.model = model;
    }

    @Override
    public void verify() {
        Map<N, C> nodes = model.initialNodes(formula.getSubFormulaAt(0));
        Map<N, C> inversion = model.invertNodeSet(nodes);
        for (Map.Entry<N, C> entry : inversion.entrySet()) {
            model.addFormula(entry.getKey(), formula, entry.getValue());
        }
    }
}
