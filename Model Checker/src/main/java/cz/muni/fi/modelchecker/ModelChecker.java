package cz.muni.fi.modelchecker;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.proposition.Proposition;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.tasks.TaskMessenger;
import cz.muni.fi.modelchecker.mpi.termination.Terminator;
import cz.muni.fi.modelchecker.verification.FormulaVerificator;
import mpi.MPI;

import java.util.HashSet;
import java.util.Set;

/**
 * Main class representing one fully configured model checker.
 */
@SuppressWarnings("UnusedDeclaration")  //this is a library class
public class ModelChecker<N extends Node, C extends ColorSet> {

    private final Set<Formula> processedFormulas = new HashSet<>();

    private final FormulaVerificator<N, C> verificator;
    private final ModelAdapter<N,C> model;

    public ModelChecker(
            ModelAdapter<N, C> model,
            StateSpacePartitioner<N> partitioner,
            TaskMessenger<N, C> taskMessenger,
            Terminator.TerminatorFactory terminatorFactory) {
        verificator = new FormulaVerificator<>(model, partitioner, taskMessenger, terminatorFactory);
        this.model = model;
    }

    public void verify(Formula formula) {

        //return from proposition or from formula that has been already processed
        if (formula instanceof Proposition || processedFormulas.contains(formula)) return;
        //process formulas recursively
        for (Formula sub : formula.getSubFormulas()) {
            verify(sub);
        }
        System.out.println(MPI.COMM_WORLD.Rank()+" Verification started: "+formula);

        verificator.verifyFormula(formula);

        System.out.println(MPI.COMM_WORLD.Rank()+" Found: "+model.initialNodes(formula).size());
        processedFormulas.add(formula);
    }

}
