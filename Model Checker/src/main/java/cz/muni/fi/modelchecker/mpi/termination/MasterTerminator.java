package cz.muni.fi.modelchecker.mpi.termination;

/**
 * Terminator running on a machine that has rank 0
 * Uses Safra's algorithm(http://fmt.cs.utwente.nl/courses/cdp/slides/cdp-8-mpi-2-4up.pdf) for termination detection.
 */
class MasterTerminator extends Terminator {

    private boolean waitingForToken = false;

    MasterTerminator(TokenMessenger m) {
        super(m, (m.getMyId() + 1) % m.getProcessCount(), m.getProcessCount() - 1);
        if (messenger.getMyId() != 0) {
            throw new IllegalStateException("Cannot launch master terminator on slave machine.");
        }
    }

    private void initProbe() {
        //status = White
        flag = 0;
        waitingForToken = true;
        messenger.sendTokenAsync(tokenDestination, new Token(0,0));
    }

    @Override
    public synchronized void setDone() {
        super.setDone();
        if (!waitingForToken) { //at this point, working is false
            //if node is idle and no token is already in the system
            //Note: It is ok to send a probe even when main task has not finished yet, because
            //probe will return only after all main tasks are finished and if any messages
            //are sent/received during this period, flag/count will reflect this and process won't terminate
            initProbe();
        }
    }

    @Override
    public void terminationLoop() {
        /*synchronized (this) {
            //if we are not working and no token is currently circulating in the system,
            //we initialize the probe. - This happens when no messages were received.
            if (!working && !waitingForToken) initProbe();
        }*/
        //wait for token to go through all other processes.
        Token token = messenger.waitForToken(tokenSource);
        while (token.flag < 2) {    //while this is not a termination token
            synchronized (this) {
                if (!waitingForToken) { //if we are not expecting a token, something is really wrong
                    throw new IllegalStateException("Master received a token he was not waiting for!");
                }
                waitingForToken = false;
                if (flag == 0 && token.flag == 0 && token.count + count == 0) {
                    //if termination criteria are met, finish this whole thing
                    //Slaves should pass this and return it to us (that will terminate this while loop)
                    messenger.sendTokenAsync(tokenDestination, new Token(2, 0));
                } else {
                    if (!working) {
                        //if node is idle, just go for another round
                        initProbe();
                    } //if we are working, just wait - setDone will init the probe
                }
            }
            //wait for another token
            token = messenger.waitForToken(tokenSource);
        }
    }
}
