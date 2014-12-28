package cz.muni.fi.modelchecker.mpi.termination;

import org.jetbrains.annotations.NotNull;

/**
 * Class used to distribute tokens between processes.
 */
public interface TokenMessenger {

    int getProcessCount();
    int getMyId();
    void sendTokenAsync(int destination, @NotNull Token token);
    @NotNull Token waitForToken(int source);

}
