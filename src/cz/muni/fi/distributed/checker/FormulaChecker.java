package cz.muni.fi.distributed.checker;

import cz.muni.fi.ctl.formula.Formula;
import cz.muni.fi.ctl.util.Log;
import cz.muni.fi.distributed.graph.Graph;
import mpi.Intracomm;

/**
 * Created by daemontus on 24/11/14.
 */
public class FormulaChecker implements Runnable, VerificationTask {

    private final Formula formula;
    private final Terminator terminator;
    private final SecondaryTaskListener secondaryTaskListener;
    private final Graph graph;
    private final Intracomm COMM;

    public FormulaChecker(Formula formula, Graph graph, Intracomm COMM) {
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
        Thread t = new Thread(this);
        t.start();
        t.join();
    }

    @Override
    public Terminator getTerminator() {
        return terminator;
    }

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
