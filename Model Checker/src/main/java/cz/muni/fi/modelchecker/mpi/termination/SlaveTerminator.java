package cz.muni.fi.modelchecker.mpi.termination;

/**
 * Terminator running on a machine that has rank different than 0.
 * Uses Safra's algorithm(http://fmt.cs.utwente.nl/courses/cdp/slides/cdp-8-mpi-2-4up.pdf) for termination detection.
 */
public class SlaveTerminator implements Terminator {

    private int flag = 0;
    private int count = 0;

    private boolean working = false;
    private Token pendingToken = null;

    private final int tokenSource;
    private final int tokenDestination;
    private final TokenMessenger messenger;

    public SlaveTerminator(TokenMessenger messenger) {
        this.messenger = messenger;
        int rank = messenger.getMyId();
        tokenSource = (rank + 1) % messenger.getProcessCount();
        tokenDestination = rank - 1;
        if (tokenDestination < 0) {
            throw new IllegalStateException("Cannot launch slave terminator on master machine");
        }
    }

    private synchronized void processToken(Token token) {
        //non blocking send (so that we do not end up stuck in synchronized section)
        messenger.sendTokenAsync(tokenDestination, new Token(Math.min(token.flag + flag, 1), token.count + count));
        //status = White
        flag = 0;
    }

    @Override
    public synchronized void messageSent() {
        count++;
    }

    @Override
    public synchronized void messageReceived() {
        count--;
        //status = Black
        flag = 1;
    }

    @Override
    public void waitForTermination() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Token token = messenger.waitForToken(tokenSource);
                while (token.flag < 2) {
                    synchronized (SlaveTerminator.this) {
                        //   System.out.println(messenger.getMyId()+" probe received: "+flag+" "+token.flag+" "+count+" "+token.count);
                        if (working) {  //if node is active, save token for later
                            pendingToken = token;
                        } else { //else pass token to next node right away
                            processToken(token);
                        }
                    }
                    token = messenger.waitForToken(tokenSource);
                }
                //termination message has been received - we pass this to next node and finish ourselves
                messenger.sendTokenAsync(tokenDestination, token);
            }
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
