package cz.muni.fi.modelchecker;

import com.github.daemontus.jafra.Terminator;
import cz.muni.fi.ctl.Atom;
import cz.muni.fi.ctl.Formula;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.tasks.TaskMessenger;
import cz.muni.fi.modelchecker.verification.FormulaVerificator;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Main class representing one fully configured model checker.
 */
@SuppressWarnings("UnusedDeclaration")  //this is a library class
public class ModelCheckerOld<N extends Node, C extends ColorSet> {

    @NotNull
    private final Set<Formula> processedFormulas = new HashSet<>();

    @NotNull
    private final FormulaVerificator<N, C> verificator;
    @NotNull
    private final ModelAdapter<N,C> model;
    @NotNull
    private final StateSpacePartitioner<N> partitioner;

    /**
     * Create new model checker with given properties.
     * @param model Providing node info and storage. Not null.
     * @param partitioner Divides the graph into separate processes.
     * @param taskMessenger Provides communication channels between processes.
     * @param terminatorFactory Creates new pre-configured terminators.
     */
    public ModelCheckerOld(
            @NotNull ModelAdapter<N, C> model,
            @NotNull StateSpacePartitioner<N> partitioner,
            @NotNull TaskMessenger<N, C> taskMessenger,
            @NotNull Terminator.Factory terminatorFactory) {
        verificator = new FormulaVerificator<>(model, partitioner, taskMessenger, terminatorFactory);
        this.model = model;
        this.partitioner = partitioner;
    }

    /**
     * Verify given formula recursively over available model and previously computed data.
     * @param formula Formula that should be verified. Not null.
     */
    public void verify(@NotNull Formula formula) {

        //return from proposition or from formula that has been already processed
        if (formula instanceof Atom || processedFormulas.contains(formula)) return;
        //process formulas recursively
        for (@NotNull Formula sub : formula.getSubFormulas()) {
            verify(sub);
        }
        System.out.println(partitioner.getMyId()+" Verification started: "+formula);

        verificator.verifyFormula(formula);

        System.out.println(partitioner.getMyId()+ " " + formula.getOperator() + " Found Nodes: "+model.initialNodes(formula).size());

       /* for (@NotNull Formula sub : formula.getSubFormulas()) {
            model.purge(sub);
        }*/

        processedFormulas.add(formula);
    }

}
