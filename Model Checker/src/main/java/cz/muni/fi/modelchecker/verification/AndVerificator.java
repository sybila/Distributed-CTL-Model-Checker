package cz.muni.fi.modelchecker.verification;

import com.github.daemontus.jafra.Terminator;
import cz.muni.fi.ctl.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Verificator for and operator.
 */
class AndVerificator<N extends Node, C extends ColorSet> extends StaticProcessor<N, C> {

    AndVerificator(@NotNull ModelAdapter<N, C> model, @NotNull Formula formula, @NotNull Terminator terminator) {
        super(model, formula, terminator);
    }

    @Override
    protected void processModel() {
        @NotNull Map<N, C> first = model.initialNodes(formula.get(0));
        @NotNull Map<N, C> second = model.initialNodes(formula.get(1));
        //intersect node sets
        for (@NotNull Map.Entry<N, C> entry : first.entrySet()) {
            C colorSet = second.get(entry.getKey());
            if (colorSet != null) {
                colorSet.intersect(entry.getValue());
                model.addFormula(entry.getKey(), formula, colorSet);
            }
        }
    }
}
