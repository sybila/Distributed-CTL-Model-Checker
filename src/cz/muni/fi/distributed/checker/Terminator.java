package cz.muni.fi.distributed.checker;

import cz.muni.fi.ctl.util.Log;
import mpi.Intracomm;
import mpi.MPI;

import static cz.muni.fi.distributed.checker.Tag.TERMINATOR;

/**
 * Listens for task termination and finishes when no tasks remain.
 */
public class Terminator implements Runnable {

    private final int PRIMARY_TERMINATION = -1;
    private final int SECONDARY_TERMINATION = -2;
    private final Intracomm COMM;
    private final VerificationTask task;
    private Thread thread;
    private int activeNodes;

    public Terminator(Intracomm COMM, VerificationTask task) {
        this.COMM = COMM;
        this.task = task;
        this.activeNodes = COMM.Size();
    }

    /**
     * Listens to terminations on other machines and behaves accordingly.
     */
    @Override
    public void run() {
        int[] buffer = new int[2];
        //if dispatcher has incomplete tasks, we will receive a message when such task is completed.
        //if active nodes isn't zero, there are still computers working
        while (task.getDispatcher().hasIncompleteRequests() || activeNodes > 0) {
            COMM.Recv(buffer, 0, buffer.length, MPI.INT, MPI.ANY_SOURCE, TERMINATOR.getTag());
            if (buffer[0] == PRIMARY_TERMINATION) {
                activeNodes--;
                Log.d("Node finished ("+COMM.Rank()+"); Terminated:"+buffer[1]+" Remaining: "+activeNodes+" Incomplete: "+task.getDispatcher().hasIncompleteRequests());
            } else if (buffer[0] == SECONDARY_TERMINATION) {
                task.getDispatcher().onRemoteTaskFinished(buffer[1]);
            }
        }
        Log.d("Terminator finished ("+COMM.Rank()+")");
    }

    public void sendTaskCompletedNotification(final Task task) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int[] buffer = new int[] { SECONDARY_TERMINATION, task.getParentId() };
                COMM.Send(buffer, 0, buffer.length, MPI.INT, task.getSource(), TERMINATOR.getTag());
            }
        }).start();
    }

    /**
     * Notify terminators on other machines that this node won't be sending
     * any more task requests. (Local computation is over)
     */
    public void sendTerminationNotification() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int[] buffer = new int[] { PRIMARY_TERMINATION, COMM.Rank() };
                for (int i=0; i< COMM.Size(); i++) {
                    COMM.Send(buffer, 0, buffer.length, MPI.INT, i, TERMINATOR.getTag());
                }
            }
        }).start();
    }

    /**
     * Start new thread with terminator running.
     */
    public Terminator execute() {
        if (thread != null) {
            throw new IllegalStateException("Can't execute same terminator twice");
        }
        thread = new Thread(this);
        thread.start();
        return this;
    }

    /**
     * Wait for terminator to finish
     */
    public void join() throws InterruptedException {
        thread.join();
    }

}
