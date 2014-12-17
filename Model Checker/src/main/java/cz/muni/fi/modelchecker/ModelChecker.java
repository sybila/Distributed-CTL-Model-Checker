package cz.muni.fi.modelchecker;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.proposition.Proposition;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.TaskManager;
import cz.muni.fi.modelchecker.mpi.termination.*;
import mpi.Comm;
import mpi.MPI;

import java.util.HashSet;
import java.util.Set;

/**
 * Main class representing one fully configured model checker.
 */
@SuppressWarnings("UnusedDeclaration")  //this is a library class
public class ModelChecker<N extends Node, C extends ColorSet> {

    private final Set<Formula> processedFormulas = new HashSet<>();

    private final TaskManager.TaskManagerFactory<N, C> taskManagerFactory;
    private final FormulaVerificator<N, C> verificator;
    private final Comm COMM;

    public ModelChecker(
            ModelAdapter<N, C> model,
            StateSpacePartitioner<N> partitioner,
            TaskManager.TaskManagerFactory<N, C> taskManagerFactory,
            Comm comm) {
        this.taskManagerFactory = taskManagerFactory;
        COMM = comm;
        verificator = new FormulaVerificator<>(MPI.COMM_WORLD.Rank(), model, partitioner);
    }

    public void verify(Formula formula) {

        //return from proposition or from formula that has been already processed
        if (formula instanceof Proposition || processedFormulas.contains(formula)) return;
        //process formulas recursively
        formula.getSubFormulas().forEach(this::verify);
        System.out.println("Verification started: "+formula);
        //prepare terminator
        Terminator terminator = MPI.COMM_WORLD.Rank() == 0 ? new MasterTerminator(COMM) : new SlaveTerminator(COMM);
        //prepare communication
        TaskManager<N,C> manager = taskManagerFactory.createTaskManager(formula, terminator, verificator, COMM);
        verificator.setTaskManager(manager);
        manager.startListening();
        //process task
        verificator.processFormula(formula);
        //wait for termination
        terminator.waitForTermination();
        manager.finishSelf();
        processedFormulas.add(formula);
    }

}
