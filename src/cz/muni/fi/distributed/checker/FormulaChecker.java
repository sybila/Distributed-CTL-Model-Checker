package cz.muni.fi.distributed.checker;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.util.Log;
import cz.muni.fi.distributed.graph.Graph;
import mpi.Intracomm;
import org.jetbrains.annotations.NotNull;

/**
 * Created by daemontus on 24/11/14.
 */
public class FormulaChecker implements Runnable, VerificationTask {

    private final Formula formula;
    @NotNull
    private final Terminator terminator;
    @NotNull
    private final SecondaryTaskListener secondaryTaskListener;
    private final Graph graph;
    @NotNull
    private final Intracomm COMM;

    public FormulaChecker(Formula formula, Graph graph, @NotNull Intracomm COMM) {
        this.formula = formula;
        this.graph = graph;
        this.COMM = COMM;
        this.terminator = new Terminator(COMM, this);
        this.secondaryTaskListener = new SecondaryTaskListener(COMM, this);
    }
    @Override
    public void run() {
        //executed background demons
        terminator.execute();
        secondaryTaskListener.execute();
        Log.d(COMM.Rank()+" Starting computation: "+formula);
        new FormulaVerificator(graph, secondaryTaskListener, 0).processFormula(formula);
        Log.d(COMM.Rank()+" Computation finished: "+formula);
        terminator.sendTerminationNotification();
        try {
            //wait for all other machines to finish
            terminator.join();
            //kill task listener
            secondaryTaskListener.finishSelf();
            secondaryTaskListener.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void execute() throws InterruptedException {
        @NotNull Thread t = new Thread(this);
        t.start();
        t.join();
    }

    @NotNull
    @Override
    public Terminator getTerminator() {
        return terminator;
    }

    @NotNull
    @Override
    public TaskDispatcher getDispatcher() {
        return secondaryTaskListener;
    }

    @Override
    public Graph getGraph() {
        return graph;
    }

    @Override
    public Formula getFormula() {
        return formula;
    }
}
