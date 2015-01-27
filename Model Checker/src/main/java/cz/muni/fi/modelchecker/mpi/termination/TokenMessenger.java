package cz.muni.fi.modelchecker.mpi.termination;

import org.jetbrains.annotations.NotNull;

/**
 * Class used to distribute tokens between processes.
 */
public interface TokenMessenger {

    /**
     * @return Total number of processes participating in termination detection.
     */
    int getProcessCount();

    /**
     * WARNING: Process ids must be a series of consecutive numbers starting from 0. (i.e. 0,1,2,3,4...)
     * @return Id of process managing this messenger.
     */
    int getMyId();

    /**
     * Send a token to a process asynchronously.
     * @param destination Id of the destination process.
     * @param token Token that should be sent to given destination.
     */
    void sendTokenAsync(int destination, @NotNull Token token);

    /**
     * Block until a token is received from a source.
     * @param source Id of the source process.
     * @return Token received from the process.
     */
    @NotNull Token waitForToken(int source);

}
