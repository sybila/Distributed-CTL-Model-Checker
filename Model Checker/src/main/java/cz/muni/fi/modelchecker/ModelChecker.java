package cz.muni.fi.modelchecker;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.formula.proposition.Proposition;
import cz.muni.fi.modelchecker.graph.ColorSet;
import cz.muni.fi.modelchecker.graph.Node;
import cz.muni.fi.modelchecker.mpi.TaskManager;
import mpi.Intracomm;
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
    private final Intracomm COMM;
    private final ModelAdapter<N,C> model;

    public ModelChecker(
            ModelAdapter<N, C> model,
            StateSpacePartitioner<N> partitioner,
            TaskManager.TaskManagerFactory<N, C> taskManagerFactory,
            Intracomm comm) {
        this.taskManagerFactory = taskManagerFactory;
        COMM = comm;
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
        //prepare terminator
        //Terminator terminator = Terminator.obtain(new MPITokenMessenger(COMM));
        //prepare communication and verificator
        TaskManager<N,C> manager = taskManagerFactory.createTaskManager(formula);
        manager.startProcessing();
        //process task
        manager.finishSelf();
       // terminator.setWorking(true);
        //wait for task termination
        //terminator.waitForTermination();
        //wait for task manager termination
        //Note: we need the terminator, because we need need a guarantee that
        //new formula will be processed only after old task managers are finished
        /*terminator = Terminator.obtain(new MPITokenMessenger(COMM));
        manager.finishSelf();
        verificator.finishSelf();
        terminator.waitForTermination();*/
        System.out.println(COMM.Rank()+" Found: "+model.initialNodes(formula).size());
        processedFormulas.add(formula);
        COMM.Barrier();
    }

}
