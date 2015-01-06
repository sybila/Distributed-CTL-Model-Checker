package cz.muni.fi.modelchecker.mpi.termination;

/**
 * Class that covers common functionality of slave and master terminators.
 */
public abstract class Terminator {

    protected int flag = 0;
    protected int count = 0;

    protected boolean finalized = false;
    protected boolean working = false;

    protected final int tokenSource;
    protected final int tokenDestination;

    protected final TokenMessenger messenger;

    public Terminator(TokenMessenger messenger, int tokenSource, int tokenDestination) {
        this.messenger = messenger;
        this.tokenDestination = tokenDestination;
        this.tokenSource = tokenSource;
    }

    public synchronized void setWorking(boolean working) {
        if (finalized) throw new IllegalStateException("Called setWorking on finalized master terminator");
        this.working = working;
    }

    public synchronized void messageSent() {
        if (finalized) throw new IllegalStateException("Called messageSent on finalized master terminator");
        count++;
    }

    public synchronized void messageReceived() {
        if (finalized) throw new IllegalStateException("Called messageReceived on finalized master terminator");
        count--;
        //status = Black
        flag = 1;
    }

    public void waitForTermination() {
        if (finalized) throw new IllegalStateException("Called waitForTermination on finalized master terminator");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                terminationLoop();
            }
        });
        t.start();
        try {
            t.join();
            finalized = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected abstract void terminationLoop();

}
