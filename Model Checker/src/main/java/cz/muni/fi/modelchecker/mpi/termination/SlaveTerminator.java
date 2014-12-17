package cz.muni.fi.modelchecker.mpi.termination;

import mpi.Comm;
import mpi.MPI;

import java.util.Arrays;

/**
 * Terminator running on a machine that has rank different than 0.
 * Uses Safra's algorithm(http://fmt.cs.utwente.nl/courses/cdp/slides/cdp-8-mpi-2-4up.pdf) for termination detection.
 */
public class SlaveTerminator implements Terminator {

    private boolean flag = false;
    private int count = 0;

    private boolean working = true;
    private int[] pendingToken = null;

    private final int tokenSource;
    private final int tokenDestination;
    private final Comm COMM;

    public SlaveTerminator(Comm comm) {
        COMM = comm;
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        tokenSource = (rank + 1) % size;
        tokenDestination = rank - 1;
        if (tokenDestination < 0){
            throw new IllegalStateException("Cannot launch slave terminator on master machine");
        }
    }

    private synchronized void processToken(int[] token) {
        //update token and send it to next node
        token[0] = (token[0] == 1 || flag) ? 1 : 0;
        token[1] = token[1] + count;
        //non blocking send (so that we do not end up stuck in synchronized section)
        COMM.Isend(token, 0, token.length, MPI.INT, tokenDestination, -1);
        //status = White
        flag = false;
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
            int[] buffer = new int[2];
            while (buffer[0] != 2) {
                COMM.Recv(buffer, 0, buffer.length, MPI.INT, tokenSource, -1);
                if (buffer[0] < 2) {    //if this is a valid token
                    synchronized (SlaveTerminator.this) {
                        if (working) {  //if node is active, save token for later
                            pendingToken = Arrays.copyOf(buffer, 2);
                        } else { //else pass token to next node right away
                            processToken(Arrays.copyOf(buffer,2));
                        }
                    }
                }
            }
            //termination message has been received - we pass this to next node and finish ourselves
            COMM.Isend(buffer, 0, 2, MPI.INT, tokenDestination, -1);
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void setWorking(boolean working) {
        this.working = working;
        if (!working && pendingToken != null) {
            //node is idle and has unprocessed tokens
            processToken(pendingToken);
            pendingToken = null;
        }
    }

}
