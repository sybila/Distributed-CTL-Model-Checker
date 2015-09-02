package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.termination.Terminator;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Verificator for or operator.
 */
class OrVerificator<N extends Node, C extends ColorSet> extends StaticProcessor<N, C> {

    OrVerificator(@NotNull ModelAdapter<N, C> model, @NotNull Formula formula, @NotNull Terminator terminator) {
        super(model, formula, terminator);
    }

    @Override
    protected void processModel() {
        for (@NotNull Map.Entry<N, C> entry : model.initialNodes(formula.get(0)).entrySet()) {
            model.addFormula(entry.getKey(), formula, entry.getValue());
        }
        for (@NotNull Map.Entry<N, C> entry : model.initialNodes(formula.get(1)).entrySet()) {
            model.addFormula(entry.getKey(), formula, entry.getValue());
        }
    }

}
