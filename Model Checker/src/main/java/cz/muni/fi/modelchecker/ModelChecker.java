package cz.muni.fi.modelchecker;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.proposition.Proposition;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.TaskManager;
import cz.muni.fi.modelchecker.mpi.termination.MPITokenMessenger;
import cz.muni.fi.modelchecker.mpi.termination.MasterTerminator;
import cz.muni.fi.modelchecker.mpi.termination.SlaveTerminator;
import cz.muni.fi.modelchecker.mpi.termination.Terminator;
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
    private final ModelAdapter<N,C> model;

    public ModelChecker(
            ModelAdapter<N, C> model,
            StateSpacePartitioner<N> partitioner,
            TaskManager.TaskManagerFactory<N, C> taskManagerFactory,
            Comm comm) {
        this.taskManagerFactory = taskManagerFactory;
        COMM = comm;
        this.model = model;
        verificator = new FormulaVerificator<>(partitioner.getMyId(), model, partitioner);
    }

    public void verify(Formula formula) {

        //return from proposition or from formula that has been already processed
        if (formula instanceof Proposition || processedFormulas.contains(formula)) return;
        //process formulas recursively
        for (Formula sub : formula.getSubFormulas()) {
            verify(sub);
        }
        System.out.println(MPI.COMM_WORLD.Rank()+" Verification started: "+formula);
        //prepare terminator
        Terminator terminator = MPI.COMM_WORLD.Rank() == 0 ? new MasterTerminator(new MPITokenMessenger(COMM)) : new SlaveTerminator(new MPITokenMessenger(COMM));
        //prepare communication
        TaskManager<N,C> manager = taskManagerFactory.createTaskManager(formula, terminator, verificator, COMM);
        verificator.setTaskManager(manager);
        manager.startListening();
        //process task
        verificator.processFormula(formula);
        //wait for task termination
        terminator.waitForTermination();
        //wait for task manager termination
        //Note: we need the terminator, because we need need a guarantee that
        //new formula will be processed only after old task managers are finished
        terminator = MPI.COMM_WORLD.Rank() == 0 ? new MasterTerminator(new MPITokenMessenger(COMM)) : new SlaveTerminator(new MPITokenMessenger(COMM));
        manager.finishSelf();
        terminator.waitForTermination();
        System.out.println(COMM.Rank()+" Found: "+model.initialNodes(formula).size());
        processedFormulas.add(formula);
    }

}
