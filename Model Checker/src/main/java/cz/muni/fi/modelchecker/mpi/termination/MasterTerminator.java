package cz.muni.fi.modelchecker.mpi.termination;

/**
 * Terminator running on a machine that has rank 0
 * Uses Safra's algorithm(http://fmt.cs.utwente.nl/courses/cdp/slides/cdp-8-mpi-2-4up.pdf) for termination detection.
 */
public class MasterTerminator implements Terminator {

    private boolean flag = false;
    private int count = 0;

    private boolean working = false;
    private boolean waitingForToken = false;
    private boolean finalized = false;

    private final int tokenSource;
    private final int tokenDestination;
    private final TokenMessenger messenger;

    public MasterTerminator(TokenMessenger messenger) {
        if (messenger.getMyId() != 0) {
            throw new IllegalStateException("Cannot launch master terminator on slave machine");
        }
        this.messenger = messenger;
        int size = messenger.getProcessCount();
        tokenSource = (messenger.getMyId() + 1) % size;
        tokenDestination = size - 1;
    }

    private synchronized void initProbe() {
        //status = White
      //  System.out.println("Init Probe");
        flag = false;
        waitingForToken = true;
        messenger.sendTokenAsync(tokenDestination, new Token(0,0));
    }

    private synchronized void initTermination() {
        messenger.sendTokenAsync(tokenDestination, new Token(2,0));
    }

    @Override
    public synchronized void setWorking(boolean working) {
        if (finalized) throw new IllegalStateException("Called setWorking on finalized master terminator");
        this.working = working;
        if (!working && !waitingForToken) {
            //if node is idle and no token is already in the system
            //Note: It is ok to send a probe even when main task has not finished yet, because
            //probe will return only after all main tasks are finished and if any messages
            //are sent/received during this period, flag/count will reflect this and process won't terminate
            initProbe();
        }
    }

    @Override
    public synchronized void messageSent() {
        if (finalized) throw new IllegalStateException("Called messageSent on finalized master terminator");
        count++;
    }

    @Override
    public synchronized void messageReceived() {
        if (finalized) throw new IllegalStateException("Called messageReceived on finalized master terminator");
        count--;
        //status = Black
        flag = true;
    }

    @Override
    public void waitForTermination() {
        if (finalized) throw new IllegalStateException("Called waitForTermination on finalized master terminator");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (MasterTerminator.this) {
                    if (!working && !waitingForToken) initProbe();
                }
                Token token = messenger.waitForToken(tokenSource);
                while (token.flag < 2) {
                    synchronized (MasterTerminator.this) {
                        if (!waitingForToken) {
                            throw new IllegalStateException("Master received a token he was not waiting for!");
                        }
                        //   System.out.println("Probe returned to master: "+flag+" "+token.flag+" "+count+" "+token.count);
                        waitingForToken = false;
                        if (!flag && token.flag == 0 && token.count + count == 0) {
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
                    token = messenger.waitForToken(tokenSource);
                }
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
}
