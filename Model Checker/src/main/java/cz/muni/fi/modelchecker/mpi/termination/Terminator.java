package cz.muni.fi.modelchecker.mpi.termination;

import org.jetbrains.annotations.NotNull;

/**
 * Class that covers common functionality of slave and master terminators.
 * Note: All terminators are initially marked as working. So even if no
 * messages are received and only local work has been performed, you should call setDone at least once.
 */
public abstract class Terminator {

    protected int flag = 0;
    protected int count = 0;

    protected boolean finalized = false;
    protected boolean working = true;

    protected final int tokenSource;
    protected final int tokenDestination;

    @NotNull
    protected final TokenMessenger messenger;

    /**
     * Create a new terminator based on given token passing interface.
     */
    public static class TerminatorFactory {

        @NotNull
        private final TokenMessenger messenger;

        public TerminatorFactory(@NotNull TokenMessenger messenger) {
            this.messenger = messenger;
        }

        @NotNull
        public Terminator createNew() {
            if (messenger.getMyId() == 0) {
                return new MasterTerminator(messenger);
            } else {
                return new SlaveTerminator(messenger);
            }
        }
    }

    /** Package private constructor - new terminators should be created using static factory. */
    Terminator(@NotNull TokenMessenger messenger, int tokenSource, int tokenDestination) {
        this.messenger = messenger;
        this.tokenDestination = tokenDestination;
        this.tokenSource = tokenSource;
    }

    /**
     * Tell terminator that local work is done and he can resume operations. (i.e. after message has been received)
     */
    public synchronized void setDone() {
        if (finalized) throw new IllegalStateException("Called setDone on finalized master terminator");
        this.working = false;
    }

    /**
     * Indicate that message has been sent from this process.
     * @throws IllegalStateException Thrown when idle terminator is indicated that message has been sent.
     * This should not happen, since only local work should be the source of messages.
     */
    public synchronized void messageSent() throws IllegalStateException {
        if (finalized) throw new IllegalStateException("Called messageSent on finalized master terminator");
        if (!working) throw new IllegalStateException("Terminator that is not working is sending messages. This is suspicious.");
        count++;
    }

    /**
     * Indicate that message has been received and this process is currently processing it.
     * WARNING: In order for terminator to properly finish, any sequence of
     * messageReceived calls must end with at least one setDone call.
     */
    public synchronized void messageReceived() {
        if (finalized) throw new IllegalStateException("Called messageReceived on finalized master terminator");
        count--;
        //status = Black
        flag = 1;
        working = true;
    }

    /**
     * Start termination detection process and wait for successful termination.
     * Terminator can receive/send messages even before this call and will keep them as internal info about process,
     * but only after this call will it start to exchange info with other processes.
     */
    public void waitForTermination() {
        if (finalized) throw new IllegalStateException("Called waitForTermination on finalized master terminator");
        terminationLoop();
        finalized = true;
    }

    /**
     * This method actually detects the termination by exchanging info with other processes until fix point is reached.
     */
    protected abstract void terminationLoop();

}
