package cz.muni.fi.modelchecker.verification;

import com.github.daemontus.jafra.Terminator;
import cz.muni.fi.ctl.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import org.jetbrains.annotations.NotNull;

/**
 * Provides common functionality for formula processors that don't need to communicate during their computation
 */
abstract class StaticProcessor<N extends Node, C extends ColorSet> implements FormulaProcessor {

    @NotNull
    private final Terminator terminator;

    @NotNull
    final ModelAdapter<N, C> model;

    @NotNull
    final Formula formula;

    StaticProcessor(@NotNull ModelAdapter<N, C> model, @NotNull Formula formula, @NotNull Terminator terminator) {
        this.terminator = terminator;
        this.model = model;
        this.formula = formula;
    }

    @Override
    public final void verify() {
        processModel();
        //ensures global synchronization - i.e. all processes are done with this formula before moving over
        terminator.setDone();
        terminator.waitForTermination();
    }

    /** Actually compute things */
    protected abstract void processModel();
}
