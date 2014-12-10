package cz.muni.fi.distributed.checker;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.distributed.graph.Graph;
import org.jetbrains.annotations.NotNull;

/**
 * Created by daemontus on 24/11/14.
 */
public interface VerificationTask {
    @NotNull
    Terminator getTerminator();
    @NotNull
    TaskDispatcher getDispatcher();
    Graph getGraph();
    Formula getFormula();
}
