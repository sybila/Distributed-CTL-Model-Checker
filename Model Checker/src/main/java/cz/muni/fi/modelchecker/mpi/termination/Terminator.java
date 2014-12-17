package cz.muni.fi.modelchecker.mpi.termination;

/**
 *
 */
public interface Terminator {

    void setWorking(boolean working);
    void messageSent();
    void messageReceived();

    void waitForTermination();

}
