package cz.muni.fi.modelchecker.mpi.termination;

/**
 * Terminator running on a machine that has rank different than 0.
 * Uses Safra's algorithm(http://fmt.cs.utwente.nl/courses/cdp/slides/cdp-8-mpi-2-4up.pdf) for termination detection.
 */
class SlaveTerminator extends Terminator {

    private Token pendingToken = null;

    SlaveTerminator(TokenMessenger m) {
        super(m, (m.getMyId() + 1) % m.getProcessCount(), m.getMyId() - 1);
        if (tokenDestination < 0) {
            throw new IllegalStateException("Cannot launch slave terminator on master machine");
        }
    }

    private void processToken(Token token) {
        //non blocking send (so that we do not end up stuck in synchronized section)
        messenger.sendTokenAsync(tokenDestination, new Token(Math.min(token.flag + flag, 1), token.count + count));
        //status = White
        flag = 0;
    }

    @Override
    public void terminationLoop() {
        //wait for token from master
        Token token = messenger.waitForToken(tokenSource);
        while (token.flag < 2) {    //while this is not terminating token
            synchronized (SlaveTerminator.this) {
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

    @Override
    public synchronized void setDone() {
        super.setDone();
        if (pendingToken != null) { //working is false here
            //node is idle and has unprocessed tokens
            processToken(pendingToken);
            pendingToken = null;
        }
    }

}
