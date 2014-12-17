package cz.muni.fi.modelchecker.mpi.termination;

import mpi.Comm;
import mpi.MPI;

/**
 * Terminator running on a machine that has rank 0
 * Uses Safra's algorithm(http://fmt.cs.utwente.nl/courses/cdp/slides/cdp-8-mpi-2-4up.pdf) for termination detection.
 */
public class MasterTerminator implements Terminator {

    private boolean flag = false;
    private int count = 0;

    private boolean working = false;
    private boolean waitingForToken = false;

    private final int tokenSource;
    private final int tokenDestination;
    private final Comm COMM;

    public MasterTerminator(Comm comm) {
        COMM = comm;
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        tokenSource = (rank + 1) % size;
        tokenDestination = size - 1;
        if (rank != 0) {
            throw new IllegalStateException("Cannot launch master terminator on slave machine");
        }
    }

    private synchronized void initProbe() {
        //status = White
        flag = false;
        waitingForToken = true;
        COMM.Isend(new int[] {0,0}, 0, 2, MPI.INT, tokenDestination, -1);
    }

    private synchronized void initTermination() {
        COMM.Isend(new int[] {2,0}, 0, 2, MPI.INT, tokenDestination, -1);
    }

    @Override
    public synchronized void setWorking(boolean working) {
        this.working = working;
        if (!working && !waitingForToken) {
            //if node is idle and no token is already in the system
            initProbe();
        }
    }

    @Override
    public synchronized void messageSent() {
        count++;
    }

    @Override
    public synchronized void messageReceived() {
        count--;
        //status = Black
        flag = true;
    }

    @Override
    public void waitForTermination() {
        Thread t = new Thread(() -> {
            synchronized (MasterTerminator.this) {
                if (!working) initProbe();
            }
            int[] buffer = new int[2];
            while (buffer[0] != 2) {
                COMM.Recv(buffer, 0, buffer.length, MPI.INT, tokenSource, -1);
                synchronized (MasterTerminator.this) {
                    if (buffer[0] < 2) {
                        if (!waitingForToken) {
                            throw new IllegalStateException("Master received a token he was not waiting for!");
                        }
                        waitingForToken = false;
                        if (!flag && buffer[0] == 0 && buffer[1] + count == 0) {
                            //if termination criteria are met, finish this whole thing
                            //Slaves should pass this and return it to us (that will terminate this while loop)
                            initTermination();
                        } else {
                            if (!working) {
                                //if node is idle, just go for another round
                                initProbe();
                            } //if we are working, just wait - setWorking will init the probe
                        }
                    }
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
