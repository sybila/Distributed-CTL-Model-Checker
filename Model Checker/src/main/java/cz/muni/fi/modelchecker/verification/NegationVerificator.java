package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.termination.Terminator;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Verificator for negation operator.
 */
class NegationVerificator<N extends Node, C extends ColorSet> extends StaticProcessor<N, C> {

    NegationVerificator(@NotNull ModelAdapter<N, C> model, @NotNull Formula formula, @NotNull Terminator terminator) {
        super(model, formula, terminator);
    }

    @Override
    protected void processModel() {
        @NotNull Map<N, C> nodes = model.initialNodes(formula.get(0));
        @NotNull Map<N, C> inversion = model.invertNodeSet(nodes);
        for (@NotNull Map.Entry<N, C> entry : inversion.entrySet()) {
            model.addFormula(entry.getKey(), formula, entry.getValue());
        }
    }
}
