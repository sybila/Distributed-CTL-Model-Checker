package cz.muni.fi.modelchecker.verification;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.modelchecker.ModelAdapter;
import cz.muni.fi.modelchecker.StateSpacePartitioner;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.TaskManager;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract formula verificator - a blueprint for verification of various operators
 */
public abstract class FormulaVerificator<N extends Node, C extends ColorSet> {

    protected final StateSpacePartitioner<N> partitioner;
    protected final ModelAdapter<N, C> model;
    protected TaskManager<N, C> taskManager;
    protected final Formula formula;
    protected final int myId;

    FormulaVerificator(int myId, @NotNull ModelAdapter<N, C> model, @NotNull StateSpacePartitioner<N> partitioner, Formula formula) {
        this.model = model;
        this.partitioner = partitioner;
        this.myId = myId;
        this.formula = formula;
    }

    public void prepareVerification(TaskManager<N, C> taskManager) {
        this.taskManager = taskManager;
    }

    /**
     * Process formula without any extra data.
     * (this is usually executed from root task on each network node)
     */
    public abstract void verifyLocalGraph();

    /**
     * Process formula, but start from given node and parameters.
     * (usually executed by secondary task requested from another node)
     */
    public abstract void processTaskData(@NotNull N internal, @NotNull N external, @NotNull C candidates);

}
